package com.dedsafio4.client.boveda;

import com.dedsafio4.boveda.BovedaMenu;
import com.dedsafio4.boveda.ModBoveda;
import com.dedsafio4.client.Dedsafio4Client;
import com.dedsafio4.client.cajero.EstiloCajero;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Pantalla de la Bóveda: arriba las 9 casillas para poner deditas físicas; al medio el panel con el
 * balance de la Hermandad, tres casilleros para escribir Rojas, Verdes y Deditas, y los botones
 * "Depositar" (las monedas de las casillas + lo escrito, que sale de tu cuenta) y "Retirar" (lo
 * escrito, en monedas). Abajo el inventario.
 */
public class BovedaScreen extends AbstractContainerScreen<BovedaMenu> {
	private static final ResourceLocation[] ICONOS = {EstiloCajero.ICONO_ROJA, EstiloCajero.ICONO_VERDE, EstiloCajero.ICONO_DEDITA};
	private static final int PANEL_Y = 31, PANEL_ALTO = 60;
	private static final int NARANJA = 0xFFF2A93B, PANEL = 0xFF141B33;
	/** Cuánto se ve el aviso de "Depositaste..." o del error. */
	private static final long DURACION_AVISO = 5000;

	/** Lo último que mandó el servidor (llega justo después de abrir la pantalla). */
	private static ModBoveda.EstadoPayload estado = new ModBoveda.EstadoPayload("", 0, "", false);
	private static long avisoDesde;

	private final EditBox[] casilleros = new EditBox[3];

	public BovedaScreen(BovedaMenu menu, Inventory inventario, Component titulo) {
		super(menu, inventario, titulo);
		imageWidth = BovedaMenu.ANCHO;
		imageHeight = BovedaMenu.INVENTARIO_Y + 58 + 18 + 8;
	}

	/** Llegó el balance (y quizás un aviso) del servidor. */
	public static void recibir(ModBoveda.EstadoPayload nuevo) {
		estado = nuevo;
		if (nuevo.mensaje().isEmpty()) return;
		avisoDesde = Util.getMillis();
		if (!nuevo.error() && Minecraft.getInstance().screen instanceof BovedaScreen pantalla) {
			for (EditBox casillero : pantalla.casilleros) casillero.setValue("");
		}
	}

	@Override
	protected void init() {
		super.init();
		avisoDesde = 0;
		int y = topPos + PANEL_Y + 22;
		for (int i = 0; i < 3; i++) {
			String anterior = casilleros[i] != null ? casilleros[i].getValue() : "";
			EditBox casillero = new EditBox(font, leftPos + 9 + i * 38, y, 24, 14, Component.literal("Cantidad"));
			casillero.setMaxLength(7);
			casillero.setFilter(texto -> texto.chars().allMatch(Character::isDigit));
			casillero.setValue(anterior);
			casilleros[i] = addRenderableWidget(casillero);
		}
		BotonBoveda depositar = addRenderableWidget(new BotonBoveda(leftPos + 124, y - 1, 42, 16, "Depositar",
				() -> enviar(ModBoveda.AccionPayload.DEPOSITAR)));
		depositar.setTooltip(Tooltip.create(Component.literal(
				"Deposita las monedas de las casillas de arriba y lo que escribas (sale de tu cuenta del banco).")));
		BotonBoveda retirar = addRenderableWidget(new BotonBoveda(leftPos + 169, y - 1, 34, 16, "Retirar",
				() -> enviar(ModBoveda.AccionPayload.RETIRAR)));
		retirar.setTooltip(Tooltip.create(Component.literal(
				"Retira lo que escribas, en monedas. Solo el Maestro y los Líderes.")));
	}

	private int cantidad(int i) {
		try {
			return Math.min(ModBoveda.MAXIMO_VIRTUAL, Integer.parseInt(casilleros[i].getValue()));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void enviar(int accion) {
		ClientPlayNetworking.send(new ModBoveda.AccionPayload(accion, cantidad(0), cantidad(1), cantidad(2)));
	}

	/** Mientras se escribe en un casillero, las teclas (como la E) no cierran la pantalla. */
	@Override
	public boolean keyPressed(int tecla, int codigo, int modificadores) {
		if (tecla == 256) return super.keyPressed(tecla, codigo, modificadores);
		for (EditBox casillero : casilleros) {
			if (casillero.isFocused()) {
				return casillero.keyPressed(tecla, codigo, modificadores) || casillero.canConsumeInput()
						|| super.keyPressed(tecla, codigo, modificadores);
			}
		}
		return super.keyPressed(tecla, codigo, modificadores);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		renderTooltip(g, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
		int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;

		// Marco gris con relieve y detalles naranjas.
		g.fill(x, y, x + w, y + h, 0xFF101014);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
		g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFF4F4F4);
		g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFF4F4F4);
		g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF5A5A5A);
		g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF5A5A5A);
		for (int ax : new int[]{x + 14, x + w - 44}) {
			g.fill(ax, y - 1, ax + 30, y + 2, NARANJA);
			g.fill(ax, y + h - 2, ax + 30, y + h + 1, NARANJA);
		}
		for (int ay : new int[]{y + 22, y + h - 50}) {
			g.fill(x - 1, ay, x + 2, ay + 26, NARANJA);
			g.fill(x + w - 2, ay, x + w + 1, ay + 26, NARANJA);
		}

