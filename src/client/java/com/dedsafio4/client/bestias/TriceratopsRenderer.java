package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.TriceratopsEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Dibuja el Triceratops con GeckoLib (geo/triceratops.geo.json, textures/entity/triceratops.png y sus animaciones,
 * los del mod Fossils and Archeology). El modelo es chico (allá crece con la edad): se dibuja al doble, y además
 * con su tamaño (el atributo scale, que cambia /triceratops escala).
 */
public class TriceratopsRenderer extends GeoEntityRenderer<TriceratopsEntity> {
	public TriceratopsRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo());
		this.withScale(2f);
		this.shadowRadius = 1.4f;
	}

	@Override
	public void scaleModelForRender(float ancho, float alto, PoseStack pose, TriceratopsEntity triceratops, BakedGeoModel modelo,
									boolean otraVez, float parcial, int luz, int overlay) {
		float escala = triceratops.getScale();
		this.shadowRadius = 1.4f * escala;
		super.scaleModelForRender(ancho * escala, alto * escala, pose, triceratops, modelo, otraVez, parcial, luz, overlay);
	}

	public static class Modelo extends GeoModel<TriceratopsEntity> {
		private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/triceratops.geo.json");
		private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/triceratops.png");
		private static final ResourceLocation ANIMACIONES = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/triceratops.animation.json");

		@Override
		public ResourceLocation getModelResource(TriceratopsEntity triceratops) {
			return MODELO;
		}

		@Override
		public ResourceLocation getTextureResource(TriceratopsEntity triceratops) {
			return TEXTURA;
		}

		@Override
		public ResourceLocation getAnimationResource(TriceratopsEntity triceratops) {
			return ANIMACIONES;
		}
	}
}
