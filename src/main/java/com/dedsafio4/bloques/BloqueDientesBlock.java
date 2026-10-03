package com.dedsafio4.bloques;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dentadura Glebanoide: tres dientes sobre una encía (un bloque de largo y un poco menos de un bloque de alto).
 * Queda mirando hacia el que lo pone y va apoyado sobre un bloque sólido.
 */
public class BloqueDientesBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<BloqueDientesBlock> CODEC = simpleCodec(BloqueDientesBlock::new);
	/** El largo de la encía va de lado a lado (norte/sur) o adelante-atrás (este/oeste). */
	private static final VoxelShape A_LO_ANCHO = Block.box(0, 0, 5.5, 16, 14, 10.5);
	private static final VoxelShape A_LO_LARGO = Block.box(5.5, 0, 0, 10.5, 14, 16);

	public BloqueDientesBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
		constructor.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		return defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		return estado.getValue(FACING).getAxis() == Direction.Axis.Z ? A_LO_ANCHO : A_LO_LARGO;
	}

	@Override
	protected boolean canSurvive(BlockState estado, LevelReader mundo, BlockPos pos) {
		return mundo.getBlockState(pos.below()).isFaceSturdy(mundo, pos.below(), Direction.UP);
	}

	/** Si se rompe el piso de abajo, se rompe. */
	@Override
	protected BlockState updateShape(BlockState estado, Direction direccion, BlockState vecino, LevelAccessor mundo, BlockPos pos, BlockPos vecinoPos) {
		return direccion == Direction.DOWN && !canSurvive(estado, mundo, pos) ? Blocks.AIR.defaultBlockState()
				: super.updateShape(estado, direccion, vecino, mundo, pos, vecinoPos);
	}
}
