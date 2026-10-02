package com.dedsafio4.nave;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Nave Veloz (el modelo "Nave 2" de Patricio): vuela, persigue a los jugadores rapidito y les hace daño al chocarlos
 * (de cerca). Sin nadie a la vista da vueltas despacio. Animaciones: "volar" siempre y "atacar" cuando está por
 * chocarte (geo/entity/nave_veloz.geo.json, animations/entity/nave_veloz.animation.json).
 */
public class NaveVelozEntity extends Monster implements GeoEntity {
	private static final EntityDataAccessor<Boolean> CARGANDO =
			SynchedEntityData.defineId(NaveVelozEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation VOLAR = RawAnimation.begin().thenLoop("volar");
	private static final RawAnimation ATACAR = RawAnimation.begin().thenLoop("atacar");

	/** Persiguiendo: bloques por tick (unos 11 por segundo, más rápido que correr). Paseando: mucho más lento. */
	private static final double VELOCIDAD = 0.55, VELOCIDAD_PASEO = 0.12;
	/** A cuánta distancia te choca, cada cuánto puede volver a pegarte y desde dónde se pone en modo ataque. */
	private static final double ALCANCE_CHOQUE = 1.4, DISTANCIA_ATAQUE = 8;
	private static final int ESPERA_CHOQUE = 15;

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);
	private int esperaChoque;
	private Vec3 destinoPaseo;

	public NaveVelozEntity(EntityType<? extends NaveVelozEntity> tipo, Level level) {
		super(tipo, level);
		this.setNoGravity(true);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 16.0)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.4)
				.add(Attributes.FLYING_SPEED, 0.8)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(CARGANDO, false);
	}

	public boolean cargando() {
		return entityData.get(CARGANDO);
	}

	@Override
	protected void registerGoals() {
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	/** El vuelo lo maneja ella misma: va derecho hacia el jugador (o a pasear) y lo choca. */
	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (esperaChoque > 0) esperaChoque--;
		LivingEntity blanco = getTarget();
		Vec3 centro = position().add(0, getBbHeight() / 2, 0);
		Vec3 hacia;
		double velocidad;
		if (blanco != null && blanco.isAlive()) {
			hacia = blanco.position().add(0, blanco.getBbHeight() * 0.6, 0);
			velocidad = VELOCIDAD;
			double distancia = hacia.distanceTo(centro);
			entityData.set(CARGANDO, distancia < DISTANCIA_ATAQUE);
			if (distancia < ALCANCE_CHOQUE + blanco.getBbWidth() / 2 && esperaChoque == 0) {
				if (doHurtTarget(blanco)) {
					level().playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1f, 1.6f);
					// Rebota un poco para atrás después de chocar.
					setDeltaMovement(getDeltaMovement().scale(-0.6).add(0, 0.25, 0));
				}
				esperaChoque = ESPERA_CHOQUE;
			}
		} else {
			entityData.set(CARGANDO, false);
			if (destinoPaseo == null || destinoPaseo.distanceTo(centro) < 1.5 || tickCount % 100 == 0) {
				destinoPaseo = centro.add((random.nextDouble() - 0.5) * 16, (random.nextDouble() - 0.4) * 6,
						(random.nextDouble() - 0.5) * 16);
			}
			hacia = destinoPaseo;
			velocidad = VELOCIDAD_PASEO;
		}
		Vec3 direccion = hacia.subtract(centro);
		if (direccion.lengthSqr() > 0.01) {
			Vec3 deseado = direccion.normalize().scale(velocidad);
			// Gira suave hacia donde quiere ir (no frena en seco).
			setDeltaMovement(getDeltaMovement().scale(0.75).add(deseado.scale(0.25)));
			float giro = (float) (Mth.atan2(direccion.z, direccion.x) * Mth.RAD_TO_DEG) - 90f;
			setYRot(Mth.approachDegrees(getYRot(), giro, 20f));
			yBodyRot = yHeadRot = getYRot();
		}
		if (tickCount % 40 == 0) level().playSound(null, blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 0.6f, 0.6f);
	}

	// Vuela: no se cae ni se lastima al caer.
	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, net.minecraft.world.damagesource.DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "principal", 5, this::animar));
	}

	private PlayState animar(AnimationState<NaveVelozEntity> estado) {
		return estado.setAndContinue(cargando() ? ATACAR : VOLAR);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
