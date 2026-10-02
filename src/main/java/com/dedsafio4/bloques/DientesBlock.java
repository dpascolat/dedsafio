package com.dedsafio4.bloques;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Dientes: tres dientes incrustados en el piso (de distinto alto). Va apoyado sobre un bloque sólido. */
public class DientesBlock extends Block {
	private static final VoxelShape FORMA = Shapes.or(
			Block.box(3, 0, 3, 6, 9, 6),
			Block.box(9, 0, 4, 12, 6.5, 7),
			Block.box(6, 0, 9, 9, 11, 12));

	public DientesBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	protected boolean canSurvive(BlockState estado, LevelReader mundo, BlockPos pos) {
		return mundo.getBlockState(pos.below()).isFaceSturdy(mundo, pos.below(), Direction.UP);
	}

	/** Si se rompe el piso de abajo, se rompen los dientes. */
	@Override
	protected BlockState updateShape(BlockState estado, Direction direccion, BlockState vecino, LevelAccessor mundo, BlockPos pos, BlockPos vecinoPos) {
		return direccion == Direction.DOWN && !canSurvive(estado, mundo, pos) ? Blocks.AIR.defaultBlockState()
				: super.updateShape(estado, direccion, vecino, mundo, pos, vecinoPos);
	}
}
