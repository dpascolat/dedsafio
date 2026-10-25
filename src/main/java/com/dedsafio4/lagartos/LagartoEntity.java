package com.dedsafio4.lagartos;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import com.dedsafio4.items.ModItems;
import com.dedsafio4.nave.ModEntidades;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import com.dedsafio4.reptisaurios.VenenoPrimitivo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import net.minecraft.world.level.Level;

/**
 * Los Reptisaurios (los tres lagartos que envenenan): el Guerrero (espadachín, marrón, espada y escudo: alterna
 * estocada y tajo, y el escudo le para un golpe de frente cada 5 segundos), el Arquero (verde, arco de huesos) y el
 * de la Lanza (naranja, lanza y escudo: estoca de cerca y de lejos le tira la lanza). Cada uno sale con uno de sus 3
 * colores al azar. Lo que está haciendo (y desde cuándo) va al cliente para las animaciones (LagartoRenderer).
 */
public class LagartoEntity extends PathfinderMob {
	/** Cada hermano: colores (escamas, oscuro, panza, cresta, garras) y su adorno. */
	public enum Variante {
		MIRA("mira", 0x2F5A1E, 0x1D3A12, 0x6B7A3A, 0x3F6B25, 0xF2F1EA),
		/** El guerrero: se lanza de frente, aguanta mucho y pega seguido. */
		REPTISAURIO_GUERRERO("reptisaurio_guerrero", 0x5A3A22, 0x3A2414, 0xC9A45A, 0xD8C23A, 0xF2F1EA),
		/** El del arco: dispara de lejos y sus flechas también envenenan. */
		REPTISAURIO_ARQUERO("reptisaurio_arquero", 0x1F9A4A, 0x0F5A2A, 0xB9E0C0, 0xE8ECE6, 0xE8ECE6),
		/** El de la lanza: pelea de lejos y envenena. */
		REPTISAURIO_LANZA("reptisaurio_lanza", 0xC07A1E, 0x7A4A10, 0xE8C870, 0x4F7A2A, 0xF2F1EA);

		public final String nombre;
		public final int escamas, oscuro, panza, cresta, garras;

		Variante(String nombre, int escamas, int oscuro, int panza, int cresta, int garras) {
			this.nombre = nombre;
			this.escamas = escamas;
			this.oscuro = oscuro;
			this.panza = panza;
			this.cresta = cresta;
			this.garras = garras;
		}
	}

	private final Variante variante;

	/** El color (0 a 2), qué está haciendo y desde cuándo (para las animaciones). */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> PIEL =
			net.minecraft.network.syncher.SynchedEntityData.defineId(LagartoEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ACCION =
			net.minecraft.network.syncher.SynchedEntityData.defineId(LagartoEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
	private static final net.minecraft.network.syncher.EntityDataAccessor<Long> T_ACCION =
			net.minecraft.network.syncher.SynchedEntityData.defineId(LagartoEntity.class, net.minecraft.network.syncher.EntityDataSerializers.LONG);
	public static final int NADA = 0, DISPARO = 1, ESTOCADA = 2, TAJO = 3, LANZAR = 4, BLOQUEO = 5;
	/** Cuándo sale la lanza de la mano al tirarla (0,4 s), cada cuánto puede tirarla y cada cuánto para un golpe con el escudo. */
	private static final int SUELTA_LANZA = 8, CADA_LANZA = 80, CADA_BLOQUEO = 100;
	private int lanzaEn, golpes;
	private long ultimoBloqueo = -CADA_BLOQUEO;

	public LagartoEntity(EntityType<? extends LagartoEntity> tipo, Level level, Variante variante) {
		super(tipo, level);
		this.variante = variante;
	}

	public Variante variante() {
		return variante;
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(PIEL, 0);
		datos.define(ACCION, NADA);
		datos.define(T_ACCION, -1000L);
	}

	public int piel() {
		return entityData.get(PIEL);
	}

	public int accion() {
		return entityData.get(ACCION);
	}

	/** Segundos desde que empezó lo que está haciendo. */
	public float tAccion(float parcial) {
		return (level().getGameTime() - entityData.get(T_ACCION) + parcial) / 20f;
	}

	private void accionar(int accion) {
		entityData.set(ACCION, accion);
		entityData.set(T_ACCION, level().getGameTime());
	}

	@Override
	public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor mundo,
			net.minecraft.world.DifficultyInstance dificultad, MobSpawnType razon, net.minecraft.world.entity.SpawnGroupData datos) {
		entityData.set(PIEL, random.nextInt(3));
		return super.finalizeSpawn(mundo, dificultad, razon, datos);
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag datos) {
		super.addAdditionalSaveData(datos);
		datos.putInt("Piel", piel());
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag datos) {
		super.readAdditionalSaveData(datos);
		entityData.set(PIEL, Math.floorMod(datos.getInt("Piel"), 3));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide || lanzaEn <= 0 || --lanzaEn > 0) return;
		// Suelta la lanza: va hacia el objetivo (un poco para arriba, porque cae).
		LivingEntity objetivo = getTarget();
		if (objetivo == null || !objetivo.isAlive()) return;
		LanzaLanzadaEntity lanza = new LanzaLanzadaEntity(level(), this);
		double dx = objetivo.getX() - getX(), dz = objetivo.getZ() - getZ();
		double dy = objetivo.getY(0.5) - lanza.getY(), plano = Math.sqrt(dx * dx + dz * dz);
		lanza.shoot(dx, dy + plano * 0.15, dz, 1.5f, 2f);
		level().addFreshEntity(lanza);
		playSound(SoundEvents.TRIDENT_THROW.value(), 1f, 0.8f);
	}

