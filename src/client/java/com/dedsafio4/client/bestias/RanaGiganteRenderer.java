package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.RanaGiganteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Dibuja la Rana Gigante con GeckoLib (geo/rana_gigante.geo.json, textures/entity/rana_gigante.png y sus animaciones). */
public class RanaGiganteRenderer extends GeoEntityRenderer<RanaGiganteEntity> {
	public RanaGiganteRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo());
		this.shadowRadius = 0.9f;
	}

	public static class Modelo extends GeoModel<RanaGiganteEntity> {
		private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/rana_gigante.geo.json");
		private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/rana_gigante.png");
		private static final ResourceLocation ANIMACIONES = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/rana_gigante.animation.json");

		@Override
		public ResourceLocation getModelResource(RanaGiganteEntity rana) {
			return MODELO;
		}

		@Override
		public ResourceLocation getTextureResource(RanaGiganteEntity rana) {
			return TEXTURA;
		}

		@Override
		public ResourceLocation getAnimationResource(RanaGiganteEntity rana) {
			return ANIMACIONES;
		}
	}
}
