package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.SoarerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Le dice a GeckoLib dónde están el modelo, la textura y las animaciones del Soarer. */
public class SoarerModelo extends GeoModel<SoarerEntity> {
	private static final ResourceLocation MODELO =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/soarer.geo.json");
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/soarer.png");
	private static final ResourceLocation ANIMACIONES =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/soarer.animation.json");

	@Override
	public ResourceLocation getModelResource(SoarerEntity soarer) {
		return MODELO;
	}

	@Override
	public ResourceLocation getTextureResource(SoarerEntity soarer) {
		return TEXTURA;
	}

	@Override
	public ResourceLocation getAnimationResource(SoarerEntity soarer) {
		return ANIMACIONES;
	}
}
