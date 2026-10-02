package com.dedsafio4.client.despegue;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.despegue.NaveViajeEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * El viaje en la nave entre las dos dimensiones, visto por los pasajeros:
 * 1. En los últimos segundos del despegue la pantalla se va a negro (salís de la atmósfera).
 * 2. Cinemática en el espacio: estrellas, el planeta de donde salís (grande) y el de destino (chico); la nave sale de
 *    uno, viaja en curva dejando una estela de fuego y entra en el otro, mientras el destino se acerca. Del Overworld
 *    (azul y verde) al planeta rojo (la Dimensión de los Órganos), o al revés si volvés.
 * 3. Al llegar, el negro se va aclarando mientras la nave aterriza.
 * Todo en la pantalla del jugador; el servidor espera lo que dura la cinemática (NaveViajeEntity.TIEMPO_ESPACIO).
 */
public final class NaveCinematica {
	private NaveCinematica() {}

	private static final ResourceLocation TIERRA = textura("textures/gui/planeta_overworld.png");
	private static final ResourceLocation ROJO = textura("textures/gui/planeta_rojo.png");
	private static final ResourceLocation NAVE = textura("textures/item/nave_biplaza.png");

	/** Cuánto tarda en ponerse negro al final de la subida, cuánto dura la cinemática y cuánto tarda en aclararse. */
	private static final int OSCURECER = 45, CINE = NaveViajeEntity.TIEMPO_ESPACIO, ACLARAR = 50;
	/** Si por algo no llega la nave nueva, igual se termina después de este tiempo. */
	private static final int ESPERA_MAXIMA = CINE + 200;

	private enum Fase { NADA, OSCURECIENDO, CINEMATICA, ACLARANDO }

