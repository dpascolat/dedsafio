package com.dedsafio4.client.despegue;

import com.dedsafio4.despegue.NaveViajeEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * El tablero que ven los que van arriba de la Nave Espacial Biplaza, medio transparente (como humo):
 * - A la izquierda, al medio: el Combustible (10 barritas, el % y "Reserva" si no alcanza para un viaje).
 * - Arriba al medio: la Altitud (la Y) con su regla de 0 a 1200.
 * - A la derecha: la Ruta de Vuelo, que va marcando cada etapa del viaje; debajo, la Velocidad en m/s.
 * - Mientras hace la cuenta regresiva: "La nave está siendo encendida" con el nombre de quien la enciende.
 */
public final class NaveTablero {
	private NaveTablero() {}

	private static final int CIAN = 0xE69FDCE8, GRIS = 0xE6CFCFCF, TENUE = 0x99E8E4D8, BLANCO = 0xF2FFFFFF;
	private static final int FONDO = 0x55000000, LLENO = 0xE67FE3C4, RESERVA = 0xE6F2B045, VACIO = 0x80D3D3D3;

	/** Velocidad que se muestra (suavizada), en m/s. */
	private static float velocidad;
	private static int estadoAntes = -1, ticksEstado;

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			Player jugador = mc.player;
			if (jugador == null || !(jugador.getVehicle() instanceof NaveViajeEntity nave)) {
				estadoAntes = -1;
				velocidad = 0;
				return;
			}
			int estado = nave.estado();
			if (estado != estadoAntes) {
				estadoAntes = estado;
				ticksEstado = 0;
			}
			ticksEstado++;
			float real = (float) jugador.position().distanceTo(new Vec3(jugador.xo, jugador.yo, jugador.zo)) * 20;
			if (estado == NaveViajeEntity.ESPACIO) {
				// En el Espacio la nave no se mueve de verdad: se muestra el salto y después cómo frena.
				real = nave.etapa() == 4 ? Math.min(99.9f, 25 + ticksEstado * 0.6f) : 30;
			}
			if (real > 200) real = velocidad;   // un salto de dimensión no es velocidad
			velocidad = Mth.lerp(0.25f, velocidad, Math.min(99.9f, real));
		});
		HudRenderCallback.EVENT.register((g, contador) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.options.hideGui || mc.player == null || !(mc.player.getVehicle() instanceof NaveViajeEntity nave)) return;
			Font font = mc.font;
			int ancho = g.guiWidth(), alto = g.guiHeight();
			combustible(g, font, nave, 8, alto / 2 - 66);
			altitud(g, font, (int) Math.floor(mc.player.getY()), ancho / 2, 4);
			int rutaY = Math.max(64, alto / 2 - 78);
			ruta(g, font, nave, ancho - 8, rutaY);
			velocidad(g, font, ancho - 58, rutaY + 136);
			if (nave.estado() == NaveViajeEntity.CUENTA) encendido(g, font, nave, ancho / 2, alto / 4);
		});
	}

	// ---------------------------------------------------------------- Combustible

	private static void combustible(GuiGraphics g, Font font, NaveViajeEntity nave, int x, int y) {
		int porcentaje = nave.combustible();
		boolean alcanza = porcentaje >= NaveViajeEntity.COMBUSTIBLE_VIAJE;
		g.fill(x - 4, y - 4, x + 88, y + 136, FONDO);
		texto(g, font, "COMBUSTIBLE", x, y, CIAN, 0.75f, -1);

		int arriba = y + 14, abajo = arriba + 102;
		// El corchete y las marquitas de 100, 50 y 0.
		g.fill(x, arriba, x + 1, abajo, TENUE);
		g.fill(x, arriba, x + 4, arriba + 1, TENUE);
		g.fill(x, abajo - 1, x + 4, abajo, TENUE);
		int[] marcas = {arriba, (arriba + abajo) / 2, abajo - 1};
		String[] numeros = {"100", "50", "0"};
		for (int i = 0; i < 3; i++) {
			g.fill(x + 32, marcas[i], x + 36, marcas[i] + 1, TENUE);
			texto(g, font, numeros[i], x + 38, marcas[i] - 2, TENUE, 0.6f, -1);
		}
		// Las 10 barritas: se llenan de abajo para arriba (cada una es un 10%).
		int llenas = Math.round(porcentaje / 10f);
		for (int i = 0; i < 10; i++) {
			boolean llena = 9 - i < llenas;
			pastilla(g, x + 5, arriba + 2 + i * 10, 24, 8, llena ? (alcanza ? LLENO : RESERVA) : VACIO);
		}
		texto(g, font, porcentaje + "%", x + 84, (arriba + abajo) / 2 + 8, alcanza ? GRIS : RESERVA, 1.4f, 1);
		// "Reserva": titila si no alcanza para un viaje.
		boolean titila = !alcanza && (Util.getMillis() / 500) % 2 == 0;
		texto(g, font, "RESERVA", x + 6, abajo + 6, titila ? RESERVA : TENUE, 0.75f, -1);
	}

	/** Una barrita con las puntas redondeadas. */
	private static void pastilla(GuiGraphics g, int x, int y, int ancho, int alto, int color) {
		g.fill(x + 2, y, x + ancho - 2, y + alto, color);
		g.fill(x + 1, y + 1, x + 2, y + alto - 1, color);
		g.fill(x + ancho - 2, y + 1, x + ancho - 1, y + alto - 1, color);
		g.fill(x, y + 2, x + 1, y + alto - 2, color);
		g.fill(x + ancho - 1, y + 2, x + ancho, y + alto - 2, color);
	}

	// ---------------------------------------------------------------- Altitud

	private static void altitud(GuiGraphics g, Font font, int y, int centro, int arriba) {
		int mitad = 80;
		g.fill(centro - mitad - 8, arriba - 2, centro + mitad + 8, arriba + 46, FONDO);
		texto(g, font, "ALTITUD / Y", centro, arriba, CIAN, 0.75f, 0);
		texto(g, font, y + " m", centro, arriba + 9, GRIS, 1.3f, 0);
		int regla = arriba + 34;
		for (int i = 0; i <= 4; i++) {
			texto(g, font, Integer.toString(i * 300), centro - mitad + i * 2 * mitad / 4, regla - 9, TENUE, 0.6f, 0);
		}
		g.fill(centro - mitad, regla, centro + mitad + 1, regla + 1, CIAN);
		for (int i = 0; i <= 20; i++) {
			int tx = centro - mitad + i * 2 * mitad / 20;
			g.fill(tx, regla - 2, tx + 1, regla, CIAN);
		}
		// El triangulito que marca la altura.
		int mx = centro - mitad + Math.round(Mth.clamp(y, 0, 1200) / 1200f * 2 * mitad);
		for (int k = 0; k < 4; k++) g.fill(mx - k, regla + 3 + k, mx + k + 1, regla + 4 + k, CIAN);
	}

	// ---------------------------------------------------------------- Ruta de vuelo

	private static void ruta(GuiGraphics g, Font font, NaveViajeEntity nave, int derecha, int y) {
		int etapa = nave.etapa();
		g.fill(derecha - 128, y - 4, derecha + 4, y + 130, FONDO);
		texto(g, font, "RUTA DE VUELO", derecha, y, CIAN, 0.75f, 1);
		g.fill(derecha - 120, y + 9, derecha, y + 10, CIAN);
		String estado = etapa == 0 && nave.estado() == NaveViajeEntity.CUENTA ? "ENCENDIENDO"
				: NaveViajeEntity.ETAPAS[etapa].toUpperCase(Locale.ROOT);
		texto(g, font, estado, derecha, y + 14, TENUE, 0.65f, 1);

		int cx = derecha - 5, primera = y + 30, paso = 14;
		g.fill(cx, primera, cx + 1, primera + paso * 6, VACIO);
		for (int i = 1; i <= 7; i++) {
			int cy = primera + (i - 1) * paso;
			boolean hecha = i < etapa, actual = i == etapa;
			int color = actual ? BLANCO : hecha ? CIAN : VACIO;
			if (actual && (Util.getMillis() / 400) % 2 == 0) color = CIAN;
			circulo(g, cx, cy, 4, color, hecha || actual);
			texto(g, font, NaveViajeEntity.ETAPAS[i].toUpperCase(Locale.ROOT), cx - 9, cy - 3,
					actual ? BLANCO : hecha ? CIAN : GRIS, 0.65f, 1);
		}
	}

	/** Un circulito de radio r (relleno o solo el borde). */
	private static void circulo(GuiGraphics g, int cx, int cy, int r, int color, boolean relleno) {
		// Por dentro se tapa la línea que une los círculos.
		for (int dy = -r; dy <= r; dy++) {
			int dx = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.5));
			if (relleno) {
				g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
			} else {
				g.fill(cx - dx, cy + dy, cx - dx + 1, cy + dy + 1, color);
				g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
				if (Math.abs(dy) == r) g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
			}
		}
	}

	// ---------------------------------------------------------------- Velocidad

	private static void velocidad(GuiGraphics g, Font font, int centro, int y) {
		g.fill(centro - 50, y - 4, centro + 54, y + 52, FONDO);
		texto(g, font, "VELOCIDAD", centro, y, CIAN, 0.75f, 0);
		String numero = String.format(Locale.ROOT, "%04.1f", velocidad);
		float escala = 2f;
		int ancho = Math.round(font.width(numero) * escala);
		// Número con borde oscuro, como en el diseño.
		for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
			texto(g, font, numero, centro - ancho / 2 + d[0], y + 11 + d[1], 0xE67E7474, escala, -1);
		}
		texto(g, font, numero, centro - ancho / 2, y + 11, 0xF2F4F2EF, escala, -1);
		texto(g, font, "m/s", centro, y + 30, TENUE, 0.75f, 0);
		// Las rayitas de abajo se prenden según lo rápido que va.
		int prendidas = Math.round(velocidad / 10f);
		for (int i = 0; i < 10; i++) {
			int rx = centro - 45 + i * 10;
			g.fill(rx, y + 42, rx + 6, y + 43, i < prendidas ? CIAN : VACIO);
		}
	}

	// ---------------------------------------------------------------- Encendido (cuenta regresiva)

	private static void encendido(GuiGraphics g, Font font, NaveViajeEntity nave, int centro, int y) {
		Entity piloto = nave.getPassengers().isEmpty() ? null : nave.getPassengers().get(0);
		String nombre = piloto == null ? "" : piloto.getName().getString().toUpperCase(Locale.ROOT);
		int mitad = 110, alto = 64;
		g.fill(centro - mitad, y, centro + mitad, y + alto, FONDO);
		// Las esquinas.
		int l = 12;
		for (int[] e : new int[][]{{centro - mitad, y, 1, 1}, {centro + mitad - 1, y, -1, 1},
				{centro - mitad, y + alto - 1, 1, -1}, {centro + mitad - 1, y + alto - 1, -1, -1}}) {
			int ex = e[0], ey = e[1];
			g.fill(Math.min(ex, ex + e[2] * l), ey, Math.max(ex, ex + e[2] * l) + 1, ey + 1, VACIO);
			g.fill(ex, Math.min(ey, ey + e[3] * l), ex + 1, Math.max(ey, ey + e[3] * l) + 1, VACIO);
		}
		texto(g, font, "LA NAVE ESTÁ SIENDO ENCENDIDA", centro, y + 7, CIAN, 0.8f, 0);
		texto(g, font, nombre, centro, y + 18, GRIS, 1.3f, 0);
		texto(g, font, "SOLO UN TRIPULANTE PUEDE ENCENDERLA", centro, y + 36, TENUE, 0.65f, 0);
		texto(g, font, "ESPERA A QUE TERMINE LA CARGA", centro, y + 45, TENUE, 0.65f, 0);
		// La carga.
		float carga = Mth.clamp(ticksEstado / (float) NaveViajeEntity.TIEMPO_CUENTA, 0, 1);
		int barra = mitad - 30;
		g.fill(centro - barra, y + 55, centro + barra, y + 56, VACIO);
		g.fill(centro - barra, y + 55, centro - barra + Math.round(2 * barra * carga), y + 56, CIAN);
	}

	/** Texto a cierta escala; alineación -1 = desde x hacia la derecha, 0 = centrado, 1 = termina en x. */
	private static void texto(GuiGraphics g, Font font, String texto, int x, int y, int color, float escala, int alineacion) {
		float ancho = font.width(texto) * escala;
		float desde = alineacion < 0 ? x : alineacion == 0 ? x - ancho / 2 : x - ancho;
		g.pose().pushPose();
		g.pose().translate(desde, y, 0);
		g.pose().scale(escala, escala, 1);
		g.drawString(font, texto, 0, 0, color, false);
		g.pose().popPose();
	}
}
