package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;

/** Dibuja el Creeper Amarillo con el modelo del creeper de siempre y su textura amarilla clarita. */
public class CreeperAmarilloRenderer extends CreeperRenderer {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_amarillo.png");

	public CreeperAmarilloRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public ResourceLocation getTextureLocation(Creeper creeper) {
		return TEXTURA;
	}
}
