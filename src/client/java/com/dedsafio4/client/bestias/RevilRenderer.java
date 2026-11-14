package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.RevilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja a Revil: modelos/revil.bin (bloquecitos de 1/48 de bloque), mirando para donde mira el mob. */
public class RevilRenderer extends EntityRenderer<RevilEntity> {
	private static final ResourceLocation BLANCO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/nucleo_blanco.png");
	private static final MallaBloques MALLA = new MallaBloques("revil");
	private static final float ESCALA = 1 / 48f;

	public RevilRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 0.6f;
	}

	@Override
	public ResourceLocation getTextureLocation(RevilEntity revil) {
		return BLANCO;
	}

	@Override
	public void render(RevilEntity revil, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		// El modelo mira para +z, como los mobs con giro 0.
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, revil.yBodyRotO, revil.yBodyRot)));
		pose.scale(ESCALA * revil.getScale(), ESCALA * revil.getScale(), ESCALA * revil.getScale());
		int overlay = OverlayTexture.pack(OverlayTexture.u(0f), OverlayTexture.v(revil.hurtTime > 0 || revil.deathTime > 0));
		MALLA.dibujar(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO)), luz, overlay);
		pose.popPose();
		super.render(revil, giro, parcial, pose, buffers, luz);
	}
}
