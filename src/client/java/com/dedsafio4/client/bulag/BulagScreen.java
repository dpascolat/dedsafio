package com.dedsafio4.client.bulag;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bulag.Bulag;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pantalla del Cráneo Explosivo (Bulag 2): fondo de venas rojas, la cuenta arriba al medio y los números
 * del 0 al 9 en bolitas rojas con pinchos, desparramados (cambian de lugar en cada cuenta). Se contesta
 * tocando las bolitas o con el teclado; cuando la respuesta tiene los dígitos justos se manda sola.
 * No se puede cerrar con Escape: la cierra el servidor.
 */
public class BulagScreen extends Screen {
	private static final ResourceLocation FONDO = textura("bulag_fondo"), NUMERO = textura("bulag_numero");
	/** Dónde van las bolitas (fracciones de la pantalla), como en el diseño. */
	private static final float[][] LUGARES = {{0.178f, 0.45f}, {0.285f, 0.655f}, {0.15f, 0.755f}, {0.29f, 0.875f},
			{0.468f, 0.81f}, {0.567f, 0.675f}, {0.675f, 0.905f}, {0.685f, 0.48f}, {0.84f, 0.41f}, {0.875f, 0.78f}};
	private static final int ROJO = 0xFFD01818, ROJO_OSCURO = 0xFF6A0808;

	private static ResourceLocation textura(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/" + nombre + ".png");
	}

	private Bulag.PantallaPayload estado;
	private long recibido;
	private String escrito = "";
	/** Qué número va en cada lugar. */
	private final List<Integer> orden = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
	private long avisoDesde;

	public BulagScreen(Bulag.PantallaPayload estado) {
		super(Component.literal("Cráneo Explosivo"));
		actualizar(estado);
	}

	public void actualizar(Bulag.PantallaPayload nuevo) {
		boolean cuentaNueva = estado == null || !nuevo.problema().equals(estado.problema())
				|| nuevo.aciertos() != estado.aciertos();
		estado = nuevo;
		recibido = Util.getMillis();
		if (cuentaNueva) {
			escrito = "";
			Collections.shuffle(orden);
		}
		if (nuevo.error()) escrito = "";
		if (!nuevo.aviso().isEmpty()) avisoDesde = recibido;
	}

	private boolean puedeEscribir() {
		return !estado.problema().isEmpty() && estado.largo() > 0;
	}

	private void escribir(int digito) {
		if (!puedeEscribir() || escrito.length() >= estado.largo()) return;
		escrito += digito;
		if (escrito.length() == estado.largo()) ClientPlayNetworking.send(new Bulag.RespuestaPayload(escrito));
	}

	private float radio() {
		return Math.max(14, width * 0.042f);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		float r = radio();
		for (int i = 0; i < 10; i++) {
			float cx = LUGARES[i][0] * width, cy = LUGARES[i][1] * height;
			if ((mx - cx) * (mx - cx) + (my - cy) * (my - cy) <= r * r) {
				escribir(orden.get(i));
				return true;
			}
		}
		return super.mouseClicked(mx, my, boton);
	}

