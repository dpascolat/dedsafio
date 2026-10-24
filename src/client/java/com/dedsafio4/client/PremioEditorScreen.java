package com.dedsafio4.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * El premio de una Misión Principal, como un cofre: arriba las 3 filas de 9 del premio y abajo tu inventario.
 * Click en algo tuyo lo toma (una copia: no se gasta) y click en una casilla del premio lo deja ahí, en el orden que
 * quieras. Con algo en el mouse, click derecho deja de a uno; sin nada, click toma lo que hay en la casilla (para
 * moverlo) y click derecho le resta uno. La ruedita suma o resta. Click afuera o Esc suelta lo que tienes en el mouse.
 * También lo usa /deditas_de_mision (el premio de las Deditas de la Misión que reparten los OP): al cerrarlo se guarda.
 */
public class PremioEditorScreen extends Screen {
	private static final int CELDA = 18, ANCHO = 9 * CELDA + 16, ALTO = 3 * CELDA + 4 * CELDA + 62;
	private final Screen anterior;
	/** Las casillas del premio (es el mismo arreglo del editor de la misión: se cambia ahí mismo). */
	private final ItemStack[] premio;
	private ItemStack enMano = ItemStack.EMPTY;
	private int x0, y0;
	/** Lo que se hace al cerrarlo (null: nada, el editor de la misión lo guarda después). */
	private final Runnable alCerrar;

	public PremioEditorScreen(Screen anterior, ItemStack[] premio) {
		this(anterior, premio, Component.literal("Premio de la misión"), null);
	}

	public PremioEditorScreen(Screen anterior, ItemStack[] premio, Component titulo, Runnable alCerrar) {
		super(titulo);
		this.anterior = anterior;
		this.premio = premio;
		this.alCerrar = alCerrar;
	}

