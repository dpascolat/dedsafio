package com.dedsafio4.client.demonios;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.demonios.MallaMobEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Los Demonios (Rosa, Morado y Rojo): el demonio encadenado hecho de cubos (geo/entity/demonio.geo.json), cada uno
 * con su textura. Flotan meciéndose arriba y abajo. GeckoLib ya los gira y les aplica el tamaño de /boss tamano.
 */
public class DemonioRenderer extends GeoEntityRenderer<MallaMobEntity> {
	public DemonioRenderer(EntityRendererProvider.Context contexto, String textura) {
		super(contexto, new Modelo(textura));
		this.shadowRadius = 0.8f;
	}

	@Override
	public void render(MallaMobEntity mob, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		if (mob.flota()) pose.translate(0, (0.25f + Mth.sin((mob.tickCount + parcial) * 0.08f) * 0.15f) * mob.getScale(), 0);
		super.render(mob, giro, parcial, pose, buffers, luz);
		pose.popPose();
	}

	static class Modelo extends GeoModel<MallaMobEntity> {
		private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/entity/demonio.geo.json");
		private static final ResourceLocation ANIMACIONES = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/entity/demonio.animation.json");
		private final ResourceLocation textura;

		Modelo(String textura) {
			this.textura = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/" + textura + ".png");
		}

		@Override
		public ResourceLocation getModelResource(MallaMobEntity mob) {
			return MODELO;
		}

		@Override
		public ResourceLocation getTextureResource(MallaMobEntity mob) {
			return textura;
		}

		@Override
		public ResourceLocation getAnimationResource(MallaMobEntity mob) {
			return ANIMACIONES;
		}
	}
}
