package com.dedsafio4.client.cajero;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Flechita blanca (para arriba o para abajo) con un brillo de arcoíris, como las del Cajero. */
public class FlechaCajero extends AbstractButton {
	public static final int ANCHO = 13, ALTO = 7;
	private final boolean arriba;
	private final Runnable accion;

	public FlechaCajero(int x, int y, boolean arriba, Runnable accion) {
		super(x, y, ANCHO, ALTO, Component.literal(arriba ? "+" : "-"));
		this.arriba = arriba;
		this.accion = accion;
	}

	@Override
	public void onPress() {
		accion.run();
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float t = EstiloCajero.tiempo();
		for (int fila = 0; fila < ALTO; fila++) {
			// La fila ancha (la base) es la de abajo si apunta para arriba.
			int ancho = 1 + 2 * (arriba ? fila : ALTO - 1 - fila);
			boolean base = arriba ? fila == ALTO - 1 : fila == 0;
			int x0 = getX() + (ANCHO - ancho) / 2, y = getY() + fila;
			for (int i = 0; i < ancho; i++) {
				int color = base ? EstiloCajero.arcoiris(t + i / (float) ANCHO)
						: (isHoveredOrFocused() ? 0xFFFFF3B0 : (i == 0 ? 0xFFC9CED6 : 0xFFFFFFFF));
				if (!active) color = 0xFF55595F;
				g.fill(x0 + i, y, x0 + i + 1, y + 1, color);
			}
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput salida) {
		defaultButtonNarrationText(salida);
	}
}
