package com.dedsafio4.client;

import com.dedsafio4.dimension.Portales;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Al entrar al Agua Rara: la pantalla se va poniendo negra mientras espera (los 3 segundos del viaje), queda negra
 * un segundo mientras te lleva y después se aclara de a poco en el lugar nuevo.
 */
public final class FundidoPortal {
	private FundidoPortal() {}

	private static final long QUEDA = 1000, SALE = 1200;
	private static long desde = -1, oscurece;
	/** Salió del agua antes de viajar: la pantalla se aclara. */
	private static boolean cancelado;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(Portales.FundidoPayload.TYPE, (payload, context) -> {
			if (payload.ticks() <= 0) {
				// Salió del agua antes de tiempo: se aclara desde lo oscura que estaba.
				if (desde >= 0) {
					long t = Util.getMillis() - desde;
					float negro = Math.min(1f, (float) t / Math.max(1, oscurece));
					cancelado = true;
					desde = Util.getMillis() - (long) ((1f - negro) * SALE);
				}
				return;
			}
			cancelado = false;
			desde = Util.getMillis();
			oscurece = payload.ticks() * 50L;
		});
		HudRenderCallback.EVENT.register((graphics, tiempo) -> dibujar(graphics));
	}

	private static void dibujar(GuiGraphics g) {
		if (desde < 0) return;
		long t = Util.getMillis() - desde;
		float negro;
		if (cancelado) {
			negro = 1f - (float) t / SALE;
			if (negro <= 0f) {
				desde = -1;
				cancelado = false;
				return;
			}
		} else if (t < oscurece) negro = (float) t / oscurece;
		else if (t < oscurece + QUEDA) negro = 1f;
		else if (t < oscurece + QUEDA + SALE) negro = 1f - (float) (t - oscurece - QUEDA) / SALE;
		else {
			desde = -1;
			return;
		}
		int alfa = Math.round(Math.max(0f, Math.min(1f, negro)) * 255f);
		g.pose().pushPose();
		g.pose().translate(0, 0, 3000);
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), alfa << 24);
		g.pose().popPose();
	}
}
