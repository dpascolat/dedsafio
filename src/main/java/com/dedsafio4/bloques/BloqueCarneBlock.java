package com.dedsafio4.bloques;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bloque de Carne: un trozo de carne chiquito, más bajo que una losa y más angosto (12 x 6 x 12 píxeles). */
public class BloqueCarneBlock extends Block {
	private static final VoxelShape FORMA = Block.box(2, 0, 2, 14, 6, 14);

	public BloqueCarneBlock(Properties propiedades) {
		super(propiedades);
	}

	/** Al romperla un jugador (en supervivencia), a veces sale un Parásito Volador de adentro. */
	@Override
	public void playerDestroy(net.minecraft.world.level.Level mundo, net.minecraft.world.entity.player.Player jugador, BlockPos pos,
							  BlockState estado, net.minecraft.world.level.block.entity.BlockEntity bloqueEntidad, net.minecraft.world.item.ItemStack herramienta) {
		super.playerDestroy(mundo, jugador, pos, estado, bloqueEntidad, herramienta);
		if (mundo instanceof net.minecraft.server.level.ServerLevel servidor) {
			com.dedsafio4.bestias.ParasitoVoladorEntity.salirDeVerruga(servidor, pos, jugador);
		}
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}
}
