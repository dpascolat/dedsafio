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
import net.minecraft.world.item.Items;

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
			g.pose().pushPose();
			g.pose().translate(lx, 0, 0);
			g.pose().scale(0.55f, 0.55f, 1);
			g.renderItem(new ItemStack(Items.BOOK), 0, 0);
			g.pose().popPose();
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
		int[] techo = {0xFFFFB0D8, 0xFFFFC8A0, 0xFFFFF0A0, 0xFFB8F0C0, 0xFFA8D8FF, 0xFFC8B0FF};
		for (int i = 0; i < 8; i++) g.fill(x + 8 - i, y + i, x + 8 + i, y + i + 1, techo[i * techo.length / 8]);
		g.fill(x + 2, y + 8, x + 14, y + 16, 0xFFF2F2F2);
		g.fill(x + 3, y + 9, x + 13, y + 15, 0xFFE6E0FF);
		g.fill(x + 6, y + 11, x + 10, y + 16, 0xFF8FA0E8);
	}

	public void dibujar(GuiGraphics g, Font font, int mx, int my, int x0, int y0, int ancho, int alto) {
		medidas(x0, y0, ancho, alto);
		List<Misiones.Mision> lista = Misiones.lista();
		elegida = Mth.clamp(elegida, 0, Math.max(0, lista.size() - 1));
		// La lupa arriba a la izquierda.
		g.renderItem(new ItemStack(Items.SPYGLASS), x0 + 16, y0 + 10);

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

	/** Para los editores: "+ Nueva" abajo de la lista y "Editar ▲ ▼ Borrar" abajo de la misión elegida. */
	private void dibujarBotones(GuiGraphics g, Font font, int mx, int my, boolean vacia) {
		botones.clear();
		if (!Catalogo.PUEDE_EDITAR_CLIENTE) return;
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

	private void tarjeta(GuiGraphics g, Font font, int i, int x, int y, int w) {
		Misiones.Mision m = Misiones.lista().get(i);
		boolean hecha = completa(i), sel = i == elegida;
		int fondo = hecha ? 0xFF2E6B2E : sel ? 0xFF6A6A6A : 0xFF4A4A4A;
		g.fill(x, y, x + w, y + ALTO_TARJETA, fondo);
		int bordeColor = sel ? 0xFFB8B8B8 : 0xFF2E2E2E;
		g.fill(x, y, x + w, y + 1, bordeColor);
		g.fill(x, y + ALTO_TARJETA - 1, x + w, y + ALTO_TARJETA, bordeColor);
		g.fill(x, y, x + 1, y + ALTO_TARJETA, bordeColor);
		g.fill(x + w - 1, y, x + w, y + ALTO_TARJETA, bordeColor);
		// Costado: el punto (verde la elegida) y la chinche.
		g.fill(x + 1, y + 1, x + 12, y + ALTO_TARJETA - 1, hecha ? 0xFF245224 : 0xFF383838);
		int punto = sel ? 0xFF3CCB5A : 0xFF151515;
		g.fill(x + 4, y + 7, x + 9, y + 12, punto);
		g.fill(x + 5, y + 6, x + 8, y + 13, punto);
		g.fill(x + 3, y + 8, x + 10, y + 11, punto);
		tachuela(g, x + 3, y + 24, fijadas.contains(m.id()) ? 0xFFF0D060 : 0xFF151515);
		// El ícono.
		g.fill(x + 15, y + 5, x + 43, y + 33, 0xFF1A1C1F);
		g.renderItem(new ItemStack(m.item()), x + 21, y + 11);
		// Título, descripción y barra.
		int tx = x + 47, tw = w - 47 - 5;
		g.fill(tx, y + 4, tx + tw, y + 15, 0xF0101010);
		titulo(g, font, m, tx + 2, y + 5, 1f, 0);
		Component texto = hecha ? Component.literal("¡Misión completada!").withColor(VERDE_TEXTO) : m.descripcion();
		g.drawString(font, recortar(font, texto, tw - 2), tx + 1, y + 18, 0xFFFFFFFF, false);
		int prog = Misiones.progresoCliente(i);
		barra(g, tx, y + 30, tx + tw - 14, prog / (float) m.cantidad(), hecha);
		g.pose().pushPose();
		g.pose().translate(tx + tw - 12, y + 29, 0);
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
