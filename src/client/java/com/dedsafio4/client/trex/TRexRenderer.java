package com.dedsafio4.client.trex;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.trex.TRexEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * El T-Rex gigante: el modelo del Tremorsaurio (geo/tremorsaurio.geo.json, con sus animaciones de quieto, caminar y
 * rugir) agrandado 8,5 veces (la cabeza queda a unos 35 bloques), con su textura naranja (textures/entity/trex.png).
 * Para el que lo maneja en primera persona la cabeza no se dibuja (si no, vería el cráneo por dentro).
 */
public class TRexRenderer extends GeoEntityRenderer<TRexEntity> {
	public static final float ESCALA = 8.5f;

	public TRexRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo());
		this.withScale(ESCALA);
		this.shadowRadius = 6f;
	}

	/** Mide mucho más que su caja de choque: se dibuja mientras cualquier parte esté a la vista. */
	@Override
	public boolean shouldRender(TRexEntity trex, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(trex.getBoundingBox().inflate(20, 25, 40));
	}

	public static class Modelo extends GeoModel<TRexEntity> {
		private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/tremorsaurio.geo.json");
		private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/trex.png");
		private static final ResourceLocation ANIMACIONES = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/tremorsaurio.animation.json");

		@Override
		public ResourceLocation getModelResource(TRexEntity trex) {
			return MODELO;
		}

		@Override
		public ResourceLocation getTextureResource(TRexEntity trex) {
			return TEXTURA;
		}

		@Override
		public ResourceLocation getAnimationResource(TRexEntity trex) {
			return ANIMACIONES;
		}

		@Override
		public void setCustomAnimations(TRexEntity trex, long id, AnimationState<TRexEntity> estado) {
			super.setCustomAnimations(trex, id, estado);
			var lentes = getAnimationProcessor().getBone("glasses");
			if (lentes != null) lentes.setHidden(true);
			var cabeza = getAnimationProcessor().getBone("head");
			Minecraft mc = Minecraft.getInstance();
			boolean manejando = mc.player != null && mc.player.getVehicle() == trex && mc.options.getCameraType().isFirstPerson();
			if (cabeza != null) cabeza.setHidden(manejando);
		}
	}
}
