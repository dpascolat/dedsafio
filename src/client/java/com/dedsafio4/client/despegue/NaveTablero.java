package com.dedsafio4.client.despegue;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.despegue.NaveViajeEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * El tablero que ven los que van arriba de la Nave Espacial Biplaza, hecho con las imágenes del diseño
 * (textures/gui/nave). A las imágenes se les borraron los valores que cambian (el %, los metros, la etapa,
 * la velocidad y el nombre) y se dibujan encima. Las medidas de cada imagen están en sus píxeles.
 * - A la izquierda, al medio: el Combustible (se llenan las barritas y "Reserva" titila si no alcanza).
 * - Arriba al medio: la Altitud (la Y) con el triangulito en la regla de 0 a 1200.
 * - A la derecha: la Ruta de Vuelo (los círculos se llenan con cada etapa) y, debajo, la Velocidad.
 * - Mientras hace la cuenta regresiva: "La nave está siendo encendida" con el nombre de quien la enciende.
 */
public final class NaveTablero {
	private NaveTablero() {}

	private static final ResourceLocation COMBUSTIBLE = textura("combustible"), PASTILLA = textura("pastilla"),
			RESERVA = textura("reserva"), ALTITUD = textura("altitud"), TRIANGULO = textura("triangulo"),
			RUTA = textura("ruta"), CIRCULO = textura("circulo"), VELOCIDAD = textura("velocidad"),
			ENCENDIDO = textura("encendido");

	private static ResourceLocation textura(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/nave/" + nombre + ".png");
	}

	/** Qué tan transparente se ve todo (como humo). */
	private static final float OPACIDAD = 0.85f;
	private static final int TEXTO = 0xFFF4F2EF, CIAN = 0xFFBFE3EA, AMBAR = 0xFFF2B045;

