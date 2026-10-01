package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.WalkerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** Le dice a GeckoLib dónde están el modelo, la textura y las animaciones del Walker. */
public class WalkerModelo extends GeoModel<WalkerEntity> {
	private static final ResourceLocation MODELO =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/walker.geo.json");
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/walker.png");
	private static final ResourceLocation ANIMACIONES =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/walker.animation.json");

	@Override
	public ResourceLocation getModelResource(WalkerEntity walker) {
		return MODELO;
	}

	@Override
	public ResourceLocation getTextureResource(WalkerEntity walker) {
		return TEXTURA;
	}

	@Override
	public ResourceLocation getAnimationResource(WalkerEntity walker) {
		return ANIMACIONES;
	}

	/** La montura y los cofres del modelo original no se muestran. */
	@Override
	public void setCustomAnimations(WalkerEntity walker, long id, AnimationState<WalkerEntity> estado) {
		super.setCustomAnimations(walker, id, estado);
		getBone("saddle").ifPresent(hueso -> hueso.setHidden(true));
		getBone("chest").ifPresent(hueso -> hueso.setHidden(true));
	}
}
