package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;

/** Dibuja el Creeper Acuático con el modelo del creeper de siempre y su textura azul. */
public class CreeperAcuaticoRenderer extends CreeperRenderer {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_acuatico.png");

	public CreeperAcuaticoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public ResourceLocation getTextureLocation(Creeper creeper) {
		return TEXTURA;
	}
}
