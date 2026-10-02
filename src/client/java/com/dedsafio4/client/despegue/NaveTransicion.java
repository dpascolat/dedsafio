package com.dedsafio4.client.despegue;

import com.dedsafio4.despegue.NaveViajeEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.util.Mth;

/**
 * El viaje entre dimensiones en la nave, sin el salto de golpe: en los últimos segundos del despegue la pantalla se
 * va poniendo blanca (como atravesar la atmósfera), queda blanca mientras cambia de dimensión (tapando también el
 * "Cargando terreno") y al llegar se va aclarando de a poco mientras la nave baja.
 */
public final class NaveTransicion {
	private NaveTransicion() {}

	/** Cuántos ticks antes del salto empieza a ponerse blanco, y cuánto tarda en aclararse al llegar. */
	private static final int ACLARAR = 50, OSCURECER = 45;
	/** Si por algo no llega la nave nueva, igual se saca el blanco después de este tiempo. */
	private static final int ESPERA_MAXIMA = 200;

	private static float blanco, blancoAntes;
	private static boolean viajando;
	private static int ticksDespegue, ticksLlegada, ticksSinNave;

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			blancoAntes = blanco;
			NaveViajeEntity nave = mc.player != null && mc.player.getVehicle() instanceof NaveViajeEntity n ? n : null;
			int estado = nave == null ? -1 : nave.estado();
			if (estado == NaveViajeEntity.DESPEGANDO) {
				ticksDespegue++;
				int desde = NaveViajeEntity.TIEMPO_DESPEGUE - OSCURECER;
				blanco = Mth.clamp((ticksDespegue - desde) / (float) OSCURECER, 0f, 1f);
				if (blanco > 0.95f) viajando = true;
				ticksLlegada = 0;
				ticksSinNave = 0;
				return;
			}
			ticksDespegue = 0;
			if (!viajando) {
				blanco = 0;
				return;
			}
			if (estado == NaveViajeEntity.ATERRIZANDO || estado == NaveViajeEntity.QUIETA) {
				// Llegó: se aclara de a poco.
				ticksLlegada++;
				blanco = 1f - Mth.clamp(ticksLlegada / (float) ACLARAR, 0f, 1f);
				if (blanco <= 0) viajando = false;
			} else {
				// Cambiando de dimensión (un momento sin nave): sigue todo blanco.
				blanco = 1f;
				if (++ticksSinNave > ESPERA_MAXIMA) {
					viajando = false;
					blanco = 0;
				}
			}
		});
		HudRenderCallback.EVENT.register((g, tiempo) -> dibujar(g, tiempo.getGameTimeDeltaPartialTick(true)));
		// La pantalla de "Cargando terreno" del cambio de dimensión también queda blanca.
		ScreenEvents.AFTER_INIT.register((mc, pantalla, ancho, alto) -> {
			if (pantalla instanceof ReceivingLevelScreen) {
				ScreenEvents.afterRender(pantalla).register((p, g, x, y, delta) -> {
					if (viajando) g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xFFFFFFFF);
				});
			}
		});
	}

	private static void dibujar(GuiGraphics g, float parcial) {
		float b = Mth.lerp(parcial, blancoAntes, blanco);
		if (b <= 0.001f) return;
		int alfa = Mth.clamp((int) (b * 255), 0, 255);
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alfa << 24) | 0xFFFFFF);
	}
}
