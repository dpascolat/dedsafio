package com.dedsafio4.client.qumara;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.qumara.QumaraEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * La barra de vida de Qumara (la imagen del usuario), arriba en el medio, mientras hay una Qumara nacida
 * y viva cerca. La parte rosa se vacía de derecha a izquierda y lo perdido queda verde oscuro; los
 * pinchitos que ya pasó (75%, 50%, 25%) se ponen grises.
 */
public final class QumaraBarra {
	private QumaraBarra() {}

	private static final ResourceLocation BARRA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/qumara_barra.png");
	/** La imagen sin el relleno rosa (solo enredaderas y flores): en color y en gris. */
	private static final ResourceLocation PINCHOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/qumara_pinchitos.png");
	private static final ResourceLocation GRIS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/qumara_barra_gris.png");
	/** Tamaño de la imagen y la parte rosa (en píxeles de la imagen). */
	private static final int ANCHO = 530, ALTO = 135, RELLENO_X0 = 110, RELLENO_X1 = 505, RELLENO_Y0 = 66, RELLENO_Y1 = 84;
	/** Los pinchitos (enredadera y flor) en la imagen: x desde, x hasta; en orden 75%, 50%, 25%. */
	private static final int[][] PINCHITOS = {{388, 426}, {290, 330}, {192, 234}};
	private static final int PINCHITO_Y0 = 40, PINCHITO_Y1 = 100;
	/** Lo perdido: verde oscuro (como en la referencia del usuario). */
	private static final int VACIO = 0xFF263523, VACIO_BRILLO = 0xFF33472F;
	private static final double DISTANCIA = 96;

	public static void dibujar(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.options.hideGui) return;
		QumaraEntity q = null;
		double mejor = DISTANCIA * DISTANCIA;
		for (QumaraEntity e : mc.level.getEntitiesOfClass(QumaraEntity.class, mc.player.getBoundingBox().inflate(DISTANCIA))) {
			double d = e.distanceToSqr(mc.player);
			if (e.nacida() && !e.derrotada() && d < mejor) {
				mejor = d;
				q = e;
			}
		}
		if (q == null) return;
		float vida = Math.max(0, Math.min(1, q.getHealth() / q.getMaxHealth()));
		float escala = 0.5f;
		int ancho = (int) (ANCHO * escala), x = (g.guiWidth() - ancho) / 2, y = -8;
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(escala, escala, 1);
		RenderSystem.enableBlend();
		g.blit(BARRA, 0, 0, 0, 0, ANCHO, ALTO, ANCHO, ALTO);
		// Lo que ya perdió: verde oscuro (con una línea más clara arriba).
		int desde = RELLENO_X0 + Math.round((RELLENO_X1 - RELLENO_X0) * vida);
		if (desde < RELLENO_X1) {
			g.fill(desde, RELLENO_Y0, RELLENO_X1, RELLENO_Y1, VACIO);
			g.fill(desde, RELLENO_Y0, RELLENO_X1, RELLENO_Y0 + 2, VACIO_BRILLO);
		}
		// Los pinchitos van encima: grises los que ya pasó; los demás, como en la imagen.
		for (int i = 0; i < PINCHITOS.length; i++) {
			int x0 = PINCHITOS[i][0], x1 = PINCHITOS[i][1];
			boolean pasado = i < q.pinchitos();
			if (!pasado && x1 < desde) continue;   // sobre la parte rosa ya se ve bien
			g.blit(pasado ? GRIS : PINCHOS, x0, PINCHITO_Y0, x0, PINCHITO_Y0, x1 - x0, PINCHITO_Y1 - PINCHITO_Y0, ANCHO, ALTO);
		}
		RenderSystem.disableBlend();
		g.pose().popPose();
	}
}
