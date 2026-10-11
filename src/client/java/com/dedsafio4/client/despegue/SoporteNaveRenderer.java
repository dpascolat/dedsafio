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
 * de bloque de Minecraft no permiten (37,5°, 57,5°, 142,5°...), por eso se dibuja a mano.
 */
public class SoporteNaveRenderer implements BlockEntityRenderer<SoporteNaveBlockEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/block/soporte_nave.png");
	private static final float TAMANO_TEXTURA = 256f;
	/** El centro del soporte y lo que mide de lado (en píxeles del modelo, con las barandas): pasa a medir 2 bloques. */
	private static final float CENTRO_X = -1.5f, CENTRO_Z = -10f, LADO = 44f;

	/** Cada cubo: desde xyz, hasta xyz, pivote xyz, giro xyz y los uv de norte, este, sur, oeste, arriba y abajo. */
	private static final float[][] CUBOS = {
			{-18f, 4f, -27f, 15f, 6f, 8f, 2f, 0f, 11f, 0f, 0f, 0f, 35f, 76f, 68f, 78f, 0f, 76f, 35f, 78f, 103f, 76f, 136f, 78f, 68f, 76f, 103f, 78f, 68f, 76f, 35f, 41f, 101f, 41f, 68f, 76f},
			{12f, 4f, 7f, 18f, 6f, 9f, 14f, 0f, 8f, 0f, 37.5f, 0f, 134f, 133f, 140f, 135f, 132f, 133f, 134f, 135f, 142f, 133f, 148f, 135f, 140f, 133f, 142f, 135f, 140f, 133f, 134f, 131f, 146f, 131f, 140f, 133f},
			{-18f, 0f, 11f, 15f, 2f, 13f, 0f, -4f, 12f, 0f, 0f, 0f, 106f, 96f, 139f, 98f, 104f, 96f, 106f, 98f, 141f, 96f, 174f, 98f, 139f, 96f, 141f, 98f, 139f, 96f, 106f, 94f, 172f, 94f, 139f, 96f},
			{-19f, 4f, 7f, -13f, 6f, 9f, -17f, 0f, 8f, 0f, 142.5f, 0f, 2f, 137f, 8f, 139f, 0f, 137f, 2f, 139f, 10f, 137f, 16f, 139f, 8f, 137f, 10f, 139f, 8f, 137f, 2f, 135f, 14f, 135f, 8f, 137f},
			{-21.3f, 0f, -31f, -15.3f, 2f, -29f, -19.3f, -4f, -30f, 0f, -135f, 0f, 18f, 137f, 24f, 139f, 16f, 137f, 18f, 139f, 26f, 137f, 32f, 139f, 24f, 137f, 26f, 139f, 24f, 137f, 18f, 135f, 30f, 135f, 24f, 137f},
			{-16.5f, 4f, -29f, 12.5f, 6f, -27f, -0.5f, 0f, -28f, 0f, 0f, 0f, 134f, 116f, 163f, 118f, 132f, 116f, 134f, 118f, 165f, 116f, 194f, 118f, 163f, 116f, 165f, 118f, 163f, 116f, 134f, 114f, 192f, 114f, 163f, 116f},
			{13.5f, 4f, -27f, 19.5f, 6f, -25f, 15.5f, 0f, -26f, 0f, 145f, 0f, 34f, 137f, 40f, 139f, 32f, 137f, 34f, 139f, 42f, 137f, 48f, 139f, 40f, 137f, 42f, 139f, 40f, 137f, 34f, 135f, 46f, 135f, 40f, 137f},
			{-0.2f, 4f, -11.641f, 31.8f, 6f, -9.641f, 16.8f, 0f, -10.641f, 0f, 90f, 0f, 134f, 104f, 166f, 106f, 132f, 104f, 134f, 106f, 168f, 104f, 200f, 106f, 166f, 104f, 168f, 106f, 166f, 104f, 134f, 102f, 198f, 102f, 166f, 104f},
			{14f, 4f, -25f, 16f, 6f, 6f, 15f, 0f, 0f, 0f, 0f, 0f, 31f, 133f, 33f, 135f, 0f, 133f, 31f, 135f, 64f, 133f, 66f, 135f, 33f, 133f, 64f, 135f, 33f, 133f, 31f, 102f, 35f, 102f, 33f, 133f},
			{-19f, 4f, -25f, -17f, 6f, 6f, -18f, 0f, 0f, 0f, 0f, 0f, 97f, 133f, 99f, 135f, 66f, 133f, 97f, 135f, 130f, 133f, 132f, 135f, 99f, 133f, 130f, 135f, 99f, 133f, 97f, 102f, 101f, 102f, 99f, 133f},
			{-21f, 0f, -29f, 18f, 2f, 10f, 0f, 0f, 11f, 0f, 0f, 0f, 39f, 39f, 78f, 41f, 0f, 39f, 39f, 41f, 117f, 39f, 156f, 41f, 78f, 39f, 117f, 41f, 78f, 39f, 39f, 0f, 117f, 0f, 78f, 39f},
			{-3f, 0.44699f, 5.45196f, 1f, 4.44699f, 10.452f, 0f, 0.86788f, 9.35328f, 57.5f, 0f, 0f, 137f, 127f, 141f, 131f, 132f, 127f, 137f, 131f, 146f, 127f, 150f, 131f, 141f, 127f, 146f, 131f, 141f, 127f, 137f, 122f, 145f, 122f, 141f, 127f},
			{-16f, 4f, 8f, 13f, 6f, 10f, 0f, 0f, 9f, 0f, 0f, 0f, 134f, 120f, 163f, 122f, 132f, 120f, 134f, 122f, 165f, 120f, 194f, 122f, 163f, 120f, 165f, 122f, 163f, 120f, 134f, 118f, 192f, 118f, 163f, 120f},
			{-21f, 0f, 10f, -15f, 2f, 12f, -19f, -4f, 11f, 0f, 142.5f, 0f, 50f, 137f, 56f, 139f, 48f, 137f, 50f, 139f, 58f, 137f, 64f, 139f, 56f, 137f, 58f, 139f, 56f, 137f, 50f, 135f, 62f, 135f, 56f, 137f},
			{-36.8f, 4f, -11.641f, -5.8f, 6f, -9.641f, -19.8f, 0f, -10.641f, 0f, 90f, 0f, 134f, 112f, 165f, 114f, 132f, 112f, 134f, 114f, 167f, 112f, 198f, 114f, 165f, 112f, 167f, 114f, 165f, 112f, 134f, 110f, 196f, 110f, 165f, 112f},
			{-41.8f, 0f, -11.641f, -4.8f, 2f, -9.641f, -21.8f, -4f, -10.641f, 0f, 90f, 0f, 106f, 84f, 143f, 86f, 104f, 84f, 106f, 86f, 145f, 84f, 182f, 86f, 143f, 84f, 145f, 86f, 143f, 84f, 106f, 82f, 180f, 82f, 143f, 84f},
			{-19.3f, 4f, -28f, -13.3f, 6f, -26f, -17.3f, 0f, -27f, 0f, -135f, 0f, 66f, 137f, 72f, 139f, 64f, 137f, 66f, 139f, 74f, 137f, 80f, 139f, 72f, 137f, 74f, 139f, 72f, 137f, 66f, 135f, 78f, 135f, 72f, 137f},
			{-18.5f, 0f, -32f, 14.5f, 2f, -30f, -0.5f, -4f, -31f, 0f, 0f, 0f, 106f, 100f, 139f, 102f, 104f, 100f, 106f, 102f, 141f, 100f, 174f, 102f, 139f, 100f, 141f, 102f, 139f, 100f, 106f, 98f, 172f, 98f, 139f, 100f},
			{14f, 0f, 10f, 20f, 2f, 12f, 16f, -4f, 11f, 0f, 37.5f, 0f, 82f, 137f, 88f, 139f, 80f, 137f, 82f, 139f, 90f, 137f, 96f, 139f, 88f, 137f, 90f, 139f, 88f, 137f, 82f, 135f, 94f, 135f, 88f, 137f},
			{-1.2f, 0f, -11.641f, 36.8f, 2f, -9.641f, 18.8f, -4f, -10.641f, 0f, 90f, 0f, 106f, 80f, 144f, 82f, 104f, 80f, 106f, 82f, 146f, 80f, 184f, 82f, 144f, 80f, 146f, 82f, 144f, 80f, 106f, 78f, 182f, 78f, 144f, 80f},
			{15.5f, 0f, -30f, 21.5f, 2f, -28f, 17.5f, -4f, -29f, 0f, 145f, 0f, 98f, 137f, 104f, 139f, 96f, 137f, 98f, 139f, 106f, 137f, 112f, 139f, 104f, 137f, 106f, 139f, 104f, 137f, 98f, 135f, 110f, 135f, 104f, 137f},
			{-19f, 0f, -31f, 16f, 2f, -29f, 0f, 0f, -30f, 0f, 0f, 0f, 106f, 88f, 141f, 90f, 104f, 88f, 106f, 90f, 143f, 88f, 178f, 90f, 141f, 88f, 143f, 90f, 141f, 88f, 106f, 86f, 176f, 86f, 141f, 88f},
			{-19f, 0f, 9f, 16f, 2f, 11f, 0f, 0f, 10f, 0f, 0f, 0f, 106f, 92f, 141f, 94f, 104f, 92f, 106f, 94f, 143f, 92f, 178f, 94f, 141f, 92f, 143f, 94f, 141f, 92f, 106f, 90f, 176f, 90f, 141f, 92f},
			{-16f, 2f, -21f, 14f, 4f, 1f, 0f, 2f, 0f, 0f, 0f, 0f, 22f, 100f, 52f, 102f, 0f, 100f, 22f, 102f, 74f, 100f, 104f, 102f, 52f, 100f, 74f, 102f, 52f, 100f, 22f, 78f, 82f, 78f, 52f, 100f},
			{14f, 0.44699f, -14.548f, 18f, 4.44699f, -9.54804f, 17f, 0.86788f, -10.6467f, 57.5f, 90f, 0f, 137f, 127f, 141f, 131f, 132f, 127f, 137f, 131f, 146f, 127f, 150f, 131f, 141f, 127f, 146f, 131f, 141f, 127f, 137f, 122f, 145f, 122f, 141f, 127f},
			{-28.6467f, 0.44699f, -6.5f, -24.6467f, 4.44699f, -1.5f, -20f, 0.86788f, -2.59867f, 57.5f, -90f, 0f, 137f, 127f, 141f, 131f, 132f, 127f, 137f, 131f, 146f, 127f, 150f, 131f, 141f, 127f, 146f, 131f, 141f, 127f, 137f, 122f, 145f, 122f, 141f, 127f},
			{-6f, 0.44699f, -32.548f, -2f, 4.44699f, -27.548f, -3f, 0.86788f, -28.6467f, 57.5f, -180f, 0f, 137f, 127f, 141f, 131f, 132f, 127f, 137f, 131f, 146f, 127f, 150f, 131f, 141f, 127f, 146f, 131f, 141f, 127f, 137f, 122f, 145f, 122f, 141f, 127f}
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
		if (c[9] != 0 || c[10] != 0 || c[11] != 0) {
			// Como Blockbench: primero se gira en X, después en Y y por último en Z, alrededor del pivote.
			pose.translate(c[6], c[7], c[8]);
			pose.mulPose(Axis.ZP.rotationDegrees(c[11]));
			pose.mulPose(Axis.YP.rotationDegrees(c[10]));
			pose.mulPose(Axis.XP.rotationDegrees(c[9]));
			pose.translate(-c[6], -c[7], -c[8]);
		}
		PoseStack.Pose p = pose.last();
		float x1 = c[0], y1 = c[1], z1 = c[2], x2 = c[3], y2 = c[4], z2 = c[5];
		// Las esquinas de cada cara mirándola de afuera: arriba-izquierda, abajo-izquierda, abajo-derecha, arriba-derecha.
		cara(p, v, c, 12, luz, overlay, 0, 0, -1, x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1);
		cara(p, v, c, 16, luz, overlay, 1, 0, 0, x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1);
		cara(p, v, c, 20, luz, overlay, 0, 0, 1, x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2);
		cara(p, v, c, 24, luz, overlay, -1, 0, 0, x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2);
		cara(p, v, c, 28, luz, overlay, 0, 1, 0, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
		cara(p, v, c, 32, luz, overlay, 0, -1, 0, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2);
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
