package com.dedsafio4.client.demonios;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.demonios.FarolEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** El Farol (geo/entity/farol.geo.json): el farol oscuro de 4 patas hecho de cubos. */
public class FarolRenderer extends GeoEntityRenderer<FarolEntity> {
	public FarolRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo());
		this.shadowRadius = 1f;
	}

	static class Modelo extends GeoModel<FarolEntity> {
		private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/entity/farol.geo.json");
		private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/farol.png");
		private static final ResourceLocation ANIMACIONES = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/entity/demonio.animation.json");

		@Override
		public ResourceLocation getModelResource(FarolEntity farol) {
			return MODELO;
		}

		@Override
		public ResourceLocation getTextureResource(FarolEntity farol) {
			return TEXTURA;
		}

		@Override
		public ResourceLocation getAnimationResource(FarolEntity farol) {
			return ANIMACIONES;
		}
	}
}
