package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nautilus Óseo (pequeño): un caparazón de hueso recto, como un palo, con la cabeza de carne y los tentáculos
 * para adelante. Vuela por la Dimensión de los Órganos en grupos de 3 a 5. No hace nada hasta que un jugador se
 * acerca a menos de 8 bloques; ahí lo persigue, y si uno ataca o encuentra a alguien, los que están cerca se
 * suman. Al pegar da 3 segundos de Levitación I. No se lo puede empujar.
 */
public class NautilusOseoEntity extends Monster {
	/** A cuántos bloques se enoja con un jugador. */
	private static final double CERCA = 8;

	public NautilusOseoEntity(EntityType<? extends NautilusOseoEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
		this.xpReward = 6;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 12.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.55)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	/** Aparece en el aire o en el piso, con cualquier luz (en los Órganos), salvo en pacífico. */
	public static boolean puedeAparecer(EntityType<NautilusOseoEntity> tipo, ServerLevelAccessor mundo, MobSpawnType razon,
										BlockPos pos, RandomSource azar) {
		return mundo.getDifficulty() != Difficulty.PEACEFUL && mundo.getBlockState(pos).isAir();
	}

	@Override
	protected PathNavigation createNavigation(Level mundo) {
		FlyingPathNavigation navegacion = new FlyingPathNavigation(this, mundo);
		navegacion.setCanOpenDoors(false);
		navegacion.setCanFloat(true);
		navegacion.setCanPassDoors(true);
		return navegacion;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
		goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				jugador -> jugador.distanceToSqr(this) < CERCA * CERCA));
	}

	/** Cuando uno encuentra a alguien, los nautilus que están cerca (sin objetivo) se suman: son peligrosos en grupo. */
	@Override
	public void setTarget(LivingEntity objetivo) {
		boolean nuevo = objetivo != null && objetivo != getTarget();
		super.setTarget(objetivo);
		if (!nuevo || level().isClientSide) return;
		for (NautilusOseoEntity otro : level().getEntitiesOfClass(NautilusOseoEntity.class, getBoundingBox().inflate(12),
				n -> n != this && n.getTarget() == null && n.isAlive())) {
			otro.setTarget(objetivo);
		}
	}

	@Override
	public boolean doHurtTarget(Entity objetivo) {
		boolean pego = super.doHurtTarget(objetivo);
		if (pego && objetivo instanceof LivingEntity vivo) {
			vivo.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0), this);
		}
		return pego;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity otro) {
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.5f;
	}
}
