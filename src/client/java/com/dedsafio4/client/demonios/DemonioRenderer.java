package com.dedsafio4.client.demonios;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.demonios.EspectroEntity;
import com.dedsafio4.demonios.ModDemonios;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Los fantasmas encadenados: los Colosos Prisioneros son el demonio hecho de cubos (geo/entity/demonio.geo.json),
 * agrandado; los Reclusos Espectrales, el fantasma chiquito (geo/entity/recluso.geo.json), achicado. Cada uno con
 * la textura de su color. Flotan meciéndose arriba y abajo. GeckoLib ya los gira y les aplica el tamaño de
 * /boss tamano.
 */
public class DemonioRenderer extends GeoEntityRenderer<EspectroEntity> {
	private final float escala;

	public DemonioRenderer(EntityRendererProvider.Context contexto, String textura, boolean coloso) {
		super(contexto, new Modelo(textura, coloso));
		this.escala = coloso ? ModDemonios.ESCALA_COLOSO : ModDemonios.ESCALA_RECLUSO;
		withScale(escala);
		this.shadowRadius = 0.6f * escala;
	}

	@Override
	public void render(EspectroEntity mob, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		float t = mob.tickCount + parcial;
		pose.translate(0, (0.12f + Mth.sin(t * 0.08f) * 0.1f) * escala * mob.getScale(), 0);
		super.render(mob, giro, parcial, pose, buffers, luz);
		pose.popPose();
	}

	static class Modelo extends GeoModel<EspectroEntity> {
		private static final ResourceLocation ANIMACIONES = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/entity/demonio.animation.json");
		private final ResourceLocation modelo, textura;

		/** Los Colosos usan el demonio grande (demonio.geo.json); los Reclusos, el chiquito (recluso.geo.json). */
		Modelo(String textura, boolean coloso) {
			this.modelo = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, coloso ? "geo/entity/demonio.geo.json" : "geo/entity/recluso.geo.json");
			this.textura = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/" + textura + ".png");
		}

		@Override
		public ResourceLocation getModelResource(EspectroEntity mob) {
			return modelo;
		}

		@Override
		public ResourceLocation getTextureResource(EspectroEntity mob) {
			return textura;
		}

		@Override
		public ResourceLocation getAnimationResource(EspectroEntity mob) {
			return ANIMACIONES;
		}
	}
}
