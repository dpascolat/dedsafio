package com.dedsafio4.client.qumara;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.qumara.BichoCuboEntity;
import com.dedsafio4.qumara.ModQumara;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Los bichos cubo (7 de alto): cuerpo 4x3,6x4, cintura, cabeza 3x3,2x3 y el borde de arriba, con la
 * textura repetida una vez por bloque y la sombra falsa del diseño (0,66 abajo → 1 arriba). El de la
 * cabeza mide 1 bloque.
 */
public class BichoCuboRenderer extends EntityRenderer<BichoCuboEntity> {
	private static final String[] PARTES = {"cuerpo", "cintura", "cabeza", "cabeza_borde"};
	private static final float[][] CAJAS = {{4, 3.6f, 4, 1.8f}, {4.1f, 0.5f, 4.1f, 3.55f}, {3, 3.2f, 3, 5.4f}, {2.4f, 0.4f, 2.4f, 6.801f}};
	/** El de la cabeza mide 1 bloque (el modelo mide 7). */
	private static final float ESCALA_CABEZA = 1f / 7f;

	private final RenderType[] tipos = new RenderType[4];
	private final boolean deLaCabeza;

	public BichoCuboRenderer(EntityRendererProvider.Context contexto, boolean deLaCabeza) {
		super(contexto);
		this.deLaCabeza = deLaCabeza;
		this.shadowRadius = deLaCabeza ? 0.4f : 2f;
		for (int i = 0; i < 4; i++) {
			tipos[i] = RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID,
					"textures/entity/bichos/" + (deLaCabeza ? "si_" : "no_") + PARTES[i] + ".png"));
		}
	}

	@Override
	public void render(BichoCuboEntity bicho, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, bicho.yBodyRotO, bicho.yBodyRot)));
		if (deLaCabeza) pose.scale(ESCALA_CABEZA, ESCALA_CABEZA, ESCALA_CABEZA);
		if (bicho.getVehicle() instanceof Player) {
			// Tiembla antes de explotar.
			float t = bicho.tickCount + parcial;
			pose.translate(Mth.sin(t * 2.3f) * 0.5f, 0, Mth.cos(t * 1.9f) * 0.5f);
		}
		int overlay = OverlayTexture.pack(0f, bicho.hurtTime > 0);
		for (int i = 0; i < 4; i++) {
			float[] c = CAJAS[i];
			caja(buffers.getBuffer(tipos[i]), pose.last(), c[0], c[1], c[2], c[3], luz, overlay);
		}
		pose.popPose();
		super.render(bicho, yaw, parcial, pose, buffers, luz);
	}

	private static void caja(VertexConsumer vc, PoseStack.Pose p, float w, float h, float d, float cy, int luz, int overlay) {
		float x0 = -w / 2, x1 = w / 2, y0 = cy - h / 2, y1 = cy + h / 2, z0 = -d / 2, z1 = d / 2;
		cara(vc, p, luz, overlay, 1, 0, 0, d, h, 0, y0, y1, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
		cara(vc, p, luz, overlay, -1, 0, 0, d, h, 0, y0, y1, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		cara(vc, p, luz, overlay, 0, 1, 0, w, d, 2, y0, y1, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
		cara(vc, p, luz, overlay, 0, -1, 0, w, d, 1, y0, y1, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		cara(vc, p, luz, overlay, 0, 0, 1, w, h, 0, y0, y1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		cara(vc, p, luz, overlay, 0, 0, -1, w, h, 0, y0, y1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, int luz, int overlay, float nx, float ny, float nz,
							 float ancho, float alto, int tipo, float y0, float y1, float... q) {
		float[] us = {0, ancho, ancho, 0}, vs = {alto, alto, 0, 0};
		for (int i = 0; i < 4; i++) {
			float y = q[i * 3 + 1];
			float fy = tipo == 1 ? 0 : tipo == 2 ? 1 : (y - y0) / (y1 - y0);
			float v = 0.66f + 0.34f * fy;
			if (tipo == 1) v *= 0.7f;
			int c = (int) (255 * Math.pow(v, 1 / 2.2));
			vc.addVertex(p, q[i * 3], y, q[i * 3 + 2]).setColor(c, c, c, 255).setUv(us[i], vs[i]).setOverlay(overlay)
					.setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(BichoCuboEntity bicho) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/bichos/no_cuerpo.png");
	}

	/** Click derecho con el Bicho de la cabeza encima: se tira (se avisa al servidor). */
	public static void registrarLanzar() {
		final boolean[] antes = {false};
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			boolean ahora = mc.options.keyUse.isDown();
			boolean lleva = mc.player != null && mc.player.getPassengers().stream()
					.anyMatch(e -> e instanceof BichoCuboEntity b && b.deLaCabeza());
			if (ahora && !antes[0] && lleva && mc.screen == null) ClientPlayNetworking.send(new ModQumara.LanzarPayload());
			antes[0] = ahora;
		});
	}
}
