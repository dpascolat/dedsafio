package com.dedsafio4.eburia;

import com.dedsafio4.bloques.ModBloques;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El Huevo Eburia crece pegado al costado de un tronco, como el cacao: pasa por tres etapas y,
 * si no lo cosechás, se pudre. La etapa 3 es la podrida y ya no sirve.
 */
public class HuevoEburiaBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<HuevoEburiaBlock> CODEC = simpleCodec(HuevoEburiaBlock::new);

	/** 0, 1, 2 = creciendo; 3 = podrido. */
	public static final IntegerProperty EDAD = IntegerProperty.create("edad", 0, 3);
	public static final int MADURO = 2, PODRIDO = 3;

	private static final VoxelShape[] FORMAS = {
			Block.box(6, 9, 11, 10, 16, 16),     // brote
			Block.box(5, 6, 9, 11, 16, 16),      // creciendo
			Block.box(4, 4, 4, 12, 12, 16),      // maduro (acostado contra el tronco)
			Block.box(4, 4, 4, 12, 12, 16)       // podrido
	};

	public HuevoEburiaBlock(Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH).setValue(EDAD, 0));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
		constructor.add(FACING, EDAD);
	}

	@Override
	public VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		VoxelShape forma = FORMAS[estado.getValue(EDAD)];
		return switch (estado.getValue(FACING)) {
			case SOUTH -> rotar(forma, 180);
			case EAST -> rotar(forma, 90);
			case WEST -> rotar(forma, 270);
			default -> forma;
		};
	}

	/** Gira la forma para el lado al que mira el huevo. */
	protected static VoxelShape rotar(VoxelShape forma, int grados) {
		var caja = forma.bounds();
		return switch (grados) {
			case 90 -> Block.box(16 - caja.maxZ * 16, caja.minY * 16, caja.minX * 16,
					16 - caja.minZ * 16, caja.maxY * 16, caja.maxX * 16);
			case 180 -> Block.box(16 - caja.maxX * 16, caja.minY * 16, 16 - caja.maxZ * 16,
					16 - caja.minX * 16, caja.maxY * 16, 16 - caja.minZ * 16);
			default -> Block.box(caja.minZ * 16, caja.minY * 16, 16 - caja.maxX * 16,
					caja.maxZ * 16, caja.maxY * 16, 16 - caja.minX * 16);
		};
	}

	/** Solo se sostiene pegado a un tronco. */
	@Override
	public boolean canSurvive(BlockState estado, LevelReader level, BlockPos pos) {
		BlockState tronco = level.getBlockState(pos.relative(estado.getValue(FACING).getOpposite()));
		return tronco.is(ModBloques.ROBLE_CLARO_LOG) || tronco.is(ModBloques.ABETO_CLARO_LOG);
	}

	@Override
	public BlockState updateShape(BlockState estado, Direction lado, BlockState vecino, LevelAccessor level,
								  BlockPos pos, BlockPos posVecino) {
		if (lado == estado.getValue(FACING).getOpposite() && !estado.canSurvive(level, pos)) {
			return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		}
		return estado;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		for (Direction lado : contexto.getNearestLookingDirections()) {
			if (!lado.getAxis().isHorizontal()) continue;
			BlockState estado = this.defaultBlockState().setValue(FACING, lado.getOpposite());
			if (estado.canSurvive(contexto.getLevel(), contexto.getClickedPos())) return estado;
		}
		return null;
	}

	@Override
	public boolean isRandomlyTicking(BlockState estado) {
		return estado.getValue(EDAD) < PODRIDO;
	}

	/** Crece de a poco y, si queda maduro mucho tiempo sin cosechar, se pudre. */
	@Override
	public void randomTick(BlockState estado, ServerLevel level, BlockPos pos, RandomSource azar) {
		int edad = estado.getValue(EDAD);
		if (edad < MADURO) {
			if (azar.nextInt(5) == 0) level.setBlock(pos, estado.setValue(EDAD, edad + 1), 2);
			return;
		}
		// Maduro: cada rato hay una chance de que se pudra.
		if (azar.nextInt(6) == 0) {
			level.setBlock(pos, estado.setValue(EDAD, PODRIDO), 2);
			level.levelEvent(2005, pos, 0);
		}
	}

	public static boolean maduro(BlockState estado) {
		return estado.getValue(EDAD) == MADURO;
	}
}
