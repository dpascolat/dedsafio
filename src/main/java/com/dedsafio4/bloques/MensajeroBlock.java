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
 * Mensajero: el modelo de Patricio (Mensajero.bbmodel). Sólo se pone en una pared, como las antorchas de pared: queda
 * pegado con la espalda a la pared y la pantalla mirando para afuera. Si se rompe la pared, se cae.
 * FACING es hacia dónde mira la pantalla (la pared está del lado contrario).
 */
public class MensajeroBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<MensajeroBlock> CODEC = simpleCodec(MensajeroBlock::new);
	private static final VoxelShape OESTE = Block.box(8, 0, 1, 16, 16, 15);
	private static final VoxelShape NORTE = Block.box(1, 0, 8, 15, 16, 16);
	private static final VoxelShape ESTE = Block.box(0, 0, 1, 8, 16, 15);
	private static final VoxelShape SUR = Block.box(1, 0, 0, 15, 16, 8);

	public MensajeroBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return switch (estado.getValue(FACING)) {
			case WEST -> OESTE;
			case EAST -> ESTE;
			case SOUTH -> SUR;
			default -> NORTE;
		};
	}

	/** Busca una pared para pegarse, empezando por la que el jugador está mirando. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		for (Direction direccion : contexto.getNearestLookingDirections()) {
			if (!direccion.getAxis().isHorizontal()) continue;
			BlockState estado = defaultBlockState().setValue(FACING, direccion.getOpposite());
			if (estado.canSurvive(contexto.getLevel(), contexto.getClickedPos())) return estado;
		}
		return null;
	}

	/** Sólo en una pared firme (detrás de la espalda). */
	@Override
	protected boolean canSurvive(BlockState estado, LevelReader level, BlockPos pos) {
		Direction frente = estado.getValue(FACING);
		BlockPos pared = pos.relative(frente.getOpposite());
		return level.getBlockState(pared).isFaceSturdy(level, pared, frente);
	}

	@Override
	protected BlockState updateShape(BlockState estado, Direction direccion, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos vecinoPos) {
		if (direccion == estado.getValue(FACING).getOpposite() && !estado.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
		return super.updateShape(estado, direccion, vecino, level, pos, vecinoPos);
	}
}
