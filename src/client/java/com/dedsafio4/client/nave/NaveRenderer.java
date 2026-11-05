package com.dedsafio4.client.nave;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.nave.NaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Dibuja la Nave con el modelo de Patricio ("PodPhora" de Blockbench, formato libre): cada cubo con su pedazo de
 * textura en cada cara y girado en el ángulo del diseño (por eso no usa los modelos comunes de Minecraft, que sólo
 * giran de a 22,5°). Las animaciones del diseño: flota subiendo y bajando (idle), se inclina para adelante cuando
 * avanza (walk) y al morir se cae de costado (death).
 */
public class NaveRenderer extends EntityRenderer<NaveEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/pod_phora.png");

	/**
	 * Los cubos del diseño (en píxeles): desde (3), hasta (3), centro de giro (3), giro en grados (3) y, por cada cara
	 * (norte, este, sur, oeste, arriba, abajo), su pedazo de la textura de 64×64 (u1, v1, u2, v2).
	 */
	private static final float[][] CUBOS = {
			{-6.75736f, 4.75736f, -5.00000f, 2.24264f, 6.75736f, 5.00000f, -6.00000f, 6.00000f, 0.00000f, 0.00000f, 0.00000f, 38.50000f, 26.00000f, 14.00000f, 35.00000f, 16.00000f, 26.00000f, 0.00000f, 36.00000f, 2.00000f, 26.00000f, 16.00000f, 35.00000f, 18.00000f, 26.00000f, 2.00000f, 36.00000f, 4.00000f, 9.00000f, 10.00000f, 0.00000f, 0.00000f, 18.00000f, 0.00000f, 9.00000f, 10.00000f},
			{4.75736f, 5.24264f, -5.00000f, 6.75736f, 14.24264f, 5.00000f, 6.00000f, 6.00000f, 0.00000f, 0.00000f, 0.00000f, 51.50000f, 0.00000f, 27.00000f, 2.00000f, 36.00000f, 0.00000f, 10.00000f, 10.00000f, 19.00000f, 2.00000f, 27.00000f, 4.00000f, 36.00000f, 10.00000f, 10.00000f, 20.00000f, 19.00000f, 28.00000f, 14.00000f, 26.00000f, 4.00000f, 28.00000f, 25.00000f, 26.00000f, 35.00000f},
			{-5.00000f, 2.00000f, -5.00000f, 1.00000f, 8.00000f, 5.00000f, 0.00000f, 7.00000f, 0.00000f, 0.00000f, 0.00000f, 45.00000f, 14.00000f, 25.00000f, 20.00000f, 31.00000f, 8.00000f, 19.00000f, 18.00000f, 25.00000f, 20.00000f, 25.00000f, 26.00000f, 31.00000f, 18.00000f, 19.00000f, 28.00000f, 25.00000f, 26.00000f, 18.00000f, 20.00000f, 8.00000f, 14.00000f, 25.00000f, 8.00000f, 35.00000f},
			{-4.00000f, 6.00000f, -4.00000f, 4.00000f, 8.00000f, 4.00000f, 0.00000f, 6.00000f, 0.00000f, 0.00000f, 0.00000f, 0.00000f, 28.00000f, 4.00000f, 36.00000f, 6.00000f, 28.00000f, 6.00000f, 36.00000f, 8.00000f, 28.00000f, 8.00000f, 36.00000f, 10.00000f, 28.00000f, 10.00000f, 36.00000f, 12.00000f, 26.00000f, 8.00000f, 18.00000f, 0.00000f, 8.00000f, 19.00000f, 0.00000f, 27.00000f},
			{-3.00000f, 6.00000f, -5.60000f, 0.00000f, 7.00000f, -4.60000f, 0.00000f, 7.00000f, 0.40000f, 0.00000f, 0.00000f, 45.00000f, 31.00000f, 28.00000f, 34.00000f, 29.00000f, 32.00000f, 20.00000f, 33.00000f, 21.00000f, 29.00000f, 31.00000f, 32.00000f, 32.00000f, 32.00000f, 21.00000f, 33.00000f, 22.00000f, 34.00000f, 30.00000f, 31.00000f, 29.00000f, 34.00000f, 30.00000f, 31.00000f, 31.00000f},
			{-1.00000f, 3.00000f, -5.60000f, 0.00000f, 6.00000f, -4.60000f, 0.00000f, 7.00000f, 0.40000f, 0.00000f, 0.00000f, 45.00000f, 24.00000f, 31.00000f, 25.00000f, 34.00000f, 25.00000f, 31.00000f, 26.00000f, 34.00000f, 31.00000f, 25.00000f, 32.00000f, 28.00000f, 28.00000f, 31.00000f, 29.00000f, 34.00000f, 33.00000f, 19.00000f, 32.00000f, 18.00000f, 33.00000f, 19.00000f, 32.00000f, 20.00000f},
			{-4.00000f, 3.00000f, -5.60000f, -1.00000f, 4.00000f, -4.60000f, 0.00000f, 7.00000f, 0.40000f, 0.00000f, 0.00000f, 45.00000f, 28.00000f, 12.00000f, 31.00000f, 13.00000f, 19.00000f, 8.00000f, 20.00000f, 9.00000f, 28.00000f, 13.00000f, 31.00000f, 14.00000f, 19.00000f, 9.00000f, 20.00000f, 10.00000f, 34.00000f, 13.00000f, 31.00000f, 12.00000f, 34.00000f, 13.00000f, 31.00000f, 14.00000f},
			{-4.00000f, 4.00000f, -5.60000f, -3.00000f, 7.00000f, -4.60000f, 0.00000f, 7.00000f, 0.40000f, 0.00000f, 0.00000f, 45.00000f, 21.00000f, 31.00000f, 22.00000f, 34.00000f, 22.00000f, 31.00000f, 23.00000f, 34.00000f, 31.00000f, 22.00000f, 32.00000f, 25.00000f, 23.00000f, 31.00000f, 24.00000f, 34.00000f, 19.00000f, 9.00000f, 18.00000f, 8.00000f, 19.00000f, 9.00000f, 18.00000f, 10.00000f},
			{-4.00000f, 2.30000f, 5.00000f, 1.00000f, 7.30000f, 6.00000f, 0.00000f, 6.30000f, 2.00000f, 0.00000f, 0.00000f, 45.00000f, 4.00000f, 27.00000f, 8.00000f, 31.00000f, 7.00000f, 31.00000f, 8.00000f, 35.00000f, 28.00000f, 18.00000f, 32.00000f, 22.00000f, 20.00000f, 31.00000f, 21.00000f, 35.00000f, 24.00000f, 19.00000f, 20.00000f, 18.00000f, 28.00000f, 18.00000f, 24.00000f, 19.00000f},
			{-1.17868f, 2.42132f, 5.00000f, 2.82132f, 6.42132f, 8.00000f, 0.70000f, 3.30000f, 7.00000f, 0.00000f, 0.00000f, 45.00000f, 28.00000f, 22.00000f, 31.00000f, 25.00000f, 28.00000f, 25.00000f, 31.00000f, 28.00000f, 28.00000f, 28.00000f, 31.00000f, 31.00000f, 4.00000f, 31.00000f, 7.00000f, 34.00000f, 17.00000f, 34.00000f, 14.00000f, 31.00000f, 20.00000f, 31.00000f, 17.00000f, 34.00000f}
	};
	/** El centro del grupo "podmodel" (de ahí gira al inclinarse y al caer). */
	private static final float CENTRO_Y = 4f;

	public NaveRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 0.5f;
	}

	@Override
	public void render(NaveEntity nave, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		// El frente del diseño mira a -Z; la nave mira para donde va.
		pose.mulPose(Axis.YP.rotationDegrees(180f - Mth.rotLerp(parcial, nave.yBodyRotO, nave.yBodyRot)));
		pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
		float t = (nave.tickCount + parcial) / 20f;
		// idle: sube medio píxel y vuelve, en 1,5 segundos.
		float fase = (t % 1.5f) / 1.5f;
		pose.translate(0, fase < 0.5f ? fase * 2 * 0.5f : (1 - fase) * 2 * 0.5f, 0);
		pose.translate(0, CENTRO_Y, 0);
		// walk: inclinada 4° para adelante mientras avanza.
		if (nave.getDeltaMovement().horizontalDistanceSqr() > 0.0004) pose.mulPose(Axis.XP.rotationDegrees(-4f));
		// death: se cae 25° de costado en medio segundo.
		if (nave.deathTime > 0) pose.mulPose(Axis.ZP.rotationDegrees(Math.min(1f, (nave.deathTime + parcial) / 10f) * 25f));
		pose.translate(0, -CENTRO_Y, 0);
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		int overlay = LivingEntityRenderer.getOverlayCoords(nave, 0);
		for (float[] c : CUBOS) cubo(pose, vc, c, luz, overlay);
		pose.popPose();
		super.render(nave, yaw, parcial, pose, buffers, luz);
	}

	/** Un cubo: lo gira alrededor de su centro (Z, Y, X como en Blockbench) y dibuja sus 6 caras. */
	private static void cubo(PoseStack pose, VertexConsumer vc, float[] c, int luz, int overlay) {
		pose.pushPose();
		pose.translate(c[6], c[7], c[8]);
		if (c[11] != 0) pose.mulPose(Axis.ZP.rotationDegrees(c[11]));
		if (c[10] != 0) pose.mulPose(Axis.YP.rotationDegrees(c[10]));
		if (c[9] != 0) pose.mulPose(Axis.XP.rotationDegrees(c[9]));
		pose.translate(-c[6], -c[7], -c[8]);
		float x0 = c[0], y0 = c[1], z0 = c[2], x1 = c[3], y1 = c[4], z1 = c[5];
		PoseStack.Pose p = pose.last();
		// Cada cara: sus 4 esquinas en el orden arriba-izquierda, arriba-derecha, abajo-derecha, abajo-izquierda
		// (mirándola desde afuera, como Blockbench reparte la textura) y hacia dónde mira.
		cara(vc, p, c, 12, luz, overlay, 0, 0, -1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0);   // norte
		cara(vc, p, c, 16, luz, overlay, 1, 0, 0, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1);    // este
		cara(vc, p, c, 20, luz, overlay, 0, 0, 1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);    // sur
		cara(vc, p, c, 24, luz, overlay, -1, 0, 0, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0);   // oeste
		cara(vc, p, c, 28, luz, overlay, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);    // arriba
		cara(vc, p, c, 32, luz, overlay, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);   // abajo
		pose.popPose();
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, float[] c, int i, int luz, int overlay, float nx, float ny, float nz,
							 float... q) {
		float u1 = c[i] / 64f, v1 = c[i + 1] / 64f, u2 = c[i + 2] / 64f, v2 = c[i + 3] / 64f;
		float[][] uv = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
		for (int k = 0; k < 4; k++) {
			vc.addVertex(p, q[k * 3], q[k * 3 + 1], q[k * 3 + 2]).setColor(255, 255, 255, 255).setUv(uv[k][0], uv[k][1])
					.setOverlay(overlay).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(NaveEntity nave) {
		return TEXTURA;
	}
}
