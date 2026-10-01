package com.dedsafio4.client.despegue;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.despegue.NaveViajeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * La nave de Patricio (geo/entity/nave_viaje.geo.json, con la textura de las partes de la nave). El hueso "raiz"
 * envuelve a toda la nave y se gira 90° para que la punta mire al cielo y el motor quede apoyado en la plataforma.
 * Durante la cuenta regresiva tiembla, cada vez más.
 */
public class NaveViajeModelo extends GeoModel<NaveViajeEntity> {
	private static final ResourceLocation MODELO =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/entity/nave_viaje.geo.json");
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/item/nave.png");
	private static final ResourceLocation ANIMACIONES =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/entity/nave_viaje.animation.json");

	@Override
	public ResourceLocation getModelResource(NaveViajeEntity nave) {
		return MODELO;
	}

	@Override
	public ResourceLocation getTextureResource(NaveViajeEntity nave) {
		return TEXTURA;
	}

	@Override
	public ResourceLocation getAnimationResource(NaveViajeEntity nave) {
		return ANIMACIONES;
	}

	@Override
	public void setCustomAnimations(NaveViajeEntity nave, long id, AnimationState<NaveViajeEntity> estado) {
		super.setCustomAnimations(nave, id, estado);
		getBone("raiz").ifPresent(hueso -> {
			hueso.setRotX((float) (Math.PI / 2));
			float temblor = switch (nave.estado()) {
				case NaveViajeEntity.CUENTA -> 0.004f + nave.tickCount % 100 * 0.0002f;
				case NaveViajeEntity.DESPEGANDO -> 0.02f;
				case NaveViajeEntity.ATERRIZANDO -> 0.008f;
				default -> 0f;
			};
			float t = nave.tickCount + estado.getPartialTick();
			hueso.setRotZ((float) Math.sin(t * 2.3) * temblor);
			hueso.setRotY((float) Math.cos(t * 1.9) * temblor);
		});
	}
}
