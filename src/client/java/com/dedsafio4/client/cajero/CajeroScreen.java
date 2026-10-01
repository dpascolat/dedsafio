package com.dedsafio4.client.cajero;

import com.dedsafio4.banco.ModCajero;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Menú del Cajero: las tres monedas y los botones "Ingresar Dinero" y "Sacar Dinero". */
public class CajeroScreen extends Screen {
	private final BlockPos pos;
	private int x, y, lado, yBotones;

	public CajeroScreen(BlockPos pos) {
		super(Component.literal("Cajero"));
		this.pos = pos;
	}

	@Override
	protected void init() {
		lado = Math.min(200, height - 16);
		x = (width - lado) / 2;
		y = (height - lado) / 2 + 4;
		int anchoBoton = (lado - 42) / 2, altoBoton = 20, yBoton = y + lado - 52;
		yBotones = yBoton;
		addRenderableWidget(new BotonCajero(x + 14, yBoton, anchoBoton, altoBoton, "Ingresar Dinero",
				() -> ClientPlayNetworking.send(new ModCajero.IngresarPayload(pos))));
		addRenderableWidget(new BotonCajero(x + lado - 14 - anchoBoton, yBoton, anchoBoton, altoBoton, "Sacar Dinero",
				() -> minecraft.setScreen(new CajeroSacarScreen(pos))));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g, mouseX, mouseY, delta);
		EstiloCajero.marco(g, font, x, y, lado, lado, "Cajero");

		// Las tres monedas grandes, una encima de la otra: verde, azul y roja.
		// Centradas entre el cartel del título y los botones (el grupo es simétrico alrededor del centro).
		float escala = lado / 200f * 3.2f, u = escala / 3.2f;
		float centroX = x + lado / 2f, centroY = (y + 18 + yBotones) / 2f;
		moneda(g, EstiloCajero.ICONO_VERDE, centroX - 30 * u, centroY + 8 * u, escala);
		moneda(g, EstiloCajero.ICONO_DEDITA, centroX, centroY - 10 * u, escala);
		moneda(g, EstiloCajero.ICONO_ROJA, centroX + 30 * u, centroY + 10 * u, escala);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
	}

	/** Una moneda (ícono de 16) agrandada, centrada en (cx, cy). */
	private static void moneda(GuiGraphics g, ResourceLocation icono, float cx, float cy, float escala) {
		g.pose().pushPose();
		g.pose().translate(cx - 8 * escala, cy - 8 * escala, 0);
		g.pose().scale(escala, escala, 1);
		g.blit(icono, 0, 0, 0, 0, 16, 16, 16, 16);
		g.pose().popPose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
