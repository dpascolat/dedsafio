package com.dedsafio4.client.cajero;

import com.dedsafio4.banco.ModCajero;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Menú del Cajero: las tres monedas y los botones "Ingresar Dinero" y "Sacar Dinero". */
public class CajeroScreen extends Screen {
	static final int ANCHO = 242, ALTO = 243;
	private static final BotonImagen.Imagen BOTON = BotonImagen.Imagen.de("cajero/boton_dinero", 83, 22),
			BOTON_ENCIMA = BotonImagen.Imagen.de("cajero/boton_dinero_encima", 85, 24);

	private final BlockPos pos;
	private int x, y;

	public CajeroScreen(BlockPos pos) {
		super(Component.literal("Cajero"));
		this.pos = pos;
	}

	@Override
	protected void init() {
		x = (width - ANCHO) / 2;
		y = Math.max(0, (height - ALTO) / 2);
		addRenderableWidget(new BotonImagen(x + 32, y + 180, BOTON, BOTON_ENCIMA, null, -1, -1, "Ingresar Dinero", 0.8f,
				() -> ClientPlayNetworking.send(new ModCajero.IngresarPayload(pos))));
		addRenderableWidget(new BotonImagen(x + 127, y + 180, BOTON, BOTON_ENCIMA, null, -1, -1, "Sacar Dinero", 0.8f,
				() -> minecraft.setScreen(new CajeroSacarScreen(pos))));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g, mouseX, mouseY, delta);
		EstiloCajero.pieza(g, "cajero/marco_grande", x, y, ANCHO, ALTO);
		g.drawCenteredString(font, title, x + ANCHO / 2, y + 8, 0xFFFFFFFF);
		EstiloCajero.pieza(g, new BotonImagen.Imagen("cajero/deditas", 126, 87, 42, 29), x + 58, y + 51);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
