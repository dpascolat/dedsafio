package com.dedsafio4.client.lagartos;

import com.dedsafio4.lagartos.LanzaLanzadaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** La lanza de hueso que vuela: la misma lanza del modelo del lancero, apuntando hacia donde va. */
public class LanzaLanzadaRenderer extends EntityRenderer<LanzaLanzadaEntity> {
	public LanzaLanzadaRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public void render(LanzaLanzadaEntity lanza, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		ModeloLagarto m = ModeloLagarto.de("lancero");
		m.reiniciar();
		ModeloLagarto.Hueso h = m.hueso("lanza");
		h.x = h.y = h.z = 0;
		h.rx = h.ry = h.rz = 0;
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(parcial, lanza.yRotO, lanza.getYRot())));
		pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(parcial, lanza.xRotO, lanza.getXRot())));
		pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
		m.dibujar(h, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ModeloLagarto.BLANCO)), luz, OverlayTexture.NO_OVERLAY, 0, false);
		pose.popPose();
		super.render(lanza, yaw, parcial, pose, buffers, luz);
	}

	@Override
	public ResourceLocation getTextureLocation(LanzaLanzadaEntity lanza) {
		return ModeloLagarto.BLANCO;
	}
}
