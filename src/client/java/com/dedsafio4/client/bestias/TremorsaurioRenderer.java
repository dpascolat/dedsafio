package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.TremorsaurioEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Dibuja el Tremorsaurio con GeckoLib: el modelo del Tremorsaurus de Alex's Caves (geo/tremorsaurio.geo.json, pasado
 * de su modelo de Citadel), un poco más grande (1,1, como allá), con su piel (normal, retro, tectónica o la de la
 * princesa, que además lleva lentes).
 */
public class TremorsaurioRenderer extends GeoEntityRenderer<TremorsaurioEntity> {
	public TremorsaurioRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo());
		this.withScale(1.1f);
		this.shadowRadius = 1.6f;
	}

	public static class Modelo extends GeoModel<TremorsaurioEntity> {
		private static final ResourceLocation MODELO = id("geo/tremorsaurio.geo.json");
		private static final ResourceLocation ANIMACIONES = id("animations/tremorsaurio.animation.json");
		private static final ResourceLocation[] PIELES = {id("textures/entity/tremorsaurio.png"),
				id("textures/entity/tremorsaurio_retro.png"), id("textures/entity/tremorsaurio_tectonic.png")};
		private static final ResourceLocation PRINCESA = id("textures/entity/tremorsaurio_princess.png");

		private static ResourceLocation id(String camino) {
			return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, camino);
		}

		@Override
		public ResourceLocation getModelResource(TremorsaurioEntity t) {
			return MODELO;
		}

		@Override
		public ResourceLocation getTextureResource(TremorsaurioEntity t) {
			if (t.princesa()) return PRINCESA;
			int p = t.piel();
			return PIELES[p >= 0 && p < PIELES.length ? p : 0];
		}

		@Override
		public ResourceLocation getAnimationResource(TremorsaurioEntity t) {
			return ANIMACIONES;
		}

		/** Los lentes sólo los lleva la princesa; y la cabeza sigue un poco a donde mira. */
		@Override
		public void setCustomAnimations(TremorsaurioEntity t, long id, AnimationState<TremorsaurioEntity> estado) {
			super.setCustomAnimations(t, id, estado);
			var lentes = getAnimationProcessor().getBone("glasses");
			if (lentes != null) lentes.setHidden(!t.princesa());
			var cuello = getAnimationProcessor().getBone("neck");
			if (cuello != null) {
				var datos = estado.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
				if (datos != null) cuello.setRotY(cuello.getRotY() + datos.netHeadYaw() * net.minecraft.util.Mth.DEG_TO_RAD * 0.5f);
			}
		}
	}
}
