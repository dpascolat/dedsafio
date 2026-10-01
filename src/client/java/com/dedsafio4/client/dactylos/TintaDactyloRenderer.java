package com.dedsafio4.client.dactylos;

import com.dedsafio4.dactylos.TintaDactyloEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** La tinta: un cubito violeta oscuro de 2,4 píxeles que gira mientras vuela. */
public class TintaDactyloRenderer extends EntityRenderer<TintaDactyloEntity> {
	private static final DactyloBebeModelo.Caja CUBO = new DactyloBebeModelo.Caja(DactyloBebeModelo.TINTA, 2.4f, 2.4f, 2.4f, 0, 0, 0);

	public TintaDactyloRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public void render(TintaDactyloEntity tinta, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		float t = (tinta.tickCount + parcial) / 20f;
		pose.pushPose();
		pose.translate(0, 0.1, 0);
		pose.mulPose(Axis.XP.rotation(t * 8));
		pose.mulPose(Axis.YP.rotation(t * 6));
		pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
		// Brilla un poco (en el diseño tiene un leve resplandor violeta).
		int brillo = Math.max(luz, LightTexture.pack(10, 10));
		DactyloBebeModelo.caja(buffers.getBuffer(RenderType.entityCutoutNoCull(DactyloBebeRenderer.TEXTURA)), pose.last(), CUBO,
				brillo, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
		pose.popPose();
		super.render(tinta, yaw, parcial, pose, buffers, luz);
	}

	@Override
	public ResourceLocation getTextureLocation(TintaDactyloEntity tinta) {
		return DactyloBebeRenderer.TEXTURA;
	}
}
