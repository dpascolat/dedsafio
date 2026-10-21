package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.ZombieDedsafioEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Map;

/**
 * Los zombis nuevos: el modelo cem/zombie_dedsafio.jem (mandíbula que se mueve, dientes y dedos) sobre el zombi de
 * Minecraft, con la piel de su tipo y encima una de las 15 camisas y uno de los 3 pantalones (siempre los mismos para
 * cada zombi). Llevan armadura y lo que tengan en la mano como un zombi normal.
 */
public class ZombieDedsafioRenderer extends HumanoidMobRenderer<ZombieDedsafioEntity, ZombieDedsafioRenderer.Modelo> {
	private static final int CAMISAS = 15, PANTALONES = 3;

	private static ResourceLocation tex(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/zombie_dedsafio/" + nombre + ".png");
	}

	private static final ResourceLocation[] CAMISA = new ResourceLocation[CAMISAS], PANTALON = new ResourceLocation[PANTALONES];
	static {
		for (int i = 0; i < CAMISAS; i++) CAMISA[i] = tex("camisa_" + (i + 1));
		for (int i = 0; i < PANTALONES; i++) PANTALON[i] = tex("pantalon_" + (i + 1));
	}

	private static final Map<ZombieDedsafioEntity.Tipo, ResourceLocation> PIEL = Map.of(
			ZombieDedsafioEntity.Tipo.COMUN, tex("piel_comun"),
			ZombieDedsafioEntity.Tipo.DESIERTO, tex("piel_desierto"),
			ZombieDedsafioEntity.Tipo.NIEVE, tex("piel_nieve"));

	public static class Modelo extends ZombieModel<ZombieDedsafioEntity> {
		private static final ResourceLocation ARCHIVO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cem/zombie_dedsafio.jem");
		private static final Map<String, String> PARTES = Map.of("head", "head", "headwear", "hat", "body", "body",
				"left_arm", "left_arm", "right_arm", "right_arm", "left_leg", "left_leg", "right_leg", "right_leg");

		private final ModeloJem jem;
		private float edad;

		public Modelo(ModelPart raiz) {
			super(raiz);
			this.jem = new ModeloJem(ARCHIVO, PARTES, raiz);
		}

		@Override
		public void setupAnim(ZombieDedsafioEntity zombi, float paso, float velocidad, float edad, float cabezaY, float cabezaX) {
			super.setupAnim(zombi, paso, velocidad, edad, cabezaY, cabezaX);
			this.edad = edad;
		}

		/** La mandíbula se abre y se cierra despacio. */
		private ModeloJem.Giro giro() {
			float m = Mth.sin(edad / 8f) / 16f;
			return id -> id.equals("lowerjaw") ? new float[]{m, 0, 0} : null;
		}

		@Override
		public void renderToBuffer(PoseStack pose, VertexConsumer vc, int luz, int overlay, int color) {
			jem.dibujar(pose, vc, luz, overlay, color, giro());
		}

		void dibujarCapa(PoseStack pose, VertexConsumer vc, int luz, int overlay, String capa) {
			jem.dibujar(pose, vc, luz, overlay, -1, giro(), capa);
		}
	}

	public ZombieDedsafioRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo(contexto.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
		this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(contexto.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
				new HumanoidArmorModel<>(contexto.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), contexto.getModelManager()));
		// La ropa: camisa y pantalón, cada uno con su textura.
		this.addLayer(new RenderLayer<>(this) {
			@Override
			public void render(PoseStack pose, MultiBufferSource buffers, int luz, ZombieDedsafioEntity zombi, float paso, float velocidad,
							   float parcial, float edad, float cabezaY, float cabezaX) {
				if (zombi.isInvisible()) return;
				int overlay = LivingEntityRenderer.getOverlayCoords(zombi, 0);
				int n = zombi.getUUID().hashCode();
				getParentModel().dibujarCapa(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CAMISA[Math.floorMod(n, CAMISAS)])),
						luz, overlay, "camisa");
				getParentModel().dibujarCapa(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(PANTALON[Math.floorMod(n >> 8, PANTALONES)])),
						luz, overlay, "pantalon");
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(ZombieDedsafioEntity zombi) {
		return PIEL.get(zombi.tipo());
	}
}
