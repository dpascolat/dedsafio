package com.dedsafio4.client.bombas;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bombas.BombaNegraEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** La Bomba Negra con el modelo "bomba" del Dedsafío 3 (geo/entity/bomba_negra.geo.json) y sus animaciones. */
public class BombaNegraRenderer extends GeoEntityRenderer<BombaNegraEntity> {
	public BombaNegraRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo());
		this.shadowRadius = 0.5f;
	}

	static class Modelo extends GeoModel<BombaNegraEntity> {
		private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/entity/bomba_negra.geo.json");
		private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/bomba_negra.png");
		private static final ResourceLocation ANIMACIONES = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/entity/bomba_negra.animation.json");

		@Override
		public ResourceLocation getModelResource(BombaNegraEntity bomba) {
			return MODELO;
		}

		@Override
		public ResourceLocation getTextureResource(BombaNegraEntity bomba) {
			return TEXTURA;
		}

		@Override
		public ResourceLocation getAnimationResource(BombaNegraEntity bomba) {
			return ANIMACIONES;
		}
	}
}
