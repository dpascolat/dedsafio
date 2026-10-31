package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * Vindicador: un Vindicador rojizo que aparece en el Limbo. Si le pega a un jugador que se cubre, le deshabilita el
 * escudo por 5 segundos (siempre, no como el hacha). Suelta una Manzana de Oro (loot_table/entities/vindicador.json).
 */
public class VindicadorEntity extends Vindicator {
	public VindicadorEntity(EntityType<? extends Vindicator> tipo, Level mundo) {
		super(tipo, mundo);
	}

	/** Le sirve cualquier lugar, con cualquier luz. */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader mundo) {
		return 0;
	}

	/** Nunca sale de líder: sin estandarte ni Mal Presagio. */
	@Override
	public boolean canBeLeader() {
		return false;
	}

	@Override
	public boolean doHurtTarget(Entity objetivo) {
		boolean cubierto = objetivo instanceof Player jugador && jugador.isBlocking();
		boolean pego = super.doHurtTarget(objetivo);
		if (cubierto && objetivo instanceof Player jugador) jugador.disableShield();
		return pego;
	}
}
