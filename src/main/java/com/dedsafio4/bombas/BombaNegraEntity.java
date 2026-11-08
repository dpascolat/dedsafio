package com.dedsafio4.bombas;

import com.dedsafio4.dimension.Portales;
import com.dedsafio4.nave.NaveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Bomba Negra (modelo "bomba" del Dedsafío 3): una bomba con alas que vuela hacia el jugador. Cuando lo tiene cerca se
 * queda quieta, hace la animación de explotar y explota con mucho daño (sin romper bloques; /dañomob lo cambia).
 * No se la puede empujar: ni los golpes, ni las flechas, ni los otros bichos la mueven.
 * Con "/mob bomba_negra" aparece sola de noche en el Overworld y en el Centro de Quiu (fuera de las cuevas).
 */
public class BombaNegraEntity extends Monster implements GeoEntity {
	public static final float DANIO_EXPLOSION = 20f;
	public static final double RADIO_EXPLOSION = 4.5;
	/** A esta distancia del objetivo empieza a explotar. */
	private static final double DISTANCIA_EXPLOSION = 2.5;
	/** Cuánto dura la animación de explotar antes del estallido. */
	private static final int MECHA = 15;

	private static final EntityDataAccessor<Boolean> EXPLOTANDO = SynchedEntityData.defineId(BombaNegraEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation VOLAR = RawAnimation.begin().thenLoop("fly"), QUIETA = RawAnimation.begin().thenLoop("idle"),
			EXPLOSION = RawAnimation.begin().thenLoop("explosion");

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);
	private int mecha;

	public BombaNegraEntity(EntityType<? extends BombaNegraEntity> tipo, Level level) {
		super(tipo, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 16.0)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				// Vuela: sin esto, al invocarla con /summon el juego le devuelve la gravedad y se cae.
				.add(Attributes.GRAVITY, 0.0);
	}

	/**
	 * De noche en el Overworld (a cielo abierto), o en el Centro de Quiu en cualquier momento, pero no en sus cuevas
	 * (donde no llega el cielo). Solo con /mob bomba_negra (eso lo controla SpawnPlacementsMixin).
	 */
	public static boolean puedeAparecer(EntityType<? extends Monster> tipo, ServerLevelAccessor mundo, MobSpawnType razon,
										BlockPos pos, RandomSource azar) {
		if (razon == MobSpawnType.SPAWNER) return true;
		if (mundo.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
		ServerLevel level = mundo.getLevel();
		if (level.dimension() == Portales.DIMENSION_NUEVA) return mundo.getBrightness(LightLayer.SKY, pos) > 0;
		return level.dimension() == Level.OVERWORLD && level.isNight() && mundo.canSeeSky(pos);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(EXPLOTANDO, false);
	}

	public boolean explotando() {
		return entityData.get(EXPLOTANDO);
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
		this.goalSelector.addGoal(1, new BombaWardenEntity.PerseguirYExplotar(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide) return;
		if (explotando()) {
			getNavigation().stop();
			setDeltaMovement(getDeltaMovement().scale(0.5));
			if (++mecha >= MECHA) explotar();
			return;
		}
		LivingEntity objetivo = this.getTarget();
		if (objetivo != null && objetivo.isAlive() && this.distanceTo(objetivo) <= DISTANCIA_EXPLOSION) {
			entityData.set(EXPLOTANDO, true);
			playSound(SoundEvents.CREEPER_PRIMED, 1f, 0.6f);
		}
	}

	/** Explosión: mucho daño alrededor y desaparece, sin romper bloques. */
	private void explotar() {
		if (!(this.level() instanceof ServerLevel level)) return;
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(0.5), this.getZ(), 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5), this.getZ(), 40, 1.5, 1.5, 1.5, 0.05);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
				4f, (1f + (this.random.nextFloat() - this.random.nextFloat()) * 0.2f) * 0.7f);
		// El daño de siempre, o el que se puso con /dañomob.
		Double cambiado = com.dedsafio4.bestias.AjustesMob.danoDe(level.getServer(), this.getType());
		float danio = cambiado != null ? cambiado.floatValue() : DANIO_EXPLOSION;
		AABB zona = this.getBoundingBox().inflate(RADIO_EXPLOSION);
		for (LivingEntity cercano : level.getEntitiesOfClass(LivingEntity.class, zona, e -> e != this && e.isAlive())) {
			if (cercano.distanceTo(this) > RADIO_EXPLOSION + 1) continue;
			cercano.invulnerableTime = 0;
			cercano.hurt(this.damageSources().explosion(this, this), NaveEntity.danioExacto(level, cercano, danio));
		}
		this.discard();
	}

	// --- No se la puede empujar ---

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity otro) {
	}

	@Override
	public void push(double x, double y, double z) {
	}

	@Override
	public void knockback(double fuerza, double x, double z) {
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElSuelo, BlockState estado, BlockPos pos) {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PHANTOM_FLAP;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_EXPLODE.value();
	}

	// --- Animaciones (las del Dedsafío 3: fly, idle, explosion) ---

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "movimiento", 4, estado -> {
			if (explotando()) return estado.setAndContinue(EXPLOSION);
			return estado.setAndContinue(estado.isMoving() ? VOLAR : QUIETA);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
