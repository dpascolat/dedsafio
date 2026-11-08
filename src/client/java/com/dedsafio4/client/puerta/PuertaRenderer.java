package com.dedsafio4.client.puerta;

import com.dedsafio4.puerta.PuertaEntity;
import com.dedsafio4.puerta.PuertaRastrillo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Dibuja la reja de la Puerta Rastrillo mientras sube o baja: cada bloque de piedra negra pulida, en su lugar. */
public class PuertaRenderer extends EntityRenderer<PuertaEntity> {
	private static final java.util.List<int[]> CELDAS = PuertaRastrillo.celdas();

	public PuertaRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 0;
	}

	@Override
	public void render(PuertaEntity puerta, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		BlockRenderDispatcher bloques = Minecraft.getInstance().getBlockRenderer();
		Direction d = puerta.derecha();
		float altura = puerta.altura(parcial), temblor = puerta.temblor(parcial);
		BlockPos origen = puerta.blockPosition();
		var mundo = puerta.level();
		for (int[] c : CELDAS) {
			float x = d.getStepX() * (c[0] + temblor), z = d.getStepZ() * (c[0] + temblor), y = c[1] + altura;
			int luzBloque = LevelRenderer.getLightColor(mundo, origen.offset(Math.round(x), Math.round(y), Math.round(z)));
			pose.pushPose();
			pose.translate(x, y, z);
			bloques.renderSingleBlock(PuertaRastrillo.BLOQUE, pose, buffers, luzBloque, OverlayTexture.NO_OVERLAY);
			pose.popPose();
		}
		super.render(puerta, giro, parcial, pose, buffers, luz);
	}

	@Override
	public boolean shouldRender(PuertaEntity puerta, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public ResourceLocation getTextureLocation(PuertaEntity puerta) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
