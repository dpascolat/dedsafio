package com.dedsafio4.bombas;

import com.dedsafio4.nave.NaveEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Bomba Warden: vuela hacia el jugador y, a diferencia del Creeper, no tiene cuenta atrás:
 * en cuanto lo tiene cerca explota en el acto. Aparece sola en la Oscuridad Profunda.
 * La explosión hace daño pero no rompe bloques.
 */
public class BombaWardenEntity extends Monster {
	public static final float DANIO_EXPLOSION = 6f;
	public static final double RADIO_EXPLOSION = 3.5;
	/** A esta distancia del objetivo explota. */
	private static final double DISTANCIA_EXPLOSION = 2.0;

	public BombaWardenEntity(EntityType<? extends BombaWardenEntity> tipo, Level level) {
		super(tipo, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
		this.xpReward = 5;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				// Vuela: sin esto, al invocarla con /summon el juego le devuelve la gravedad y se cae.
				.add(Attributes.GRAVITY, 0.0);
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
		this.goalSelector.addGoal(1, new PerseguirYExplotar(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
		return SoundEvents.WARDEN_HEARTBEAT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.SCULK_BLOCK_HIT;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide) {
			chispa();
			return;
		}
		LivingEntity objetivo = this.getTarget();
		if (objetivo != null && objetivo.isAlive() && this.distanceTo(objetivo) <= DISTANCIA_EXPLOSION) {
			explotar();
		}
	}

	/** Humito y fuego en la mecha, que va atrás. */
	private void chispa() {
		if (this.tickCount % 2 != 0) return;
		Vec3 atras = Vec3.directionFromRotation(0f, this.getYRot()).scale(-0.8);
		double x = this.getX() + atras.x, y = this.getY(0.9), z = this.getZ() + atras.z;
		this.level().addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.01, 0);
		if (this.random.nextInt(3) == 0) this.level().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
	}

	/** Explosión: hace daño alrededor y desaparece, sin romper bloques. */
	private void explotar() {
		if (!(this.level() instanceof ServerLevel level)) return;
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(0.5), this.getZ(), 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(0.5), this.getZ(), 30, 1.2, 1.2, 1.2, 0.05);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
				4f, (1f + (this.random.nextFloat() - this.random.nextFloat()) * 0.2f) * 0.7f);
		AABB zona = this.getBoundingBox().inflate(RADIO_EXPLOSION);
		for (LivingEntity cercano : level.getEntitiesOfClass(LivingEntity.class, zona, e -> e != this && e.isAlive())) {
			if (cercano.distanceTo(this) > RADIO_EXPLOSION + 1) continue;
			cercano.invulnerableTime = 0;
			cercano.hurt(this.damageSources().explosion(this, this), NaveEntity.danioExacto(level, cercano, DANIO_EXPLOSION));
		}
		this.discard();
	}

	/** Va derecho al objetivo; el estallido lo decide la propia bomba cuando lo tiene cerca. */
	static class PerseguirYExplotar extends Goal {
		private final BombaWardenEntity bomba;
		private int recalcular;

		PerseguirYExplotar(BombaWardenEntity bomba) {
			this.bomba = bomba;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity objetivo = bomba.getTarget();
			return objetivo != null && objetivo.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity objetivo = bomba.getTarget();
			if (objetivo == null) return;
			bomba.getLookControl().setLookAt(objetivo, 30f, 30f);
			if (--recalcular > 0) return;
			recalcular = 1;
			// Apunta a la altura del pecho para que llegue de frente y no por debajo.
			bomba.getMoveControl().setWantedPosition(objetivo.getX(), objetivo.getY(0.7), objetivo.getZ(), 1.2);
		}
	}
}
