package com.dedsafio4.client.cajero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Botón oscuro con borde de arcoíris, como los del Cajero. */
public class BotonCajero extends AbstractButton {
	private final Runnable accion;

	public BotonCajero(int x, int y, int ancho, int alto, String texto, Runnable accion) {
		super(x, y, ancho, alto, Component.literal(texto));
		this.accion = accion;
	}

	@Override
	public void onPress() {
		accion.run();
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
		int x = getX(), y = getY(), ancho = getWidth(), alto = getHeight();
		g.fill(x - 1, y - 1, x + ancho + 1, y + alto + 1, 0xFF05060A);
		float t = EstiloCajero.tiempo();
		EstiloCajero.bordeArcoiris(g, x, y, ancho, alto, 0.95f + t, 1.6f + t);
		g.fill(x + 1, y + 1, x + ancho - 1, y + alto - 1, isHoveredOrFocused() ? 0xFF2A3246 : 0xFF151A26);
		var font = Minecraft.getInstance().font;
		// Si el texto no entra, se achica un poco.
		float escala = Math.min(1f, (ancho - 8) / (float) font.width(getMessage()));
		g.pose().pushPose();
		g.pose().translate(x + ancho / 2f, y + alto / 2f, 0);
		g.pose().scale(escala, escala, 1);
		g.drawString(font, getMessage(), -font.width(getMessage()) / 2, -4, active ? 0xFFFFFFFF : 0xFF8A8F99, true);
		g.pose().popPose();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput salida) {
		defaultButtonNarrationText(salida);
	}
}
