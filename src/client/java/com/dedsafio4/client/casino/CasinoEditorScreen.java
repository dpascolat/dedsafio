package com.dedsafio4.client.casino;

import com.dedsafio4.casino.CasinoEditor;
import com.dedsafio4.items.ModItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * El editor de premios del Casino (/casino editar o el botón de la tabla de premios). A la izquierda las 8 figuras;
 * a la derecha, para la figura elegida, 5 casillas del premio chico (2 iguales) y 5 del grande (3 iguales).
 * Se toca una casilla y abajo se elige el ítem (con el buscador), la cantidad (− y +; con Shift de a 10), "Mano"
 * pone lo que tienes en la mano (con encantamientos) y "Borrar" la vacía. Cada cambio se guarda solo.
 */
public class CasinoEditorScreen extends Screen {
	private static final int DORADO = 0xFFF2C230, ROJO = 0xFF7A1020, TEXTO = 0xFFE8E2D8, GRIS = 0xFF9A948C;
	private static final int CASILLA = 22, RES = 18;
	private static final String[] FIGURAS = {"hierro", "botella de experiencia", "pechera de hierro", "pico de hierro",
			"libro de encantamientos", "filete", "poción", "corazon"};
	private static final String[] NOMBRES = {"Hierro", "Experiencia", "Pechera", "Pico", "Libro", "Filete", "Poción", "Corazón"};

	/** "figura|iguales" → la fila que mandó el servidor. */
	private final Map<String, CasinoEditor.Fila> filas = new HashMap<>();
	private int figura = 0;
	/** La casilla elegida: 2 o 3 iguales y cuál (0 a 4); iguales 0 = ninguna. */
	private int selIguales = 0, selCasilla = 0;

	private int x, y, ancho, alto, xd, yRes, columnasRes, filasRes, scrollRes;
	private EditBox buscador;
	private List<Item> resultados = List.of();
	private static List<Item> todos;
	private static List<String> nombres;