	@Override
	public boolean keyPressed(int tecla, int codigo, int modificadores) {
		if (tecla >= GLFW.GLFW_KEY_0 && tecla <= GLFW.GLFW_KEY_9) {
			escribir(tecla - GLFW.GLFW_KEY_0);
			return true;
		}
		if (tecla >= GLFW.GLFW_KEY_KP_0 && tecla <= GLFW.GLFW_KEY_KP_9) {
			escribir(tecla - GLFW.GLFW_KEY_KP_0);
			return true;
		}
		if (tecla == GLFW.GLFW_KEY_BACKSPACE && !escrito.isEmpty() && escrito.length() < estado.largo()) {
			escrito = escrito.substring(0, escrito.length() - 1);
			return true;
		}
		return super.keyPressed(tecla, codigo, modificadores);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.blit(FONDO, 0, 0, width, height, 0, 0, 480, 270, 480, 270);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		long ahora = Util.getMillis();
		int centro = width / 2;

		// Arriba: nivel, aciertos y lugares.
		String arriba = "NIVEL " + estado.nivel() + "   ·   CUENTAS " + estado.aciertos() + "/" + estado.cuentas()
				+ "   ·   SALVADOS " + estado.salvados() + "/" + estado.cupo();
		texto(g, arriba, centro, 8, 0xFF8A1010, 0xFFFFFFFF, 1f);

		if (puedeEscribir()) {
			// La cuenta, grande y roja.
			float escala = Math.max(2.5f, width / 120f);
			String cuenta = estado.problema() + " =";
			texto(g, cuenta, centro, (int) (height * 0.24f), ROJO, 0xFFFFF4E8, escala);
			// Gotitas.
			int ancho = (int) (font.width(cuenta) * escala);
			for (int i = 0; i < 3; i++) {
				int gx = centro - ancho / 2 + ancho * (i * 2 + 1) / 7, gy = (int) (height * 0.24f + 8 * escala);
				g.fill(gx, gy, gx + 1, gy + 3 + i * 2, ROJO);
				g.fill(gx - 1, gy + 3 + i * 2, gx + 2, gy + 5 + i * 2, ROJO);
			}
			// Lo que vas escribiendo: _ _ _
			StringBuilder casillas = new StringBuilder();
			for (int i = 0; i < estado.largo(); i++) {
				casillas.append(i < escrito.length() ? escrito.charAt(i) : '_').append(i < estado.largo() - 1 ? " " : "");
			}
			texto(g, casillas.toString(), centro, (int) (height * 0.38f), 0xFFFFFFFF, ROJO_OSCURO, escala * 0.8f);
			// Tiempo que queda.
			int quedan = Math.max(0, (int) Math.ceil((estado.tiempo() / 20f) - (ahora - recibido) / 1000f));
			int colorTiempo = quedan <= 5 && (ahora / 250) % 2 == 0 ? 0xFFFF2020 : 0xFF8A1010;
			texto(g, quedan + " s", centro, (int) (height * 0.53f), colorTiempo, 0xFFFFFFFF, 1.6f);
		}

		// El aviso (cuenta regresiva, "¡Incorrecto!", "Se acabaron los lugares").
		if (!estado.aviso().isEmpty() && (!puedeEscribir() || ahora - avisoDesde < 1500)) {
			float escala = puedeEscribir() ? 1.6f : Math.max(2.5f, width / 140f);
			int y = puedeEscribir() ? (int) (height * 0.46f) : (int) (height * 0.3f);
			texto(g, estado.aviso(), centro, y, estado.error() ? 0xFFFF2020 : ROJO, 0xFFFFFFFF, escala);
		}

		// Las bolitas con los números.
		float r = radio();
		for (int i = 0; i < 10; i++) {
			float cx = LUGARES[i][0] * width, cy = LUGARES[i][1] * height;
			boolean encima = (mouseX - cx) * (mouseX - cx) + (mouseY - cy) * (mouseY - cy) <= r * r;
			float lado = r * (encima && puedeEscribir() ? 2.6f : 2.4f);
			g.pose().pushPose();
			g.pose().translate(cx - lado / 2, cy - lado / 2, 0);
			g.pose().scale(lado / 64f, lado / 64f, 1);
			g.blit(NUMERO, 0, 0, 0, 0, 64, 64, 64, 64);
			g.pose().popPose();
			texto(g, Integer.toString(orden.get(i)), (int) cx, (int) (cy - r * 0.5f) + 1, 0xFFFFFFFF, ROJO_OSCURO, r / 8f);
		}
	}

	/** Texto centrado en x, con borde (para que se lea arriba del fondo). */
	private void texto(GuiGraphics g, String texto, int x, int y, int color, int borde, float escala) {
		float desde = x - font.width(texto) * escala / 2;
		g.pose().pushPose();
		g.pose().translate(desde, y, 0);
		g.pose().scale(escala, escala, 1);
		for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) g.drawString(font, texto, d[0], d[1], borde, false);
		g.drawString(font, texto, 0, 0, color, false);
		g.pose().popPose();
	}
}
