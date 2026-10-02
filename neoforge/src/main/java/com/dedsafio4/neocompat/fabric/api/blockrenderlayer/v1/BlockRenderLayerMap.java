package com.dedsafio4.neocompat.fabric.api.blockrenderlayer.v1;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

public final class BlockRenderLayerMap {
	public static final BlockRenderLayerMap INSTANCE = new BlockRenderLayerMap();

	@SuppressWarnings("deprecation")
	public void putBlock(Block bloque, RenderType capa) {
		ItemBlockRenderTypes.setRenderLayer(bloque, capa);
	}

	public void putBlocks(RenderType capa, Block... bloques) {
		for (Block b : bloques) putBlock(b, capa);
	}
}
