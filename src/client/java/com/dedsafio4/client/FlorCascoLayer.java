package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * La flor arriba del Casco de Netherita con recubrimiento de planta (el modelo "flor" de Blockbench): un centro y 8
 * pétalos en dos vueltas (4 más inclinados y 4 más bajos, entre medio), sobre la cabeza del jugador.
 * Medidas en píxeles (1/16 de bloque), con y para arriba desde el giro de la cabeza; textura flor_casco.png (16 × 16).
 */
public class FlorCascoLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/flor_casco.png");
	private static final float P = 1 / 16f;
	/** Las caras de los pétalos (uv en píxeles de la textura): arriba, abajo, costados (este/oeste) y puntas (norte/sur). */
	private static final float[] PETALO_ARRIBA = {4, 0, 8, 7}, PETALO_ABAJO = {8, 0, 12, 7}, PETALO_COSTADO = {0, 8, 7, 9}, PETALO_PUNTA = {12, 0, 16, 1};

	public FlorCascoLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre) {
		super(padre);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador, float limbSwing,
					   float limbSwingAmount, float parcial, float edad, float cabezaYaw, float cabezaPitch) {
		if (!jugador.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CASCO_NETHERITA_PLANTA) || jugador.isInvisible()) return;
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		pose.pushPose();
		getParentModel().getHead().translateAndRotate(pose);
		pose.scale(P, -P, -P);   // en píxeles, y para arriba
		// El centro.
		caja(pose, vc, luz, -2, 8.65f, -2, 2, 9.65f, 2, new float[]{0, 0, 4, 4}, new float[]{0, 0, 4, 4}, new float[]{0, 4, 4, 5}, new float[]{0, 4, 4, 5});
		// Los 4 pétalos de arriba (más inclinados) y los 4 de abajo, girados 45° entre ellos.
		for (int i = 0; i < 4; i++) petalo(pose, vc, luz, 8.65f, i * 90, -24, 2.75f, 8.3f, 9.0f, 6.05f);
		for (int i = 0; i < 4; i++) petalo(pose, vc, luz, 8.35f, 45 + i * 90, -18, 2.25f, 8.0f, 8.7f, 7.2165f);
		pose.popPose();
	}

	private static void petalo(PoseStack pose, VertexConsumer vc, int luz, float pivoteY, float giroY, float inclinacion,
							   float medioAncho, float y0, float y1, float largo) {
		pose.pushPose();
		pose.translate(0, pivoteY, 0);
		pose.mulPose(Axis.YP.rotationDegrees(giroY));
		pose.mulPose(Axis.XP.rotationDegrees(inclinacion));
		pose.translate(0, -pivoteY, 0);
		caja(pose, vc, luz, -medioAncho, y0, 0.5f, medioAncho, y1, largo, PETALO_ARRIBA, PETALO_ABAJO, PETALO_COSTADO, PETALO_PUNTA);
		pose.popPose();
	}

	/** Una caja con su uv: arriba, abajo, costados (x) y frente/atrás (z). */
	private static void caja(PoseStack pose, VertexConsumer vc, int luz, float x0, float y0, float z0, float x1, float y1, float z1,
							 float[] arriba, float[] abajo, float[] costado, float[] punta) {
		PoseStack.Pose p = pose.last();
		cara(vc, p, luz, arriba, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
		cara(vc, p, luz, abajo, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
		cara(vc, p, luz, costado, 1, 0, 0, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
		cara(vc, p, luz, costado, -1, 0, 0, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
		cara(vc, p, luz, punta, 0, 0, 1, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
		cara(vc, p, luz, punta, 0, 0, -1, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, int luz, float[] uv, float nx, float ny, float nz, float... q) {
		float[][] esquinas = {{uv[0], uv[3]}, {uv[2], uv[3]}, {uv[2], uv[1]}, {uv[0], uv[1]}};
		for (int i = 0; i < 4; i++) {
			vc.addVertex(p, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]).setColor(-1).setUv(esquinas[i][0] / 16f, esquinas[i][1] / 16f)
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}
}
