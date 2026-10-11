package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.HornetEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Dibuja a Hornet con GeckoLib (geo/hornet.geo.json, textures/entity/hornet.png, animations/hornet.animation.json). */
public class HornetRenderer extends GeoEntityRenderer<HornetEntity> {
	public HornetRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo());
		this.shadowRadius = 0.4f;
	}

	public static class Modelo extends GeoModel<HornetEntity> {
		private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/hornet.geo.json");
		private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/hornet.png");
		private static final ResourceLocation ANIMACIONES = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/hornet.animation.json");

		@Override
		public ResourceLocation getModelResource(HornetEntity hornet) {
			return MODELO;
		}

		@Override
		public ResourceLocation getTextureResource(HornetEntity hornet) {
			return TEXTURA;
		}

		@Override
		public ResourceLocation getAnimationResource(HornetEntity hornet) {
			return ANIMACIONES;
		}
	}
}