	private static Fase fase = Fase.NADA;
	private static int ticks;
	/** Hacia dónde va: true = del Overworld al planeta rojo. */
	private static boolean haciaRojo;

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			NaveViajeEntity nave = mc.player != null && mc.player.getVehicle() instanceof NaveViajeEntity n ? n : null;
			int estado = nave == null ? -1 : nave.estado();
			switch (fase) {
				case NADA -> {
					if (estado == NaveViajeEntity.DESPEGANDO) {
						fase = Fase.OSCURECIENDO;
						ticks = 0;
						haciaRojo = mc.level != null && mc.level.dimension().equals(Level.OVERWORLD);
					}
				}
				case OSCURECIENDO -> {
					if (estado == NaveViajeEntity.DESPEGANDO) ticks++;
					// Al terminar la subida (o al saltar de dimensión) empieza la cinemática.
					if (estado != NaveViajeEntity.DESPEGANDO) {
						if (estado == NaveViajeEntity.QUIETA || estado == NaveViajeEntity.CUENTA) fase = Fase.NADA;
						else empezar(Fase.CINEMATICA);
					}
				}
				case CINEMATICA -> {
					ticks++;
					if (estado == NaveViajeEntity.ATERRIZANDO || estado == NaveViajeEntity.QUIETA || ticks > ESPERA_MAXIMA) {
						empezar(Fase.ACLARANDO);
					}
				}
				case ACLARANDO -> {
					if (++ticks >= ACLARAR) fase = Fase.NADA;
				}
			}
		});
		HudRenderCallback.EVENT.register((g, tiempo) -> dibujar(g, tiempo.getGameTimeDeltaPartialTick(true)));
		// La pantalla de "Cargando terreno" del cambio de dimensión también muestra la cinemática.
		ScreenEvents.AFTER_INIT.register((mc, pantalla, ancho, alto) -> {
			if (pantalla instanceof ReceivingLevelScreen) {
				ScreenEvents.afterRender(pantalla).register((p, g, x, y, delta) -> {
					if (fase == Fase.OSCURECIENDO || fase == Fase.CINEMATICA) cinematica(g, fase == Fase.CINEMATICA ? ticks + delta : 0);
				});
			}
		});
	}

	private static void empezar(Fase nueva) {
		fase = nueva;
		ticks = 0;
	}

	private static void dibujar(GuiGraphics g, float parcial) {
		switch (fase) {
			case OSCURECIENDO -> {
				int desde = NaveViajeEntity.TIEMPO_DESPEGUE - OSCURECER;
				negro(g, Mth.clamp((ticks + parcial - desde) / OSCURECER, 0f, 1f));
			}
			case CINEMATICA -> cinematica(g, ticks + parcial);
			case ACLARANDO -> negro(g, 1f - Mth.clamp((ticks + parcial) / ACLARAR, 0f, 1f));
			default -> {}
		}
	}

	private static void negro(GuiGraphics g, float cuanto) {
		if (cuanto <= 0.001f) return;
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), (Mth.clamp((int) (cuanto * 255), 0, 255) << 24));
	}

	/** Un cuadro de la cinemática, {@code t} ticks desde que empezó. */
	private static void cinematica(GuiGraphics g, float t) {
		int ancho = g.guiWidth(), alto = g.guiHeight();
		float p = Mth.clamp(t / CINE, 0f, 1f);
		float suave = p * p * (3 - 2 * p);
		g.fill(0, 0, ancho, alto, 0xFF03030A);
		estrellas(g, t, ancho, alto);

		// Los planetas: el de salida grande abajo a la izquierda y se va alejando; el destino chico arriba a la
		// derecha y se va acercando (la "cámara" acompaña a la nave).
		float tamSalida = alto * 0.75f * (1 - 0.45f * suave);
		float salidaX = ancho * 0.2f - suave * ancho * 0.3f, salidaY = alto * 0.68f + suave * alto * 0.1f;
		float tamDestino = alto * (0.16f + 0.62f * suave);
		float destinoX = ancho * 0.84f - suave * ancho * 0.24f, destinoY = alto * 0.34f + suave * alto * 0.12f;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		planeta(g, haciaRojo ? TIERRA : ROJO, salidaX, salidaY, tamSalida);
		planeta(g, haciaRojo ? ROJO : TIERRA, destinoX, destinoY, tamDestino);

		// La nave: sale del borde del planeta de salida, hace una curva y entra en el de destino.
		float[] ahora = posicionNave(p, salidaX, salidaY, tamSalida, destinoX, destinoY);
		// La estela: dónde estuvo hace un ratito.
		for (int k = 12; k >= 1; k--) {
			float pk = Mth.clamp((t - k * 1.5f) / CINE, 0f, 1f);
			if (pk <= 0.02f) continue;
			float[] antes = posicionNave(pk, salidaX, salidaY, tamSalida, destinoX, destinoY);
			int a = (int) (200 * (1 - k / 13f));
			int color = k < 5 ? 0xFFE070 : k < 9 ? 0xFF8A30 : 0xC03010;
			int lado = Math.max(1, (int) (4 * (1 - k / 14f)));
			g.fill((int) antes[0] - lado / 2, (int) antes[1] - lado / 2, (int) antes[0] + lado / 2 + 1, (int) antes[1] + lado / 2 + 1,
					(a << 24) | color);
		}
		float tamNave = 28 * (p < 0.85f ? 1 : 1 - (p - 0.85f) / 0.15f);   // se achica al entrar en el planeta
		if (tamNave > 0.5f) {
			float[] adelante = posicionNave(Math.min(1, p + 0.01f), salidaX, salidaY, tamSalida, destinoX, destinoY);
			float angulo = (float) Math.toDegrees(Math.atan2(adelante[1] - ahora[1], adelante[0] - ahora[0])) + 90f;
			PoseStack pose = g.pose();
			pose.pushPose();
			pose.translate(ahora[0], ahora[1], 0);
			pose.mulPose(Axis.ZP.rotationDegrees(angulo));
			g.blit(NAVE, (int) (-tamNave / 2), (int) (-tamNave / 2), (int) tamNave, (int) tamNave, 0, 0, 16, 16, 16, 16);
			pose.popPose();
		}
		RenderSystem.disableBlend();

		String texto = haciaRojo ? "Viajando a la Dimensión de los Órganos..." : "Volviendo al Overworld...";
		Minecraft mc = Minecraft.getInstance();
		g.drawCenteredString(mc.font, texto, ancho / 2, alto - 24, 0xFFC6CFD6);

		// Entra desde negro y se va a negro al final.
		negro(g, 1f - Mth.clamp(t / 15f, 0f, 1f));
		negro(g, Mth.clamp((t - (CINE - 15)) / 15f, 0f, 1f));
	}

	/** Dónde está la nave (en pantalla) en la parte {@code p} del viaje: una curva que sube y baja. */
	private static float[] posicionNave(float p, float salidaX, float salidaY, float tamSalida, float destinoX, float destinoY) {
		float x0 = salidaX + tamSalida * 0.3f, y0 = salidaY - tamSalida * 0.3f;
		float cx = (x0 + destinoX) / 2, cy = Math.min(y0, destinoY) - 60;
		float u = 1 - p;
		return new float[]{u * u * x0 + 2 * u * p * cx + p * p * destinoX, u * u * y0 + 2 * u * p * cy + p * p * destinoY};
	}

	private static void planeta(GuiGraphics g, ResourceLocation textura, float x, float y, float tam) {
		int t = Math.max(2, (int) tam);
		g.blit(textura, (int) (x - t / 2f), (int) (y - t / 2f), t, t, 0, 0, 96, 96, 96, 96);
	}

	/** Estrellas fijas que titilan y se corren despacio (las de adelante más rápido). */
	private static void estrellas(GuiGraphics g, float t, int ancho, int alto) {
		for (int i = 0; i < 140; i++) {
			long h = (i * 2654435761L) ^ (i * 40503L);
			float profundidad = 0.3f + ((h >>> 3) & 7) / 10f;
			float x = ((h >>> 8) & 0xFFFF) / 65535f * ancho - t * 0.4f * profundidad;
			x = ((x % ancho) + ancho) % ancho;
			float y = ((h >>> 24) & 0xFFFF) / 65535f * alto;
			int brillo = (int) (140 + 115 * (0.5f + 0.5f * Mth.sin(t * 0.15f + i)));
			int lado = profundidad > 0.8f ? 2 : 1;
			g.fill((int) x, (int) y, (int) x + lado, (int) y + lado, 0xFF000000 | brillo << 16 | brillo << 8 | Math.min(255, brillo + 20));
		}
	}

	private static ResourceLocation textura(String ruta) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, ruta);
	}
}
