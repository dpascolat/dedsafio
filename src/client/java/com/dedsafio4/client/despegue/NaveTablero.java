package com.dedsafio4.client.despegue;

import com.dedsafio4.despegue.NaveViajeEntity;
import com.mojang.blaze3d.systems.RenderSystem;
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
 * El tablero que ven los que van arriba de la Nave Espacial Biplaza, dibujado todo con código (rayas, rectángulos y
 * letras, sin imágenes), con el diseño del panel: rayas finas celestes y letras claras.
 * - A la izquierda, al medio: el Combustible (se llenan las barritas y "Reserva" titila si no alcanza).
 * - Arriba al medio: la Altitud (la Y) con el triangulito en la regla de 0 a 1200.
 * - A la derecha: la Ruta de Vuelo (los círculos se llenan con cada etapa) y, debajo, la Velocidad.
 * - Mientras hace la cuenta regresiva: "La nave está siendo encendida" con el nombre de quien la enciende.
 */
public final class NaveTablero {
	private NaveTablero() {}

	/** Los colores (con un poco de transparencia, como humo). */
	private static final int TEXTO = 0xE6F4F2EF, CIAN = 0xE6BFE3EA, LINEA = 0xCCA9C9D6, APAGADO = 0x66A0A8B4,
			VERDE = 0xE67FE3C4, AMBAR = 0xE6F2B045, GRIS = 0xB3C6CFD6;

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
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			combustible(g, font, nave, 6, alto / 2f - 40);
			altitud(g, font, (int) Math.floor(mc.player.getY()), ancho / 2f, 4);
			float rutaY = Math.max(56, alto / 2f - 75);
			ruta(g, font, nave, ancho - 8, rutaY);
			velocidad(g, font, ancho - 96, rutaY + 124);
			if (nave.estado() == NaveViajeEntity.CUENTA) encendido(g, font, nave, ancho / 2f, alto / 4f);
		});
	}

	// ---------------------------------------------------------------- Combustible

	private static void combustible(GuiGraphics g, Font font, NaveViajeEntity nave, float x, float y) {
		int porcentaje = nave.combustible();
		boolean alcanza = porcentaje >= NaveViajeEntity.COMBUSTIBLE_VIAJE;
		texto(g, font, "COMBUSTIBLE", x, y, 0.75f, TEXTO, -1);
		float arriba = y + 12;
		// El corchete de la izquierda.
		linea(g, x, arriba, x + 0.5f, arriba + 61, LINEA);
		linea(g, x, arriba, x + 3, arriba + 0.5f, LINEA);
		linea(g, x, arriba + 60.5f, x + 3, arriba + 61, LINEA);
		// Las barritas se llenan de abajo para arriba (cada una es un 10%).
		int llenas = Math.round(porcentaje / 10f);
		for (int i = 0; i < 10; i++) {
			float py = arriba + 1 + i * 6;
			boolean llena = 9 - i < llenas;
			pastilla(g, x + 5, py, 14, 4, llena ? (alcanza ? VERDE : AMBAR) : APAGADO);
		}
		// La regla de la derecha: 100, 50 y 0.
		String[] numeros = {"100", "", "50", "", "0"};
		for (int i = 0; i < 5; i++) {
			float ry = arriba + 3 + i * 14;
			linea(g, x + 22, ry, x + 25, ry + 0.5f, LINEA);
			if (!numeros[i].isEmpty()) texto(g, font, numeros[i], x + 27, ry - 1.5f, 0.5f, TEXTO, -1);
		}
		texto(g, font, porcentaje + "%", x + 40, arriba + 26, 1.25f, alcanza ? TEXTO : AMBAR, -1);
		// "Reserva": titila si no alcanza para un viaje.
		boolean titila = !alcanza && (Util.getMillis() / 500) % 2 == 0;
		texto(g, font, "RESERVA", x + 2, arriba + 65, 0.5f, titila ? AMBAR : GRIS, -1);
	}

	// ---------------------------------------------------------------- Altitud

	private static void altitud(GuiGraphics g, Font font, int y, float centro, float arriba) {
		float desde = centro - 80, hasta = centro + 80;
		texto(g, font, "ALTITUD / Y", centro, arriba, 0.5f, GRIS, 0);
		float regla = arriba + 15;
		linea(g, desde, regla, hasta, regla + 0.5f, LINEA);
		for (int i = 0; i <= 16; i++) {
			float rx = desde + i * 10;
			boolean grande = i % 4 == 0;
			linea(g, rx - 0.25f, regla - (grande ? 2.5f : 1.5f), rx + 0.25f, regla, LINEA);
			if (grande) texto(g, font, String.valueOf(i * 75), rx, regla - 7, 0.5f, TEXTO, 0);
		}
		// El triangulito que marca la altura.
		float marca = desde + Mth.clamp(y, 0, 1200) / 1200f * 160;
		for (int fila = 0; fila < 3; fila++) linea(g, marca - fila - 0.5f, regla + 1.5f + fila, marca + fila + 0.5f, regla + 2.5f + fila, CIAN);
		texto(g, font, y + " m", centro, regla + 6, 1f, TEXTO, 0);
	}

	// ---------------------------------------------------------------- Ruta de vuelo

	private static void ruta(GuiGraphics g, Font font, NaveViajeEntity nave, float derecha, float y) {
		int etapa = nave.etapa();
		texto(g, font, "RUTA DE VUELO", derecha, y, 0.75f, TEXTO, 1);
		linea(g, derecha - 110, y + 8, derecha, y + 8.5f, LINEA);
		String estado = etapa == 0 && nave.estado() == NaveViajeEntity.CUENTA ? "ENCENDIENDO"
				: NaveViajeEntity.ETAPAS[etapa].toUpperCase(Locale.ROOT);
		texto(g, font, estado, derecha, y + 11, 0.5f, CIAN, 1);
		boolean parpadeo = (Util.getMillis() / 400) % 2 == 0;
		float cx = derecha - 3, primera = y + 26;
		// La raya que une los círculos.
		linea(g, cx - 0.25f, primera, cx + 0.25f, primera + 6 * 13, LINEA);
		for (int i = 1; i <= 7; i++) {
			boolean hecha = i < etapa, actual = i == etapa;
			float cy = primera + (i - 1) * 13;
			int color = actual ? (parpadeo ? 0xFFFFFFFF : CIAN) : hecha ? 0xE67FD8E6 : 0;
			circulo(g, cx, cy, color);
			texto(g, font, NaveViajeEntity.ETAPAS[i].toUpperCase(Locale.ROOT), cx - 7, cy - 2, 0.55f,
					actual ? 0xFFFFFFFF : hecha ? 0xE69FDCE8 : GRIS, 1);
		}
	}

	// ---------------------------------------------------------------- Velocidad

	private static void velocidad(GuiGraphics g, Font font, float x, float y) {
		texto(g, font, "VELOCIDAD", x, y, 1f, CIAN, -1);
		String numero = String.format(Locale.ROOT, "%04.1f", velocidad);
		texto(g, font, numero, x, y + 12, 2f, TEXTO, -1);
		texto(g, font, "m/s", x + font.width(numero) * 2 + 4, y + 18, 0.75f, TEXTO, -1);
		// Las rayitas de abajo se prenden según lo rápido que va.
		int prendidas = Math.round(velocidad / 10f);
		for (int i = 0; i < 10; i++) {
			float rx = x + i * 8;
			linea(g, rx, y + 33, rx + 5, y + 33.75f, i < prendidas ? 0xE69FDCE8 : LINEA);
		}
	}

	// ---------------------------------------------------------------- Encendido (cuenta regresiva)

	private static void encendido(GuiGraphics g, Font font, NaveViajeEntity nave, float centro, float y) {
		Entity piloto = nave.getPassengers().isEmpty() ? null : nave.getPassengers().get(0);
		String nombre = piloto == null ? "" : piloto.getName().getString().toUpperCase(Locale.ROOT);
		float x = centro - 110, derecha = centro + 110, abajo = y + 62;
		// Las cuatro esquinas.
		linea(g, x, y, x + 8, y + 0.5f, LINEA);
		linea(g, x, y, x + 0.5f, y + 6, LINEA);
		linea(g, derecha - 8, y, derecha, y + 0.5f, LINEA);
		linea(g, derecha - 0.5f, y, derecha, y + 6, LINEA);
		linea(g, x, abajo - 0.5f, x + 8, abajo, LINEA);
		linea(g, x, abajo - 6, x + 0.5f, abajo, LINEA);
		linea(g, derecha - 8, abajo - 0.5f, derecha, abajo, LINEA);
		linea(g, derecha - 0.5f, abajo - 6, derecha, abajo, LINEA);
		texto(g, font, "LA NAVE ESTÁ SIENDO ENCENDIDA", centro, y + 6, 0.6f, CIAN, 0);
		texto(g, font, nombre, centro, y + 16, 1f, TEXTO, 0);
		texto(g, font, "SOLO UN TRIPULANTE PUEDE ENCENDERLA", centro, y + 32, 0.5f, GRIS, 0);
		texto(g, font, "ESPERA A QUE TERMINE LA CARGA", centro, y + 38, 0.5f, GRIS, 0);
		// La carga.
		float carga = Mth.clamp(ticksEstado / (float) NaveViajeEntity.TIEMPO_CUENTA, 0, 1);
		linea(g, centro - 70, y + 50, centro + 70, y + 51, 0x55FFFFFF);
		if (carga > 0) linea(g, centro - 70, y + 50, centro - 70 + 140 * carga, y + 51, CIAN);
	}

	// ---------------------------------------------------------------- Dibujitos

	/** Un rectángulo con medidas con coma (para rayas finitas). */
	private static void linea(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
		g.pose().pushPose();
		g.pose().translate(x1, y1, 0);
		g.pose().scale((x2 - x1) / 100f, (y2 - y1) / 100f, 1);
		g.fill(0, 0, 100, 100, color);
		g.pose().popPose();
	}

	/** Una barrita con las puntas redondeadas. */
	private static void pastilla(GuiGraphics g, float x, float y, float w, float h, int color) {
		linea(g, x + 1, y, x + w - 1, y + h, color);
		linea(g, x, y + 1, x + 1, y + h - 1, color);
		linea(g, x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	/** Un circulito de la Ruta de Vuelo: el borde siempre; adentro pintado si {@code relleno} no es 0. */
	private static void circulo(GuiGraphics g, float cx, float cy, int relleno) {
		// El fondo oscuro tapa la raya que pasa por el medio.
		linea(g, cx - 2, cy - 2, cx + 2, cy + 2, relleno != 0 ? relleno : 0x99223040);
		// Borde (un octágono de rayitas).
		linea(g, cx - 1.5f, cy - 2.5f, cx + 1.5f, cy - 2, LINEA);
		linea(g, cx - 1.5f, cy + 2, cx + 1.5f, cy + 2.5f, LINEA);
		linea(g, cx - 2.5f, cy - 1.5f, cx - 2, cy + 1.5f, LINEA);
		linea(g, cx + 2, cy - 1.5f, cx + 2.5f, cy + 1.5f, LINEA);
		linea(g, cx - 2, cy - 2, cx - 1.5f, cy - 1.5f, LINEA);
		linea(g, cx + 1.5f, cy - 2, cx + 2, cy - 1.5f, LINEA);
		linea(g, cx - 2, cy + 1.5f, cx - 1.5f, cy + 2, LINEA);
		linea(g, cx + 1.5f, cy + 1.5f, cx + 2, cy + 2, LINEA);
	}

	/**
	 * Texto con la parte de arriba en {@code y}, a ese tamaño (1 = normal).
	 * Alineación: -1 = desde x, 0 = centrado, 1 = termina en x.
	 */
	private static void texto(GuiGraphics g, Font font, String texto, float x, float y, float escala, int color, int alineacion) {
		float ancho = font.width(texto) * escala;
		float desde = alineacion < 0 ? x : alineacion == 0 ? x - ancho / 2 : x - ancho;
		g.pose().pushPose();
		g.pose().translate(desde, y, 0);
		g.pose().scale(escala, escala, 1);
		g.drawString(font, texto, 0, 0, color, true);
		g.pose().popPose();
	}
}
