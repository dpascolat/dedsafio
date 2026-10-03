package com.dedsafio4.bestias;

import com.dedsafio4.nave.ModEntidades;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * Creeper Raíz de Azalea: un bloque de hojas de azalea con cara de creeper, una flor arriba y 4 patitas
 * (1,5 bloques de alto con la flor). Se porta como un creeper (al activarse la flor se cierra), pero la
 * explosión es chica y no rompe bloques: a los jugadores que estén a menos de 4 bloques los atrapa con
 * raíces (RaicesEntity), de las que tienen que librarse haciendo click rápido.
 */
public class CreeperAzaleaEntity extends Creeper {
	private static final float POTENCIA = 1.5f, ALCANCE = 4;

	public CreeperAzaleaEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
		// La explosión del creeper queda en 0: la de verdad (chiquita y sin romper nada) la hace tick().
		CompoundTag datos = new CompoundTag();
		datos.putByte("ExplosionRadius", (byte) 0);
		super.readAdditionalSaveData(datos);
	}

	@Override
	public void tick() {
		super.tick();
		// Al explotar queda muerto y descartado en el mismo tick (si muere peleando, lo quitan como KILLED).
		if (!level().isClientSide && dead && getRemovalReason() == RemovalReason.DISCARDED && level() instanceof ServerLevel mundo) {
			mundo.explode(this, getX(), getY(), getZ(), POTENCIA, Level.ExplosionInteraction.NONE);
			for (ServerPlayer jugador : mundo.players()) {
				if (jugador.isCreative() || jugador.isSpectator() || !jugador.isAlive()) continue;
				if (jugador.distanceToSqr(this) > ALCANCE * ALCANCE || RaicesEntity.atrapado(jugador)) continue;
				RaicesEntity raices = ModEntidades.RAICES.create(mundo);
				if (raices == null) continue;
				raices.atrapar(jugador);
				mundo.addFreshEntity(raices);
			}
		}
	}
}