		// Las 9 casillas de arriba, oscuras.
		for (int col = 0; col < 9; col++) {
			int sx = x + BovedaMenu.SLOTS_X + col * 18, sy = y + BovedaMenu.CASILLAS_Y;
			g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF1A1A1E);
			g.fill(sx, sy, sx + 16, sy + 16, 0xFF34343A);
		}

		// El panel azul oscuro con borde naranja.
		int px = x + 5, py = y + PANEL_Y, pw = w - 10;
		g.fill(px, py, px + pw, py + PANEL_ALTO, NARANJA);
		g.fill(px + 1, py + 1, px + pw - 1, py + PANEL_ALTO - 1, PANEL);
		g.fill(px + 2, py + 2, px + pw - 2, py + 3, 0xFF26315A);

		dibujarBalance(g, x + w / 2, py + 7);
		for (int i = 0; i < 3; i++) {
			g.blit(ICONOS[i], x + 9 + i * 38 + 25, py + 24, 10, 10, 0, 0, 16, 16, 16, 16);
		}
		dibujarAviso(g, x + w / 2, py + PANEL_ALTO - 12, pw - 8);

		// El inventario, con casillas como las de siempre.
		for (int fila = 0; fila < 4; fila++) {
			int sy = y + BovedaMenu.INVENTARIO_Y + fila * 18 + (fila == 3 ? 4 : 0);
			for (int col = 0; col < 9; col++) {
				int sx = x + BovedaMenu.SLOTS_X + col * 18;
				g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
				g.fill(sx, sy, sx + 17, sy + 17, 0xFFFFFFFF);
				g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
			}
		}
	}

	/** "Balance: 1 (roja) 23 (verde) 58 (dedita)", centrado. */
	private void dibujarBalance(GuiGraphics g, int centro, int y) {
		long balance = Math.max(0, estado.balance());
		String[] partes = {Long.toString(balance / 10_000), Long.toString(balance / 100 % 100), Long.toString(balance % 100)};
		String etiqueta = "Balance: ";
		int ancho = font.width(etiqueta);
		for (String parte : partes) ancho += font.width(parte) + 2 + 9 + 5;
		int x = centro - (ancho - 5) / 2;
		g.drawString(font, etiqueta, x, y, 0xFFFFFFFF, true);
		x += font.width(etiqueta);
		for (int i = 0; i < 3; i++) {
			g.drawString(font, partes[i], x, y, 0xFFFFFFFF, true);
			x += font.width(partes[i]) + 2;
			g.blit(ICONOS[i], x, y - 1, 9, 9, 0, 0, 16, 16, 16, 16);
			x += 9 + 5;
		}
	}

	/** Abajo del panel: el último aviso unos segundos, o cuánto tienes en tu cuenta. */
	private void dibujarAviso(GuiGraphics g, int centro, int y, int anchoMaximo) {
		boolean hayAviso = !estado.mensaje().isEmpty() && Util.getMillis() - avisoDesde < DURACION_AVISO;
		String texto;
		int color;
		if (hayAviso) {
			texto = estado.mensaje();
			color = estado.error() ? 0xFFFF6B6B : 0xFF7CF08A;
		} else {
			texto = "Tu cuenta: " + ModBoveda.formatear(Math.max(0, Dedsafio4Client.saldo())) + " deditas"
					+ (estado.hermandad().isEmpty() ? "" : "  ·  " + estado.hermandad());
			color = 0xFF9AA3B5;
		}
		float escala = Math.min(0.8f, anchoMaximo / (float) font.width(texto));
		g.pose().pushPose();
		g.pose().translate(centro, y, 0);
		g.pose().scale(escala, escala, 1);
		g.drawString(font, texto, -font.width(texto) / 2, 0, color, false);
		g.pose().popPose();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		// Sin títulos: el diseño no los lleva.
	}

	/** Botón azul oscuro con borde lila, como en el diseño. */
	private static class BotonBoveda extends AbstractButton {
		private final Runnable accion;

		BotonBoveda(int x, int y, int ancho, int alto, String texto, Runnable accion) {
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
			g.fill(x, y, x + ancho, y + alto, isHoveredOrFocused() ? 0xFFE0C8FF : 0xFFB48CF0);
			g.fill(x + 1, y + 1, x + ancho - 1, y + alto - 1, isHoveredOrFocused() ? 0xFF2E3A6A : 0xFF1C2448);
			var font = Minecraft.getInstance().font;
			float escala = Math.min(1f, (ancho - 4) / (float) font.width(getMessage()));
			g.pose().pushPose();
			g.pose().translate(x + ancho / 2f, y + alto / 2f, 0);
			g.pose().scale(escala, escala, 1);
			g.drawString(font, getMessage(), -font.width(getMessage()) / 2, -4, 0xFFFFFFFF, true);
			g.pose().popPose();
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput salida) {
			defaultButtonNarrationText(salida);
		}
	}
}
