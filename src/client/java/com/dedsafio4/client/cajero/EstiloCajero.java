package com.dedsafio4.client.cajero;

import com.dedsafio4.Dedsafio4;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Lo que se dibuja igual en las pantallas del Cajero: el marco metálico con luces, y las monedas. */
public final class EstiloCajero {
	private EstiloCajero() {}

	public static final ResourceLocation ICONO_DEDITA = icono("dedita");
	public static final ResourceLocation ICONO_VERDE = icono("dedita_verde");
	public static final ResourceLocation ICONO_ROJA = icono("dedita_roja");

	private static ResourceLocation icono(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/" + nombre + ".png");
	}

	/** Color del arcoíris (0..1 da toda la vuelta). */
	public static int arcoiris(float matiz) {
		return 0xFF000000 | Mth.hsvToRgb(((matiz % 1f) + 1f) % 1f, 0.75f, 1f);
	}

	/** Avanza despacio con el tiempo, para que las luces se muevan. */
	public static float tiempo() {
		return (Util.getMillis() % 6000L) / 6000f;
	}

	/** Borde de 1 píxel en degradé de arcoíris, de izquierda a derecha. */
	public static void bordeArcoiris(GuiGraphics g, int x, int y, int ancho, int alto, float desde, float hasta) {
		for (int i = 0; i < ancho; i++) {
			int color = arcoiris(desde + (hasta - desde) * i / Math.max(1, ancho - 1));
			g.fill(x + i, y, x + i + 1, y + 1, color);
			g.fill(x + i, y + alto - 1, x + i + 1, y + alto, color);
		}
		g.fillGradient(x, y, x + 1, y + alto, arcoiris(desde), arcoiris(desde + 0.08f));
		g.fillGradient(x + ancho - 1, y, x + ancho, y + alto, arcoiris(hasta), arcoiris(hasta + 0.08f));
	}

	/** El marco metálico con luces de colores; adentro queda oscuro. Con título, lleva el cartelito arriba. */
	public static void marco(GuiGraphics g, Font font, int x, int y, int ancho, int alto, String titulo) {
		float t = tiempo();
		g.fill(x, y, x + ancho, y + alto, 0xFF05060A);
		g.fillGradient(x + 1, y + 1, x + ancho - 1, y + alto - 1, 0xFFD3D8DF, 0xFF7E848F);
		g.fill(x + 2, y + 2, x + ancho - 2, y + 3, 0xFFF1F4F8);                     // brillo de arriba
		g.fill(x + 6, y + 6, x + ancho - 6, y + alto - 6, 0xFF2A2E38);             // canaleta
		g.fill(x + 7, y + 7, x + ancho - 7, y + alto - 7, 0xFF07080B);             // pantalla

		// Luces cyan en las esquinas de arriba y abajo.
		int largo = Math.max(10, ancho / 8);
		for (int[] esquina : new int[][]{{x + 10, y + 2}, {x + ancho - 10 - largo, y + 2},
				{x + 10, y + alto - 4}, {x + ancho - 10 - largo, y + alto - 4}}) {
			g.fill(esquina[0], esquina[1], esquina[0] + largo, esquina[1] + 2, 0xFF3FE6F2);
		}
		// Tiras de luces de colores a los costados.
		int tiraAlto = alto / 3, tiraY = y + (alto - tiraAlto) / 2;
		for (int i = 0; i * 3 < tiraAlto; i++) {
			int color = arcoiris(t + i * 0.06f);
			int yy = tiraY + i * 3;
			g.fill(x + 2, yy, x + 5, yy + 2, color);
			g.fill(x + ancho - 5, yy, x + ancho - 2, yy + 2, color);
		}
		// Rejillas.
		for (int i = 0; i < 4; i++) {
			g.fill(x + 2, y + 16 + i * 3, x + 5, y + 17 + i * 3, 0xFF4A505B);
			g.fill(x + ancho - 5, y + 16 + i * 3, x + ancho - 2, y + 17 + i * 3, 0xFF4A505B);
		}
		// Luces de abajo, al medio.
		int abajo = Math.min(60, ancho / 3);
		for (int i = 0; i < abajo; i += 3) {
			g.fill(x + (ancho - abajo) / 2 + i, y + alto - 5, x + (ancho - abajo) / 2 + i + 2, y + alto - 2,
					arcoiris(t * 2 + i / (float) abajo));
		}

		if (titulo != null) {
			int anchoCartel = font.width(titulo) + 28, cx = x + (ancho - anchoCartel) / 2;
			g.fill(cx - 1, y - 1, cx + anchoCartel + 1, y + 17, 0xFF05060A);
			g.fillGradient(cx, y, cx + anchoCartel, y + 16, 0xFFC5CAD2, 0xFF80868F);
			g.fill(cx + 3, y + 2, cx + anchoCartel - 3, y + 14, 0xFF151923);
			g.drawString(font, titulo, cx + (anchoCartel - font.width(titulo)) / 2, y + 4, 0xFFFFFFFF, true);
		}
	}

	/** Las monedas del saldo: 100 deditas = 1 Verde, 100 Verdes = 1 Roja. */
	public record Moneda(String cantidad, ResourceLocation icono) {}

	public static List<Moneda> monedas(long saldo) {
		List<Moneda> lista = new ArrayList<>();
		if (saldo >= 10_000) lista.add(new Moneda(Long.toString(saldo / 10_000), ICONO_ROJA));
		if (saldo >= 100) lista.add(new Moneda(Long.toString(saldo / 100 % 100), ICONO_VERDE));
		lista.add(new Moneda(Long.toString(saldo % 100), ICONO_DEDITA));
		return lista;
	}

	private static final int ICONO = 16, SEPARACION = 4, ENTRE_MONEDAS = 7;

	/** Ancho de la fila de monedas (a tamaño normal, íconos de 16). */
	public static int anchoMonedas(Font font, long saldo) {
		return anchoMonedas(font, saldo, ICONO);
	}

	/** Ancho de la fila de monedas con íconos de ese tamaño. */
	public static int anchoMonedas(Font font, long saldo, int icono) {
		List<Moneda> lista = monedas(saldo);
		int ancho = ENTRE_MONEDAS * (lista.size() - 1);
		for (Moneda m : lista) ancho += font.width(m.cantidad()) + SEPARACION + icono;
		return ancho;
	}

	/** Dibuja "2 (verde) 70 (dedita)" empezando en x, con los íconos de 16 de alto a partir de y. */
	public static void dibujarMonedas(GuiGraphics g, Font font, int x, int y, long saldo) {
		dibujarMonedas(g, font, x, y, saldo, ICONO);
	}

	/** Igual, con íconos de ese tamaño (la fila mide lo más alto entre el texto y el ícono). */
	public static void dibujarMonedas(GuiGraphics g, Font font, int x, int y, long saldo, int icono) {
		int alto = Math.max(icono, font.lineHeight);
		int textoY = y + (alto - font.lineHeight) / 2 + 1, iconoY = y + (alto - icono) / 2;
		for (Moneda m : monedas(saldo)) {
			g.drawString(font, m.cantidad(), x, textoY, 0xFFFFFFFF, true);
			x += font.width(m.cantidad()) + SEPARACION;
			g.blit(m.icono(), x, iconoY, 0, 0, icono, icono, icono, icono);
			x += icono + ENTRE_MONEDAS;
		}
	}
}
