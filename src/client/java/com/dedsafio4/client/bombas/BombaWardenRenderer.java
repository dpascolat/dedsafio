package com.dedsafio4.client.bombas;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bombas.BombaWardenEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja la Bomba Warden (la textura es blanca: el color va en cada pieza del modelo). */
public class BombaWardenRenderer extends MobRenderer<BombaWardenEntity, BombaWardenModelo> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "bomba_warden"), "main");

	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/blanco.png");

	public BombaWardenRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new BombaWardenModelo(contexto.bakeLayer(CAPA)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(BombaWardenEntity bomba) {
		return TEXTURA;
	}
}
