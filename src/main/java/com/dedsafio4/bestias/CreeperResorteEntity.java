package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * Creeper Resorte: una cabeza de creeper sobre un resorte. Aparece de día en el Overworld. Su explosión no rompe
 * nada: a los jugadores que estén cerca los lanza hacia arriba hasta una gran altura (unos 50 bloques).
 */
public class CreeperResorteEntity extends Creeper {
	private static final float POTENCIA = 1.5f, ALCANCE = 5;
	/** Con cuánta fuerza salen para arriba. */
	private static final double IMPULSO = 3.5;

	public CreeperResorteEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
		// La explosión del creeper queda en 0: la de verdad (chiquita y sin romper nada) la hace tick().
		CompoundTag datos = new CompoundTag();
		datos.putByte("ExplosionRadius", (byte) 0);
		super.readAdditionalSaveData(datos);
	}

	/** Con sol también le sirve el lugar (a los monstruos normales la luz no los deja aparecer). */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader mundo) {
		return 0;
	}

	@Override
	public void tick() {
		super.tick();
		// Al explotar queda muerto y descartado en el mismo tick (si muere peleando, lo quitan como KILLED).
		if (!level().isClientSide && dead && getRemovalReason() == RemovalReason.DISCARDED && level() instanceof ServerLevel mundo) {
			mundo.explode(this, getX(), getY(), getZ(), POTENCIA, Level.ExplosionInteraction.NONE);
			mundo.playSound(null, blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 2f, 1.4f);
			for (ServerPlayer jugador : mundo.players()) {
				if (jugador.isCreative() || jugador.isSpectator() || !jugador.isAlive()) continue;
				if (jugador.distanceToSqr(this) > ALCANCE * ALCANCE) continue;
				jugador.setDeltaMovement(jugador.getDeltaMovement().x, IMPULSO, jugador.getDeltaMovement().z);
				jugador.hurtMarked = true;
			}
		}
	}
}