	public CasinoEditorScreen(CasinoEditor.AbrirPayload datos) {
		super(Component.literal("Editar premios del casino"));
		actualizar(datos);
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(CasinoEditor.AbrirPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			if (Minecraft.getInstance().screen instanceof CasinoEditorScreen abierta) abierta.actualizar(payload);
			else Minecraft.getInstance().setScreen(new CasinoEditorScreen(payload));
		}));
	}

	private void actualizar(CasinoEditor.AbrirPayload datos) {
		filas.clear();
		for (CasinoEditor.Fila f : datos.filas()) filas.put(f.figura() + "|" + f.iguales(), f);
	}

	@Override
	protected void init() {
		ancho = Math.min(400, width - 16);
		alto = Math.min(250, height - 16);
		x = (width - ancho) / 2;
		y = (height - alto) / 2;
		xd = x + 112;
		int yEditor = y + 112;
		addRenderableWidget(Button.builder(Component.literal("−"), b -> cambiarCantidad(hasShiftDown() ? -10 : -1))
				.bounds(xd, yEditor + 12, 18, 16).build());
		addRenderableWidget(Button.builder(Component.literal("+"), b -> cambiarCantidad(hasShiftDown() ? 10 : 1))
				.bounds(xd + 50, yEditor + 12, 18, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Mano"), b -> ponerMano())
				.bounds(xd + 74, yEditor + 12, 46, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Borrar"), b -> poner(ItemStack.EMPTY))
				.bounds(xd + 124, yEditor + 12, 50, 16).build());
		addRenderableWidget(Button.builder(Component.literal("De siempre"), b -> restaurar(2))
				.bounds(xd + 5 * CASILLA + 8, y + 42, 70, 16).build());
		addRenderableWidget(Button.builder(Component.literal("De siempre"), b -> restaurar(3))
				.bounds(xd + 5 * CASILLA + 8, y + 84, 70, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
				.bounds(x + 8, y + alto - 24, 96, 18).build());
		String antes = buscador == null ? "" : buscador.getValue();
		buscador = new EditBox(font, xd, yEditor + 34, x + ancho - 8 - xd, 14, Component.literal("Buscar"));
		buscador.setHint(Component.literal("Busca el ítem del premio...").withColor(0xFF8C929C));
		buscador.setMaxLength(50);
		buscador.setValue(antes);
		buscador.setResponder(this::buscar);
		addRenderableWidget(buscador);
		yRes = yEditor + 52;
		columnasRes = Math.max(1, (x + ancho - 8 - xd) / RES);
		filasRes = Math.max(1, (y + alto - 6 - yRes) / RES);
		buscar(antes);
	}

	// ---------------------------------------------------------------- Datos

	private CasinoEditor.Fila fila(int iguales) {
		return filas.get(FIGURAS[figura] + "|" + iguales);
	}

	/** Las 5 casillas de una fila (las vacías como EMPTY). */
	private ItemStack[] casillas(int iguales) {
		ItemStack[] c = new ItemStack[CasinoEditor.CASILLAS];
		java.util.Arrays.fill(c, ItemStack.EMPTY);
		CasinoEditor.Fila f = fila(iguales);
		if (f != null) for (int i = 0; i < Math.min(c.length, f.items().size()); i++) c[i] = f.items().get(i).copy();
		return c;
	}

	private ItemStack elegida() {
		return selIguales == 0 ? ItemStack.EMPTY : casillas(selIguales)[selCasilla];
	}

	/** Guarda la fila con la casilla elegida cambiada (y la muestra ya, sin esperar al servidor). */
	private void poner(ItemStack item) {
		if (selIguales == 0) return;
		ItemStack[] c = casillas(selIguales);
		c[selCasilla] = item;
		List<ItemStack> lista = new ArrayList<>();
		for (ItemStack s : c) if (!s.isEmpty()) lista.add(s);
		filas.put(FIGURAS[figura] + "|" + selIguales, new CasinoEditor.Fila(FIGURAS[figura], selIguales, true, lista));
		// Las llenas quedan al principio: la elegida sigue siendo la del ítem que se tocó.
		if (!item.isEmpty()) selCasilla = lista.indexOf(item);
		ClientPlayNetworking.send(new CasinoEditor.GuardarPayload(FIGURAS[figura], selIguales, false, lista));
	}

	private void restaurar(int iguales) {
		ClientPlayNetworking.send(new CasinoEditor.GuardarPayload(FIGURAS[figura], iguales, true, List.of()));
	}

	private void cambiarCantidad(int cuanto) {
		ItemStack item = elegida();
		if (item.isEmpty()) return;
		item.setCount(Mth.clamp(item.getCount() + cuanto, 1, CasinoEditor.MAXIMO));
		poner(item);
	}

	private void ponerMano() {
		if (minecraft == null || minecraft.player == null) return;
		ItemStack mano = minecraft.player.getMainHandItem();
		if (!mano.isEmpty()) poner(mano.copy());
	}

	private void elegirItem(Item item) {
		if (selIguales == 0) return;
		ItemStack antes = elegida();
		poner(new ItemStack(item, antes.isEmpty() ? 1 : antes.getCount()));
	}

	// ---------------------------------------------------------------- Buscador

	private static String normalizar(String t) {
		return Normalizer.normalize(t.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	private void buscar(String texto) {
		if (todos == null) {
			todos = new ArrayList<>();
			nombres = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				if (item == Items.AIR) continue;
				todos.add(item);
				nombres.add(normalizar(new ItemStack(item).getHoverName().getString()) + " "
						+ BuiltInRegistries.ITEM.getKey(item).getPath().replace('_', ' '));
			}
		}
		scrollRes = 0;
		String q = normalizar(texto.trim());
		if (q.isEmpty()) {
			resultados = todos;
			return;
		}
		List<Item> empiezan = new ArrayList<>(), contienen = new ArrayList<>();
		for (int i = 0; i < todos.size(); i++) {
			String n = nombres.get(i);
			if (n.startsWith(q)) empiezan.add(todos.get(i));
			else if (n.contains(q)) contienen.add(todos.get(i));
		}
		empiezan.addAll(contienen);
		resultados = empiezan;
	}

	// ---------------------------------------------------------------- Dibujo

	private static ItemStack icono(int i) {
		return switch (i) {
			case 0 -> new ItemStack(Items.IRON_INGOT);
			case 1 -> new ItemStack(Items.EXPERIENCE_BOTTLE);
			case 2 -> new ItemStack(Items.IRON_CHESTPLATE);
			case 3 -> new ItemStack(Items.IRON_PICKAXE);
			case 4 -> new ItemStack(Items.ENCHANTED_BOOK);
			case 5 -> new ItemStack(Items.COOKED_BEEF);
			case 6 -> new ItemStack(Items.POTION);
			default -> new ItemStack(ModItems.CORAZON);
		};
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		super.renderBackground(g, mx, my, parcial);
		g.fill(x - 2, y - 2, x + ancho + 2, y + alto + 2, DORADO);
		g.fill(x, y, x + ancho, y + alto, 0xF0180A0C);
		g.fill(x, y, x + ancho, y + 22, ROJO);
		String titulo = "CASINO  ·  EDITAR PREMIOS";
		g.drawString(font, titulo, x + (ancho - font.width(titulo)) / 2, y + 7, DORADO, true);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		super.render(g, mx, my, parcial);
		ItemStack tooltip = ItemStack.EMPTY;
		// Las figuras.
		for (int i = 0; i < FIGURAS.length; i++) {
			int fy = y + 28 + i * 22;
			boolean encima = mx >= x + 6 && mx < x + 104 && my >= fy && my < fy + 20;
			g.fill(x + 6, fy, x + 104, fy + 20, i == figura ? 0x90F2C230 : encima ? 0x40F2C230 : 0x20FFFFFF);
			g.renderItem(icono(i), x + 9, fy + 2);
			g.drawString(font, NOMBRES[i], x + 29, fy + 6, i == figura ? 0xFFFFFFFF : TEXTO, false);
		}
		// Las 2 filas de casillas.
		for (int iguales = 2; iguales <= 3; iguales++) {
			int fy = iguales == 2 ? y + 28 : y + 70;
			CasinoEditor.Fila f = fila(iguales);
			boolean propio = f != null && f.propio();
			g.drawString(font, (iguales == 2 ? "2 iguales (premio chico)" : "3 iguales (premio grande)")
					+ (propio ? "" : " · de siempre"), xd, fy, propio ? TEXTO : GRIS, false);
			ItemStack[] c = casillas(iguales);
			for (int i = 0; i < c.length; i++) {
				int cx = xd + i * CASILLA, cy = fy + 12;
				boolean sel = selIguales == iguales && selCasilla == i;
				g.fill(cx, cy, cx + 20, cy + 20, sel ? DORADO : 0xFF5A4A3A);
				g.fill(cx + 1, cy + 1, cx + 19, cy + 19, 0xFF2A1A1C);
				if (!c[i].isEmpty()) {
					g.renderItem(c[i], cx + 2, cy + 2);
					g.renderItemDecorations(font, c[i], cx + 2, cy + 2);
					if (mx >= cx && mx < cx + 20 && my >= cy && my < cy + 20) tooltip = c[i];
				}
			}
			if (propio && f.items().isEmpty()) g.drawString(font, "Nada", xd + 5 * CASILLA + 82, fy + 18, GRIS, false);
		}
		// La casilla elegida.
		int yEditor = y + 112;
		ItemStack sel = elegida();
		String que = selIguales == 0 ? "Toca una casilla para elegir su premio."
				: "Casilla " + (selCasilla + 1) + " de " + selIguales + " iguales: "
				+ (sel.isEmpty() ? "vacía (elige un ítem abajo)" : sel.getHoverName().getString());
		g.drawString(font, font.plainSubstrByWidth(que, x + ancho - 8 - xd), xd, yEditor, TEXTO, false);
		String cantidad = sel.isEmpty() ? "-" : String.valueOf(sel.getCount());
		g.drawString(font, cantidad, xd + 34 - font.width(cantidad) / 2, yEditor + 16, DORADO, false);
		// Los resultados del buscador.
		int max = Math.max(0, (resultados.size() + columnasRes - 1) / columnasRes - filasRes);
		scrollRes = Mth.clamp(scrollRes, 0, max);
		for (int k = 0; k < columnasRes * filasRes; k++) {
			int idx = scrollRes * columnasRes + k;
			if (idx >= resultados.size()) break;
			int rx = xd + (k % columnasRes) * RES, ry = yRes + (k / columnasRes) * RES;
			boolean encima = mx >= rx && mx < rx + RES && my >= ry && my < ry + RES;
			if (encima) g.fill(rx, ry, rx + RES, ry + RES, 0x60FFFFFF);
			ItemStack item = new ItemStack(resultados.get(idx));
			g.renderItem(item, rx + 1, ry + 1);
			if (encima) tooltip = item;
		}
		g.drawString(font, "Shift + −/+ : de a 10", x + 8, y + alto - 38, GRIS, false);
		if (!tooltip.isEmpty()) g.renderTooltip(font, tooltip, mx, my);
	}

	// ---------------------------------------------------------------- Mouse

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		if (super.mouseClicked(mx, my, boton)) return true;
		for (int i = 0; i < FIGURAS.length; i++) {
			int fy = y + 28 + i * 22;
			if (mx >= x + 6 && mx < x + 104 && my >= fy && my < fy + 20) {
				figura = i;
				selIguales = 0;
				return true;
			}
		}
		for (int iguales = 2; iguales <= 3; iguales++) {
			int cy = (iguales == 2 ? y + 28 : y + 70) + 12;
			for (int i = 0; i < CasinoEditor.CASILLAS; i++) {
				int cx = xd + i * CASILLA;
				if (mx >= cx && mx < cx + 20 && my >= cy && my < cy + 20) {
					// Una casilla vacía se elige como la primera libre (las llenas van siempre al principio).
					ItemStack[] c = casillas(iguales);
					int libre = 0;
					while (libre < c.length && !c[libre].isEmpty()) libre++;
					selIguales = iguales;
					selCasilla = c[i].isEmpty() ? Math.min(libre, c.length - 1) : i;
					return true;
				}
			}
		}
		for (int k = 0; k < columnasRes * filasRes; k++) {
			int idx = scrollRes * columnasRes + k;
			if (idx >= resultados.size()) break;
			int rx = xd + (k % columnasRes) * RES, ry = yRes + (k / columnasRes) * RES;
			if (mx >= rx && mx < rx + RES && my >= ry && my < ry + RES) {
				elegirItem(resultados.get(idx));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		if (my >= yRes) scrollRes -= (int) Math.signum(dy);
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
