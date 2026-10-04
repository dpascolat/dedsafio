package com.dedsafio4.client.casino;

import com.dedsafio4.casino.TablaPremiosPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * La tabla de premios del Casino, que aparece al hacer /casino 1: por cada figura de las ruedas, qué se gana con
 * 2 iguales y con 3 iguales (sale UNA de las opciones, al azar), con el dibujito de cada premio, la cantidad y el
 * nivel si viene encantado. Se baja con la ruedita del mouse.
 */
public class TablaPremiosScreen extends Screen {
	private static final int DORADO = 0xFFF2C230, ROJO = 0xFF7A1020, TEXTO = 0xFFE8E2D8, GRIS = 0xFF9A948C;
	private static final int RENGLON = 14, TITULO = 22;

	private final TablaPremiosPayload tabla;
	private int x, y, ancho, alto, scroll, total;

	public TablaPremiosScreen(TablaPremiosPayload tabla) {
		super(Component.literal("Tabla de premios " + tabla.tabla()));
		this.tabla = tabla;
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(TablaPremiosPayload.TYPE, (payload, context) ->
				context.client().execute(() -> Minecraft.getInstance().setScreen(new TablaPremiosScreen(payload))));
	}

	@Override
	protected void init() {
		ancho = Math.min(340, width - 20);
		alto = Math.min(height - 20, 260);
		x = (width - ancho) / 2;
		y = (height - alto) / 2;
		total = 0;
		for (TablaPremiosPayload.Fila f : tabla.filas()) total += altoFila(f);
		addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
				.bounds(x + ancho - 64, y + alto - 24, 56, 18).build());
		addRenderableWidget(Button.builder(Component.literal("Editar premios"), b -> {
			if (minecraft != null && minecraft.player != null) minecraft.player.connection.sendCommand("casino editar");
		}).bounds(x + ancho - 160, y + alto - 24, 92, 18).build());
	}

	private static int altoFila(TablaPremiosPayload.Fila f) {
		return TITULO + RENGLON * (1 + Math.max(1, f.dos().size()) + 1 + Math.max(1, f.tres().size())) + 6;
	}

	private int zonaArriba() {
		return y + 28;
	}

	private int zonaAbajo() {
		return y + alto - 30;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		super.renderBackground(g, mx, my, parcial);
		g.fill(x - 2, y - 2, x + ancho + 2, y + alto + 2, DORADO);
		g.fill(x, y, x + ancho, y + alto, 0xF0180A0C);
		g.fill(x, y, x + ancho, y + 24, ROJO);
		String titulo = "CASINO  ·  TABLA DE PREMIOS " + tabla.tabla();
		g.drawString(font, titulo, x + (ancho - font.width(titulo)) / 2, y + 8, DORADO, true);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		super.render(g, mx, my, parcial);
		int arriba = zonaArriba(), abajo = zonaAbajo();
		scroll = Mth.clamp(scroll, 0, Math.max(0, total - (abajo - arriba)));
		g.enableScissor(x, arriba, x + ancho, abajo);
		int yy = arriba - scroll;
		for (TablaPremiosPayload.Fila f : tabla.filas()) {
			g.fill(x + 6, yy, x + ancho - 6, yy + TITULO - 4, 0x40F2C230);
			g.renderItem(f.icono(), x + 10, yy + 1);
			g.drawString(font, f.nombre(), x + 32, yy + 5, DORADO, true);
			yy += TITULO;
			yy = premios(g, "2 iguales (premio chico):", f.dos(), yy);
			yy = premios(g, "3 iguales (premio grande):", f.tres(), yy);
			yy += 6;
		}
		g.disableScissor();

		if (total > abajo - arriba) {
			// La barrita del costado.
			int largo = abajo - arriba, barra = Math.max(12, largo * largo / total);
			int by = arriba + (largo - barra) * scroll / Math.max(1, total - largo);
			g.fill(x + ancho - 4, by, x + ancho - 2, by + barra, DORADO);
		}
		String pie = "Sale UNA, al azar.";
		g.drawString(font, pie, x + 8, y + alto - 19, GRIS, false);
	}

	private int premios(GuiGraphics g, String etiqueta, List<TablaPremiosPayload.Opcion> opciones, int yy) {
		g.drawString(font, etiqueta, x + 14, yy + 3, TEXTO, false);
		yy += RENGLON;
		if (opciones.isEmpty()) {
			g.drawString(font, "Nada", x + 30, yy + 3, GRIS, false);
			return yy + RENGLON;
		}
		for (int i = 0; i < opciones.size(); i++) {
			TablaPremiosPayload.Opcion o = opciones.get(i);
			g.pose().pushPose();
			g.pose().translate(x + 26, yy - 1, 0);
			g.pose().scale(0.75f, 0.75f, 1);
			g.renderItem(o.item(), 0, 0);
			g.pose().popPose();
			String texto = (i > 0 ? "o " : "") + (o.texto().isEmpty() ? elegido(o.item()) : o.texto());
			// Si no entra, se achica un poco.
			float escala = Math.min(1f, (ancho - 52) / (float) font.width(texto));
			g.pose().pushPose();
			g.pose().translate(x + 42, yy + 3 + (1 - escala) * 4, 0);
			g.pose().scale(escala, escala, 1);
			g.drawString(font, texto, 0, 0, 0xFFFFFFFF, false);
			g.pose().popPose();
			yy += RENGLON;
		}
		return yy;
	}

	/** El texto de un premio elegido con /casino premio: "3 Espada de diamante (Filo III)". */
	private static String elegido(net.minecraft.world.item.ItemStack item) {
		StringBuilder t = new StringBuilder(item.getCount() + " " + item.getHoverName().getString());
		var encantamientos = item.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
				net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
		if (encantamientos.isEmpty()) encantamientos = item.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
				net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
		List<String> nombres = new java.util.ArrayList<>();
		for (var e : encantamientos.entrySet()) {
			nombres.add(net.minecraft.world.item.enchantment.Enchantment.getFullname(e.getKey(), e.getIntValue()).getString());
		}
		if (!nombres.isEmpty()) t.append(" (").append(String.join(", ", nombres)).append(")");
		return t.toString();
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		scroll -= (int) (dy * RENGLON * 2);
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
