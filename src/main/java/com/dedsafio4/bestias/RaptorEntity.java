package com.dedsafio4.bestias;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Los dinosaurios raptor del diseño:
 * Los dos se desplazan en manada: atacan a los jugadores y, cuando uno pelea, los de su manada que estén
 * cerca se suman.
 * - Dromoraptor Rojo (el naranja, púas azules): depredador de fuerza brutal.
 * - Dromoraptor Azul (el turquesa, púas rojas y brazos largos): extremadamente veloz. Aparece en la
 *   Sabana del Centro de Quiu.
 * La animación (caminar, quieto y rugir) la hace RaptorRenderer en el cliente.
 */
public class RaptorEntity extends PathfinderMob {
	/** Evento para que el cliente haga la animación de rugir. */
	private static final byte EVENTO_RUGIDO = 60;
	/** Lo que dura el rugido (1,6 segundos). */
	public static final int TIEMPO_RUGIDO = 32;

	/** En el cliente: ticks desde que empezó a rugir (-1 = no está rugiendo). */
	public int rugido = -1;

	public RaptorEntity(EntityType<? extends RaptorEntity> tipo, Level level) {
		super(tipo, level);
	}

	/** Se mira el tipo (y no un campo) porque los objetivos se arman antes de terminar el constructor. */
	public boolean azul() {
		return getType() == com.dedsafio4.nave.ModEntidades.DROMORAPTOR_AZUL;
	}

	/** Dromoraptor Rojo: fuerza brutal. */
	public static AttributeSupplier.Builder atributosDromoraptor() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.ATTACK_DAMAGE, 9.0)
				.add(Attributes.ATTACK_KNOCKBACK, 1.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	/** Dromoraptor Azul: extremadamente veloz (pega menos que el Rojo). */
	public static AttributeSupplier.Builder atributosDromoraptorAzul() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 22.0)
				.add(Attributes.MOVEMENT_SPEED, 0.45)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		// Cazan: avisan a la manada y buscan jugadores.
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public void rugir() {
		if (level().isClientSide) return;
		level().broadcastEntityEvent(this, EVENTO_RUGIDO);
		playSound(SoundEvents.RAVAGER_ROAR, 1.2f, azul() ? 1.7f : 1.5f);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			if (rugido >= 0 && ++rugido >= TIEMPO_RUGIDO) rugido = -1;
		} else if (getRandom().nextInt(500) == 0) {
			rugir();
		}
	}

	@Override
	public void setTarget(LivingEntity objetivo) {
		boolean nuevo = objetivo != null && getTarget() == null;
		super.setTarget(objetivo);
		if (!nuevo) return;
		rugir();
		// La manada: los Dromoraptores del mismo color de alrededor que no estén peleando van contra el mismo.
		if (!level().isClientSide) {
			for (RaptorEntity otro : level().getEntitiesOfClass(RaptorEntity.class, getBoundingBox().inflate(16),
					r -> r != this && r.getType() == getType() && r.getTarget() == null)) {
				otro.setTarget(objetivo);
			}
		}
	}

	@Override
	public void handleEntityEvent(byte evento) {
		if (evento == EVENTO_RUGIDO) rugido = 0;
		else super.handleEntityEvent(evento);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.HOGLIN_HURT;
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getDeathSound() {
		return SoundEvents.HOGLIN_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 1.5f;
	}
}
