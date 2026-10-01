package com.dedsafio4.dactylos;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Dáctylo Bebé: un pterodáctilo chiquito que surca los cielos del Centro de Quiu. Aletea sin parar,
 * se queda volando cerca del jugador y cada 4 segundos le escupe tinta que le tapa la visión.
 */
public class DactyloBebeEntity extends Monster {
	/** Aviso al cliente: acaba de disparar (para que levante la cabeza). */
	public static final byte EVENTO_DISPARO = 60;
	/** Cada cuánto dispara (4 segundos). */
	private static final int ESPERA_DISPARO = 80;

	/** Del lado del cliente: en qué tick disparó por última vez. */
	public int ultimoDisparo = -100;

	public DactyloBebeEntity(EntityType<? extends Monster> tipo, Level level) {
		super(tipo, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 12)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 32);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navegacion = new FlyingPathNavigation(this, level);
		navegacion.setCanOpenDoors(false);
		navegacion.setCanFloat(true);
		navegacion.setCanPassDoors(true);
		return navegacion;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new DispararGoal(this));
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
	protected void checkFallDamage(double y, boolean enElPiso, net.minecraft.world.level.block.state.BlockState estado, BlockPos pos) {}

	@Override
	public void handleEntityEvent(byte evento) {
		if (evento == EVENTO_DISPARO) ultimoDisparo = tickCount;
		else super.handleEntityEvent(evento);
	}

	/** Escupe tinta hacia el objetivo, desde el pico. */
	void disparar(LivingEntity objetivo) {
		TintaDactyloEntity tinta = new TintaDactyloEntity(level(), this);
		Vec3 pico = position().add(getLookAngle().scale(0.6)).add(0, 0.55, 0);
		tinta.setPos(pico);
		double dx = objetivo.getX() - pico.x, dy = objetivo.getY(0.6) - pico.y, dz = objetivo.getZ() - pico.z;
		tinta.shoot(dx, dy, dz, 1.1f, 1.5f);
		level().addFreshEntity(tinta);
		playSound(SoundEvents.LLAMA_SPIT, 1f, 1.4f);
		level().broadcastEntityEvent(this, EVENTO_DISPARO);
	}

	/** Aparece en el cielo del Centro de Quiu (de día o de noche), no en pacífico. */
	public static boolean puedeAparecer(EntityType<DactyloBebeEntity> tipo, ServerLevelAccessor mundo, MobSpawnType razon,
										BlockPos pos, RandomSource random) {
		return mundo.getDifficulty() != Difficulty.PEACEFUL;
	}

	/** Al aparecer solo, sube al cielo (entre 8 y 16 bloques sobre el suelo), si hay lugar. */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor mundo, DifficultyInstance dificultad, MobSpawnType razon,
										@Nullable SpawnGroupData datos) {
		if (razon == MobSpawnType.NATURAL || razon == MobSpawnType.CHUNK_GENERATION) {
			int subir = 8 + random.nextInt(9);
			BlockPos arriba = blockPosition();
			for (int i = 0; i < subir && mundo.getBlockState(arriba.above()).isAir() && mundo.getBlockState(arriba.above(2)).isAir(); i++) {
				arriba = arriba.above();
			}
			moveTo(arriba.getX() + 0.5, arriba.getY(), arriba.getZ() + 0.5, getYRot(), 0);
		}
		return super.finalizeSpawn(mundo, dificultad, razon, datos);
	}

	/** Vuela a unos 8 bloques del jugador y 4 más arriba, y cada 4 segundos le escupe tinta. */
	private static class DispararGoal extends Goal {
		private final DactyloBebeEntity dactylo;
		private int espera = 40;

		DispararGoal(DactyloBebeEntity dactylo) {
			this.dactylo = dactylo;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity objetivo = dactylo.getTarget();
			return objetivo != null && objetivo.isAlive() && dactylo.distanceToSqr(objetivo) < 32 * 32;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity objetivo = dactylo.getTarget();
			if (objetivo == null) return;
			dactylo.getLookControl().setLookAt(objetivo, 30, 30);
			Vec3 desde = dactylo.position().subtract(objetivo.position()).multiply(1, 0, 1);
			if (desde.lengthSqr() < 0.01) desde = new Vec3(1, 0, 0);
			Vec3 lugar = objetivo.position().add(desde.normalize().scale(8)).add(0, 4, 0);
			dactylo.getMoveControl().setWantedPosition(lugar.x, lugar.y, lugar.z, 1.0);
			if (--espera <= 0 && dactylo.distanceToSqr(objetivo) < 20 * 20 && dactylo.hasLineOfSight(objetivo)) {
				dactylo.disparar(objetivo);
				espera = ESPERA_DISPARO;
			}
		}
	}
}
