package com.dedsafio4.client.cofres;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.Set;

/** El Cofre Amarillo en la mano y en el inventario: el diseño simple, cerrado, con las patas apoyadas abajo. */
public final class CofreAmarilloItem {
	private CofreAmarilloItem() {}

	private static final MallaCofre MALLA = new MallaCofre("cofre_amarillo.json", Set.of(1), -1);

	public static void dibujar(PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		pose.pushPose();
		pose.translate(0.5, 0, 0.5);
		pose.scale(0.8f, 0.8f, 0.8f);
		pose.translate(0, 4 / 16f, 0);
		MALLA.dibujar(pose, buffers, luz, overlay, 0f);
		pose.popPose();
	}
}
