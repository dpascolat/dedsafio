package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vindicator;

/** Dibuja al Vindicador con el modelo del de siempre y su textura rojiza. */
public class VindicadorRenderer extends VindicatorRenderer {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/vindicador.png");

	public VindicadorRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public ResourceLocation getTextureLocation(Vindicator vindicador) {
		return TEXTURA;
	}
}
