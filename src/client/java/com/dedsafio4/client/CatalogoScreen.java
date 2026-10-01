package com.dedsafio4.client;

import com.dedsafio4.catalogo.Catalogo;
import com.dedsafio4.items.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;

/**
 * El Catálogo (tecla G), como la imagen del diseño: marco plateado con detalles de colores, las 4
 * pestañas a la izquierda (libro, espada, pergamino y lupa; por ahora solo el libro) y los ítems del mod en
 * dos paneles, una familia por fila. Los ocultos se ven como el Signo de Interrogación. Al tocar un ítem
 * que tiene crafteo aparece su crafteo (3×3, con el borde de colores).
 */
public class CatalogoScreen extends Screen {
	private static final int COLUMNAS = 6;
	/** El tamaño de cada casilla: lo más grande que entre en el marco (entre 20 y 30). */
	private int CELDA = 20;
	private static final int[] ARCOIRIS = {0xFFFF5A5A, 0xFFFFB347, 0xFFFFF06A, 0xFF7CFF8A, 0xFF5AD8FF, 0xFF8A7CFF, 0xFFE07CFF};
	/** Las pestañas que ya tienen su dibujo (del diseño); las demás se arman con un ítem. */
	private static final net.minecraft.resources.ResourceLocation[] DIBUJO_PESTANA = {
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/catalogo/pestana_libro.png"),
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/catalogo/pestana_espada.png"),
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/catalogo/pestana_papel.png"),
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/catalogo/pestana_lupa.png")};
	/** El marco del diseño (836×470; lo de adentro es transparente). */
	private static final net.minecraft.resources.ResourceLocation MARCO =
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/catalogo/marco.png");
	private static final ItemStack[] PESTANAS = {new ItemStack(Items.BOOK), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.PAPER), new ItemStack(Items.SPYGLASS)};

	private int x0, y0, ancho, alto, tab, xIzq, xDer, yGrilla, filasVisibles, desplazamiento;
	/** El crafteo que se está mostrando (o null). */
	private RecipeHolder<CraftingRecipe> receta;
	private Item recetaDe;
	/** 0: el catálogo de ítems (el libro); 1: las Misiones (el pergamino); 2: el buscador (la lupa). */
	private int modo;
	/** El buscador: escribís el nombre de cualquier ítem o bloque (del mod o de Minecraft) y aparece. */
	private net.minecraft.client.gui.components.EditBox buscador;
	private String textoBuscado = "";
	private List<Item> resultados = List.of();
	private static List<Item> todosLosItems;
	/** La casilla que un editor está eligiendo (panel, fila, columna), o null; y el panel para elegir con su buscador. */
	private int[] eligiendo;
	private net.minecraft.client.gui.components.EditBox buscadorCasilla;
	private List<Item> resultadosCasilla = List.of();
	private int despCasilla, sx, sy, sw, sh;
	private static final int CS = 18;
	private static List<String> nombresNormalizados;
	private final MisionesVista misiones = new MisionesVista();
	/**
	 * Todo se arma en un lienzo fijo de 420×236 (la forma del marco) y se agranda o achica para llenar la
	 * pantalla: así se ve igual con cualquier Gui Scale.
	 */
	private static final int VW = 420, VH = 236;
	private float escala = 1, offX, offY;
	/** Lo que hay que mostrar con el mouse encima (se dibuja al final, sin agrandar). */
	private ItemStack tooltipItem;
	private Component tooltipTexto;

	private double virtualX(double mx) {
		return (mx - offX) / escala;
	}

	private double virtualY(double my) {
		return (my - offY) / escala;
	}

	public CatalogoScreen() {
		super(Component.literal("Catálogo"));
	}

