package com.dedsafio4.bestias;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Soarer: la bestia voladora. Por ahora camina, mira alrededor y se defiende si la atacan;
 * usa el modelo y las animaciones que pasó el usuario (hacen falta con GeckoLib instalado).
 */
public class SoarerEntity extends PathfinderMob implements GeoEntity {
	private static final RawAnimation QUIETO = RawAnimation.begin().thenLoop("misc.ground_idle");
	private static final RawAnimation CAMINAR = RawAnimation.begin().thenLoop("move.walk");
	private static final RawAnimation ATACAR = RawAnimation.begin().thenPlay("attack.attack");

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);
	/** Ticks que dura la animación de ataque en pantalla. */
	private int atacando;

	public SoarerEntity(EntityType<? extends SoarerEntity> tipo, Level level) {
		super(tipo, level);
		this.xpReward = 20;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 60.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public boolean doHurtTarget(net.minecraft.world.entity.Entity objetivo) {
		this.atacando = 20;
		return super.doHurtTarget(objetivo);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.atacando > 0) this.atacando--;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.RAVAGER_AMBIENT;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "principal", 5, this::animar));
	}

	private PlayState animar(AnimationState<SoarerEntity> estado) {
		if (this.atacando > 0) return estado.setAndContinue(ATACAR);
		if (estado.isMoving()) return estado.setAndContinue(CAMINAR);
		return estado.setAndContinue(QUIETO);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
