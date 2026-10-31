package com.dedsafio4.client.cajero;

import com.dedsafio4.banco.ModCajero;
import com.dedsafio4.client.Dedsafio4Client;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Sacar dinero: con las flechitas se elige cuántas Rojas, Verdes y Deditas sacar (Shift = de a 10),
 * sin pasarse del saldo, y con "Sacar" el servidor las descuenta y te las da.
 */
public class CajeroSacarScreen extends Screen {
	private static final String[] MONEDAS = {"cajero/moneda_roja", "cajero/moneda_verde", "cajero/moneda_azul"};
	private static final long[] VALORES = {10_000, 100, 1};
	/** Cuánto hay entre una columna de monedas y la siguiente. */
	private static final int COLUMNA = 50;

	private final BlockPos pos;
	private final int[] cantidades = new int[3];
	private int x, y;
	private String mensaje = "";
	private BotonImagen botonSacar;

	public CajeroSacarScreen(BlockPos pos) {
		super(Component.literal("Sacar Dinero"));
		this.pos = pos;
	}

	private static long saldo() {
		return Math.max(0, Dedsafio4Client.saldo());
	}

	private long total() {
		long total = 0;
		for (int i = 0; i < 3; i++) total += cantidades[i] * VALORES[i];
		return total;
	}

	@Override
	protected void init() {
		x = (width - CajeroScreen.ANCHO) / 2;
		y = Math.max(0, (height - CajeroScreen.ALTO) / 2);
		BotonImagen.Imagen arriba = BotonImagen.Imagen.de("cajero/flecha_arriba", 11, 9), arribaEncima = BotonImagen.Imagen.de("cajero/flecha_arriba_encima", 13, 10);
		BotonImagen.Imagen abajo = BotonImagen.Imagen.de("cajero/flecha_abajo", 11, 9), abajoEncima = BotonImagen.Imagen.de("cajero/flecha_abajo_encima", 13, 10);
		for (int i = 0; i < 3; i++) {
			int moneda = i, cx = x + 65 + COLUMNA * i;
			addRenderableWidget(new BotonImagen(cx, y + 82, arriba, arribaEncima, null, -1, -1, "", 1, () -> cambiar(moneda, +1)));
			addRenderableWidget(new BotonImagen(cx, y + 121, abajo, abajoEncima, null, -1, 0, "", 1, () -> cambiar(moneda, -1)));
		}
		botonSacar = addRenderableWidget(new BotonImagen(x + 102, y + 194, new BotonImagen.Imagen("cajero/boton_sacar", 38, 18, 114, 54),
				new BotonImagen.Imagen("cajero/boton_sacar_encima", 40, 20, 120, 60), new BotonImagen.Imagen("cajero/boton_sacar_apagado", 38, 18, 114, 54),
				-1, -1, "", 1, this::sacar));
	}

	/** Suma o resta una moneda (Shift: de a 10), sin pasarse del saldo ni bajar de 0. */
	private void cambiar(int moneda, int direccion) {
		int paso = hasShiftDown() ? 10 : 1;
		for (int i = 0; i < paso; i++) {
			if (direccion > 0) {
				if (total() + VALORES[moneda] > saldo() || cantidades[moneda] >= ModCajero.MAXIMO_POR_MONEDA) break;
				cantidades[moneda]++;
			} else if (cantidades[moneda] > 0) {
				cantidades[moneda]--;
			}
		}
		mensaje = "";
	}

	private void sacar() {
		long total = total();
		if (total <= 0) return;
		if (total > saldo()) {
			mensaje = "No tienes suficientes deditas.";
			return;
		}
		ClientPlayNetworking.send(new ModCajero.SacarPayload(pos, cantidades[0], cantidades[1], cantidades[2]));
		mensaje = "Sacaste " + total + (total == 1 ? " dedita." : " deditas.");
		java.util.Arrays.fill(cantidades, 0);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g, mouseX, mouseY, delta);
		botonSacar.active = total() > 0;
		EstiloCajero.pieza(g, "cajero/marco_grande", x, y, CajeroScreen.ANCHO, CajeroScreen.ALTO);
		g.drawCenteredString(font, title, x + CajeroScreen.ANCHO / 2, y + 8, 0xFFFFFFFF);

		// Saldo: 1 (verde) 29 (dedita)
		String etiqueta = "Saldo: ";
		int icono = 10, anchoMonedas = EstiloCajero.anchoMonedas(font, saldo(), icono);
		int sx = x + (CajeroScreen.ANCHO - font.width(etiqueta) - anchoMonedas) / 2, sy = y + 64;
		g.drawString(font, etiqueta, sx, sy + 2, 0xFFEBCF78, true);
		EstiloCajero.dibujarMonedas(g, font, sx + font.width(etiqueta), sy, saldo(), icono);

		// Cada columna: la moneda y cuántas vas a sacar.
		for (int i = 0; i < 3; i++) {
			EstiloCajero.pieza(g, new BotonImagen.Imagen(MONEDAS[i], 8, 8, 10, 10), x + 62 + COLUMNA * i, y + 101);
			g.drawString(font, Integer.toString(cantidades[i]), x + 75 + COLUMNA * i, y + 102, 0xFFFFFFFF, true);
		}

		// El recuadro: cuánto vas a sacar, o el aviso.
		EstiloCajero.pieza(g, "cajero/recuadro", x + 61, y + 151, 120, 30);
		String texto = !mensaje.isEmpty() ? mensaje : total() > 0 ? "Total: " + total() : "";
		int color = mensaje.startsWith("Sacaste") ? 0xFF7CF08A : !mensaje.isEmpty() ? 0xFFFF6B6B : 0xFFFFFFFF;
		if (!texto.isEmpty()) {
			float escala = Math.min(1f, 112f / font.width(texto));
			g.pose().pushPose();
			g.pose().translate(x + 121f, y + 166f, 0);
			g.pose().scale(escala, escala, 1);
			g.drawString(font, texto, -font.width(texto) / 2, -4, color, false);
			g.pose().popPose();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
