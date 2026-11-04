package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.catalogo.Catalogo;
import com.dedsafio4.catalogo.Misiones;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Las Misiones dentro del Catálogo (la pestaña del pergamino), como la imagen: a la izquierda la lista de
 * tarjetas (punto, pin, ícono, el título, la descripción cortada y la barra de progreso); a la derecha la misión
 * elegida en grande. Las completadas se ven verdes con "¡Misión completada!". La casita de abajo vuelve al
 * Catálogo. Los editores además ven botones para crear, cambiar, mover y borrar misiones.
 */
public final class MisionesVista {
	private static final ResourceLocation DEDITA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/dedita.png");
	// Los dibujos del Diario de NavithLabs: las tarjetas (elegida, completada y normal, 446×75), el puntito verde o
	// apagado, el librito de las Misiones Guía, la casita y la lupa.
	private static final ResourceLocation TARJETA_ELEGIDA = diario("misiones/tarjeta_elegida"), TARJETA_HECHA = diario("misiones/tarjeta_hecha"),
			TARJETA_NORMAL = diario("misiones/tarjeta_normal"), PUNTO_ACTIVO = diario("misiones/punto_activo"),
			PUNTO_INACTIVO = diario("misiones/punto_inactivo"), LIBRO = diario("misiones/libro"), CASITA = diario("casita"), LUPA = diario("lupa");

	private static ResourceLocation diario(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/diario/" + nombre + ".png");
	}
	private static final int TEAL = 0xFF5ADCC8, AMARILLO = 0xFFFFFF55, VERDE_TEXTO = 0xFF7CFC6A, ALTO_TARJETA = 38;

	private int elegida, desplazamiento;
	/** Cuánto está agrandado el lienzo del Catálogo y dónde empieza (para el recorte de la lista). */
	private float escala = 1, offX, offY;

	public void transformar(float escala, float offX, float offY) {
		this.escala = escala;
		this.offX = offX;
		this.offY = offY;
	}
	private final Set<String> fijadas = new HashSet<>();
	private int x0, y0, ancho, alto, xLista, anchoLista, yLista, altoLista, xCasa, yCasa;

	/** Un botón de editor: dónde está y qué hace. */
	private record Boton(int x, int y, int w, String texto, String accion) {
		boolean encima(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + 12;
		}
	}

	private final List<Boton> botones = new ArrayList<>();

	private static boolean completa(int i) {
		return Misiones.progresoCliente(i) >= Misiones.lista().get(i).cantidad();
	}

	private void medidas(int x0, int y0, int ancho, int alto) {
		this.x0 = x0; this.y0 = y0; this.ancho = ancho; this.alto = alto;
		xLista = x0 + 16;
		anchoLista = ancho / 2 - 20;
		yLista = y0 + 28;
		altoLista = alto - 28 - 36;
		xCasa = x0 + ancho / 2 - 8;
		yCasa = y0 + alto - 34;
	}

