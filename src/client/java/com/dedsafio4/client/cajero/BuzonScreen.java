package com.dedsafio4.client.cajero;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * El Buzón de correo (se abre al tocar el Mensajero): el marco metálico del Cajero, alto y angosto, con el título
 * arriba, el botón "+" en la esquina de arriba a la izquierda y las flechas "<<<" y ">>>" abajo para pasar de página.
 * Por ahora está vacío: el "+" y las flechas todavía no hacen nada.
 */
public class BuzonScreen extends Screen {
	private final BlockPos pos;
	private int x, y, ancho, alto, pagina;

	public BuzonScreen(BlockPos pos) {
		super(Component.literal("Buzón de correo"));
		this.pos = pos;
	}

	@Override
	protected void init() {
		alto = Math.min(250, height - 16);
		ancho = alto * 4 / 5;
		x = (width - ancho) / 2;
		y = (height - alto) / 2;
		int anchoFlecha = ancho * 21 / 100, altoFlecha = Math.max(16, alto * 7 / 100), yFlecha = y + alto - 12 - altoFlecha;
		addRenderableWidget(new BotonCajero(x + 14, yFlecha, anchoFlecha, altoFlecha, "<<<", () -> pagina = Math.max(0, pagina - 1)));
		addRenderableWidget(new BotonCajero(x + ancho - 14 - anchoFlecha, yFlecha, anchoFlecha, altoFlecha, ">>>", () -> pagina++));
		int lado = Math.max(18, ancho / 7);
		addRenderableWidget(new BotonMas(x - 1, y - 1, lado, () -> {}));
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
				int color = EstiloCajero.arcoiris(0.9f + 0.15f * (i + largo) / (2f * largo));
				if (i >= -grosor / 2 && i < grosor - grosor / 2) continue;   // el medio lo deja la barra de arriba a abajo
				g.fill(x + c + i, y + c - grosor / 2, x + c + i + 1, y + c - grosor / 2 + grosor, color);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput salida) {
			defaultButtonNarrationText(salida);
		}
	}
}
