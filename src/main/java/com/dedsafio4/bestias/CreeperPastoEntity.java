package com.dedsafio4.bestias;

import com.dedsafio4.cambios.Cambios;
import com.dedsafio4.nave.ModEntidades;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creeper de Pasto: un creeper chiquito (1,5 veces 1/4 de uno normal), hecho de pasto. Se porta como un
 * creeper: te persigue, se infla y explota. Con "/cambio creepers_pasto 1" sale del pasto que se rompe.
 */
public class CreeperPastoEntity extends Creeper {
	/** Probabilidad de que salga uno al romper pasto (o en cada explosión que rompe pasto). */
	public static final float PROBABILIDAD = 0.33f;

	public CreeperPastoEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
	}

	/** Pasto, pasto alto o bloque de pasto. */
	public static boolean esPasto(BlockState estado) {
		return estado.is(Blocks.GRASS_BLOCK) || estado.is(Blocks.SHORT_GRASS) || estado.is(Blocks.TALL_GRASS);
	}

	/** Si el cambio está activo, con 33% hace salir un Creeper de Pasto en esa posición. */
	public static void quizasAparecer(ServerLevel mundo, BlockPos pos) {
		if (Cambios.nivel(mundo.getServer(), Cambios.CREEPERS_PASTO) < 1) return;
		if (mundo.getRandom().nextFloat() >= PROBABILIDAD) return;
		ModEntidades.CREEPER_PASTO.spawn(mundo, pos, MobSpawnType.EVENT);
	}
}
