package com.dedsafio4.client.lagartos;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.lagartos.LagartoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja a los lagartos (la textura es blanca: el color va en cada pieza del modelo). */
public class LagartoRenderer extends MobRenderer<LagartoEntity, LagartoModelo> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/blanco.png");

	public LagartoRenderer(EntityRendererProvider.Context contexto, LagartoEntity.Variante variante) {
		super(contexto, new LagartoModelo(variante), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(LagartoEntity lagarto) {
		return TEXTURA;
	}
}