	@Override
	protected void init() {
		// El lienzo fijo, agrandado para ocupar casi toda la pantalla (con la forma del marco, 836×470).
		escala = Math.min((width - 12f) / VW, (height - 10f) / VH);
		offX = (width - VW * escala) / 2f;
		offY = (height - VH * escala) / 2f;
		ancho = VW;
		alto = VH;
		x0 = 0;
		y0 = 0;
		misiones.transformar(escala, offX, offY);
		tab = Mth.clamp((alto - 30 - 18) / 4, 20, 48);
		xIzq = x0 + 16 + tab + 12;
		CELDA = Mth.clamp((x0 + ancho - 22 - xIzq - 12) / (COLUMNAS * 2), 20, 30);
		xDer = xIzq + COLUMNAS * CELDA + 12;
		yGrilla = y0 + 16;
		filasVisibles = Math.max(1, (alto - 34) / CELDA);
		buscador = new net.minecraft.client.gui.components.EditBox(font, xIzq, yGrilla, x0 + ancho - 22 - xIzq, 14, Component.literal("Buscar"));
		buscador.setHint(Component.literal("Escribí un ítem o bloque...").withColor(0xFF8C929C));
		buscador.setMaxLength(50);
		buscador.setValue(textoBuscado);
		buscador.setResponder(t -> {
			textoBuscado = t;
			buscar(t);
		});
		buscador.setVisible(modo == 2);   // (lo dibuja y le pasa el mouse y las teclas la pantalla, dentro del lienzo)
		buscar(textoBuscado);
		// El panel para elegir qué va en una casilla (lo dibuja y le pasa las teclas la pantalla, así queda encima).
		sw = Math.min(ancho - 60, 300);
		sh = Math.min(alto - 40, 180);
		sx = x0 + (ancho - sw) / 2;
		sy = y0 + (alto - sh) / 2;
		String antes = buscadorCasilla == null ? "" : buscadorCasilla.getValue();
		buscadorCasilla = new net.minecraft.client.gui.components.EditBox(font, sx + 8, sy + 20, sw - 16 - 56, 14, Component.literal("Buscar"));
		buscadorCasilla.setHint(Component.literal("Buscá cualquier ítem o bloque...").withColor(0xFF8C929C));
		buscadorCasilla.setMaxLength(50);
		buscadorCasilla.setValue(antes);
		buscadorCasilla.setResponder(t -> {
			resultadosCasilla = filtrar(t, true);
			despCasilla = 0;
		});
		if (eligiendo != null) {
			buscadorCasilla.setFocused(true);
			resultadosCasilla = filtrar(antes, true);
		}
	}

	// --- Buscador ---

