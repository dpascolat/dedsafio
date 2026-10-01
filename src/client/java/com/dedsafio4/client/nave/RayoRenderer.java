package com.dedsafio4.client.nave;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.nave.RayoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Rayo celeste brillante: 5x5 píxeles de sección y 1 bloque de largo, orientado como una flecha. */
public class RayoRenderer extends EntityRenderer<RayoEntity> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/blanco.png");
	private static final float MEDIO_LADO = 2.5f / 16f, MEDIO_LARGO = 0.5f;
	/** Celeste por fuera y un núcleo más claro. */
	private static final float[] CELESTE = {0.35f, 0.85f, 1f, 0.85f}, NUCLEO = {0.85f, 0.98f, 1f, 1f};

	public RayoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public void render(RayoEntity rayo, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, rayo.yRotO, rayo.getYRot()) - 90f));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, rayo.xRotO, rayo.getXRot())));
		VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
		Matrix4f m = pose.last().pose();
		caja(vc, m, MEDIO_LARGO, MEDIO_LADO, CELESTE);
		caja(vc, m, MEDIO_LARGO * 0.95f, MEDIO_LADO * 0.45f, NUCLEO);
		pose.popPose();
		super.render(rayo, yaw, partialTick, pose, buffers, luz);
	}

	/** Caja a lo largo del eje X (el largo del rayo). */
	private static void caja(VertexConsumer vc, Matrix4f m, float x, float s, float[] c) {
		float[][] caras = {
				{-x, -s, -s, x, -s, -s, x, s, -s, -x, s, -s},
				{-x, -s, s, -x, s, s, x, s, s, x, -s, s},
				{-x, -s, -s, -x, -s, s, x, -s, s, x, -s, -s},
				{-x, s, -s, x, s, -s, x, s, s, -x, s, s},
				{-x, -s, -s, -x, s, -s, -x, s, s, -x, -s, s},
				{x, -s, -s, x, -s, s, x, s, s, x, s, -s},
		};
		for (float[] q : caras) {
			for (int i = 0; i < 4; i++) {
				vc.addVertex(m, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]).setColor(c[0], c[1], c[2], c[3]);
			}
		}
	}

	@Override
	public ResourceLocation getTextureLocation(RayoEntity rayo) {
		return TEXTURA;
	}
}
