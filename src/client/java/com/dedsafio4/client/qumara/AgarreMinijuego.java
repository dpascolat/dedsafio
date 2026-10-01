package com.dedsafio4.client.qumara;

import com.dedsafio4.qumara.ModQumara;
import com.dedsafio4.qumara.QumaraEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * El minijuego del agarrado por Qumara: un cosito recorre la barra de colores de un lado al otro y hay que
 * pulsar el espacio cuando está dentro del recuadro blanco; cada acierto lo cambia de lugar. Arriba, los
 * 15 cuadraditos (blancos los que ya acertaste); abajo, el tiempo que queda para pulsar (10 s) y los
 * fallos (5 y te mata).
 *
 * El cliente decide si acertó (ve el cosito y el recuadro) y se lo manda al servidor, que lleva la cuenta.
 */
public final class AgarreMinijuego {
	private AgarreMinijuego() {}

	/** Largo de la barra y ancho del recuadro (fracción de la barra). */
	private static final int LARGO = 220;
	private static final float ANCHO_ZONA = 0.16f;
	/** Entre pulsación y pulsación (para que no se pueda apretar sin parar). */
	private static final long ESPERA_MS = 150;

	private static long agarreVisto = Long.MIN_VALUE;
	private static float fase;
	private static long ultimoNanos, ultimaPulsada;
	private static long flashHasta;
	private static boolean flashBien;
	/** Después de un acierto, no deja pulsar hasta que el servidor lo cuente (así no vale dos veces el mismo recuadro). */
	private static int esperandoAciertos = -1;
	private static long esperandoHasta;