	/** El Guerrero para con el escudo un golpe de frente cada 5 segundos. */
	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		if (!level().isClientSide && esGuerrero() && fuente.getEntity() != null && level().getGameTime() - ultimoBloqueo >= CADA_BLOQUEO) {
			Vec3 hacia = fuente.getEntity().position().subtract(position()).multiply(1, 0, 1).normalize();
			Vec3 mira = Vec3.directionFromRotation(0, yBodyRot);
			if (hacia.dot(mira) > 0.4) {
				ultimoBloqueo = level().getGameTime();
				accionar(BLOQUEO);
				playSound(SoundEvents.SHIELD_BLOCK, 1f, 0.8f + random.nextFloat() * 0.4f);
				return false;
			}
		}
		return super.hurt(fuente, cantidad);
	}

	/** El Reptisaurio Salvaje aparece en las cuevas: donde no llega el cielo, a oscuras y sobre piso firme. */
	public static boolean puedeAparecerEnCueva(EntityType<LagartoEntity> tipo, LevelAccessor mundo, MobSpawnType razon,
											   BlockPos pos, RandomSource azar) {
		if (razon == MobSpawnType.SPAWNER) return true;
		return mundo.getBrightness(LightLayer.SKY, pos) == 0 && mundo.getBrightness(LightLayer.BLOCK, pos) < 8
				&& mundo.getBlockState(pos.below()).isSolidRender(mundo, pos.below());
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	/** El Guerrero es el que aguanta: más vida, algo de armadura y golpes más fuertes. */
	public static AttributeSupplier.Builder atributosGuerrero() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.ARMOR, 6.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	/** Dispara una flecha con Veneno Primitivo. */
	public void disparar(LivingEntity objetivo) {
		accionar(DISPARO);
		Arrow flecha = new Arrow(this.level(), this, new ItemStack(Items.ARROW), null);
		flecha.setBaseDamage(2.0);
		flecha.addEffect(new MobEffectInstance(VenenoPrimitivo.EFECTO, VenenoPrimitivo.DURACION, 0, false, true, true));
		double dx = objetivo.getX() - this.getX();
		double dy = objetivo.getY(0.6) - flecha.getY();
		double dz = objetivo.getZ() - this.getZ();
		double plano = Math.sqrt(dx * dx + dz * dz);
		flecha.shoot(dx, dy + plano * 0.12, dz, 1.6f, 2f);
		this.level().addFreshEntity(flecha);
		this.playSound(SoundEvents.ARROW_SHOOT, 1f, 1f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
	}

	/** Dispara de lejos y se aleja si lo acorralan. */
	static class ArcoGoal extends Goal {
		private static final double ALCANCE = 16.0, IDEAL = 8.0, MUY_CERCA = 5.0;
		private static final int CADENCIA = 40;

		private final LagartoEntity lagarto;
		private int enfriamiento = CADENCIA;

		ArcoGoal(LagartoEntity lagarto) {
			this.lagarto = lagarto;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity objetivo = lagarto.getTarget();
			return objetivo != null && objetivo.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity objetivo = lagarto.getTarget();
			if (objetivo == null) return;
			lagarto.getLookControl().setLookAt(objetivo, 30f, 30f);
			double distancia = lagarto.distanceTo(objetivo);
			boolean loVe = lagarto.hasLineOfSight(objetivo);

			lagarto.setAggressive(true);
			if (distancia < MUY_CERCA) {
				Vec3 atras = lagarto.position().subtract(objetivo.position()).normalize().scale(4);
				Vec3 destino = lagarto.position().add(atras);
				lagarto.getNavigation().moveTo(destino.x, destino.y, destino.z, 1.2);
			} else if (distancia > IDEAL || !loVe) {
				lagarto.getNavigation().moveTo(objetivo, 1.0);
			} else {
				lagarto.getNavigation().stop();
			}

			if (--enfriamiento <= 0 && distancia <= ALCANCE && loVe) {
				enfriamiento = CADENCIA;
				lagarto.swing(InteractionHand.MAIN_HAND);
				lagarto.disparar(objetivo);
			}
		}

		@Override
		public void stop() {
			lagarto.getNavigation().stop();
			lagarto.setAggressive(false);
		}
	}

	/**
	 * Pelea con la lanza: se queda a distancia (ni muy lejos ni pegado) y estoca. Si el objetivo se le acerca
	 * demasiado, retrocede; si está lejos (más de 6 bloques), le tira la lanza (cada 4 segundos).
	 */
	static class LanzaGoal extends Goal {
		/** Alcance de la lanza y distancia a la que trata de quedarse. */
		private static final double ALCANCE = 4.5, IDEAL = 3.6, MUY_CERCA = 2.6;
		private static final int CADENCIA = 30;

		private final LagartoEntity lagarto;
		private int enfriamiento, enfriamientoLanzar = 40;

		LanzaGoal(LagartoEntity lagarto) {
			this.lagarto = lagarto;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity objetivo = lagarto.getTarget();
			return objetivo != null && objetivo.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity objetivo = lagarto.getTarget();
			if (objetivo == null) return;
			lagarto.getLookControl().setLookAt(objetivo, 30f, 30f);
			double distancia = lagarto.distanceTo(objetivo);
			lagarto.setAggressive(true);
			enfriamientoLanzar--;
			if (distancia > 6 && distancia < 20 && enfriamientoLanzar <= 0 && lagarto.lanzaEn <= 0 && lagarto.hasLineOfSight(objetivo)) {
				enfriamientoLanzar = CADA_LANZA;
				lagarto.lanzaEn = SUELTA_LANZA;
				lagarto.accionar(LANZAR);
			}

			if (distancia < MUY_CERCA) {
				// Demasiado cerca: retrocede sin dejar de mirarlo.
				Vec3 atras = lagarto.position().subtract(objetivo.position()).normalize().scale(3);
				Vec3 destino = lagarto.position().add(atras);
				lagarto.getNavigation().moveTo(destino.x, destino.y, destino.z, 1.1);
			} else if (distancia > IDEAL) {
				lagarto.getNavigation().moveTo(objetivo, 1.0);
			} else {
				lagarto.getNavigation().stop();
			}

			if (--enfriamiento <= 0 && distancia <= ALCANCE && lagarto.hasLineOfSight(objetivo)) {
				enfriamiento = CADENCIA;
				lagarto.swing(InteractionHand.MAIN_HAND);
				lagarto.accionar(ESTOCADA);
				lagarto.doHurtTarget(objetivo);
			}
		}

		@Override
		public void stop() {
			lagarto.getNavigation().stop();
			lagarto.setAggressive(false);
		}
	}

	/**
	 * ¿Es el Reptisaurio con Lanza? Se mira por el tipo y no por el campo, porque Minecraft
	 * arma las metas del mob dentro del constructor de arriba, antes de guardar la variante.
	 */
	public boolean conLanza() {
		return this.getType() == com.dedsafio4.nave.ModEntidades.REPTISAURIO_LANZA;
	}

	/** ¿Es el Reptisaurio Arquero? */
	public boolean conArco() {
		return this.getType() == com.dedsafio4.nave.ModEntidades.REPTISAURIO_ARQUERO;
	}

	/** ¿Es el Reptisaurio Guerrero? */
	public boolean esGuerrero() {
		return this.getType() == com.dedsafio4.nave.ModEntidades.REPTISAURIO_GUERRERO;
	}

	/** Los Reptisaurios: pelean, envenenan y sueltan sangre. */
	public boolean esReptisaurio() {
		return conLanza() || conArco() || esGuerrero();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		if (conLanza()) {
			this.goalSelector.addGoal(1, new LanzaGoal(this));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		} else if (conArco()) {
			this.goalSelector.addGoal(1, new ArcoGoal(this));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		} else if (esGuerrero()) {
			// Va de frente y pega seguido.
			this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		} else {
			// Reptisaurio Salvaje: muy territorial, ataca al instante.
			this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		}
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	/** Con la lanza pega de lejos y deja Veneno Primitivo. */
	@Override
	public boolean doHurtTarget(Entity objetivo) {
		// El Guerrero alterna estocada y tajo.
		if (esGuerrero()) accionar(golpes++ % 2 == 0 ? ESTOCADA : TAJO);
		boolean pego = super.doHurtTarget(objetivo);
		if (pego && esReptisaurio() && objetivo instanceof LivingEntity victima) {
			VenenoPrimitivo.aplicar(victima);
		}
		return pego;
	}

	/** Siempre suelta Sangre de Reptisaurio. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource fuente, boolean muertePorJugador) {
		super.dropCustomDeathLoot(level, fuente, muertePorJugador);
		if (esReptisaurio() || this.getType() == ModEntidades.MIRA) {
			this.spawnAtLocation(new ItemStack(ModItems.SANGRE_REPTISAURIO));
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SNIFFER_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.SNIFFER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SNIFFER_DEATH;
	}
}
