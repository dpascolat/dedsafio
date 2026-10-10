package com.dedsafio4.client.despegue;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.despegue.SoporteNaveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Dibuja el Soporte de Nave con los cubos de Soporte_nave.bbmodel (copiados abajo, en píxeles de Blockbench),
 * achicado para que la base ocupe justo 2×2 bloques. Algunos cubos están girados en ángulos que los modelos
 * de bloque de Minecraft no permiten (37,5°, 142,5°...), por eso se dibuja a mano.
 */
public class SoporteNaveRenderer implements BlockEntityRenderer<SoporteNaveBlockEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/block/soporte_nave.png");
	private static final float TAMANO_TEXTURA = 256f;
	/** El centro de la base y su lado (en píxeles del modelo): el lado pasa a medir 2 bloques. */
	private static final float CENTRO_X = -1.5f, CENTRO_Z = -9.5f, LADO = 43f;

	/** Cada cubo: desde xyz, hasta xyz, pivote xyz, giro en Y y los uv de norte, este, sur, oeste, arriba y abajo. */
	private static final float[][] CUBOS = {
			{-18f, 0f, -27f, 15f, 2f, 8f, 2f, 0f, 11f, 0f, 35f, 78f, 68f, 80f, 0f, 78f, 35f, 80f, 103f, 78f, 136f, 80f, 68f, 78f, 103f, 80f, 68f, 78f, 35f, 43f, 101f, 43f, 68f, 78f},
			{12f, 0f, 7f, 18f, 2f, 9f, 14f, 0f, 8f, 37.5f, 2f, 123f, 8f, 125f, 0f, 123f, 2f, 125f, 10f, 123f, 16f, 125f, 8f, 123f, 10f, 125f, 8f, 123f, 2f, 121f, 14f, 121f, 8f, 123f},
			{-16f, 0f, 8f, 13f, 2f, 10f, 0f, 0f, 9f, 0f, 2f, 119f, 31f, 121f, 0f, 119f, 2f, 121f, 33f, 119f, 62f, 121f, 31f, 119f, 33f, 121f, 31f, 119f, 2f, 117f, 60f, 117f, 31f, 119f},
			{-19f, 0f, 7f, -13f, 2f, 9f, -17f, 0f, 8f, 142.5f, 18f, 123f, 24f, 125f, 16f, 123f, 18f, 125f, 26f, 123f, 32f, 125f, 24f, 123f, 26f, 125f, 24f, 123f, 18f, 121f, 30f, 121f, 24f, 123f},
			{-36.8f, 0f, -11.641f, -5.8f, 2f, -9.641f, -19.8f, 0f, -10.641f, 90f, 70f, 115f, 101f, 117f, 68f, 115f, 70f, 117f, 103f, 115f, 134f, 117f, 101f, 115f, 103f, 117f, 101f, 115f, 70f, 113f, 132f, 113f, 101f, 115f},
			{-19.3f, 0f, -28f, -13.3f, 2f, -26f, -17.3f, 0f, -27f, -135f, 34f, 123f, 40f, 125f, 32f, 123f, 34f, 125f, 42f, 123f, 48f, 125f, 40f, 123f, 42f, 125f, 40f, 123f, 34f, 121f, 46f, 121f, 40f, 123f},
			{-16.5f, 0f, -29f, 12.5f, 2f, -27f, -0.5f, 0f, -28f, 0f, 64f, 119f, 93f, 121f, 62f, 119f, 64f, 121f, 95f, 119f, 124f, 121f, 93f, 119f, 95f, 121f, 93f, 119f, 64f, 117f, 122f, 117f, 93f, 119f},
			{13.5f, 0f, -27f, 19.5f, 2f, -25f, 15.5f, 0f, -26f, 145f, 50f, 123f, 56f, 125f, 48f, 123f, 50f, 125f, 58f, 123f, 64f, 125f, 56f, 123f, 58f, 125f, 56f, 123f, 50f, 121f, 62f, 121f, 56f, 123f},
			{-0.2f, 0f, -11.641f, 31.8f, 2f, -9.641f, 16.8f, 0f, -10.641f, 90f, 2f, 115f, 34f, 117f, 0f, 115f, 2f, 117f, 36f, 115f, 68f, 117f, 34f, 115f, 36f, 117f, 34f, 115f, 2f, 113f, 66f, 113f, 34f, 115f},
			{14f, 0f, -25f, 16f, 2f, 6f, 15f, 0f, 0f, 0f, 31f, 111f, 33f, 113f, 0f, 111f, 31f, 113f, 64f, 111f, 66f, 113f, 33f, 111f, 64f, 113f, 33f, 111f, 31f, 80f, 35f, 80f, 33f, 111f},
			{-19f, 0f, -25f, -17f, 2f, 6f, -18f, 0f, 0f, 0f, 97f, 111f, 99f, 113f, 66f, 111f, 97f, 113f, 130f, 111f, 132f, 113f, 99f, 111f, 130f, 113f, 99f, 111f, 97f, 80f, 101f, 80f, 99f, 111f},
			{-23f, 0f, -31f, 20f, 0f, 12f, 0f, 0f, 11f, 0f, 43f, 43f, 86f, 43f, 0f, 43f, 43f, 43f, 129f, 43f, 172f, 43f, 86f, 43f, 129f, 43f, 86f, 43f, 43f, 0f, 129f, 0f, 86f, 43f}
	};

	public SoporteNaveRenderer(BlockEntityRendererProvider.Context contexto) {}

	@Override
	public void render(SoporteNaveBlockEntity entidad, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		pose.pushPose();
		// Al medio de los 2×2 bloques, apenas arriba del piso (para que no parpadee con el bloque de abajo).
		pose.translate(1, 0.01, 1);
		dibujar(pose, buffers, luz, overlay);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(SoporteNaveBlockEntity entidad) {
		return true;
	}

	/** El modelo centrado en (0, 0, 0), con la base de 2×2 bloques. */
	public static void dibujar(PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		pose.pushPose();
		float escala = 2f / LADO;
		pose.scale(escala, escala, escala);
		pose.translate(-CENTRO_X, 0, -CENTRO_Z);
		for (float[] c : CUBOS) cubo(pose, v, c, luz, overlay);
		pose.popPose();
	}

	private static void cubo(PoseStack pose, VertexConsumer v, float[] c, int luz, int overlay) {
		pose.pushPose();
		if (c[9] != 0) {
			pose.translate(c[6], c[7], c[8]);
			pose.mulPose(Axis.YP.rotationDegrees(c[9]));
			pose.translate(-c[6], -c[7], -c[8]);
		}
		PoseStack.Pose p = pose.last();
		float x1 = c[0], y1 = c[1], z1 = c[2], x2 = c[3], y2 = c[4], z2 = c[5];
		// Las esquinas de cada cara mirándola de afuera: arriba-izquierda, abajo-izquierda, abajo-derecha, arriba-derecha.
		cara(p, v, c, 10, luz, overlay, 0, 0, -1, x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1);
		cara(p, v, c, 14, luz, overlay, 1, 0, 0, x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1);
		cara(p, v, c, 18, luz, overlay, 0, 0, 1, x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2);
		cara(p, v, c, 22, luz, overlay, -1, 0, 0, x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2);
		cara(p, v, c, 26, luz, overlay, 0, 1, 0, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
		cara(p, v, c, 30, luz, overlay, 0, -1, 0, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2);
		pose.popPose();
	}

	private static void cara(PoseStack.Pose p, VertexConsumer v, float[] c, int uv, int luz, int overlay,
							 float nx, float ny, float nz, float... e) {
		float u1 = c[uv] / TAMANO_TEXTURA, v1 = c[uv + 1] / TAMANO_TEXTURA, u2 = c[uv + 2] / TAMANO_TEXTURA, v2 = c[uv + 3] / TAMANO_TEXTURA;
		float[][] uvs = {{u1, v1}, {u1, v2}, {u2, v2}, {u2, v1}};
		for (int i = 0; i < 4; i++) {
			v.addVertex(p, e[i * 3], e[i * 3 + 1], e[i * 3 + 2]).setColor(-1).setUv(uvs[i][0], uvs[i][1])
					.setOverlay(overlay).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}
}
