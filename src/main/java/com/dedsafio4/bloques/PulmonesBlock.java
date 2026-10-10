package com.dedsafio4.bloques;

import com.mojang.serialization.MapCodec;
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

/**
 * Pulmones: un arbolito rosa de 2 bloques de alto (el diseño "Árbol rosa", vóxel por vóxel) que crece en las minas de
 * Gleba. Se atraviesa como una planta y necesita piso firme abajo. Solo suelta algo si lo rompes con una espada
 * (loot_table/blocks/pulmones.json).
 */
public class PulmonesBlock extends Block {
	public static final MapCodec<PulmonesBlock> CODEC = simpleCodec(PulmonesBlock::new);
	/** El contorno: el tronco abajo y la copa arriba (2 bloques de alto). */
	private static final VoxelShape FORMA = Shapes.or(Block.box(5, 0, 5, 11, 12, 11), Block.box(0, 12, 2, 16, 32, 16));

	public PulmonesBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return Shapes.empty();
	}

	@Override
	protected boolean canSurvive(BlockState estado, LevelReader level, BlockPos pos) {
		BlockPos abajo = pos.below();
		return level.getBlockState(abajo).isFaceSturdy(level, abajo, Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState estado, Direction direccion, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos vecinoPos) {
		if (direccion == Direction.DOWN && !canSurvive(estado, level, pos)) return Blocks.AIR.defaultBlockState();
		return super.updateShape(estado, direccion, vecino, level, pos, vecinoPos);
	}
}
