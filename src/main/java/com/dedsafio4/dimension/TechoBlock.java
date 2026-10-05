package com.dedsafio4.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * La Roca del Techo del Centro de Quiu: el techo de piedra que tapa el cielo de la dimensión, como una cueva
 * gigante. Se ve como piedra, pero deja pasar la luz del día: abajo todo sigue iluminado (y los bichos aparecen
 * igual que antes). No se puede romper.
 */
public class TechoBlock extends Block {
	public TechoBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState estado, BlockGetter mundo, BlockPos pos) {
		return true;
	}

	@Override
	protected int getLightBlock(BlockState estado, BlockGetter mundo, BlockPos pos) {
		return 0;
	}
}