	@Override
	protected void init() {
		x0 = (width - ANCHO) / 2;
		y0 = (height - ALTO) / 2;
		addRenderableWidget(Button.builder(Component.literal("Listo"), b -> onClose())
				.bounds(x0 + ANCHO - 58, y0 + ALTO - 22, 50, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Vaciar"), b -> java.util.Arrays.fill(premio, ItemStack.EMPTY))
				.bounds(x0 + 8, y0 + ALTO - 22, 50, 16).build());
	}

	@Override
	public void onClose() {
		if (alCerrar != null) alCerrar.run();
		minecraft.setScreen(anterior);
	}

	private int[] posPremio(int i) {
		return new int[]{x0 + 8 + i % 9 * CELDA, y0 + 18 + i / 9 * CELDA};
	}

	/** La mochila (9 a 35) arriba y la barra (0 a 8) abajo, como en el inventario de siempre. */
	private int[] posInventario(int i) {
		int fila = i < 9 ? 3 : (i - 9) / 9;
		return new int[]{x0 + 8 + i % 9 * CELDA, y0 + 18 + 3 * CELDA + 14 + fila * CELDA + (i < 9 ? 4 : 0)};
	}

	private static boolean dentro(double mx, double my, int[] p) {
		return mx >= p[0] && mx < p[0] + CELDA && my >= p[1] && my < p[1] + CELDA;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		for (int i = 0; i < 36; i++) {
			if (!dentro(mx, my, posInventario(i))) continue;
			ItemStack pila = minecraft.player.getInventory().getItem(i);
			enMano = pila.isEmpty() ? ItemStack.EMPTY : pila.copyWithCount(boton == 1 ? 1 : pila.getCount());
			return true;
		}
		for (int i = 0; i < premio.length; i++) {
			if (!dentro(mx, my, posPremio(i))) continue;
			clickPremio(i, boton);
			return true;
		}
		if (super.mouseClicked(mx, my, boton)) return true;
		enMano = ItemStack.EMPTY;   // click afuera: se suelta
		return true;
	}

	private void clickPremio(int i, int boton) {
		ItemStack ahi = premio[i];
		if (enMano.isEmpty()) {
			if (ahi.isEmpty()) return;
			if (boton == 1) {
				ahi.shrink(1);
				if (ahi.isEmpty()) premio[i] = ItemStack.EMPTY;
			} else {
				enMano = ahi;
				premio[i] = ItemStack.EMPTY;
			}
			return;
		}
		if (boton == 1) {
			if (ahi.isEmpty()) premio[i] = enMano.copyWithCount(1);
			else if (ItemStack.isSameItemSameComponents(ahi, enMano) && ahi.getCount() < ahi.getMaxStackSize()) ahi.grow(1);
			else return;
			enMano.shrink(1);
			return;
		}
		if (ahi.isEmpty()) {
			premio[i] = enMano;
			enMano = ItemStack.EMPTY;
		} else if (ItemStack.isSameItemSameComponents(ahi, enMano)) {
			int pasa = Math.min(enMano.getCount(), ahi.getMaxStackSize() - ahi.getCount());
			ahi.grow(pasa);
			enMano.shrink(pasa);
		} else {
			premio[i] = enMano;
			enMano = ahi;
		}
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		for (int i = 0; i < premio.length; i++) {
			if (!dentro(mx, my, posPremio(i)) || premio[i].isEmpty()) continue;
			int n = Math.min(premio[i].getMaxStackSize(), premio[i].getCount() + (dy > 0 ? 1 : -1));
			if (n <= 0) premio[i] = ItemStack.EMPTY;
			else premio[i].setCount(n);
			return true;
		}
		return super.mouseScrolled(mx, my, dx, dy);
	}

	@Override
	public boolean keyPressed(int tecla, int scan, int mods) {
		if (tecla == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && !enMano.isEmpty()) {
			enMano = ItemStack.EMPTY;
			return true;
		}
		return super.keyPressed(tecla, scan, mods);
	}

	private void casilla(GuiGraphics g, int[] p, ItemStack pila, boolean encima, int borde) {
		g.fill(p[0], p[1], p[0] + CELDA, p[1] + CELDA, encima ? 0xFF8A8A8A : borde);
		g.fill(p[0] + 1, p[1] + 1, p[0] + CELDA - 1, p[1] + CELDA - 1, 0xFF151821);
		if (pila.isEmpty()) return;
		g.renderItem(pila, p[0] + 1, p[1] + 1);
		g.renderItemDecorations(font, pila, p[0] + 1, p[1] + 1);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		super.render(g, mx, my, parcial);
		g.fill(x0, y0, x0 + ANCHO, y0 + ALTO, 0xF0101318);
		g.fill(x0, y0, x0 + ANCHO, y0 + 2, 0xFFFFD84A);
		g.drawString(font, "Premio (3 × 9)", x0 + 8, y0 + 6, 0xFFFFD84A);
		ItemStack encimaDe = null;
		for (int i = 0; i < premio.length; i++) {
			int[] p = posPremio(i);
			boolean encima = dentro(mx, my, p);
			casilla(g, p, premio[i], encima, 0xFF6A5A20);
			if (encima && !premio[i].isEmpty()) encimaDe = premio[i];
		}
		g.drawString(font, "Tu inventario (no se gasta)", x0 + 8, posInventario(9)[1] - 10, 0xFFC0C0C0);
		var inv = minecraft.player.getInventory();
		for (int i = 0; i < 36; i++) {
			int[] p = posInventario(i);
			boolean encima = dentro(mx, my, p);
			casilla(g, p, inv.getItem(i), encima, 0xFF2E323C);
			if (encima && !inv.getItem(i).isEmpty()) encimaDe = inv.getItem(i);
		}
		g.pose().pushPose();
		g.pose().translate(x0 + 62, y0 + ALTO - 20, 0);
		g.pose().scale(0.5f, 0.5f, 1);
		g.drawString(font, "Clic derecho: de a uno", 0, 0, 0xFF8C929C);
		g.drawString(font, "Ruedita: + / -", 0, 10, 0xFF8C929C);
		g.drawString(font, "Esc o clic afuera: soltar", 0, 20, 0xFF8C929C);
		g.pose().popPose();
		if (!enMano.isEmpty()) {
			g.pose().pushPose();
			g.pose().translate(0, 0, 300);
			g.renderItem(enMano, mx - 8, my - 8);
			g.renderItemDecorations(font, enMano, mx - 8, my - 8);
			g.pose().popPose();
		} else if (encimaDe != null) {
			g.renderTooltip(font, encimaDe, mx, my);
		}
	}
}
