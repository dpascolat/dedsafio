package com.dedsafio4.client.despegue;

import com.dedsafio4.despegue.NaveViajeEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;

/**
 * El viaje en la nave visto por los pasajeros, en el juego (ver EspacioCielo para los planetas):
 * 1. En los últimos segundos de la subida la pantalla se va a negro (salís de la atmósfera).
 * 2. Aparecés en el Espacio: se aclara y viajás de verdad unos 15 segundos (estrellas, los dos planetas y polvo
 *    espacial pasando rápido). Al final se vuelve a negro.
 * 3. Llegás al cielo del planeta de destino: se aclara mientras la nave aterriza.
 * Mientras cambia de dimensión (y en el "Cargando terreno") queda negro.
 */
public final class NaveCinematica {
	private NaveCinematica() {}

	/** Cuánto tardan los fundidos a negro y desde negro, en ticks. */
	private static final int OSCURECER = 45, FUNDIDO = 20, ACLARAR_LLEGADA = 40;
	/** Si por algo no aparece la nave nueva, igual se saca el negro después de este tiempo. */
	private static final int ESPERA_MAXIMA = 200;

	private static int estadoAntes = -1, ticksEstado, ticksSinNave;
	/** Está en medio de un viaje (entre el final de la subida y el aterrizaje). */
	private static boolean viajando;
	private static float negro, negroAntes;

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			negroAntes = negro;
			NaveViajeEntity nave = mc.player != null && mc.player.getVehicle() instanceof NaveViajeEntity n ? n : null;
			int estado = nave == null ? -1 : nave.estado();
			if (estado != estadoAntes) {
				ticksEstado = 0;
				estadoAntes = estado;
			}
			ticksEstado++;
			if (nave != null) ticksSinNave = 0;
			switch (estado) {
				case NaveViajeEntity.DESPEGANDO -> {
					int desde = NaveViajeEntity.TIEMPO_DESPEGUE - OSCURECER;
					negro = Mth.clamp((ticksEstado - desde) / (float) OSCURECER, 0f, 1f);
					if (negro > 0.95f) viajando = true;
				}
				case NaveViajeEntity.ESPACIO -> {
					viajando = true;
					int fin = NaveViajeEntity.TIEMPO_ESPACIO;
					negro = Math.max(1f - ticksEstado / (float) FUNDIDO, Mth.clamp((ticksEstado - (fin - FUNDIDO)) / (float) FUNDIDO, 0f, 1f));
					polvo(mc);
				}
				case NaveViajeEntity.ATERRIZANDO, NaveViajeEntity.QUIETA -> {
					if (viajando) {
						negro = 1f - Mth.clamp(ticksEstado / (float) ACLARAR_LLEGADA, 0f, 1f);
						if (negro <= 0) viajando = false;
					} else {
						negro = 0;
					}
				}
				case -1 -> {
					// Cambiando de dimensión (un momento sin nave): sigue negro.
					if (viajando) {
						negro = 1f;
						if (++ticksSinNave > ESPERA_MAXIMA) {
							viajando = false;
							negro = 0;
						}
					} else {
						negro = 0;
					}
				}
				default -> {
					viajando = false;
					negro = 0;
				}
			}
		});
		HudRenderCallback.EVENT.register((g, tiempo) ->
				pintarNegro(g, Mth.lerp(tiempo.getGameTimeDeltaPartialTick(true), negroAntes, negro)));
		// La pantalla de "Cargando terreno" del cambio de dimensión también queda negra.
		ScreenEvents.AFTER_INIT.register((mc, pantalla, ancho, alto) -> {
			if (pantalla instanceof ReceivingLevelScreen) {
				ScreenEvents.afterRender(pantalla).register((p, g, x, y, delta) -> {
					if (viajando) pintarNegro(g, 1f);
				});
			}
		});
	}

	/** Cuánto va del viaje por el Espacio, de 0 a 1 (para los planetas de EspacioCielo). */
	public static float progresoEspacio(float parcial) {
		if (estadoAntes != NaveViajeEntity.ESPACIO) return 0f;
		return Mth.clamp((ticksEstado + parcial) / NaveViajeEntity.TIEMPO_ESPACIO, 0f, 1f);
	}

	/** Polvo espacial pasando rápido de arriba para abajo, para que se sienta la velocidad. */
	private static void polvo(Minecraft mc) {
		if (mc.level == null || mc.player == null) return;
		for (int i = 0; i < 6; i++) {
			double x = mc.player.getX() + (mc.level.random.nextDouble() - 0.5) * 24;
			double y = mc.player.getY() + 20 + mc.level.random.nextDouble() * 25;
			double z = mc.player.getZ() + (mc.level.random.nextDouble() - 0.5) * 24;
			mc.level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, -2.2, 0);
		}
	}

	private static void pintarNegro(GuiGraphics g, float cuanto) {
		if (cuanto <= 0.001f) return;
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), Mth.clamp((int) (cuanto * 255), 0, 255) << 24);
	}
}
