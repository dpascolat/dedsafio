package com.dedsafio4.client.cajero;

import com.dedsafio4.banco.ModCajero;
import com.dedsafio4.client.Dedsafio4Client;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Sacar dinero: con las flechitas se elige cuántas Rojas, Verdes y Deditas sacar (Shift = de a 10),
 * sin pasarse del saldo, y con "Sacar" el servidor las descuenta y te las da.
 */
public class CajeroSacarScreen extends Screen {
	private static final ResourceLocation[] ICONOS = {EstiloCajero.ICONO_ROJA, EstiloCajero.ICONO_VERDE, EstiloCajero.ICONO_DEDITA};
	private static final long[] VALORES = {10_000, 100, 1};

	private final BlockPos pos;
	private final int[] cantidades = new int[3];
	private int x, y, lado;
	private String mensaje = "";

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

	private int columnaX(int i) {
		return x + lado * (i + 1) / 4 + (i - 1) * lado / 20;
	}

	@Override
	protected void init() {
		lado = Math.min(200, height - 16);
		x = (width - lado) / 2;
		y = (height - lado) / 2 + 4;
		for (int i = 0; i < 3; i++) {
			int moneda = i, cx = columnaX(i) - FlechaCajero.ANCHO / 2;
			addRenderableWidget(new FlechaCajero(cx, y + lado * 30 / 100, true, () -> cambiar(moneda, +1)));
			addRenderableWidget(new FlechaCajero(cx, y + lado * 48 / 100, false, () -> cambiar(moneda, -1)));
		}
		addRenderableWidget(new BotonCajero(x + (lado - 56) / 2, y + lado * 78 / 100, 56, 20, "Sacar", this::sacar));
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
		if (total <= 0) {
			mensaje = "Elegí cuántas monedas sacar.";
			return;
		}
		if (total > saldo()) {
			mensaje = "No tenés suficientes deditas.";
			return;
		}
		ClientPlayNetworking.send(new ModCajero.SacarPayload(pos, cantidades[0], cantidades[1], cantidades[2]));
		mensaje = "Sacaste " + total + (total == 1 ? " dedita." : " deditas.");
		java.util.Arrays.fill(cantidades, 0);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g, mouseX, mouseY, delta);
		EstiloCajero.marco(g, font, x, y, lado, lado, "Sacar Dinero");

		// Saldo: 1 (verde) 29 (dedita)
		float escala = 0.7f;
		String etiqueta = "Saldo: ";
		int anchoMonedas = Math.round(EstiloCajero.anchoMonedas(font, saldo()) * escala);
		int sx = x + (lado - font.width(etiqueta) - anchoMonedas) / 2, sy = y + lado * 19 / 100;
		g.drawString(font, etiqueta, sx, sy, 0xFFFFD34D, true);
		g.pose().pushPose();
		g.pose().translate(sx + font.width(etiqueta), sy - 2, 0);
		g.pose().scale(escala, escala, 1);
		EstiloCajero.dibujarMonedas(g, font, 0, 0, saldo());
		g.pose().popPose();

		// Cada columna: la moneda y cuántas vas a sacar.
		for (int i = 0; i < 3; i++) {
			String cantidad = Integer.toString(cantidades[i]);
			int ancho = 11 + 3 + font.width(cantidad);
			int cx = columnaX(i) - ancho / 2, cy = y + lado * 39 / 100;
			g.pose().pushPose();
			g.pose().translate(cx, cy - 1, 0);
			g.pose().scale(11 / 16f, 11 / 16f, 1);
			g.blit(ICONOS[i], 0, 0, 0, 0, 16, 16, 16, 16);
			g.pose().popPose();
			g.drawString(font, cantidad, cx + 14, cy + 1, 0xFFFFFFFF, true);
		}

		// El recuadro oscuro: cuánto vas a sacar, o el aviso.
		int rx = x + lado / 4, ry = y + lado * 60 / 100, rAncho = lado / 2, rAlto = lado * 12 / 100;
		g.fill(rx, ry, rx + rAncho, ry + rAlto, 0xFF15181F);
		String texto = !mensaje.isEmpty() ? mensaje : total() > 0 ? "Total: " + total() : "";
		int color = mensaje.startsWith("Sacaste") ? 0xFF7CF08A : !mensaje.isEmpty() ? 0xFFFF6B6B : 0xFFC6CFD6;
		if (!texto.isEmpty()) {
			float escalaTexto = Math.min(1f, (rAncho - 6) / (float) font.width(texto));
			g.pose().pushPose();
			g.pose().translate(rx + rAncho / 2f, ry + rAlto / 2f, 0);
			g.pose().scale(escalaTexto, escalaTexto, 1);
			g.drawString(font, texto, -font.width(texto) / 2, -4, color, true);
			g.pose().popPose();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
