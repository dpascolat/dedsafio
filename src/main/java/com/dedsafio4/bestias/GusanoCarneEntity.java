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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Gusano de Carne (el diseño del Parásito): un gusano chico y lento, con boca de lamprea, que vive adentro de la
 * Grasa de Gleba. Sale al romper un bloque de Grasa Líquida o Coagulada (30 %) y ataca enseguida a quien tenga cerca.
 */
public class GusanoCarneEntity extends Monster {
	public GusanoCarneEntity(EntityType<? extends GusanoCarneEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.xpReward = 3;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 8.0)
				.add(Attributes.MOVEMENT_SPEED, 0.18)
				.add(Attributes.ATTACK_DAMAGE, 2.0)
				.add(Attributes.FOLLOW_RANGE, 12.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SILVERFISH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SILVERFISH_DEATH;
	}
}
