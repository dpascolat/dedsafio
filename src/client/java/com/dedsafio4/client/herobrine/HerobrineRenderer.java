package com.dedsafio4.client.herobrine;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.herobrine.HerobrineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/**
 * Herobrine: la skin clásica (la de Steve de Minecraft) con los ojos blancos que brillan. Cuando lo
 * alumbran con la Linterna se va poniendo rojo.
 */
public class HerobrineRenderer extends MobRenderer<HerobrineEntity, HerobrineRenderer.Modelo> {
	private static final ResourceLocation SKIN = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
	private static final ResourceLocation OJOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/herobrine_ojos.png");

	public HerobrineRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo(contexto.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		addLayer(new Ojos(this));
	}

	@Override
	public ResourceLocation getTextureLocation(HerobrineEntity herobrine) {
		return SKIN;
	}

	/** El modelo de jugador, teñido de rojo según qué tanto lo alumbraron. */
	public static class Modelo extends PlayerModel<HerobrineEntity> {
		private float rojo;

		Modelo(ModelPart raiz) {
			super(raiz, false);
		}

		@Override
		public void setupAnim(HerobrineEntity herobrine, float paso, float cuanto, float edad, float yaw, float pitch) {
			rojo = herobrine.rojo();
			super.setupAnim(herobrine, paso, cuanto, edad, yaw, pitch);
		}

		@Override
		public void renderToBuffer(PoseStack pose, VertexConsumer vc, int luz, int overlay, int color) {
			int resto = (int) (255 * (1 - 0.85f * rojo));
			int tinte = FastColor.ARGB32.color(FastColor.ARGB32.alpha(color), FastColor.ARGB32.red(color),
					FastColor.ARGB32.green(color) * resto / 255, FastColor.ARGB32.blue(color) * resto / 255);
			super.renderToBuffer(pose, vc, luz, overlay, tinte);
		}
	}

	/** Los ojos blancos, que brillan en la oscuridad. */
	private static class Ojos extends EyesLayer<HerobrineEntity, Modelo> {
		Ojos(RenderLayerParent<HerobrineEntity, Modelo> padre) {
			super(padre);
		}

		@Override
		public RenderType renderType() {
			return RenderType.eyes(OJOS);
		}
	}
}
