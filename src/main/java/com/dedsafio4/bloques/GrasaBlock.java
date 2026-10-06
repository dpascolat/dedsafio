package com.dedsafio4.bloques;

import com.dedsafio4.nave.ModEntidades;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grasa Líquida y Grasa Coagulada (la gelatina amarilla de Gleba): al romperlas, 30 % de que salga un Gusano de Carne.
 */
public class GrasaBlock extends Block {
	private static final float PROBABILIDAD_GUSANO = 0.3f;

	public GrasaBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected void spawnAfterBreak(BlockState estado, ServerLevel mundo, BlockPos pos, ItemStack herramienta, boolean experiencia) {
		super.spawnAfterBreak(estado, mundo, pos, herramienta, experiencia);
		if (mundo.random.nextFloat() < PROBABILIDAD_GUSANO) {
			ModEntidades.GUSANO_CARNE.spawn(mundo, pos, MobSpawnType.TRIGGERED);
		}
	}
}
