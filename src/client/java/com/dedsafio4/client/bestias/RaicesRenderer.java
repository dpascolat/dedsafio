package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.RaicesEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Las raíces del Creeper Raíz de Azalea: 8 raíces en un círculo alrededor del jugador, de 9 segmentos cada
 * una, que crecen desde el suelo segmento por segmento (0,9 s), se curvan hacia el centro y ondulan.
 * Al romperse se achican hasta desaparecer (0,6 s).
 */
public class RaicesRenderer extends EntityRenderer<RaicesEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/raices.png");
	private static final int RAIZ = 0, RAIZ_OSCURA = 1, HOJA = 2, COLORES = 3;
	private static final int RAICES = 8, SEGMENTOS = 9;

	public RaicesRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public void render(RaicesEntity raices, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		float tiempo = raices.tickCount + parcial, t = tiempo / 20f;
		float crece = Math.min(1, tiempo / RaicesEntity.TIEMPO_CRECER);
		float rota = raices.ticksRota() < 0 ? 0 : Math.min(1, (raices.ticksRota() + parcial) / RaicesEntity.TIEMPO_ROMPER);
		if (rota >= 1) return;
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		pose.pushPose();
		pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
		for (int r = 0; r < RAICES; r++) {
			float a = r / (float) RAICES * Mth.TWO_PI;
			pose.pushPose();
			pose.translate(Mth.cos(a) * 9, 0, Mth.sin(a) * 9);
			pose.mulPose(Axis.YP.rotation(-a + Mth.PI));   // la raíz mira hacia el jugador
			for (int i = 0; i < SEGMENTOS; i++) {
				if (i > 0) pose.translate(0, 2.6f, 0);
				pose.mulPose(Axis.XP.rotation(0.12f * Mth.sin(r + i) + (rota > 0 ? 0 : 0.05f * Mth.sin(t * 6 + r))));
				pose.mulPose(Axis.ZP.rotation(i == 0 ? -0.35f : -0.12f - (i > 4 ? 0.06f : 0)));
				float escala = Mth.clamp(crece * SEGMENTOS - i * 0.85f, 0.001f, 1) * (1 - rota);
				pose.scale(escala, escala, escala);
				float s = 2.4f - i * 0.18f;
				caja(vc, pose.last(), i % 3 == 2 ? RAIZ_OSCURA : RAIZ, -s / 2, -0.1f, -s / 2, s / 2, 2.7f, s / 2, luz);
				if (i % 3 == 1) caja(vc, pose.last(), HOJA, -s / 2 - 1.4f, 1.2f, -0.6f, -s / 2 + 0.2f, 1.6f, 0.6f, luz);
			}
			pose.popPose();
		}
		pose.popPose();
		super.render(raices, yaw, parcial, pose, buffers, luz);
	}

	private static void caja(VertexConsumer vc, PoseStack.Pose p, int color, float x0, float y0, float z0, float x1, float y1, float z1, int luz) {
		float u = (color * 4 + 2) / (COLORES * 4f), v = 0.5f;
		cara(vc, p, u, v, luz, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
		cara(vc, p, u, v, luz, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
		cara(vc, p, u, v, luz, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		cara(vc, p, u, v, luz, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
		cara(vc, p, u, v, luz, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		cara(vc, p, u, v, luz, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, float u, float v, int luz, float nx, float ny, float nz, float... q) {
		for (int i = 0; i < 12; i += 3) {
			vc.addVertex(p, q[i], q[i + 1], q[i + 2]).setColor(255, 255, 255, 255).setUv(u, v)
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	@Override
	public boolean shouldRender(RaicesEntity raices, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public ResourceLocation getTextureLocation(RaicesEntity raices) {
		return TEXTURA;
	}
}
