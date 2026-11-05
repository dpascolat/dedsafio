package com.dedsafio4.bestias;

import com.dedsafio4.items.Linternas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ojo Volador (modelo "OjoVolador" de Patricio): un ojo gigante que flota, te persigue y te muerde.
 * Si un jugador le apunta con la Linterna prendida, se va poniendo blanco y al segundo de luz desaparece
 * en una nube de humo (sin soltar nada). También se lo puede matar con armas.
 * No aparece solo: solo con el huevo o con /summon dedsafio4:ojo_volador.
 */
public class OjoVoladorEntity extends Monster {
	/** Un segundo con la luz encima: desaparece. */
	public static final int TICKS_CON_LUZ = 20;
	/** Qué tan alumbrado está (0 nada, 1 a punto de desaparecer), para el dibujo. */
	private static final EntityDataAccessor<Float> LUZ = SynchedEntityData.defineId(OjoVoladorEntity.class, EntityDataSerializers.FLOAT);

	private int conLuz;

	public OjoVoladorEntity(EntityType<? extends OjoVoladorEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 24.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.5)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(LUZ, 0f);
	}

	public float luz() {
		return entityData.get(LUZ);
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
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel mundo) || !isAlive()) return;
		// Alumbrado con la Linterna: se pone blanco y al segundo desaparece. Si le sacan la luz, se va recuperando.
		boolean alumbrado = mundo.players().stream().anyMatch(p -> !p.isSpectator() && Linternas.ilumina(p, this));
		conLuz = alumbrado ? conLuz + 1 : Math.max(0, conLuz - 2);
		entityData.set(LUZ, Math.min(1f, conLuz / (float) TICKS_CON_LUZ));
		if (alumbrado && conLuz % 4 == 0) mundo.sendParticles(ParticleTypes.SMOKE, getX(), getY(0.5), getZ(), 4, 0.4, 0.5, 0.4, 0.01);
		if (conLuz >= TICKS_CON_LUZ) desaparecer(mundo);
	}

	private void desaparecer(ServerLevel mundo) {
		mundo.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 30, 0.5, 0.7, 0.5, 0.02);
		mundo.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.6f);
		discard();
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
		return SoundEvents.PHANTOM_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.PHANTOM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PHANTOM_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.7f;
	}
}
