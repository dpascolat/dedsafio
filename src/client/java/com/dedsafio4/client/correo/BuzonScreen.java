package com.dedsafio4.client.correo;

import com.dedsafio4.client.cajero.BotonImagen;
import com.dedsafio4.client.cajero.EstiloCajero;
import com.dedsafio4.correo.Correo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * El Buzón de correo (se abre al tocar el Mensajero): el título arriba, el "+" en la esquina para escribir un mensaje,
 * la lista de mensajes ("De: Fulano", el comienzo y hace cuánto llegó) y las flechas abajo para pasar de página.
 * Click en un mensaje lo abre; click derecho lo borra.
 */
public class BuzonScreen extends Screen {
	private static final int ANCHO = 176, ALTO = 222, FILAS = 7, TARJETA_X = 26, TARJETA_Y = 28, TARJETA_ANCHO = 124, TARJETA_ALTO = 17, ENTRE_FILAS = 21;
	private static final float CHICO = 2f / 3f;
	private int x, y, pagina;
	private BotonImagen anterior, siguiente;

	public BuzonScreen() {
		super(Component.literal("Buzón de correo"));
	}

	@Override
	protected void init() {
		x = (width - ANCHO) / 2;
		y = Math.max(0, (height - ALTO) / 2);
		addRenderableWidget(new BotonImagen(x + 5, y + 5, BotonImagen.Imagen.de("correo/boton_mas", 20, 16), BotonImagen.Imagen.de("correo/boton_mas_encima", 20, 16),
				() -> ClientPlayNetworking.send(new Correo.AccionPayload("abrir", -1))));
		anterior = addRenderableWidget(new BotonImagen(x + 18, y + 188, BotonImagen.Imagen.de("correo/flecha_izq", 37, 16), null,
				() -> pagina = Math.max(0, pagina - 1)));
		siguiente = addRenderableWidget(new BotonImagen(x + 121, y + 188, BotonImagen.Imagen.de("correo/flecha_der", 37, 16), null,
				() -> pagina = Math.min(paginas() - 1, pagina + 1)));
	}

	private int paginas() {
		return Math.max(1, (CorreoCliente.cartas().size() + FILAS - 1) / FILAS);
	}

	/** "35s", "50m", "71h", "3d". */
	static String hace(long segundos) {
		if (segundos < 60) return segundos + "s";
		if (segundos < 3600) return segundos / 60 + "m";
		if (segundos < 86400 * 4) return segundos / 3600 + "h";
		return segundos / 86400 + "d";
	}

	private void chico(GuiGraphics g, Component texto, float tx, float ty, int color) {
		g.pose().pushPose();
		g.pose().translate(tx, ty, 0);
		g.pose().scale(CHICO, CHICO, 1);
		g.drawString(font, texto, 0, 0, color, false);
		g.pose().popPose();
	}

	/** Corta el texto para que entre en ese ancho (en píxeles de pantalla, con la letra chica). */
	private Component cortar(Component texto, int ancho) {
		int lugar = (int) (ancho / CHICO);
		if (font.width(texto) <= lugar) return texto;
		return Component.literal(font.substrByWidth(texto, lugar - font.width("...")).getString() + "...").withStyle(texto.getStyle());
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g, mouseX, mouseY, delta);
		EstiloCajero.pieza(g, "correo/marco_lista", x, y, ANCHO, ALTO);
		g.drawCenteredString(font, title, x + ANCHO / 2, y + 17 - 4, 0xFFFFFFFF);

		// Los mensajes de esta página.
		List<Correo.CartaVista> cartas = CorreoCliente.cartas();
		pagina = Math.max(0, Math.min(pagina, paginas() - 1));
		anterior.active = pagina > 0;
		siguiente.active = pagina < paginas() - 1;
		if (cartas.isEmpty()) g.drawCenteredString(font, "No tienes mensajes.", x + ANCHO / 2, y + 90, 0xFF9AA2AE);
		for (int k = 0; k < FILAS; k++) {
			int i = pagina * FILAS + k;
			if (i >= cartas.size()) break;
			Correo.CartaVista c = cartas.get(i);
			int tx = x + TARJETA_X, ty = y + TARJETA_Y + k * ENTRE_FILAS;
			boolean encima = mouseX >= tx && mouseX < tx + TARJETA_ANCHO && mouseY >= ty && mouseY < ty + TARJETA_ALTO;
			if (encima) EstiloCajero.pieza(g, "correo/tarjeta_encima", tx - 1, ty - 1, 126, 19);
			else EstiloCajero.pieza(g, "correo/tarjeta", tx, ty, TARJETA_ANCHO, TARJETA_ALTO);
			int color = c.leida() ? 0xFFA8B0C0 : 0xFFFFFFFF;
			String tiempo = hace(CorreoCliente.segundos(c));
			int anchoTiempo = Math.round(font.width(tiempo) * CHICO);
			// "De: Eón ✦" (la ✦ si tiene objetos para sacar) y abajo el asunto o el comienzo del mensaje.
			Component nombre = Component.literal("De: " + c.autor()).withColor(color);
			if (c.conObjetos()) nombre = nombre.copy().append(Component.literal(" ✦").withColor(0xFFF4E04A));
			chico(g, cortar(nombre, TARJETA_ANCHO - 12 - anchoTiempo), tx + 4, ty + 2.33f, color);
			String resumen = (c.asunto().isEmpty() ? c.texto() : c.asunto()).replace('\n', ' ');
			chico(g, cortar(Component.literal(resumen), TARJETA_ANCHO - 8), tx + 4, ty + 11, 0xFF8A90A0);
			chico(g, Component.literal(tiempo), tx + TARJETA_ANCHO - 5 - anchoTiempo, ty + 2.33f, color);
		}
		if (paginas() > 1) {
			Component p = Component.literal((pagina + 1) + "/" + paginas());
			chico(g, p, x + ANCHO / 2f - font.width(p) * CHICO / 2, y + 193, 0xFFAAAAAA);
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		if (super.mouseClicked(mx, my, boton)) return true;
		List<Correo.CartaVista> cartas = CorreoCliente.cartas();
		int tx = x + TARJETA_X;
		for (int k = 0; k < FILAS; k++) {
			int i = pagina * FILAS + k, ty = y + TARJETA_Y + k * ENTRE_FILAS;
			if (i >= cartas.size() || mx < tx || mx >= tx + TARJETA_ANCHO || my < ty || my >= ty + TARJETA_ALTO) continue;
			ClientPlayNetworking.send(new Correo.AccionPayload(boton == 1 ? "borrar" : "abrir", cartas.get(i).id()));
			return true;
		}
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
