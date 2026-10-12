package com.dedsafio4.bloques;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La Cuerda: un bloque finito (4×4 píxeles en el medio, todo el alto) que se trepa como una escalera de mano
 * (está en el tag minecraft:climbable): manteniendo el espacio sube a la misma velocidad, y agachado te quedas
 * agarrado. Se pasa a través (no choca).
 */
public class CuerdaBlock extends Block {
	public static final MapCodec<CuerdaBlock> CODEC = simpleCodec(CuerdaBlock::new);
	private static final VoxelShape FORMA = Block.box(6, 0, 6, 10, 16, 10);

	public CuerdaBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}
}
