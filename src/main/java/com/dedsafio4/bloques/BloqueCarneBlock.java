package com.dedsafio4.bloques;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bloque de Carne: un trozo de carne chiquito, más bajo que una losa y más angosto (12 x 6 x 12 píxeles). */
public class BloqueCarneBlock extends Block {
	private static final VoxelShape FORMA = Block.box(2, 0, 2, 14, 6, 14);

	public BloqueCarneBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}
}
