package com.dedsafio4.client.cajero;

import com.dedsafio4.banco.CajeroMenu;
import com.dedsafio4.client.Dedsafio4Client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ingresar dinero: la grilla de 3x3 para poner deditas, el botón "Ingresar" y el saldo. */
public class CajeroIngresarScreen extends AbstractContainerScreen<CajeroMenu> {
	public CajeroIngresarScreen(CajeroMenu menu, Inventory inventario, Component titulo) {
		super(menu, inventario, titulo);
		imageWidth = 177;
		imageHeight = 166;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new BotonImagen(leftPos + 107, topPos + 34, new BotonImagen.Imagen("cajero/boton_ingresar", 38, 18, 114, 54),
				new BotonImagen.Imagen("cajero/boton_ingresar_encima", 40, 20, 120, 60), null, -1, -1, "", 1,
				() -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CajeroMenu.BOTON_INGRESAR)));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		renderTooltip(g, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
		EstiloCajero.pieza(g, "cajero/marco_ingresar", leftPos, topPos, imageWidth, imageHeight);
		EstiloCajero.pieza(g, "cajero/cuadricula", leftPos + 39, topPos + 14, 57, 57);
		EstiloCajero.casillasInventario(g, leftPos + 7, topPos + 83, 139 - 83);

		// El saldo, debajo del botón.
		long saldo = Math.max(0, Dedsafio4Client.saldo());
		// Con mucho saldo se achica, para no pisar la grilla.
		float escala = Math.min(0.75f, 70f / EstiloCajero.anchoMonedas(font, saldo, 10));
		int ancho = Math.round(EstiloCajero.anchoMonedas(font, saldo, 10) * escala);
		g.pose().pushPose();
		g.pose().translate(leftPos + 126 - ancho / 2f, topPos + 57, 0);
		g.pose().scale(escala, escala, 1);
		EstiloCajero.dibujarMonedas(g, font, 0, 0, saldo, 10);
		g.pose().popPose();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		// Sin títulos: el diseño no los lleva.
	}
}
