package com.dedsafio4.client.nave;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.nave.NaveEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja la nave con su modelo de colores (la textura es blanca: el color va en el modelo). */
public class NaveRenderer extends MobRenderer<NaveEntity, NaveModelo> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/blanco.png");

	public NaveRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new NaveModelo(), 0.6f);
	}

	@Override
	public ResourceLocation getTextureLocation(NaveEntity nave) {
		return TEXTURA;
	}
}
