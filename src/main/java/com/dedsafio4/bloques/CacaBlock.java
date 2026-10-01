package com.dedsafio4.bloques;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * CACA: se cae como la arena y se rompe igual de rápido. Ocupa media altura,
 * como una losa, aunque el montoncito de arriba se vea más alto.
 */
public class CacaBlock extends FallingBlock {
	public static final MapCodec<CacaBlock> CODEC = simpleCodec(CacaBlock::new);
	/** Media altura, como una losa. */
	private static final VoxelShape FORMA = box(0, 0, 0, 16, 8, 16);

	public CacaBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public MapCodec<? extends FallingBlock> codec() {
		return CODEC;
	}

	@Override
	public VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	/** El color del polvillo que deja al caer. */
	@Override
	public int getDustColor(BlockState estado, BlockGetter level, BlockPos pos) {
		return 0xFF331F12;
	}
}
