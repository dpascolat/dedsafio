package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Creeper Acuático: aparece en el agua del Overworld, respira abajo del agua (tag can_breathe_under_water) y nada
 * hacia su objetivo. Explota como un creeper normal y deja con Náusea a los jugadores que estén cerca.
 */
public class CreeperAcuaticoEntity extends Creeper {
	private static final float ALCANCE = 7;
	/** 30 segundos. */
	private static final int NAUSEA = 30 * 20;
	/** Cuánto se empuja por tick hacia su objetivo. */
	private static final double NADO = 0.04;

	public CreeperAcuaticoEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
		setPathfindingMalus(PathType.WATER, 0);
	}

	/** En el agua, con cualquier luz (fuera de pacífico). */
	public static boolean puedeAparecer(EntityType<CreeperAcuaticoEntity> tipo, ServerLevelAccessor mundo, MobSpawnType razon,
										BlockPos pos, RandomSource azar) {
		return mundo.getDifficulty() != Difficulty.PEACEFUL && mundo.getFluidState(pos).is(FluidTags.WATER);
	}

	/** Minecraft no deja aparecer a los mobs adentro de un líquido, salvo a los que avisan que pueden. */
	@Override
	public boolean checkSpawnObstruction(LevelReader mundo) {
		return mundo.isUnobstructed(this);
	}

	/** Con sol también le sirve el lugar (a los monstruos normales la luz no los deja aparecer). */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader mundo) {
		return 0;
	}

	/** Sin FloatGoal: no se queda flotando arriba, se hunde y nada. */
	@Override
	protected void registerGoals() {
		super.registerGoals();
		goalSelector.getAvailableGoals().removeIf(g -> g.getGoal() instanceof FloatGoal);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		LivingEntity objetivo = getTarget();
		if (level().isClientSide || !isInWater() || objetivo == null || !objetivo.isAlive()) return;
		Vec3 hacia = objetivo.position().subtract(position()).normalize();
		setDeltaMovement(getDeltaMovement().scale(0.8).add(hacia.scale(NADO)));
		getLookControl().setLookAt(objetivo);
	}

	@Override
	public void tick() {
		super.tick();
		// Al explotar queda muerto y descartado en el mismo tick (si muere peleando, lo quitan como KILLED).
		if (!level().isClientSide && dead && getRemovalReason() == RemovalReason.DISCARDED && level() instanceof ServerLevel mundo) {
			for (ServerPlayer jugador : mundo.players()) {
				if (jugador.isCreative() || jugador.isSpectator() || !jugador.isAlive()) continue;
				if (jugador.distanceToSqr(this) > ALCANCE * ALCANCE) continue;
				jugador.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA), this);
			}
		}
	}
}
