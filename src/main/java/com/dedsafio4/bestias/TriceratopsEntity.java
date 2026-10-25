package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Triceratops (el del mod Fossils and Archeology): no pega con la boca ni rompe armadura. Cada 20 segundos, si hay un jugador
 * cerca, carga la embestida (2 segundos quieto, mirándolo) y sale corriendo en línea recta hacia donde estaba,
 * rompiendo todos los bloques que tenga adelante (menos los irrompibles). Al jugador que choca lo manda lejos y le
 * saca 5 corazones. La Puerta de Hueso lo frena: aguanta 2 embestidas y a la tercera se cae. /triceratops escala <tamaño> cambia el tamaño de los Triceratops (1 es el normal).
 */
public class TriceratopsEntity extends Monster implements GeoEntity {
	/** 0: normal, 1: cargando la embestida, 2: embistiendo (el cliente lo usa para la animación). */
	private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(TriceratopsEntity.class, EntityDataSerializers.INT);
	private static final int NORMAL = 0, CARGANDO = 1, EMBISTIENDO = 2;

	/** Cada cuánto embiste (20 segundos), cuánto tarda en cargar (2 segundos) y cuánto dura la corrida como mucho. */
	private static final int CADA = 20 * 20, CARGA = 40, CORRIDA = 50;
	/** A qué distancia busca jugadores para embestir, y qué tan rápido corre (bloques por tick). */
	private static final double ALCANCE = 24, VELOCIDAD = 0.65;
	/** Lo que saca la embestida (10 = 5 corazones). */
	private static final float DANIO_EMBESTIDA = 10f;

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);

	private int espera = CADA, tiempo;
	private Player objetivo;
	private Vec3 direccion = Vec3.ZERO;

	public TriceratopsEntity(EntityType<? extends TriceratopsEntity> tipo, Level level) {
		super(tipo, level);
	}

	/**
	 * /triceratops escala <tamaño> [triceratops]: cambia el tamaño (y la caja de choque) del Triceratops más cercano
	 * (a 16 bloques), o de los que se elijan. 1 es el tamaño normal; de 0,1 a 10.
	 */
	public static void registrarComandos(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
		dispatcher.register(net.minecraft.commands.Commands.literal("triceratops").requires(s -> s.hasPermission(2))
				.then(net.minecraft.commands.Commands.literal("escala")
						.then(net.minecraft.commands.Commands.argument("tamaño", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.1f, 10f))
								.executes(c -> {
									var fuente = c.getSource();
									var cerca = fuente.getLevel().getEntitiesOfClass(TriceratopsEntity.class,
											new AABB(fuente.getPosition(), fuente.getPosition()).inflate(16));
									cerca.sort(java.util.Comparator.comparingDouble(t -> t.distanceToSqr(fuente.getPosition())));
									return escalar(c, cerca.isEmpty() ? java.util.List.of() : java.util.List.of(cerca.get(0)));
								})
								.then(net.minecraft.commands.Commands.argument("triceratops", net.minecraft.commands.arguments.EntityArgument.entities())
										.executes(c -> escalar(c, net.minecraft.commands.arguments.EntityArgument.getEntities(c, "triceratops")))))));
	}

	private static int escalar(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c,
							   java.util.Collection<? extends net.minecraft.world.entity.Entity> entidades) {
		float tamaño = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c, "tamaño");
		int n = 0;
		for (var e : entidades) {
			if (!(e instanceof TriceratopsEntity t)) continue;
			var atributo = t.getAttribute(Attributes.SCALE);
			if (atributo == null) continue;
			atributo.setBaseValue(tamaño);
			n++;
		}
		if (n == 0) {
			c.getSource().sendFailure(net.minecraft.network.chat.Component.literal("No hay ningún Triceratops cerca (a 16 bloques)."));
			return 0;
		}
		int total = n;
		c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
				(total == 1 ? "El Triceratops ahora tiene tamaño " : total + " Triceratops ahora tienen tamaño ") + tamaño + "."), true);
		return n;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 80.0)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.MOVEMENT_SPEED, 0.22)
				.add(Attributes.STEP_HEIGHT, 1.5)
				.add(Attributes.FOLLOW_RANGE, ALCANCE);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder constructor) {
		super.defineSynchedData(constructor);
		constructor.define(ESTADO, NORMAL);
	}

	private int estado() {
		return this.entityData.get(ESTADO);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	/** No pega: lo único que hace es embestir. */
	@Override
	public boolean doHurtTarget(net.minecraft.world.entity.Entity objetivo) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) return;
		switch (estado()) {
			case CARGANDO -> cargar();
			case EMBISTIENDO -> embestir();
			default -> {
				if (--espera > 0) return;
				objetivo = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), ALCANCE,
						p -> p instanceof Player j && !j.isSpectator() && !j.isCreative() && j.isAlive());
				if (objetivo == null) {
					espera = 20;   // nadie cerca: vuelve a mirar en 1 segundo
					return;
				}
				tiempo = 0;
				this.entityData.set(ESTADO, CARGANDO);
				this.playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.7f);
			}
		}
	}

	/** Quieto, mirando al jugador y raspando el piso. */
	private void cargar() {
		this.getNavigation().stop();
		if (objetivo == null || !objetivo.isAlive()) {
			terminar();
			return;
		}
		this.getLookControl().setLookAt(objetivo, 30f, 30f);
		mirarHacia(objetivo.position().subtract(this.position()));
		if (tiempo % 10 == 0) this.playSound(SoundEvents.RAVAGER_STEP, 1.5f, 0.6f);
		if (++tiempo < CARGA) return;
		Vec3 d = objetivo.position().subtract(this.position()).multiply(1, 0, 1);
		direccion = d.lengthSqr() < 1.0E-4 ? this.getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
		tiempo = 0;
		this.entityData.set(ESTADO, EMBISTIENDO);
	}

	/** Corre en línea recta rompiendo lo que tenga adelante; al jugador que choca lo manda lejos. */
	private void embestir() {
		this.getNavigation().stop();
		mirarHacia(direccion);
		this.setDeltaMovement(direccion.x * VELOCIDAD, this.getDeltaMovement().y, direccion.z * VELOCIDAD);
		this.hurtMarked = true;
		// Una Puerta de Hueso cerrada adelante: la golpea y se frena (a la tercera embestida, la puerta se cae).
		AABB frente = this.getBoundingBox().move(direccion.scale(VELOCIDAD + 0.3));
		for (var puerta : this.level().getEntitiesOfClass(com.dedsafio4.puertas.PuertaHuesoEntity.class, frente)) {
			if (!puerta.embestida(direccion)) continue;
			this.setDeltaMovement(direccion.x * -0.3, 0.2, direccion.z * -0.3);
			this.playSound(SoundEvents.RAVAGER_ATTACK, 2f, 0.7f);
			terminar();
			return;
		}
		romperAdelante();
		for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.5))) {
			if (p.isSpectator() || p.isCreative()) continue;
			p.hurt(this.damageSources().mobAttack(this), DANIO_EMBESTIDA);
			p.setDeltaMovement(direccion.x * 2.5, 0.8, direccion.z * 2.5);
			p.hurtMarked = true;
			this.playSound(SoundEvents.RAVAGER_ATTACK, 2f, 0.8f);
			terminar();
			return;
		}
		if (++tiempo >= CORRIDA) terminar();
	}

	/** Rompe los bloques que están justo delante del cuerpo (los irrompibles, como la roca madre, no). */
	private void romperAdelante() {
		if (!(this.level() instanceof ServerLevel mundo)) return;
		AABB caja = this.getBoundingBox().move(direccion.scale(VELOCIDAD + 0.3)).deflate(0.05, 0, 0.05);
		boolean rompio = false;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(caja.minX, caja.minY + 0.5, caja.minZ),
				BlockPos.containing(caja.maxX, caja.maxY, caja.maxZ))) {
			BlockState b = mundo.getBlockState(pos);
			if (b.isAir() || b.getDestroySpeed(mundo, pos) < 0 || !b.getFluidState().isEmpty() && b.getCollisionShape(mundo, pos).isEmpty()) continue;
			rompio |= mundo.destroyBlock(pos, true, this);
		}
		if (rompio) this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.4f);
	}

	private void mirarHacia(Vec3 d) {
		if (d.lengthSqr() < 1.0E-4) return;
		float yaw = (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90f;
		this.setYRot(yaw);
		this.yBodyRot = yaw;
		this.yHeadRot = yaw;
	}

	/** Para las pruebas: embiste ya (sin esperar los 20 segundos). */
	public void embestirYa() {
		espera = 1;
	}

	public int estadoActual() {
		return estado();
	}

	private void terminar() {
		objetivo = null;
		tiempo = 0;
		espera = CADA;
		this.entityData.set(ESTADO, NORMAL);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "movimiento", 4, estado -> switch (estado()) {
			case CARGANDO -> estado.setAndContinue(RawAnimation.begin().thenLoop("animation.triceratops.ram_windup"));
			case EMBISTIENDO -> estado.setAndContinue(RawAnimation.begin().thenLoop("animation.triceratops.ram"));
			default -> estado.isMoving()
					? estado.setAndContinue(RawAnimation.begin().thenLoop("animation.triceratops.walk"))
					: estado.setAndContinue(RawAnimation.begin().thenLoop("animation.triceratops.idle"));
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.RAVAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RAVAGER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.8f;
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}
}