	// Lugares dentro de cada imagen (en sus píxeles).
	private static final float[] PASTILLAS_Y = {91.5f, 125.75f, 160, 194, 228.25f, 262.5f, 296.5f, 330.75f, 365, 399};
	private static final float[] CIRCULOS_Y = {135.75f, 189.25f, 242.75f, 294.25f, 347.25f, 400, 452.75f};
	private static final float[][] RENGLONES_Y = {{128.5f, 143.5f}, {177.5f, 196.75f}, {231.25f, 250.5f},
			{287.25f, 302.25f}, {335.25f, 354.75f}, {388.25f, 407.5f}, {445.75f, 460.75f}};
	private static final float[] RAYITAS_X = {77.25f, 117.25f, 157, 197, 236.75f, 276.75f, 316.5f, 356.5f, 396.5f, 436.25f};

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
			combustible(g, font, nave, 6, alto / 2f - 45, 70);
			altitud(g, font, (int) Math.floor(mc.player.getY()), ancho / 2f - 85, 4, 170);
			float rutaY = Math.max(56, alto / 2f - 75);
			ruta(g, font, nave, ancho - 126, rutaY, 120);
			velocidad(g, font, ancho - 96, rutaY + 124, 90);
			if (nave.estado() == NaveViajeEntity.CUENTA) encendido(g, font, nave, ancho / 2f - 110, alto / 4f, 220);
			g.setColor(1, 1, 1, 1);
		});
	}

	/** Pone el dibujo en (x, y) con ese ancho, para dibujar adentro en los píxeles de la imagen. */
	private static void empezar(GuiGraphics g, float x, float y, float ancho, int anchoImagen) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		float escala = ancho / anchoImagen;
		g.pose().scale(escala, escala, 1);
	}

	private static void imagen(GuiGraphics g, ResourceLocation textura, int ancho, int alto) {
		g.setColor(1, 1, 1, OPACIDAD);
		g.blit(textura, 0, 0, 0, 0, ancho, alto, ancho, alto);
	}

	/** Un pedacito de la imagen otra vez, teñido de un color (para prender una parte). */
	private static void tenir(GuiGraphics g, ResourceLocation textura, float x, float y, float w, float h,
							  int anchoImagen, int altoImagen, int color) {
		g.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, OPACIDAD);
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.blit(textura, 0, 0, Math.round(w), Math.round(h), x, y, Math.round(w), Math.round(h), anchoImagen, altoImagen);
		g.pose().popPose();
	}

	private static void sprite(GuiGraphics g, ResourceLocation textura, float x, float y, int w, int h, int color) {
		g.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, OPACIDAD);
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.blit(textura, 0, 0, 0, 0, w, h, w, h);
		g.pose().popPose();
	}

	// ---------------------------------------------------------------- Combustible

	private static void combustible(GuiGraphics g, Font font, NaveViajeEntity nave, float x, float y, float ancho) {
		int porcentaje = nave.combustible();
		boolean alcanza = porcentaje >= NaveViajeEntity.COMBUSTIBLE_VIAJE;
		empezar(g, x, y, ancho, 388);
		imagen(g, COMBUSTIBLE, 388, 500);
		// Las barritas se llenan de abajo para arriba (cada una es un 10%).
		int llenas = Math.round(porcentaje / 10f);
		for (int i = 0; i < 10; i++) {
			if (9 - i < llenas) sprite(g, PASTILLA, 62, PASTILLAS_Y[i], 62, 25, alcanza ? 0x7FE3C4 : AMBAR);
		}
		// "Reserva": titila si no alcanza para un viaje.
		boolean titila = !alcanza && (Util.getMillis() / 500) % 2 == 0;
		sprite(g, RESERVA, 42.5f, 467.5f, 135, 20, titila ? AMBAR : 0xFFFFFF);
		g.setColor(1, 1, 1, 1);
		texto(g, font, porcentaje + "%", 379, 244.5f, 23.5f, alcanza ? TEXTO : AMBAR, 1);
		g.pose().popPose();
	}

	// ---------------------------------------------------------------- Altitud

	private static void altitud(GuiGraphics g, Font font, int y, float x, float arriba, float ancho) {
		empezar(g, x, arriba, ancho, 500);
		imagen(g, ALTITUD, 500, 118);
		float marca = 12.5f + Mth.clamp(y, 0, 1200) / 1200f * (472 - 12.5f);
		sprite(g, TRIANGULO, marca - 10.5f, 95, 21, 11, CIAN);
		g.setColor(1, 1, 1, 1);
		texto(g, font, y + " m", 242, 42, 15, TEXTO, 0);
		g.pose().popPose();
	}

	// ---------------------------------------------------------------- Ruta de vuelo

	private static void ruta(GuiGraphics g, Font font, NaveViajeEntity nave, float x, float y, float ancho) {
		int etapa = nave.etapa();
		empezar(g, x, y, ancho, 472);
		imagen(g, RUTA, 472, 500);
		boolean parpadeo = (Util.getMillis() / 400) % 2 == 0;
		for (int i = 1; i <= 7; i++) {
			boolean hecha = i < etapa, actual = i == etapa;
			if (!hecha && !actual) continue;
			int color = actual ? (parpadeo ? 0xFFFFFF : 0xBFE3EA) : 0x7FD8E6;
			sprite(g, CIRCULO, 429.25f - 11.5f, CIRCULOS_Y[i - 1] - 11.5f, 23, 23, color);
			float[] renglon = RENGLONES_Y[i - 1];
			tenir(g, RUTA, 85, renglon[0], 315, renglon[1] - renglon[0], 472, 500, actual ? 0xFFFFFF : 0x9FDCE8);
		}
		g.setColor(1, 1, 1, 1);
		String estado = etapa == 0 && nave.estado() == NaveViajeEntity.CUENTA ? "ENCENDIENDO"
				: NaveViajeEntity.ETAPAS[etapa].toUpperCase(Locale.ROOT);
		texto(g, font, estado, 427, 98.75f, 15, TEXTO, 1);
		g.pose().popPose();
	}

	// ---------------------------------------------------------------- Velocidad

	private static void velocidad(GuiGraphics g, Font font, float x, float y, float ancho) {
		empezar(g, x, y, ancho, 500);
		imagen(g, VELOCIDAD, 500, 375);
		// Las rayitas de abajo se prenden según lo rápido que va.
		int prendidas = Math.round(velocidad / 10f);
		for (int i = 0; i < prendidas && i < 10; i++) {
			tenir(g, VELOCIDAD, RAYITAS_X[i], 328, 18, 5, 500, 375, 0x9FDCE8);
		}
		g.setColor(1, 1, 1, 1);
		String numero = String.format(Locale.ROOT, "%04.1f", velocidad);
		// Número con borde oscuro, como en el diseño.
		for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
			g.pose().pushPose();
			g.pose().translate(d[0] * 3, d[1] * 3, 0);
			texto(g, font, numero, 223.5f, 177, 45, 0xFF7E7474, 0);
			g.pose().popPose();
		}
		texto(g, font, numero, 223.5f, 177, 45, TEXTO, 0);
		g.pose().popPose();
	}

	// ---------------------------------------------------------------- Encendido (cuenta regresiva)

	private static void encendido(GuiGraphics g, Font font, NaveViajeEntity nave, float x, float y, float ancho) {
		Entity piloto = nave.getPassengers().isEmpty() ? null : nave.getPassengers().get(0);
		String nombre = piloto == null ? "" : piloto.getName().getString().toUpperCase(Locale.ROOT);
		empezar(g, x, y, ancho, 500);
		imagen(g, ENCENDIDO, 500, 180);
		g.setColor(1, 1, 1, 1);
		texto(g, font, nombre, 250, 67, 12.5f, TEXTO, 0);
		// La carga.
		float carga = Mth.clamp(ticksEstado / (float) NaveViajeEntity.TIEMPO_CUENTA, 0, 1);
		g.fill(130, 168, 370, 170, 0x55FFFFFF);
		g.fill(130, 168, 130 + Math.round(240 * carga), 170, CIAN);
		g.pose().popPose();
	}

	/**
	 * Texto dentro de la imagen: centrado en alto en {@code yCentro}, de {@code alto} píxeles de la imagen.
	 * Alineación: -1 = desde x, 0 = centrado, 1 = termina en x.
	 */
	private static void texto(GuiGraphics g, Font font, String texto, float x, float yCentro, float alto, int color, int alineacion) {
		float escala = alto / 7f;
		float ancho = font.width(texto) * escala;
		float desde = alineacion < 0 ? x : alineacion == 0 ? x - ancho / 2 : x - ancho;
		g.pose().pushPose();
		g.pose().translate(desde, yCentro - alto / 2, 0);
		g.pose().scale(escala, escala, 1);
		g.drawString(font, texto, 0, 0, color, false);
		g.pose().popPose();
	}
}