	/** La Qumara que agarró al jugador de este cliente, con el minijuego en marcha (o null). */
	public static QumaraEntity activa() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !(mc.player.getVehicle() instanceof QumaraEntity q)) return null;
		return q.agarradoId() == mc.player.getId() && q.minijuegoActivo() ? q : null;
	}

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			QumaraEntity q = activa();
			if (q == null) return;
			while (mc.options.keyJump.consumeClick()) pulsar(q);
		});
	}

	/** Dónde está el cosito (0 a 1), yendo y viniendo; cada vez un poco más rápido. */
	public static float marcador() {
		float f = fase % 2;
		return f < 1 ? f : 2 - f;
	}

	/** El centro del recuadro: cambia con cada acierto (igual para el mismo agarre). */
	public static float zona(QumaraEntity q) {
		long semilla = q.inicioAgarre() * 31 + q.aciertos() * 7919L;
		java.util.Random r = new java.util.Random(semilla);
		return 0.12f + r.nextFloat() * 0.76f;
	}

	public static boolean enLaZona(QumaraEntity q) {
		return Math.abs(marcador() - zona(q)) <= ANCHO_ZONA / 2 + 0.01f;
	}

	public static void pulsar(QumaraEntity q) {
		long ahora = System.currentTimeMillis();
		if (ahora - ultimaPulsada < ESPERA_MS) return;
		if (q.aciertos() == esperandoAciertos && ahora < esperandoHasta) return;
		ultimaPulsada = ahora;
		boolean bien = enLaZona(q);
		if (bien) {
			esperandoAciertos = q.aciertos();
			esperandoHasta = ahora + 1000;
		}
		flashBien = bien;
		flashHasta = ahora + 250;
		ClientPlayNetworking.send(new ModQumara.MinijuegoPayload(bien));
	}

	/** Avanza el cosito con el tiempo real (cada cuadro). */
	private static void avanzar(QumaraEntity q) {
		long n = System.nanoTime();
		if (agarreVisto != q.inicioAgarre()) {
			agarreVisto = q.inicioAgarre();
			fase = 0;
			ultimoNanos = n;
		}
		float dt = Math.min(0.1f, (n - ultimoNanos) / 1e9f);
		ultimoNanos = n;
		fase += dt * (0.75f + 0.05f * q.aciertos());
	}

	private static final int[] COLORES = {0xD42A2A, 0xF08A20, 0xF5D23A, 0xF2F2F2, 0x3A5BE0, 0xA02CC8};

	private static int degrade(float k) {
		float x = Mth.clamp(k, 0, 1) * (COLORES.length - 1);
		int i = Math.min(COLORES.length - 2, (int) x);
		float f = x - i;
		int a = COLORES[i], b = COLORES[i + 1];
		int r = (int) Mth.lerp(f, (a >> 16) & 0xFF, (b >> 16) & 0xFF), g = (int) Mth.lerp(f, (a >> 8) & 0xFF, (b >> 8) & 0xFF),
				bl = (int) Mth.lerp(f, a & 0xFF, b & 0xFF);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	private static void borde(GuiGraphics g, int x0, int y0, int x1, int y1, int grosor, int color) {
		g.fill(x0, y0, x1, y0 + grosor, color);
		g.fill(x0, y1 - grosor, x1, y1, color);
		g.fill(x0, y0, x0 + grosor, y1, color);
		g.fill(x1 - grosor, y0, x1, y1, color);
	}

	public static void dibujar(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		QumaraEntity q = activa();
		if (q == null || mc.options.hideGui) return;
		avanzar(q);
		// Arriba del centro (abajo están los corazones y la barra).
		int cx = g.guiWidth() / 2, y = g.guiHeight() / 2 - 26, x0 = cx - LARGO / 2;
		long ahora = System.currentTimeMillis();

		// Los 15 cuadraditos (blancos los acertados) y, a la derecha, los 5 fallos.
		int n = QumaraEntity.ACIERTOS_PARA_SALVARSE, lado = 7, paso = 9;
		int fx = cx - (n * paso) / 2;
		for (int i = 0; i < n; i++) {
			int x = fx + i * paso, yy = y - 22;
			if (i < q.aciertos()) {
				g.fill(x, yy, x + lado, yy + lado, 0xFF111111);
				g.fill(x + 1, yy + 1, x + lado - 1, yy + lado - 1, 0xFFF4F4F4);
			} else {
				g.fill(x, yy, x + lado, yy + lado, 0xFF111111);
			}
		}

		// La barra de colores.
		for (int i = 0; i < LARGO; i++) g.fill(x0 + i, y - 1, x0 + i + 1, y + 2, degrade(i / (float) (LARGO - 1)));
		// El recuadro (verde o rojo un ratito al pulsar).
		int zc = x0 + Math.round(zona(q) * LARGO), zw = Math.round(ANCHO_ZONA * LARGO);
		int relleno = ahora < flashHasta ? (flashBien ? 0xFF7CE06A : 0xFFE05050) : 0xFFEEF2FA;
		g.fill(zc - zw / 2, y - 6, zc + zw / 2, y + 7, relleno);
		borde(g, zc - zw / 2 - 2, y - 8, zc + zw / 2 + 2, y + 9, 2, 0xFF151515);
		// El cosito.
		int mx = x0 + Math.round(marcador() * LARGO);
		g.fill(mx - 3, y - 11, mx + 3, y + 12, 0xFF3A3A3A);
		g.fill(mx - 2, y - 10, mx + 2, y + 11, 0xFFE6E6E6);

		// Lo que queda para pulsar (10 s) y los fallos.
		float queda = Mth.clamp(q.tiempoParaPulsar(mc.getTimer().getGameTimeDeltaPartialTick(true)) / QumaraEntity.SEGUNDOS_SIN_PULSAR, 0, 1);
		int tw = 60, ty = y + 14;
		g.fill(cx - tw / 2 - 1, ty - 1, cx + tw / 2 + 1, ty + 6, 0xFF3A3A3A);
		g.fill(cx - tw / 2, ty, cx - tw / 2 + Math.round(tw * queda), ty + 5, queda < 0.3f ? 0xFFFF6A6A : 0xFFF4F4F4);
		for (int i = 0; i < QumaraEntity.FALLOS_PARA_MORIR; i++) {
			int x = cx + tw / 2 + 8 + i * 8;
			g.fill(x, ty - 1, x + 6, ty + 5, 0xFF151515);
			g.fill(x + 1, ty, x + 5, ty + 4, i < q.fallos() ? 0xFFE03030 : 0xFF4A4A4A);
		}

		String texto = "PULSA [" + mc.options.keyJump.getTranslatedKeyMessage().getString().toUpperCase() + "]";
		g.drawCenteredString(mc.font, texto, cx, y - 36, 0xFFFFFFFF);
	}
}
