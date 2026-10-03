package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.RaicesEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * El jugador atrapado por las raíces del Creeper Raíz de Azalea: cada click (o salto) se manda al servidor
 * para librarse, y en la pantalla se ve "¡Atrapado! Haz click rápido" con la barra de cuánto falta.
 */
public final class RaicesCliente {
	private RaicesCliente() {}

	private static boolean saltoAntes;

	/** Las raíces que tienen atrapado al jugador, o null. */
	private static RaicesEntity atrapado(Minecraft mc) {
		if (mc.player == null || mc.level == null) return null;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof RaicesEntity r && r.objetivo() == mc.player.getId() && !r.rota()) return r;
		}
		return null;
	}

	public static void registrar() {
		ClientTickEvents.START_CLIENT_TICK.register(mc -> {   // antes que Minecraft use los clicks
			RaicesEntity raices = atrapado(mc);
			boolean salto = mc.options.keyJump.isDown();
			if (raices != null) {
				while (mc.options.keyAttack.consumeClick()) ClientPlayNetworking.send(new RaicesEntity.ForcejeoPayload());
				if (salto && !saltoAntes) ClientPlayNetworking.send(new RaicesEntity.ForcejeoPayload());
			}
			saltoAntes = salto;
		});
		HudRenderCallback.EVENT.register((g, contador) -> {
			Minecraft mc = Minecraft.getInstance();
			RaicesEntity raices = atrapado(mc);
			if (raices == null || mc.options.hideGui) return;
			int ancho = 140, x = (g.guiWidth() - ancho) / 2, y = g.guiHeight() / 2 + 30;
			String texto = "¡Atrapado! Haz click rápido";
			g.drawString(mc.font, texto, (g.guiWidth() - mc.font.width(texto)) / 2, y - 12, 0xFFE274AC, true);
			g.fill(x - 1, y - 1, x + ancho + 1, y + 7, 0xFF2A1A0C);
			g.fill(x, y, x + ancho, y + 6, 0xFF4A3018);
			g.fill(x, y, x + Math.round(ancho * raices.progreso()), y + 6, 0xFF3AAA3A);
		});
	}
}
