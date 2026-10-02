package com.dedsafio4.dimension;

import com.dedsafio4.bloques.ModBloques;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pasto Rosa: como el pasto de Minecraft, pero crece sobre la carne de la Dimensión de los Órganos. Es rojo oscuro
 * (como carne casi quemada) y más alto que el pasto normal: un bloque y medio (menos que el pasto alto de dos).
 */
public class PastoRosaBlock extends BushBlock {
	public static final MapCodec<PastoRosaBlock> CODEC = simpleCodec(PastoRosaBlock::new);
	private static final VoxelShape FORMA = Block.box(2, 0, 2, 14, 16, 14);

	public PastoRosaBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected MapCodec<? extends BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected boolean mayPlaceOn(BlockState suelo, BlockGetter mundo, BlockPos pos) {
		return suelo.is(ModBloques.CARNE_ROSA) || suelo.is(ModBloques.CARNE_ROJA) || suelo.is(ModBloques.CARNE_CON_VENAS)
				|| super.mayPlaceOn(suelo, mundo, pos);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		net.minecraft.world.phys.Vec3 corrido = estado.getOffset(mundo, pos);
		return FORMA.move(corrido.x, corrido.y, corrido.z);
	}
}
