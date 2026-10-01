package com.dedsafio4.client.cajero;

import com.dedsafio4.banco.CajeroMenu;
import com.dedsafio4.client.Dedsafio4Client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ingresar dinero: la grilla de 3x3 para poner deditas, el botón "Ingresar" y el saldo. */
public class CajeroIngresarScreen extends AbstractContainerScreen<CajeroMenu> {
	private static final int BORDE = 6;

	public CajeroIngresarScreen(CajeroMenu menu, Inventory inventario, Component titulo) {
		super(menu, inventario, titulo);
		imageWidth = 176;
		imageHeight = CajeroMenu.INVENTARIO_Y + 58 + 18 + 8;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new BotonCajero(leftPos + 108, topPos + 38, 56, 20, "Ingresar",
				() -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CajeroMenu.BOTON_INGRESAR)));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		renderTooltip(g, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
		int x = leftPos - BORDE, y = topPos - BORDE;
		EstiloCajero.marco(g, font, x, y, imageWidth + BORDE * 2, imageHeight + BORDE * 2, null);

		// La grilla: cada casilla con su borde de arcoíris.
		float t = EstiloCajero.tiempo();
		for (int fila = 0; fila < 3; fila++) {
			for (int col = 0; col < 3; col++) {
				int cx = leftPos + CajeroMenu.GRILLA_X + col * CajeroMenu.CELDA;
				int cy = topPos + CajeroMenu.GRILLA_Y + fila * CajeroMenu.CELDA;
				float matiz = t + (fila * 3 + col) / 9f;
				EstiloCajero.bordeArcoiris(g, cx, cy, 20, 20, matiz, matiz + 0.12f);
				g.fill(cx + 1, cy + 1, cx + 19, cy + 19, 0xFF0E121B);
			}
		}

		// El inventario, en el panel metálico de abajo.
		int invY = topPos + CajeroMenu.INVENTARIO_Y - 6;
		g.fillGradient(leftPos - 1, invY, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFFB9BEC6, 0xFF80868F);
		for (int fila = 0; fila < 4; fila++) {
			int sy = topPos + CajeroMenu.INVENTARIO_Y + fila * 18 + (fila == 3 ? 4 : 0);
			for (int col = 0; col < 9; col++) {
				int sx = leftPos + 8 + col * 18;
				g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF3A3F4A);
				g.fill(sx, sy, sx + 16, sy + 16, 0xFF1A1D24);
			}
		}

		// El saldo, debajo del botón.
		long saldo = Math.max(0, Dedsafio4Client.saldo());
		float escala = 0.75f;
		int ancho = Math.round(EstiloCajero.anchoMonedas(font, saldo) * escala);
		g.pose().pushPose();
		g.pose().translate(leftPos + 136 - ancho / 2f, topPos + 66, 0);
		g.pose().scale(escala, escala, 1);
		EstiloCajero.dibujarMonedas(g, font, 0, 0, saldo);
		g.pose().popPose();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		// Sin títulos: el diseño no los lleva.
	}
}