	private static String normalizar(String t) {
		return java.text.Normalizer.normalize(t.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	private static void prepararNombres() {
		if (todosLosItems != null) return;
		todosLosItems = new java.util.ArrayList<>();
		nombresNormalizados = new java.util.ArrayList<>();
		for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (item == Items.AIR) continue;
			todosLosItems.add(item);
			nombresNormalizados.add(normalizar(new ItemStack(item).getHoverName().getString()) + " "
					+ net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath().replace('_', ' '));
		}
	}

	/**
	 * Los ítems cuyo nombre tiene lo escrito (primero los que empiezan así). paraEditar: también los ocultos,
	 * y sin escribir nada salen todos.
	 */
	private static List<Item> filtrar(String texto, boolean paraEditar) {
		String q = normalizar(texto.trim());
		prepararNombres();
		if (q.isEmpty()) return paraEditar ? todosLosItems : List.of();
		List<Item> empiezan = new java.util.ArrayList<>(), contienen = new java.util.ArrayList<>();
		for (int i = 0; i < todosLosItems.size(); i++) {
			Item item = todosLosItems.get(i);
			if (!paraEditar && Catalogo.ocultoEnCliente(item)) continue;
			String n = nombresNormalizados.get(i);
			if (n.startsWith(q)) empiezan.add(item);
			else if (n.contains(q)) contienen.add(item);
		}
		empiezan.addAll(contienen);
		return empiezan;
	}

	private void buscar(String texto) {
		desplazamiento = 0;
		List<Item> l = filtrar(texto, false);
		resultados = l.size() > 400 ? l.subList(0, 400) : l;
	}

	private int columnasBusqueda() {
		return Math.max(1, (x0 + ancho - 22 - xIzq) / CELDA);
	}

	private int filasBusqueda() {
		return Math.max(1, (alto - 34 - 18) / CELDA);
	}

	/** Cambia de pestaña (0 libro, 2 misiones, 3 lupa; la espada todavía no hace nada). */
	private void cambiarPestana(int i) {
		if (i == 0) modo = 0;
		else if (i == 2) modo = 1;
		else if (i == 3) modo = 2;
		else return;
		desplazamiento = 0;
		buscador.setVisible(modo == 2);
		buscador.setFocused(modo == 2);
	}

	/** Para las pruebas. */
	public void buscarPrueba(String texto) {
		cambiarPestana(3);
		buscador.setValue(texto);
	}

	public List<Item> resultados() {
		return resultados;
	}

	private static boolean editor() {
		return Catalogo.PUEDE_EDITAR_CLIENTE;
	}

	/** Las filas con algo (y, para los editores, dos vacías más para poner cosas nuevas). */
	private int filasTotales() {
		int f = Math.max(Catalogo.filasEnCliente(0), Catalogo.filasEnCliente(1));
		return Math.min(Catalogo.MAX_FILAS, f + (editor() ? 2 : 0));
	}

	/** Un editor tocó una casilla: se abre el panel para elegir qué va ahí. */
	private void abrirSelector(int panel, int fila, int columna) {
		eligiendo = new int[]{panel, fila, columna};
		buscadorCasilla.setValue("");
		resultadosCasilla = filtrar("", true);
		despCasilla = 0;
		buscadorCasilla.setFocused(true);
	}

	/** Pone ese ítem (o nada) en la casilla que se estaba eligiendo, y se lo manda al servidor. */
	private void ponerEnCasilla(@org.jetbrains.annotations.Nullable Item item) {
		if (eligiendo == null) return;
		int panel = eligiendo[0], fila = eligiendo[1], columna = eligiendo[2];
		Catalogo.ponerEnCliente(panel, fila, columna, item);
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new Catalogo.EditarPayload(panel, fila, columna, item == null ? "" : Catalogo.id(item)));
		minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
				net.minecraft.sounds.SoundEvents.ITEM_PICKUP, item == null ? 0.8f : 1.2f));
		eligiendo = null;
	}

	/** Para las pruebas: elige para esa casilla el resultado n de buscar ese texto (o la vacía con n = -1). */
	public void elegirPrueba(int panel, int fila, int columna, String texto, int n) {
		abrirSelector(panel, fila, columna);
		buscadorCasilla.setValue(texto);
		if (n >= 0) ponerEnCasilla(resultadosCasilla.get(n));
		else if (n == -1) ponerEnCasilla(null);
	}

	public List<Item> resultadosCasilla() {
		return resultadosCasilla;
	}

	private int columnasSelector() {
		return Math.max(1, (sw - 20) / CS);
	}

	private int filasSelector() {
		return Math.max(1, (sh - 44) / CS);
	}

	/** El panel para elegir: título, buscador, botón para vaciar la casilla y la grilla de todos los ítems. */
	private void dibujarSelector(GuiGraphics g, int mx, int my, float parcial) {
		g.pose().pushPose();
		g.pose().translate(0, 0, 300);
		g.fill(sx, sy, sx + sw, sy + sh, 0xFF141820);
		bordeArcoiris(g, sx, sy, sx + sw, sy + sh, 2);
		g.drawString(font, "Elegí qué va en esta casilla", sx + 8, sy + 7, 0xFFFFFFFF, true);
		g.drawString(font, "x", sx + sw - 12, sy + 6, 0xFFFF7070, true);
		buscadorCasilla.render(g, mx, my, parcial);
		int bx = sx + sw - 8 - 50;
		boolean sobreVaciar = mx >= bx && mx < bx + 50 && my >= sy + 20 && my < sy + 34;
		g.fill(bx, sy + 20, bx + 50, sy + 34, sobreVaciar ? 0xFF8A3A3A : 0xFF5E2A2A);
		g.drawCenteredString(font, "Vaciar", bx + 25, sy + 23, 0xFFFFFFFF);
		int cols = columnasSelector(), filas = filasSelector(), gx = sx + 8, gy = sy + 40;
		int total = (resultadosCasilla.size() + cols - 1) / cols;
		despCasilla = Mth.clamp(despCasilla, 0, Math.max(0, total - filas));
		Item bajo = null;
		for (int f = 0; f < filas; f++) {
			for (int c = 0; c < cols; c++) {
				int i = (f + despCasilla) * cols + c;
				int x = gx + c * CS, y = gy + f * CS;
				borde(g, x, y, x + CS + 1, y + CS + 1, 0xFF18202C);
				if (i >= resultadosCasilla.size()) continue;
				if (mx >= x && mx < x + CS && my >= y && my < y + CS) {
					g.fill(x + 1, y + 1, x + CS, y + CS, 0x40FFFFFF);
					bajo = resultadosCasilla.get(i);
				}
				g.renderItem(new ItemStack(resultadosCasilla.get(i)), x + 1, y + 1);
			}
		}
		if (resultadosCasilla.isEmpty()) g.drawCenteredString(font, "No se encontró nada.", sx + sw / 2, gy + 20, 0xFF8C929C);
		// La barra para bajar.
		int bx2 = sx + sw - 8, by0 = gy, by1 = gy + filas * CS;
		g.fill(bx2, by0, bx2 + 2, by1, 0xFF2A313C);
		if (total > filas) {
			int largo = Math.max(10, (by1 - by0) * filas / total);
			int desde = by0 + (by1 - by0 - largo) * despCasilla / Math.max(1, total - filas);
			g.fill(bx2, desde, bx2 + 2, desde + largo, 0xFFC9CDD4);
		}
		g.pose().popPose();
		if (bajo != null) tooltipItem = new ItemStack(bajo);
	}

	/** Un click con el panel para elegir abierto. */
	private void clickSelector(double mx, double my, int boton) {
		if (mx < sx || mx >= sx + sw || my < sy || my >= sy + sh || (mx >= sx + sw - 14 && my < sy + 16)) {
			eligiendo = null;   // afuera o en la x: se cierra sin cambiar nada
			return;
		}
		if (buscadorCasilla.isMouseOver(mx, my)) {
			buscadorCasilla.mouseClicked(mx, my, boton);
			buscadorCasilla.setFocused(true);
			return;
		}
		int bx = sx + sw - 8 - 50;
		if (mx >= bx && mx < bx + 50 && my >= sy + 20 && my < sy + 34) {
			ponerEnCasilla(null);
			return;
		}
		int cols = columnasSelector(), gx = sx + 8, gy = sy + 40;
		int c = (int) ((mx - gx) / CS), f = (int) ((my - gy) / CS);
		if (mx >= gx && my >= gy && c < cols && f < filasSelector()) {
			int i = (f + despCasilla) * cols + c;
			if (i < resultadosCasilla.size()) ponerEnCasilla(resultadosCasilla.get(i));
		}
	}

	@Override
	public boolean charTyped(char c, int mods) {
		if (eligiendo != null) return buscadorCasilla.charTyped(c, mods);
		if (modo == 2 && buscador.isFocused()) return buscador.charTyped(c, mods);
		return super.charTyped(c, mods);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// --- Dibujo ---

	private static void borde(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
		g.fill(x0, y0, x1, y0 + 1, color);
		g.fill(x0, y1 - 1, x1, y1, color);
		g.fill(x0, y0, x0 + 1, y1, color);
		g.fill(x1 - 1, y0, x1, y1, color);
	}

	/** Un borde de colores (arcoíris que da la vuelta). */
	private static void bordeArcoiris(GuiGraphics g, int x0, int y0, int x1, int y1, int grosor) {
		int w = x1 - x0, h = y1 - y0, n = ARCOIRIS.length;
		for (int i = 0; i < w; i++) {
			int c = ARCOIRIS[i * n / Math.max(1, w)];
			g.fill(x0 + i, y0, x0 + i + 1, y0 + grosor, c);
			g.fill(x1 - 1 - i, y1 - grosor, x1 - i, y1, c);
		}
		for (int i = 0; i < h; i++) {
			int c = ARCOIRIS[i * n / Math.max(1, h)];
			g.fill(x1 - grosor, y0 + i, x1, y0 + i + 1, c);
			g.fill(x0, y1 - 1 - i, x0 + grosor, y1 - i, c);
		}
	}

	private static void tiraArcoiris(GuiGraphics g, int x, int y, int w, int h, boolean vertical) {
		int n = ARCOIRIS.length;
		int largo = vertical ? h : w;
		for (int i = 0; i < largo; i++) {
			int c = ARCOIRIS[i * n / Math.max(1, largo)];
			if (vertical) g.fill(x, y + i, x + w, y + i + 1, c);
			else g.fill(x + i, y, x + i + 1, y + h, c);
		}
	}

	/** Una caja plateada con el interior oscuro (el marco y las pestañas). */
	private static void cajaPlateada(GuiGraphics g, int x0, int y0, int x1, int y1, int grosor) {
		g.fill(x0 + 1, y0, x1 - 1, y1, 0xFF8C929C);
		g.fill(x0, y0 + 1, x1, y1 - 1, 0xFF8C929C);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFD5D9E0);
		g.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 3, 0xFFF4F6F9);
		g.fill(x0 + grosor, y0 + grosor, x1 - grosor, y1 - grosor, 0xFF5E646E);
		g.fill(x0 + grosor + 1, y0 + grosor + 1, x1 - grosor - 1, y1 - grosor - 1, 0xF20B0E14);
	}

	@Override
	public void render(GuiGraphics g, int mxReal, int myReal, float parcial) {
		renderBackground(g, mxReal, myReal, parcial);
		tooltipItem = null;
		tooltipTexto = null;
		g.pose().pushPose();
		g.pose().translate(offX, offY, 0);
		g.pose().scale(escala, escala, 1);
		dibujarLienzo(g, (int) Math.floor(virtualX(mxReal)), (int) Math.floor(virtualY(myReal)), parcial);
		g.pose().popPose();
		if (tooltipItem != null) g.renderTooltip(font, tooltipItem, mxReal, myReal);
		else if (tooltipTexto != null) g.renderTooltip(font, tooltipTexto, mxReal, myReal);
	}

	/** Todo lo de adentro, en el lienzo de 420×236 (mx, my: el mouse en el lienzo). */
	private void dibujarLienzo(GuiGraphics g, int mx, int my, float parcial) {
		// El fondo oscuro y el marco del diseño.
		g.fill(x0 + ancho * 3 / 100, y0 + alto * 4 / 100, x0 + ancho * 97 / 100, y0 + alto * 95 / 100, 0xF20B0E14);
		com.mojang.blaze3d.systems.RenderSystem.enableBlend();
		g.blit(MARCO, x0, y0, ancho, alto, 0, 0, 836, 470, 836, 470);
		com.mojang.blaze3d.systems.RenderSystem.disableBlend();
		if (modo == 2) buscador.render(g, mx, my, parcial);
		if (modo == 1) {
			misiones.dibujar(g, font, mx, my, x0, y0, ancho, alto);
			return;
		}

		// Pestañas: la elegida se ve normal y las otras un poco más oscuras.
		int elegida = modo == 2 ? 3 : 0;
		for (int i = 0; i < PESTANAS.length; i++) {
			int tx = x0 + 16, ty = y0 + 16 + i * (tab + 6);
			if (DIBUJO_PESTANA[i] != null) {
				com.mojang.blaze3d.systems.RenderSystem.enableBlend();
				g.blit(DIBUJO_PESTANA[i], tx, ty, tab, tab, 0, 0, 40, 40, 40, 40);
				com.mojang.blaze3d.systems.RenderSystem.disableBlend();
				if (i != elegida) g.fill(tx + 3, ty + 3, tx + tab - 3, ty + tab - 3, 0x50000000);
				continue;
			}
			cajaPlateada(g, tx, ty, tx + tab, ty + tab, 3);
			if (i == 0) bordeArcoiris(g, tx + 4, ty + 4, tx + tab - 4, ty + tab - 4, 1);
			float escala = tab / 26f;
			g.pose().pushPose();
			g.pose().translate(tx + tab / 2f - 8 * escala, ty + tab / 2f - 8 * escala, 0);
			g.pose().scale(escala, escala, 1);
			g.renderItem(PESTANAS[i], 0, 0);
			g.pose().popPose();
		}

		if (modo == 2) {
			dibujarBusqueda(g, mx, my);
			ayudaEditor(g);
			if (receta != null) dibujarReceta(g);
			return;
		}
		// Los dos paneles de ítems.
		Item bajoMouse = null;
		boolean bajoOculto = false;
		int total = filasTotales();
		desplazamiento = Mth.clamp(desplazamiento, 0, Math.max(0, total - filasVisibles));
		for (int f = 0; f < filasVisibles; f++) {
			int fila = f + desplazamiento, y = yGrilla + f * CELDA;
			for (int panel = 0; panel < 2; panel++) {
				int xp = panel == 0 ? xIzq : xDer;
				for (int c = 0; c < COLUMNAS; c++) {
					int x = xp + c * CELDA;
					borde(g, x, y, x + CELDA + 1, y + CELDA + 1, 0xFF18202C);
					if (eligiendo != null && eligiendo[0] == panel && eligiendo[1] == fila && eligiendo[2] == c) g.fill(x + 1, y + 1, x + CELDA, y + CELDA, 0x605AD8FF);
					Item item = Catalogo.itemEnCliente(panel, fila, c);
					if (item == null) continue;
					boolean oculto = Catalogo.ocultoEnCliente(item);
					boolean encima = mx >= x && mx < x + CELDA && my >= y && my < y + CELDA && receta == null && eligiendo == null;
					if (encima) {
						g.fill(x + 1, y + 1, x + CELDA, y + CELDA, 0x30FFFFFF);
						bajoMouse = item;
						bajoOculto = oculto;
					}
					float esc = (CELDA - 4) / 16f;
					g.pose().pushPose();
					g.pose().translate(x + 2, y + 2, 0);
					g.pose().scale(esc, esc, 1);
					g.renderItem(new ItemStack(oculto ? ModItems.SIGNO_INTERROGACION : item), 0, 0);
					g.pose().popPose();
				}
			}
		}
		// La barra para bajar.
		int bx = x0 + ancho - 18, by0 = yGrilla, by1 = yGrilla + filasVisibles * CELDA;
		g.fill(bx, by0, bx + 2, by1, 0xFF2A313C);
		if (total > filasVisibles) {
			int largo = Math.max(12, (by1 - by0) * filasVisibles / total);
			int desde = by0 + (by1 - by0 - largo) * desplazamiento / Math.max(1, total - filasVisibles);
			g.fill(bx, desde, bx + 2, desde + largo, 0xFFC9CDD4);
		} else {
			g.fill(bx, by0, bx + 2, by1, 0xFF8C929C);
		}

		ayudaEditor(g);
		if (receta != null) {
			dibujarReceta(g);
		} else if (eligiendo != null) {
			dibujarSelector(g, mx, my, parcial);
		} else if (bajoMouse != null) {
			if (bajoOculto) tooltipTexto = Component.literal("???").withColor(0xE8C547);
			else tooltipItem = new ItemStack(bajoMouse);
		}
	}

	/** Para los editores: cómo se cambia (abajo, chiquito). */
	private void ayudaEditor(GuiGraphics g) {
		if (!editor()) return;
		if (modo == 2) return;
		String t = "Editor: click en una casilla para elegir qué va · click derecho: ver crafteo";
		g.pose().pushPose();
		g.pose().translate(x0 + ancho / 2f, y0 + alto - 24, 0);
		g.pose().scale(0.6f, 0.6f, 1);
		g.drawCenteredString(font, t, 0, 0, 0xFF8FD8FF);
		g.pose().popPose();
	}

	/** Los resultados del buscador, en una grilla debajo de la caja de texto. */
	private void dibujarBusqueda(GuiGraphics g, int mx, int my) {
		int cols = columnasBusqueda(), filas = filasBusqueda(), yb = yGrilla + 18;
		int total = (resultados.size() + cols - 1) / cols;
		desplazamiento = Mth.clamp(desplazamiento, 0, Math.max(0, total - filas));
		Item bajo = null;
		for (int f = 0; f < filas; f++) {
			for (int c = 0; c < cols; c++) {
				int x = xIzq + c * CELDA, y = yb + f * CELDA;
				borde(g, x, y, x + CELDA + 1, y + CELDA + 1, 0xFF18202C);
				int i = (f + desplazamiento) * cols + c;
				if (i >= resultados.size()) continue;
				Item item = resultados.get(i);
				if (mx >= x && mx < x + CELDA && my >= y && my < y + CELDA && receta == null) {
					g.fill(x + 1, y + 1, x + CELDA, y + CELDA, 0x30FFFFFF);
					bajo = item;
				}
				float esc = (CELDA - 4) / 16f;
				g.pose().pushPose();
				g.pose().translate(x + 2, y + 2, 0);
				g.pose().scale(esc, esc, 1);
				g.renderItem(new ItemStack(item), 0, 0);
				g.pose().popPose();
			}
		}
		if (!textoBuscado.isBlank() && resultados.isEmpty()) {
			g.drawCenteredString(font, "No se encontró nada.", xIzq + cols * CELDA / 2, yb + CELDA, 0xFF8C929C);
		}
		// La barra para bajar.
		int bx = x0 + ancho - 18, by0 = yb, by1 = yb + filas * CELDA;
		g.fill(bx, by0, bx + 2, by1, 0xFF2A313C);
		if (total > filas) {
			int largo = Math.max(12, (by1 - by0) * filas / total);
			int desde = by0 + (by1 - by0 - largo) * desplazamiento / Math.max(1, total - filas);
			g.fill(bx, desde, bx + 2, desde + largo, 0xFFC9CDD4);
		}
		if (bajo != null) tooltipItem = new ItemStack(bajo);
	}

	/** El crafteo del ítem tocado: 3×3 con el borde de colores, la flecha y el resultado. */
	private void dibujarReceta(GuiGraphics g) {
		int w = Math.max(124, font.width(new ItemStack(recetaDe).getHoverName()) + 14), h = 84, x = VW / 2 - w / 2, y = VH / 2 - h / 2;
		g.pose().pushPose();
		g.pose().translate(0, 0, 400);
		g.fill(x, y, x + w, y + h, 0xF0141820);
		bordeArcoiris(g, x, y, x + w, y + h, 2);
		g.drawString(font, new ItemStack(recetaDe).getHoverName(), x + 6, y + 6, 0xFFFFFFFF, true);
		int gx = x + 8, gy = y + 20;
		List<Ingredient> ingredientes = receta.value().getIngredients();
		int anchoR = 3, altoR = 3;
		if (receta.value() instanceof ShapedRecipe s) {
			anchoR = s.getWidth();
			altoR = s.getHeight();
		}
		long t = System.currentTimeMillis() / 1000;
		for (int fy = 0; fy < 3; fy++) {
			for (int fx = 0; fx < 3; fx++) {
				int sx = gx + fx * 19, sy = gy + fy * 19;
				g.fill(sx, sy, sx + 18, sy + 18, 0xFF0E1118);
				g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF2A3140);
				int i = receta.value() instanceof ShapedRecipe ? (fx < anchoR && fy < altoR ? fy * anchoR + fx : -1) : fy * 3 + fx;
				if (i < 0 || i >= ingredientes.size()) continue;
				ItemStack[] opciones = ingredientes.get(i).getItems();
				if (opciones.length > 0) g.renderItem(opciones[(int) (t % opciones.length)], sx + 1, sy + 1);
			}
		}
		g.drawString(font, "→", gx + 3 * 19 + 6, gy + 23, 0xFFFFFFFF, true);
		int rx = gx + 3 * 19 + 20, ry = gy + 18;
		g.fill(rx, ry, rx + 22, ry + 22, 0xFF0E1118);
		g.fill(rx + 1, ry + 1, rx + 21, ry + 21, 0xFF2A3140);
		ItemStack resultado = receta.value().getResultItem(minecraft.level.registryAccess());
		g.renderItem(resultado, rx + 3, ry + 3);
		g.renderItemDecorations(font, resultado, rx + 3, ry + 3);
		g.pose().popPose();
	}

	// --- Mouse ---

	@Override
	public boolean mouseClicked(double mxReal, double myReal, int boton) {
		double mx = virtualX(mxReal), my = virtualY(myReal);
		if (modo == 1) {
			if (misiones.click(mx, my)) modo = 0;   // la casita vuelve al catálogo
			return true;
		}
		if (receta != null) {
			receta = null;   // cualquier click cierra el crafteo
			return true;
		}
		if (eligiendo != null) {
			clickSelector(mx, my, boton);
			return true;
		}
		// Las pestañas: libro, (espada, más adelante), pergamino (Misiones) y lupa (buscador).
		for (int i = 0; i < PESTANAS.length; i++) {
			int tx = x0 + 16, ty = y0 + 16 + i * (tab + 6);
			if (mx >= tx && mx < tx + tab && my >= ty && my < ty + tab) {
				cambiarPestana(i);
				return true;
			}
		}
		if (modo == 2) {
			if (buscador.isMouseOver(mx, my)) {
				buscador.mouseClicked(mx, my, boton);
				buscador.setFocused(true);
				return true;
			}
			int cols = columnasBusqueda(), yb = yGrilla + 18;
			int c = (int) ((mx - xIzq) / CELDA), f = (int) ((my - yb) / CELDA);
			if (mx >= xIzq && my >= yb && c < cols && f < filasBusqueda()) {
				int i = (f + desplazamiento) * cols + c;
				if (i < resultados.size()) mostrarReceta(resultados.get(i));
			}
			return true;
		}
		for (int f = 0; f < filasVisibles; f++) {
			int fila = f + desplazamiento, y = yGrilla + f * CELDA;
			for (int panel = 0; panel < 2; panel++) {
				int xp = panel == 0 ? xIzq : xDer;
				for (int c = 0; c < COLUMNAS; c++) {
					int x = xp + c * CELDA;
					if (mx < x || mx >= x + CELDA || my < y || my >= y + CELDA) continue;
					Item item = Catalogo.itemEnCliente(panel, fila, c);
					if (editor() && boton == 0) {
						if (fila < Catalogo.MAX_FILAS) abrirSelector(panel, fila, c);
					} else if (item != null && (editor() || !Catalogo.ocultoEnCliente(item))) {
						mostrarReceta(item);
					}
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, boton);
	}

	/** Si el ítem tiene crafteo, lo muestra (si no, no pasa nada). */
	public void mostrarReceta(Item item) {
		if (minecraft == null || minecraft.level == null) return;
		for (RecipeHolder<CraftingRecipe> r : minecraft.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
			if (r.value().getResultItem(minecraft.level.registryAccess()).is(item) && !r.value().getIngredients().isEmpty()) {
				receta = r;
				recetaDe = item;
				return;
			}
		}
	}

	/** Para las pruebas: pasar a las Misiones. */
	public void abrirMisiones() {
		cambiarPestana(2);
	}

	public boolean mostrandoReceta() {
		return receta != null;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		if (eligiendo != null) {
			despCasilla -= (int) Math.signum(dy);
			return true;
		}
		if (modo == 1) {
			misiones.scroll(dy);
			return true;
		}
		desplazamiento -= (int) Math.signum(dy);
		return true;
	}

	@Override
	public boolean keyPressed(int tecla, int scan, int mods) {
		if (receta != null && tecla == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			receta = null;
			return true;
		}
		// El panel para elegir: Esc lo cierra; lo demás va a su buscador.
		if (eligiendo != null) {
			if (tecla == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) eligiendo = null;
			else buscadorCasilla.keyPressed(tecla, scan, mods);
			return true;
		}
		// Escribiendo en el buscador, las teclas son para el texto.
		if (modo == 2 && buscador.isFocused() && tecla != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			return buscador.keyPressed(tecla, scan, mods) || buscador.canConsumeInput() || super.keyPressed(tecla, scan, mods);
		}
		// La G también lo cierra.
		if (CatalogoCliente.ABRIR.matches(tecla, scan)) {
			onClose();
			return true;
		}
		return super.keyPressed(tecla, scan, mods);
	}
}
