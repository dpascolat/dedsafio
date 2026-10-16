package com.dedsafio4.client.crafteo;

import com.dedsafio4.client.cajero.BotonCajero;
import com.dedsafio4.client.cajero.EstiloCajero;
import com.dedsafio4.crafteo.CrafteoMenu;
import com.dedsafio4.crafteo.Crafteos;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** La pantalla de /crafteo: la grilla de 3×3, la flecha, el resultado, "GUARDAR" y el inventario. */
public class CrafteoScreen extends AbstractContainerScreen<CrafteoMenu> {
	public CrafteoScreen(CrafteoMenu menu, Inventory inventario, Component titulo) {
		super(menu, inventario, titulo);
		imageWidth = CrafteoMenu.ANCHO;
		imageHeight = CrafteoMenu.ALTO;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new BotonCajero(leftPos + CrafteoMenu.RESULTADO_X - 17, topPos + CrafteoMenu.RESULTADO_Y + 26, 50, 14, "GUARDAR",
				() -> ClientPlayNetworking.send(new Crafteos.GuardarPayload())));
	}

	private static void casilla(GuiGraphics g, int x, int y, int borde) {
		g.fill(x - 1, y - 1, x + 17, y + 17, borde);
		g.fill(x, y, x + 16, y + 16, 0xFF151821);
	}

	@Override
	protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
		int l = leftPos, t = topPos;
		EstiloCajero.marco(g, font, l, t, imageWidth, imageHeight, null);
		for (int i = 0; i < 9; i++) {
			casilla(g, l + CrafteoMenu.GRILLA_X + i % 3 * 18, t + CrafteoMenu.GRILLA_Y + i / 3 * 18,
					EstiloCajero.arcoiris(0.16f + i % 3 / 3f * 0.85f));
		}
		int rx = l + CrafteoMenu.RESULTADO_X, ry = t + CrafteoMenu.RESULTADO_Y;
		g.fill(rx - 5, ry - 5, rx + 21, ry + 21, 0xFFFFD84A);
		g.fill(rx - 4, ry - 4, rx + 20, ry + 20, 0xFF151821);
		g.drawString(font, "→", l + CrafteoMenu.GRILLA_X + 3 * 18 + 12, ry + 4, 0xFFFFFFFF, true);
		for (int fila = 0; fila < 4; fila++) {
			for (int col = 0; col < 9; col++) {
				casilla(g, l + CrafteoMenu.INVENTARIO_X + col * 18, t + CrafteoMenu.INVENTARIO_Y + (fila < 3 ? fila * 18 : 58), 0xFF2A2E38);
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, (imageWidth - font.width(title)) / 2, 10, 0xFFFFFFFF, false);
		g.pose().pushPose();
		g.pose().translate(8, CrafteoMenu.GRILLA_Y + 58, 0);
		g.pose().scale(0.5f, 0.5f, 1);
		String[] ayuda = {
				"Pon el ítem en el resultado (aparece su",
				"crafteo de ahora) y arma el nuevo.",
				"No se gasta nada · mano vacía: vacía",
				"Click derecho en el resultado: +1"};
		for (int i = 0; i < ayuda.length; i++) g.drawString(font, ayuda[i], 0, i * 10, 0xFF8FD8FF, false);
		g.pose().popPose();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		renderTooltip(g, mouseX, mouseY);
	}
}
