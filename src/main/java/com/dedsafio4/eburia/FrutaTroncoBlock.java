package com.dedsafio4.eburia;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Frutas que crecen pegadas al costado del tronco como el Huevo Eburia (misma lógica: tres etapas
 * y se pudren si no las cosechás), con harina de hueso: el Huevo Eburia, la Fruta Solaria, la Baya Uvina
 * y el Arándano Nocturno.
 */
public class FrutaTroncoBlock extends HuevoEburiaBlock implements BonemealableBlock {
	public static final MapCodec<FrutaTroncoBlock> CODEC = simpleCodec(FrutaTroncoBlock::new);

	/** Cajas del diseño de la Fruta Solaria (con el tronco al sur), más el tallo que llega al tronco. */
	public static final VoxelShape[] FORMAS_SOLARIA = {
			Block.box(5, 8, 13, 12, 14, 16),
			Block.box(3, 4, 11, 13, 15, 16),
			Block.box(1, 1, 9, 15, 15, 16),
			Block.box(1, 1, 9, 15, 15, 16)
	};
	/** Las del Huevo Eburia (el nido). */
	public static final VoxelShape[] FORMAS_HUEVO = {
			Block.box(4, 8, 13, 11, 16, 16),
			Block.box(2, 4, 11, 14, 16, 16),
			Block.box(0, 0, 9, 16, 16, 16),
			Block.box(0, 0, 9, 16, 16, 16)
	};
	/** Las del Arándano Nocturno. */
	public static final VoxelShape[] FORMAS_ARANDANO = {
			Block.box(2, 8, 13, 8, 13, 16),
			Block.box(2, 5, 11, 12, 14, 16),
			Block.box(2, 2, 9, 15, 15, 16),
			Block.box(2, 2, 9, 15, 15, 16)
	};
	/** Las de la Baya Uvina. */
	public static final VoxelShape[] FORMAS_UVINA = {
			Block.box(3, 8, 13, 10, 14, 16),
			Block.box(2, 4, 11, 12, 15, 16),
			Block.box(2, 1, 9, 15, 15, 16),
			Block.box(2, 1, 9, 15, 15, 16)
	};

	private final VoxelShape[] formas;

	public FrutaTroncoBlock(Properties propiedades) {
		this(propiedades, FORMAS_SOLARIA);
	}

	public FrutaTroncoBlock(Properties propiedades, VoxelShape[] formas) {
		super(propiedades);
		this.formas = formas;
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		VoxelShape forma = formas[estado.getValue(EDAD)];
		return switch (estado.getValue(FACING)) {
			case SOUTH -> rotar(forma, 180);
			case EAST -> rotar(forma, 90);
			case WEST -> rotar(forma, 270);
			default -> forma;
		};
	}

	// Harina de hueso: sube una etapa (no la madura de más ni la pudre).
	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState estado) {
		return estado.getValue(EDAD) < MADURO;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource azar, BlockPos pos, BlockState estado) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource azar, BlockPos pos, BlockState estado) {
		level.setBlock(pos, estado.setValue(EDAD, Math.min(MADURO, estado.getValue(EDAD) + 1)), 2);
	}
}