	/**
	 * El título con su ícono: "📖 Misión Guía | + N 🪙" (verde agua) o "★ Misión Principal - Día N" (amarillo).
	 * anchoMax: si no entra, sigue en otro renglón (solo en grande). Devuelve el alto que ocupó.
	 */
	static int titulo(GuiGraphics g, Font font, Misiones.Mision m, int x, int y, float escala, int anchoMax) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(escala, escala, 1);
		int alto;
		if (m.principal()) {
			List<FormattedCharSequence> lineas = anchoMax > 0
					? font.split(Component.literal("★ " + m.titulo()), (int) (anchoMax / escala))
					: List.of(Component.literal("★ " + m.titulo()).getVisualOrderText());
			int ly = 1;
			for (FormattedCharSequence l : lineas) {
				int lx = anchoMax > 0 ? ((int) (anchoMax / escala) - font.width(l)) / 2 : 0;
				g.drawString(font, l, lx, ly, AMARILLO, false);
				ly += 10;
			}
			alto = (int) (ly * escala);
		} else {
			String texto = m.titulo();
			int anchoTodo = 12 + font.width(texto) + 3 + 9;
			int lx = anchoMax > 0 ? Math.max(0, ((int) (anchoMax / escala) - anchoTodo) / 2) : 0;
			g.blit(LIBRO, lx, 0, 10, 8, 0, 0, 13, 11, 13, 11);
			g.drawString(font, texto, lx + 12, 1, TEAL, false);
			g.blit(DEDITA, lx + 12 + font.width(texto) + 3, 0, 9, 9, 0, 0, 16, 16, 16, 16);
			alto = (int) (11 * escala);
		}
		g.pose().popPose();
		return alto;
	}

	private static void barra(GuiGraphics g, int x0, int y, int x1, float lleno, boolean completa) {
		g.fill(x0, y, x1, y + 3, 0xFF2A2A2A);
		int w = Math.round((x1 - x0) * Mth.clamp(lleno, 0, 1));
		if (w > 0) g.fill(x0, y, x0 + w, y + 3, completa ? 0xFFB9C4B9 : 0xFFA8A8A8);
	}

	/** La misión en grande: el título arriba, el ítem grande, el texto y la barra con "0/8". */
	static void detalle(GuiGraphics g, Font font, Misiones.Mision m, int prog, boolean hecha, int xd, int y, int wd) {
		y += titulo(g, font, m, xd, y, 1.4f, wd) + 6;
		g.pose().pushPose();
		g.pose().translate(xd + wd / 2f - 24, y, 0);
		g.pose().scale(3, 3, 1);
		g.renderItem(new ItemStack(m.item()), 0, 0);
		g.pose().popPose();
		int yt = y + 56;
		List<FormattedCharSequence> lineas = font.split(hecha ? Component.literal("¡Misión completada!").withColor(VERDE_TEXTO) : m.descripcion(), wd);
		for (FormattedCharSequence l : lineas) {
			g.drawString(font, l, xd, yt, 0xFFFFFFFF, true);
			yt += 10;
		}
		yt += 8;
		barra(g, xd, yt, xd + wd, prog / (float) m.cantidad(), hecha);
		String cuenta = prog + "/" + m.cantidad();
		g.drawString(font, cuenta, xd + (wd - font.width(cuenta)) / 2, yt + 7, 0xFFFFFFFF, true);
	}

	private static void tachuela(GuiGraphics g, int x, int y, int color) {
		// Una chinche chiquita en diagonal.
		g.fill(x + 3, y, x + 6, y + 1, color);
		g.fill(x + 2, y + 1, x + 6, y + 3, color);
		g.fill(x + 1, y + 3, x + 5, y + 4, color);
		g.fill(x + 1, y + 4, x + 2, y + 6, color);
		g.fill(x, y + 6, x + 1, y + 7, color);
	}

	private static void casita(GuiGraphics g, int x, int y) {
		g.blit(CASITA, x, y, 16, 16, 0, 0, 16, 16, 16, 16);
	}

	public void dibujar(GuiGraphics g, Font font, int mx, int my, int x0, int y0, int ancho, int alto) {
		medidas(x0, y0, ancho, alto);
		List<Misiones.Mision> lista = Misiones.lista();
		elegida = Mth.clamp(elegida, 0, Math.max(0, lista.size() - 1));
		// La lupa arriba a la izquierda.
		g.blit(LUPA, x0 + 16, y0 + 10, 13, 13, 0, 0, 13, 13, 13, 13);

		// La lista (las fijadas primero).
		List<Integer> orden = orden();
		int visibles = Math.max(1, altoLista / (ALTO_TARJETA + 3));
		desplazamiento = Mth.clamp(desplazamiento, 0, Math.max(0, orden.size() - visibles));
		// El recorte va en coordenadas de pantalla (el lienzo del Catálogo está agrandado).
		g.enableScissor((int) (offX + xLista * escala), (int) (offY + yLista * escala),
				(int) Math.ceil(offX + (xLista + anchoLista + 6) * escala), (int) Math.ceil(offY + (yLista + altoLista) * escala));
		for (int k = 0; k < orden.size(); k++) {
			int i = orden.get(k);
			int y = yLista + (k - desplazamiento) * (ALTO_TARJETA + 3);
			if (y > yLista + altoLista || y + ALTO_TARJETA < yLista) continue;
			tarjeta(g, font, i, xLista, y, anchoLista);
		}
		g.disableScissor();
		// La barra para bajar.
		int bx = xLista + anchoLista + 3;
		g.fill(bx, yLista, bx + 2, yLista + altoLista, 0xFF2A313C);
		int largo = orden.size() <= visibles ? altoLista : Math.max(12, altoLista * visibles / orden.size());
		int desde = orden.size() <= visibles ? yLista : yLista + (altoLista - largo) * desplazamiento / Math.max(1, orden.size() - visibles);
		g.fill(bx, desde, bx + 2, desde + largo, 0xFFC9CDD4);

		// La elegida, en grande.
		int xd = x0 + ancho / 2 + 6, wd = ancho / 2 - 26;
		if (lista.isEmpty()) {
			String t = "No hay misiones.";
			g.drawString(font, t, xd + (wd - font.width(t)) / 2, y0 + 90, 0xFFA0A0A0, true);
		} else {
			detalle(g, font, lista.get(elegida), Misiones.progresoCliente(elegida), completa(elegida), xd, y0 + 20, wd);
		}

		casita(g, xCasa, yCasa);
		dibujarBotones(g, font, mx, my, lista.isEmpty());
	}

	/** Para los OP: "+ Nueva" abajo de la lista y "Editar ▲ ▼ Borrar" abajo de la misión elegida. */
	private void dibujarBotones(GuiGraphics g, Font font, int mx, int my, boolean vacia) {
		botones.clear();
		var mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.player == null || !mc.player.hasPermissions(2)) return;
		int y = yCasa + 3;
		botones.add(new Boton(xLista, y, font.width("+ Nueva misión") + 8, "+ Nueva misión", "nueva"));
		if (!vacia) {
			int x = x0 + ancho / 2 + 6;
			for (String[] b : new String[][]{{"Editar", "editar"}, {"▲", "subir"}, {"▼", "bajar"}, {"Borrar", "borrar"}}) {
				int w = font.width(b[0]) + 8;
				botones.add(new Boton(x, y, w, b[0], b[1]));
				x += w + 3;
			}
		}
		for (Boton b : botones) {
			boolean encima = b.encima(mx, my);
			g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + 12, encima ? 0xFFB8B8B8 : 0xFF6A6A6A);
			g.fill(b.x() + 1, b.y() + 1, b.x() + b.w() - 1, b.y() + 11, b.accion().equals("borrar") ? 0xFF5A2020 : 0xFF2E2E2E);
			g.drawString(font, b.texto(), b.x() + 4, b.y() + 2, 0xFFFFFFFF, false);
		}
	}

	private List<Integer> orden() {
		List<Misiones.Mision> lista = Misiones.lista();
		List<Integer> orden = new ArrayList<>();
		for (int i = 0; i < lista.size(); i++) if (fijadas.contains(lista.get(i).id())) orden.add(i);
		for (int i = 0; i < lista.size(); i++) if (!fijadas.contains(lista.get(i).id())) orden.add(i);
		return orden;
	}

	/** Una medida del dibujo de la tarjeta (446 de ancho) pasada al ancho de esta tarjeta. */
	private static int enX(int w, int dibujo) {
		return Math.round(w * dibujo / 446f);
	}

	/** La tarjeta: el dibujo (elegida, completada o normal) y encima el punto, la chinche, el ícono, el texto y la barra. */
	private void tarjeta(GuiGraphics g, Font font, int i, int x, int y, int w) {
		Misiones.Mision m = Misiones.lista().get(i);
		boolean hecha = completa(i), sel = i == elegida;
		g.blit(hecha ? TARJETA_HECHA : sel ? TARJETA_ELEGIDA : TARJETA_NORMAL, x, y, w, ALTO_TARJETA, 0, 0, 446, 75, 446, 75);
		// Costado: el punto (verde la elegida) y la chinche.
		g.blit(sel ? PUNTO_ACTIVO : PUNTO_INACTIVO, x + 3, y + 5, 6, 6, 0, 0, 8, 8, 8, 8);
		tachuela(g, x + 3, y + 24, fijadas.contains(m.id()) ? 0xFFF0D060 : 0xFF151515);
		// El ícono, en el medio de su cuadro.
		g.renderItem(new ItemStack(m.item()), x + enX(w, 68) - 8, y + 11);
		// Título, descripción y barra (en sus lugares del dibujo).
		int tx = x + enX(w, 105), tw = enX(w, 330);
		titulo(g, font, m, tx + 2, y + 6, 1f, 0);
		Component texto = hecha ? Component.literal("¡Misión completada!").withColor(VERDE_TEXTO) : m.descripcion();
		g.drawString(font, recortar(font, texto, tw - 2), tx + 1, y + 18, 0xFFFFFFFF, false);
		int prog = Misiones.progresoCliente(i);
		barra(g, tx, y + 30, x + enX(w, 400), prog / (float) m.cantidad(), hecha);
		g.pose().pushPose();
		g.pose().translate(x + enX(w, 404), y + 29, 0);
		g.pose().scale(0.6f, 0.6f, 1);
		g.drawString(font, prog + "/" + m.cantidad(), 0, 0, 0xFFD0D0D0, false);
		g.pose().popPose();
	}

	/** El texto cortado con "..." si no entra. */
	private static FormattedCharSequence recortar(Font font, Component texto, int ancho) {
		if (font.width(texto) <= ancho) return texto.getVisualOrderText();
		FormattedText corto = font.substrByWidth(texto, ancho - font.width("..."));
		return Language.getInstance().getVisualOrder(FormattedText.composite(corto, FormattedText.of("...")));
	}

	/** true si tocó la casita (volver al Catálogo). */
	public boolean click(double mx, double my) {
		if (mx >= xCasa && mx < xCasa + 16 && my >= yCasa && my < yCasa + 16) return true;
		for (Boton b : botones) {
			if (!b.encima(mx, my)) continue;
			sonido();
			accion(b.accion());
			return false;
		}
		List<Integer> orden = orden();
		for (int k = 0; k < orden.size(); k++) {
			int y = yLista + (k - desplazamiento) * (ALTO_TARJETA + 3);
			if (my < Math.max(y, yLista) || my >= Math.min(y + ALTO_TARJETA, yLista + altoLista) || mx < xLista || mx >= xLista + anchoLista) continue;
			int i = orden.get(k);
			if (mx < xLista + 12 && my >= y + 20) {
				String id = Misiones.lista().get(i).id();
				if (!fijadas.remove(id)) fijadas.add(id);   // la chinche fija la misión arriba
			} else {
				elegida = i;
			}
			sonido();
			return false;
		}
		return false;
	}

	private static void sonido() {
		Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
				net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
	}

	private void accion(String accion) {
		Minecraft mc = Minecraft.getInstance();
		List<Misiones.Mision> lista = Misiones.lista();
		boolean hay = elegida >= 0 && elegida < lista.size();
		switch (accion) {
			case "nueva" -> mc.setScreen(new MisionEditorScreen(mc.screen, -1, null));
			case "editar" -> {
				if (hay) mc.setScreen(new MisionEditorScreen(mc.screen, elegida, lista.get(elegida)));
			}
			case "subir", "bajar" -> {
				if (!hay) return;
				ClientPlayNetworking.send(new Misiones.EditarPayload(accion, elegida, lista.get(elegida)));
				elegida = Mth.clamp(elegida + (accion.equals("subir") ? -1 : 1), 0, lista.size() - 1);
			}
			case "borrar" -> {
				if (!hay) return;
				net.minecraft.client.gui.screens.Screen volver = mc.screen;
				int i = elegida;
				Misiones.Mision m = lista.get(i);
				mc.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(si -> {
					if (si) ClientPlayNetworking.send(new Misiones.EditarPayload("borrar", i, m));
					mc.setScreen(volver);
				}, Component.literal("¿Borrar esta misión?"), Component.literal(m.titulo()).append(" · ")
						.append(Component.translatable(m.item().getDescriptionId()))));
			}
			default -> {}
		}
	}

	public void scroll(double dy) {
		desplazamiento -= (int) Math.signum(dy);
	}
}
