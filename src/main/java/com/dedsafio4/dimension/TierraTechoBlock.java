package com.dedsafio4.dimension;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * La Tierra del Techo del Centro de Quiu: como la Roca del Techo (deja pasar la luz y no se rompe), pero se ve como
 * algún tipo de tierra ({@link #TIPOS}), mezclados en manchas.
 */
public class TierraTechoBlock extends TechoBlock {
	/** Las texturas de Minecraft de cada tipo, en el orden del número {@code tipo}. */
	public static final String[] TIPOS = {"dirt", "coarse_dirt", "rooted_dirt", "podzol_top", "mycelium_top", "mud", "packed_mud"};
	public static final IntegerProperty TIPO = IntegerProperty.create("tipo", 0, TIPOS.length - 1);

	public TierraTechoBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any().setValue(TIPO, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TIPO);
	}
}
