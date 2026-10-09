package com.dedsafio4.client.correo;

import com.dedsafio4.client.cajero.BotonCajero;
import com.dedsafio4.client.cajero.EstiloCajero;
import com.dedsafio4.correo.Correo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * El Buzón de correo (se abre al tocar el Mensajero): el marco metálico del Cajero, alto y angosto, con el título
 * arriba, el "+" en la esquina para escribir un mensaje, la lista de mensajes ("De: Fulano" y hace cuánto llegó) y
 * las flechas "<<<" y ">>>" abajo para pasar de página. Click en un mensaje lo abre; click derecho lo borra.
 */
public class BuzonScreen extends Screen {
	private static final int ALTO_FILA = 12;
	private int x, y, ancho, alto, pagina, yLista, filas;

	public BuzonScreen() {
		super(Component.literal("Buzón de correo"));
	}

	@Override
	protected void init() {
		alto = Math.min(250, height - 16);
		ancho = alto * 4 / 5;
		x = (width - ancho) / 2;
		y = (height - alto) / 2;
		int anchoFlecha = ancho * 21 / 100, altoFlecha = Math.max(16, alto * 7 / 100), yFlecha = y + alto - 12 - altoFlecha;
		yLista = y + 36;
		filas = Math.max(1, (yFlecha - 6 - yLista) / ALTO_FILA);
		addRenderableWidget(new BotonCajero(x + 14, yFlecha, anchoFlecha, altoFlecha, "<<<", () -> pagina = Math.max(0, pagina - 1)));
		addRenderableWidget(new BotonCajero(x + ancho - 14 - anchoFlecha, yFlecha, anchoFlecha, altoFlecha, ">>>",
				() -> pagina = Math.min(paginas() - 1, pagina + 1)));
		int lado = Math.max(18, ancho / 7);
		addRenderableWidget(new BotonMas(x - 1, y - 1, lado, () -> ClientPlayNetworking.send(new Correo.AccionPayload("abrir", -1))));
	}

	private int paginas() {
		return Math.max(1, (CorreoCliente.cartas().size() + filas - 1) / filas);
	}

	/** "35s", "50m", "71h", "3d". */
	static String hace(long segundos) {
		if (segundos < 60) return segundos + "s";
		if (segundos < 3600) return segundos / 60 + "m";
		if (segundos < 86400 * 4) return segundos / 3600 + "h";
		return segundos / 86400 + "d";
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g, mouseX, mouseY, delta);
		EstiloCajero.marco(g, font, x, y, ancho, alto, null);
		// El título, grande, arriba al medio.
		float escala = 1.6f;
		g.pose().pushPose();
		g.pose().translate(x + ancho / 2f - font.width(title) * escala / 2f, y + 16, 0);
		g.pose().scale(escala, escala, 1);
		g.drawString(font, title, 0, 0, 0xFFFFFFFF, false);
		g.pose().popPose();

		// Los mensajes de esta página.
		List<Correo.CartaVista> cartas = CorreoCliente.cartas();
		pagina = Math.max(0, Math.min(pagina, paginas() - 1));
		int x0 = x + 14, x1 = x + ancho - 14;
		if (cartas.isEmpty()) {
			String t = "No tienes mensajes.";
			g.drawString(font, t, x + (ancho - font.width(t)) / 2, yLista + 4, 0xFF6A7080, false);
		}
		for (int k = 0; k < filas; k++) {
			int i = pagina * filas + k;
			if (i >= cartas.size()) break;
			Correo.CartaVista c = cartas.get(i);
			int yf = yLista + k * ALTO_FILA;
			boolean encima = mouseX >= x0 && mouseX < x1 && mouseY >= yf && mouseY < yf + ALTO_FILA - 1;
			g.fill(x0, yf, x1, yf + ALTO_FILA - 1, encima ? 0xFF3A4A66 : c.leida() ? 0xFF161C28 : 0xFF223048);
			g.fill(x0, yf + ALTO_FILA - 2, x1, yf + ALTO_FILA - 1, 0xFF2E3A50);
			int color = c.leida() ? 0xFFA8B0C0 : 0xFFFFFFFF;
			String tiempo = hace(CorreoCliente.segundos(c));
			// "De: Eón · Misión diaria ✦" (el subtítulo en gris; la ✦ si tiene objetos para sacar).
			Component nombre = Component.literal("De: " + c.autor()).withColor(color);
			if (!c.asunto().isEmpty()) nombre = nombre.copy().append(Component.literal(" · " + c.asunto()).withColor(0xFF8A90A0));
			if (c.conObjetos()) nombre = nombre.copy().append(Component.literal(" ✦").withColor(0xFFF4E04A));
			int anchoNombre = x1 - x0 - 10 - font.width(tiempo);
			g.drawString(font, font.width(nombre) <= anchoNombre ? nombre.getVisualOrderText()
					: net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(nombre, anchoNombre)), x0 + 3, yf + 2, color, false);
			g.drawString(font, tiempo, x1 - 3 - font.width(tiempo), yf + 2, color, false);
		}
		if (paginas() > 1) {
			String p = (pagina + 1) + "/" + paginas();
			g.drawString(font, p, x + (ancho - font.width(p)) / 2, y + alto - 24, 0xFF8A90A0, false);
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		if (super.mouseClicked(mx, my, boton)) return true;
		int x0 = x + 14, x1 = x + ancho - 14;
		List<Correo.CartaVista> cartas = CorreoCliente.cartas();
		for (int k = 0; k < filas; k++) {
			int i = pagina * filas + k, yf = yLista + k * ALTO_FILA;
			if (i >= cartas.size() || mx < x0 || mx >= x1 || my < yf || my >= yf + ALTO_FILA - 1) continue;
			ClientPlayNetworking.send(new Correo.AccionPayload(boton == 1 ? "borrar" : "abrir", cartas.get(i).id()));
			return true;
		}
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** El cuadrado oscuro con borde de arcoíris y una cruz de colores. */
	private static class BotonMas extends AbstractButton {
		private final Runnable accion;

		BotonMas(int x, int y, int lado, Runnable accion) {
			super(x, y, lado, lado, Component.literal("+"));
			this.accion = accion;
		}

		@Override
		public void onPress() {
			accion.run();
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
			int x = getX(), y = getY(), lado = getWidth();
			float t = EstiloCajero.tiempo();
			g.fill(x - 1, y - 1, x + lado + 1, y + lado + 1, 0xFF05060A);
			EstiloCajero.bordeArcoiris(g, x, y, lado, lado, 0.95f + t, 1.6f + t);
			g.fill(x + 1, y + 1, x + lado - 1, y + lado - 1, isHoveredOrFocused() ? 0xFF2A3246 : 0xFF151A26);
			// La cruz: la barra de arriba a abajo (amarillo → celeste) y la de costado (rosa → violeta).
			int c = lado / 2, largo = lado * 3 / 10, grosor = Math.max(2, lado / 9);
			g.fillGradient(x + c - grosor / 2, y + c - largo, x + c - grosor / 2 + grosor, y + c + largo, 0xFFF4E04A, 0xFF4FD8F0);
			for (int i = -largo; i < largo; i++) {
				if (i >= -grosor / 2 && i < grosor - grosor / 2) continue;   // el medio lo deja la barra de arriba a abajo
				int color = EstiloCajero.arcoiris(0.9f + 0.15f * (i + largo) / (2f * largo));
				g.fill(x + c + i, y + c - grosor / 2, x + c + i + 1, y + c - grosor / 2 + grosor, color);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput salida) {
			defaultButtonNarrationText(salida);
		}
	}
}
