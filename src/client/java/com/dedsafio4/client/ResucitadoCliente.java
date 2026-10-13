package com.dedsafio4.client;

import com.dedsafio4.baneos.Baneos;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Cuando reviven a alguien (Bloque de Baneados): arriba en la pantalla de todos, "(cara) X ha resucitado" unos
 * segundos. Como el revivido no está conectado, su skin se busca aparte (si no aparece, la de siempre).
 */
public final class ResucitadoCliente {
	private ResucitadoCliente() {}

	private static final long DURA = 5000, FUNDIDO = 500;
	private static final int CELESTE = 0x46CBCC;
	private static String nombre;
	private static long desde = -1;
	private static PlayerSkin skin;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(Baneos.ResucitadoPayload.TYPE, (payload, context) -> {
			nombre = payload.nombre();
			desde = Util.getMillis();
			UUID uuid;
			try {
				uuid = UUID.fromString(payload.uuid());
			} catch (IllegalArgumentException e) {
				uuid = null;
			}
			skin = DefaultPlayerSkin.get(uuid != null ? uuid : new UUID(0, 0));
			if (uuid != null) buscarSkin(uuid, payload.nombre());
		});
		HudRenderCallback.EVENT.register((g, tiempo) -> dibujar(g));
	}

	/** La skin de verdad del revivido (en segundo plano: hay que pedirla a Mojang). */
	private static void buscarSkin(UUID uuid, String quien) {
		Minecraft mc = Minecraft.getInstance();
		CompletableFuture.supplyAsync(() -> {
			var resultado = mc.getMinecraftSessionService().fetchProfile(uuid, false);
			return resultado != null ? resultado.profile() : new GameProfile(uuid, quien);
		}, Util.ioPool()).thenCompose(perfil -> mc.getSkinManager().getOrLoad(perfil))
				.thenAccept(s -> mc.execute(() -> {
					if (quien.equals(nombre)) skin = s;
				}))
				.exceptionally(e -> null);
	}

	private static void dibujar(GuiGraphics g) {
		if (desde < 0 || nombre == null) return;
		long t = Util.getMillis() - desde;
		if (t > DURA) {
			desde = -1;
			return;
		}
		float alfa = Math.min(1f, Math.min(t, DURA - t) / (float) FUNDIDO);
		if (alfa <= 0.02f) return;
		Minecraft mc = Minecraft.getInstance();
		Component texto = Component.literal(nombre + " ha resucitado");
		float escala = 1.5f;
		int ancho = mc.font.width(texto), cara = 8;
		float total = (cara + 4 + ancho) * escala;
		g.pose().pushPose();
		g.pose().translate(g.guiWidth() / 2f - total / 2f, 28, 400);
		g.pose().scale(escala, escala, 1);
		int a = Math.round(alfa * 255) << 24;
		g.setColor(1f, 1f, 1f, alfa);
		if (skin != null) PlayerFaceRenderer.draw(g, skin, 0, 0, cara);
		g.setColor(1f, 1f, 1f, 1f);
		g.drawString(mc.font, texto, cara + 4, 0, a | CELESTE, true);
		g.pose().popPose();
	}
}
