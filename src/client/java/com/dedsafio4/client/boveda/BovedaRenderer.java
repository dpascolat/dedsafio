package com.dedsafio4.client.boveda;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.boveda.BovedaBlock;
import com.dedsafio4.boveda.BovedaBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja la Bóveda entera desde su bloque principal, como en el diseño: la losa blanca con su frente
 * pintado (rejilla, arcoíris, parlante y pantalla), la rueda de timón, las bisagras y, atrás, la placa
 * y los cerrojos. Animación de abrir (4 s): la rueda gira media vuelta, los cerrojos se corren, la
 * puerta se separa un poco de la pared y gira 105° hacia afuera sobre las bisagras.
 *
 * Medidas de la puerta en bloques: x de 0 (bisagras) a 2, y de 0 a 3, z de -GROSOR (atrás) a 0 (frente).
 */
public class BovedaRenderer implements BlockEntityRenderer<BovedaBlockEntity> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/boveda.png");
	private static final float P = 1 / 16f, GROSOR = 6 * P, TEX = 128f;

	// Colores lisos de la textura (cuadraditos de 8x8 en y = 112).
	private static final int BLANCO = 0, BORDE = 1, HIERRO = 2, ACERO = 3, MADERA = 4, MADERA_OSCURA = 5;

	/** Los cubitos de la rueda (13x13 píxeles): aro, rayos, manijas y el centro más grueso. */
	private static final List<int[]> RUEDA = new ArrayList<>();

	static {
		for (int gx = -6; gx <= 6; gx++) {
			for (int gy = -6; gy <= 6; gy++) {
				double r = Math.hypot(gx, gy);
				int ax = Math.abs(gx), ay = Math.abs(gy);
				boolean aro = r >= 3.5 && r < 4.6;
				boolean rayo = r < 3.5 && (gx == 0 || gy == 0 || ax == ay);
				boolean manija = ((gx == 0 || gy == 0) && r >= 4.6) || (ax == 4 && ay == 4);
				if (aro || rayo || manija) RUEDA.add(new int[]{gx, gy, r < 1.5 ? 1 : 0});
			}
		}
	}

	public BovedaRenderer(BlockEntityRendererProvider.Context contexto) {}

	private static float tramo(float t, float a, float b) {
		return Math.min(1f, Math.max(0f, (t - a) / (b - a)));
	}

	/** easeInOutCubic, como en el diseño. */
	private static float suave(float t) {
		return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	@Override
	public void render(BovedaBlockEntity boveda, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		float t = boveda.progreso(parcial);
		float giroRueda = -180f * suave(tramo(t, 0f, 0.25f));
		float cerrojos = -0.3f * suave(tramo(t, 0.15f, 0.32f));
		float separacion = 0.06f * suave(tramo(t, 0.32f, 0.42f));
		float giroPuerta = -105f * suave(tramo(t, 0.42f, 1f));

		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURA));
		pose.pushPose();
		pose.translate(0.5, 0, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-boveda.getBlockState().getValue(BovedaBlock.FACING).toYRot()));
		// Bisagra: borde izquierdo (mirando de frente) del bloque principal, al ras del frente.
		pose.translate(-0.5, 0, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(giroPuerta));
		pose.translate(0, 0, separacion);
		PoseStack.Pose p = pose.last();

		// La losa: el frente pintado, los cantos y la parte de atrás.
		cara(vc, p, new float[]{0, 0, 0}, new float[]{2, 0, 0}, new float[]{2, 3, 0}, new float[]{0, 3, 0},
				0, 0, 64 / TEX, 96 / TEX, 0, 0, 1, luz, overlay);
		caja(vc, p, 0, 0, -GROSOR, 2, 3, -0.001f, BORDE, false, luz, overlay);

		// Bisagras.
		for (float y : new float[]{0.4f, 1.5f, 2.6f}) {
			caja(vc, p, -0.5f * P, y - 2.5f * P, 0, 4.5f * P, y + 2.5f * P, 1.5f * P, HIERRO, true, luz, overlay);
			caja(vc, p, -1.5f * P, y - 3 * P, -1.5f * P, 1.5f * P, y + 3 * P, 1.5f * P, HIERRO, true, luz, overlay);
		}

		// Atrás: la placa de acero, la barra y los 3 cerrojos (salen por el costado y se esconden al abrir).
		caja(vc, p, 0.15f, 0.15f, -GROSOR - P, 1.85f, 2.85f, -GROSOR, ACERO, true, luz, overlay);
		caja(vc, p, 1.64f, 0.3f, -GROSOR - 2.5f * P, 1.76f, 2.7f, -GROSOR - P, HIERRO, true, luz, overlay);
		for (float y : new float[]{0.5f, 1.5f, 2.5f}) {
			caja(vc, p, 1.75f + cerrojos, y - 1.5f * P, -GROSOR / 2 - 1.5f * P, 2.25f + cerrojos, y + 1.5f * P,
					-GROSOR / 2 + 1.5f * P, ACERO, true, luz, overlay);
		}

		// La rueda: gira sobre su centro.
		pose.pushPose();
		pose.translate(9 * P, 3 - 12 * P, 1.6f * P);
		pose.mulPose(Axis.ZP.rotationDegrees(giroRueda));
		PoseStack.Pose pr = pose.last();
		for (int[] cubo : RUEDA) {
			float x = cubo[0] * P, y = cubo[1] * P;
			boolean centro = cubo[2] == 1;
			caja(vc, pr, x - P / 2, y - P / 2, -P / 2, x + P / 2, y + P / 2, centro ? 1.5f * P : P / 2,
					centro ? MADERA_OSCURA : MADERA, true, luz, overlay);
		}
		caja(vc, pr, -P / 2, -P / 2, -1.6f * P, P / 2, P / 2, -P / 2, HIERRO, true, luz, overlay);
		pose.popPose();

		// La pantalla turquesa brilla en la oscuridad.
		float x0 = 21 * P, x1 = 29 * P, y0 = 3 - 44 * P, y1 = 3 - 36 * P, z = 0.002f;
		cara(vc, p, new float[]{x0, y0, z}, new float[]{x1, y0, z}, new float[]{x1, y1, z}, new float[]{x0, y1, z},
				42 / TEX, 72 / TEX, 58 / TEX, 88 / TEX, 0, 0, 1, LightTexture.FULL_BRIGHT, overlay);
		pose.popPose();
	}

	/**
	 * Una caja de color liso. Con {@code frente} en false no se dibuja la cara de adelante (la losa, que
	 * ya tiene el frente pintado).
	 */
	private static void caja(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1,
							 int color, boolean frente, int luz, int overlay) {
		float u0 = (color * 8 + 2) / TEX, u1 = (color * 8 + 6) / TEX, v0 = 114 / TEX, v1 = 118 / TEX;
		if (frente) {
			cara(vc, p, new float[]{x0, y0, z1}, new float[]{x1, y0, z1}, new float[]{x1, y1, z1}, new float[]{x0, y1, z1},
					u0, v0, u1, v1, 0, 0, 1, luz, overlay);
		}
		// Atrás, en blanco si es la losa.
		float ua = frente ? u0 : (BLANCO * 8 + 2) / TEX, ub = frente ? u1 : (BLANCO * 8 + 6) / TEX;
		cara(vc, p, new float[]{x1, y0, z0}, new float[]{x0, y0, z0}, new float[]{x0, y1, z0}, new float[]{x1, y1, z0},
				ua, v0, ub, v1, 0, 0, -1, luz, overlay);
		cara(vc, p, new float[]{x1, y0, z1}, new float[]{x1, y0, z0}, new float[]{x1, y1, z0}, new float[]{x1, y1, z1},
				u0, v0, u1, v1, 1, 0, 0, luz, overlay);
		cara(vc, p, new float[]{x0, y0, z0}, new float[]{x0, y0, z1}, new float[]{x0, y1, z1}, new float[]{x0, y1, z0},
				u0, v0, u1, v1, -1, 0, 0, luz, overlay);
		cara(vc, p, new float[]{x0, y1, z1}, new float[]{x1, y1, z1}, new float[]{x1, y1, z0}, new float[]{x0, y1, z0},
				u0, v0, u1, v1, 0, 1, 0, luz, overlay);
		cara(vc, p, new float[]{x0, y0, z0}, new float[]{x1, y0, z0}, new float[]{x1, y0, z1}, new float[]{x0, y0, z1},
				u0, v0, u1, v1, 0, -1, 0, luz, overlay);
	}

	/** Un cuadrado: a abajo-izquierda, b abajo-derecha, c arriba-derecha, d arriba-izquierda (mirándolo de afuera). */
	private static void cara(VertexConsumer vc, PoseStack.Pose p, float[] a, float[] b, float[] c, float[] d,
							 float u0, float v0, float u1, float v1, float nx, float ny, float nz, int luz, int overlay) {
		vertice(vc, p, a, u0, v1, nx, ny, nz, luz, overlay);
		vertice(vc, p, b, u1, v1, nx, ny, nz, luz, overlay);
		vertice(vc, p, c, u1, v0, nx, ny, nz, luz, overlay);
		vertice(vc, p, d, u0, v0, nx, ny, nz, luz, overlay);
	}

	private static void vertice(VertexConsumer vc, PoseStack.Pose p, float[] xyz, float u, float v,
								float nx, float ny, float nz, int luz, int overlay) {
		vc.addVertex(p, xyz[0], xyz[1], xyz[2]).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay)
				.setLight(luz).setNormal(p, nx, ny, nz);
	}

	@Override
	public boolean shouldRenderOffScreen(BovedaBlockEntity boveda) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
