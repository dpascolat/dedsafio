package com.dedsafio4.client.qumara;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.qumara.CirculoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Círculo Explosivo como en el diseño: celdas de 1 bloque en el piso. Borde rojo (brilla), borde
 * interior a cuadros, y un relleno rojo translúcido que crece desde el centro mientras parpadea cada vez
 * más rápido; al final vibra. Después de explotar queda la marca quemada.
 */
public class CirculoRenderer extends EntityRenderer<CirculoEntity> {
	private static final ResourceLocation BLANCO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/blanco.png");
	private static final int R = (int) CirculoEntity.RADIO;

	public CirculoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public void render(CirculoEntity c, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		float tick = c.tickCount + parcial, aviso = c.ticksAviso();
		PoseStack.Pose p = pose.last();
		if (tick < aviso) {
			float tSeg = tick / 20f, k = tick / aviso;
			float parpadeo = 0.5f + 0.5f * Mth.sin(tSeg * (2 + 14 * k) * (5f / (aviso / 20f)));
			float brillo = Math.min(1, 0.45f + parpadeo * (0.35f + 0.2f * k));
			float vibra = k > 0.8f ? Mth.sin(tSeg * 40) * 0.12f : 0;
			float alcance = k * (R - 1.1f) + 0.4f;
			int alfaRelleno = (int) (255 * (0.25f + 0.35f * parpadeo));
			// Primero los bordes (sólidos) y después el relleno (translúcido): un buffer a la vez.
			VertexConsumer borde = buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO));
			for (int x = -R; x < R; x++) {
				for (int z = -R; z < R; z++) {
					float d = (float) Math.hypot(x + 0.5, z + 0.5);
					if (d <= R && d > R - 1.1f) {
						celda(borde, p, x + vibra, z, 0.05f, rgb(0xD62828, brillo), 255);
					} else if (d <= R - 1.1f && d > R - 1.9f && ((x + z) & 1) == 0) {
						celda(borde, p, x, z, 0.04f, rgb(0x8A1010, 0.6f + 0.4f * brillo), 255);
					}
				}
			}
			VertexConsumer relleno = buffers.getBuffer(RenderType.entityTranslucentEmissive(BLANCO));
			for (int x = -R; x < R; x++) {
				for (int z = -R; z < R; z++) {
					float d = (float) Math.hypot(x + 0.5, z + 0.5);
					boolean bordeInterior = d > R - 1.9f && ((x + z) & 1) == 0;
					if (d <= R - 1.1f && d <= alcance && !bordeInterior) celda(relleno, p, x, z, 0.03f, 0xFF4A3A, alfaRelleno);
				}
			}
		} else {
			// La marca quemada (se desvanece al final).
			float e = (tick - aviso) / CirculoEntity.TICKS_QUEMADO;
			int alfa = (int) (255 * Math.max(0, Math.min(1, (1 - e) * 3)));
			VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(BLANCO));
			for (int x = -R; x < R; x++) {
				for (int z = -R; z < R; z++) {
					double d = Math.hypot(x + 0.5, z + 0.5) + Math.sin(x * 1.6 + z * 0.9) * 0.8;
					if (d < R - 0.5) celdaLuz(vc, p, x, z, 0.02f, d < R / 2f ? 0x1E1A18 : 0x3A3A3A, alfa, luz);
				}
			}
		}
		super.render(c, yaw, parcial, pose, buffers, luz);
	}

	private static int rgb(int color, float f) {
		int r = (int) (((color >> 16) & 0xFF) * f), g = (int) (((color >> 8) & 0xFF) * f), b = (int) ((color & 0xFF) * f);
		return (r << 16) | (g << 8) | b;
	}

	/** Una celda de 1×1 en el piso, a esa altura, que brilla (no le importa la luz). */
	private static void celda(VertexConsumer vc, PoseStack.Pose p, float x, float z, float y, int color, int alfa) {
		celdaLuz(vc, p, x, z, y, color, alfa, LightTexture.FULL_BRIGHT);
	}

	private static void celdaLuz(VertexConsumer vc, PoseStack.Pose p, float x, float z, float y, int color, int alfa, int luz) {
		int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
		vertice(vc, p, x, y, z, r, g, b, alfa, luz);
		vertice(vc, p, x, y, z + 1, r, g, b, alfa, luz);
		vertice(vc, p, x + 1, y, z + 1, r, g, b, alfa, luz);
		vertice(vc, p, x + 1, y, z, r, g, b, alfa, luz);
	}

	private static void vertice(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, int r, int g, int b, int a, int luz) {
		vc.addVertex(p, x, y, z).setColor(r, g, b, a).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(p, 0, 1, 0);
	}

	@Override
	public boolean shouldRender(CirculoEntity c, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(c.getBoundingBox().inflate(CirculoEntity.RADIO, 1, CirculoEntity.RADIO));
	}

	@Override
	public ResourceLocation getTextureLocation(CirculoEntity c) {
		return BLANCO;
	}
}
