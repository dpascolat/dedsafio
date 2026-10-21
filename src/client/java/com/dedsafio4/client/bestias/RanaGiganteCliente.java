package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.RanaGiganteEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Cuando una Rana Gigante te tiene agarrado: cada vez que aprietas espacio se avisa al servidor (hacen falta 100) y
 * abajo en la pantalla sale la barra de apretar espacio, que brilla un momento con cada apretón.
 */
public final class RanaGiganteCliente {
	private RanaGiganteCliente() {}

	private static final ResourceLocation BARRA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/rana_gigante_barra.png");
	private static final ResourceLocation BARRA_BRILLO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/rana_gigante_barra_brillo.png");
	private static final int TEX_ANCHO = 329, TEX_ALTO = 98, ANCHO = 164, ALTO = 49, DESDE_ABAJO = 90, BRILLO = 6;

	private static boolean agarrado, tragado, saltoAntes;
	private static int brillo;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(RanaGiganteEntity.AgarrePayload.TYPE, (payload, contexto) -> {
			agarrado = payload.agarrado();
			tragado = payload.tragado();
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (brillo > 0) brillo--;
			if (!agarrado || tragado || mc.player == null) {
				saltoAntes = false;
				return;
			}
			boolean salto = mc.options.keyJump.isDown();
			if (salto && !saltoAntes) {
				ClientPlayNetworking.send(new RanaGiganteEntity.EspacioPayload());
				brillo = BRILLO;
			}
			saltoAntes = salto;
		});
		HudRenderCallback.EVENT.register((g, tiempo) -> dibujar(g));
	}

	private static void dibujar(GuiGraphics g) {
		if (!agarrado || tragado) return;
		int x = (g.guiWidth() - ANCHO) / 2, y = g.guiHeight() - DESDE_ABAJO;
		g.blit(brillo > 0 ? BARRA_BRILLO : BARRA, x, y, ANCHO, ALTO, 0, 0, TEX_ANCHO, TEX_ALTO, TEX_ANCHO, TEX_ALTO);
	}
}
