package com.dedsafio4.nave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Nave: vuela, persigue a los jugadores quedándose a unos 8 bloques y un poco más arriba,
 * y les dispara un rayo celeste cada 1,5 segundos (1 de daño).
 * Al morir explota: 4 de daño a todo lo que esté cerca, sin romper bloques.
 */
public class NaveEntity extends Monster {
	/** Ticks entre disparos: 1,5 segundos. */
	public static final int CADENCIA = 30;
	public static final float DANIO_EXPLOSION = 4f;
	public static final double RADIO_EXPLOSION = 4.0;
	private static final double DISTANCIA_IDEAL = 8.0, ALTURA_SOBRE_OBJETIVO = 4.0, ALCANCE = 32.0;
	private static final double VELOCIDAD_MAXIMA = 0.45;

	public NaveEntity(EntityType<? extends NaveEntity> tipo, Level level) {
		super(tipo, level);
		this.moveControl = new NaveMoveControl(this);
		this.setNoGravity(true);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navegacion = new FlyingPathNavigation(this, level);
		navegacion.setCanOpenDoors(false);
		navegacion.setCanFloat(true);
		return navegacion;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new AtacarConRayo(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** El cuerpo siempre mira para donde mira la nave (no gira como un mob que camina). */
	@Override
	protected BodyRotationControl createBodyControl() {
		return new BodyRotationControl(this) {
			@Override
			public void clientTick() {
				NaveEntity.this.yBodyRot = NaveEntity.this.getYRot();
				NaveEntity.this.yHeadRot = NaveEntity.this.getYRot();
			}
		};
	}

	/** Vuela: sin gravedad, se mueve con su velocidad y se frena de a poco en el aire. */
	@Override
	public void travel(Vec3 direccion) {
		if (this.isControlledByLocalInstance()) {
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.setDeltaMovement(this.getDeltaMovement().scale(0.91));
		}
		this.calculateEntityAnimation(false);
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElSuelo, BlockState estado, BlockPos pos) {
	}

	/**
	 * Minecraft sube o baja el daño de los mobs a los jugadores según la dificultad
	 * (Fácil: la mitad + 1, sin pasar el original; Difícil: +50 %). Esto lo compensa para que
	 * el rayo haga siempre 1 y la explosión siempre 4, en cualquier dificultad.
	 */
	public static float danioExacto(Level level, Entity objetivo, float danio) {
		if (!(objetivo instanceof Player)) return danio;
		return switch (level.getDifficulty()) {
			case EASY -> danio <= 2f ? danio : (danio - 1f) * 2f;
			case HARD -> danio / 1.5f;
			default -> danio;
		};
	}

	void mirarHacia(double x, double z) {
		float rumbo = (float) (Mth.atan2(z - this.getZ(), x - this.getX()) * Mth.RAD_TO_DEG) - 90f;
		this.setYRot(Mth.rotLerp(0.35f, this.getYRot(), rumbo));
		this.yBodyRot = this.getYRot();
		this.yHeadRot = this.getYRot();
	}

	private void disparar(LivingEntity objetivo) {
		Vec3 frente = Vec3.directionFromRotation(0f, this.getYRot());
		Vec3 origen = this.position().add(frente.scale(0.8)).add(0, 0.22, 0);
		RayoEntity rayo = new RayoEntity(this.level(), this, origen);
		rayo.shoot(objetivo.getX() - origen.x, objetivo.getY(0.5) - origen.y, objetivo.getZ() - origen.z, 2.2f, 1.0f);
		this.level().addFreshEntity(rayo);
		this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.5f, 1.8f + this.random.nextFloat() * 0.3f);
	}

	@Override
	public void die(DamageSource fuente) {
		super.die(fuente);
		if (this.level() instanceof ServerLevel level) explotar(level);
	}

	/** Explosión solo visual y de daño: 4 de daño a los que estén cerca, no rompe bloques. */
	private void explotar(ServerLevel level) {
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(0.5), this.getZ(), 1, 0, 0, 0, 0);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
				4f, (1f + (this.random.nextFloat() - this.random.nextFloat()) * 0.2f) * 0.7f);
		AABB zona = this.getBoundingBox().inflate(RADIO_EXPLOSION);
		for (LivingEntity cercano : level.getEntitiesOfClass(LivingEntity.class, zona, e -> e != this && e.isAlive())) {
			if (cercano.distanceTo(this) <= RADIO_EXPLOSION + 1) {
				cercano.invulnerableTime = 0;   // que un rayo recién recibido no le reste daño a la explosión
				cercano.hurt(this.damageSources().explosion(this, this), danioExacto(level, cercano, DANIO_EXPLOSION));
			}
		}
	}

	/** Se acerca a unos 8 bloques del objetivo, un poco más arriba, lo mira y le dispara. */
	static class AtacarConRayo extends Goal {
		private final NaveEntity nave;
		private int enfriamiento;
		private int recalcular;

		AtacarConRayo(NaveEntity nave) {
			this.nave = nave;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity objetivo = nave.getTarget();
			return objetivo != null && objetivo.isAlive();
		}

		@Override
		public void start() {
			enfriamiento = CADENCIA;
			recalcular = 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity objetivo = nave.getTarget();
			if (objetivo == null) return;
			nave.mirarHacia(objetivo.getX(), objetivo.getZ());
			nave.getLookControl().setLookAt(objetivo, 30f, 30f);

			if (--recalcular <= 0) {
				recalcular = 10;
				Vec3 desde = nave.position().subtract(objetivo.position()).multiply(1, 0, 1);
				if (desde.lengthSqr() < 0.01) desde = new Vec3(1, 0, 0);
				Vec3 destino = objetivo.position().add(desde.normalize().scale(DISTANCIA_IDEAL)).add(0, ALTURA_SOBRE_OBJETIVO, 0);
				nave.getMoveControl().setWantedPosition(destino.x, destino.y, destino.z, 1.0);
			}

			if (--enfriamiento <= 0 && nave.distanceTo(objetivo) <= ALCANCE && nave.hasLineOfSight(objetivo)) {
				nave.disparar(objetivo);
				enfriamiento = CADENCIA;
			}
		}
	}

	/** Acelera hacia el destino sin cambiar para dónde mira (eso lo decide el ataque). */
	static class NaveMoveControl extends MoveControl {
		private final NaveEntity nave;

		NaveMoveControl(NaveEntity nave) {
			super(nave);
			this.nave = nave;
		}

		@Override
		public void tick() {
			if (this.operation != Operation.MOVE_TO) return;
			Vec3 hacia = new Vec3(this.wantedX - nave.getX(), this.wantedY - nave.getY(), this.wantedZ - nave.getZ());
			double distancia = hacia.length();
			if (distancia < 0.6) {
				this.operation = Operation.WAIT;
				nave.setDeltaMovement(nave.getDeltaMovement().scale(0.5));
				return;
			}
			Vec3 velocidad = nave.getDeltaMovement().add(hacia.scale(0.04 * this.speedModifier / distancia));
			if (velocidad.length() > VELOCIDAD_MAXIMA) velocidad = velocidad.normalize().scale(VELOCIDAD_MAXIMA);
			nave.setDeltaMovement(velocidad);
			// Sin objetivo, mira hacia donde vuela.
			if (nave.getTarget() == null) nave.mirarHacia(this.wantedX, this.wantedZ);
		}
	}
}
