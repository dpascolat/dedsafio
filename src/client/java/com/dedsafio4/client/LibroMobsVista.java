package com.dedsafio4.client;

import com.dedsafio4.catalogo.Catalogo;
import com.dedsafio4.catalogo.LibroMobs;
import com.dedsafio4.items.ModItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La pestaña del libro en la G: "Cambios & Mobs". Una fila por día: la ruleta adelante (con su texto, "Día N" o el que
 * pongan los editores) y los mobs de ese día, girando despacio; los que todavía no se habilitaron con /mob salen como
 * "?" de colores.
 * Click en un mob que ya se ve: su ficha (el nombre arriba y el mob grande; con el mouse encima, su información).
 * Los editores: click en una casilla vacía para elegir el mob (click derecho la vacía), en la ficha escriben la
 * información y cambian el mob, y con click en la ruleta cambian el texto del día.
 */
final class LibroMobsVista {
	private static final ResourceLocation RULETA = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/catalogo/ruleta_icono.png");
	private static final int FILA = 34, ICONO = 26;
	private static final int ROJO = 0xFFE0503C, ROSA = 0xFFFF7AC8, CELESTE = 0xFF8FD8FF;

	/** Los mobs de mentira para dibujar (uno por tipo). */
	private final Map<EntityType<?>, Entity> modelos = new HashMap<>();
	private int desplazamiento;
	/** La casilla que un editor está eligiendo (día, casilla) o null; y el panel con su buscador. */
	private int[] eligiendo;
	private EditBox buscador;
	private List<EntityType<?>> resultados = List.of();
	private int despPanel;
	private int px, py, pw, ph;
	/** La ficha abierta: el mob y su casilla (día, casilla); el cuadro para escribir la información (editores). */
	private String ficha;
	private int[] fichaCasilla;
	private EditBox infoBox;
	/** El día al que un editor le está cambiando el texto (0 = ninguno) y su cuadro. */
	private int diaTitulo;
	private EditBox tituloBox;
	/** Dónde se dibujó el libro la última vez (para los clicks de la ficha). */
	private int lx0, ly0, lx1, ly1;
	/** Lo que hay que mostrar con el mouse encima (lo dibuja la pantalla): una línea, o varias. */
	Component tooltip;
	List<FormattedCharSequence> tooltipLineas;

	private static boolean editor() {
		return Catalogo.PUEDE_EDITAR_CLIENTE;
	}

	/** ¿Hay algo abierto encima del libro (que se queda con los clicks)? */
	boolean eligiendo() {
		return eligiendo != null || ficha != null || diaTitulo > 0;
	}

	/** Los días que se muestran: hasta el último que tiene algo (los editores, uno más para seguir llenando). */
	private static int dias() {
		int ultimo = LibroMobs.DIAS_CLIENTE.isEmpty() ? 0 : LibroMobs.DIAS_CLIENTE.lastKey();
		return Math.min(LibroMobs.MAX_DIAS, Math.max(editor() ? ultimo + 1 : ultimo, 1));
	}

	private static String mobEn(int dia, int casilla) {
		List<String> l = LibroMobs.DIAS_CLIENTE.get(dia);
		return l == null || casilla >= l.size() ? "" : l.get(casilla);
	}

	private static EntityType<?> tipoDe(String id) {
		ResourceLocation rl = ResourceLocation.tryParse(id);
		return rl == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
	}

	private Entity modelo(EntityType<?> tipo) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		Entity e = modelos.get(tipo);
		if (e == null || e.level() != mc.level) {
			try {
				e = tipo.create(mc.level);
			} catch (Exception ignorado) {
				e = null;
			}
			modelos.put(tipo, e);
		}
		return e;
	}

	// --- Dibujo ---

	void dibujar(GuiGraphics g, Font font, int mx, int my, float parcial, int xIzq, int yArriba, int xDer, int yAbajo) {
		tooltip = null;
		tooltipLineas = null;
		lx0 = xIzq;
		ly0 = yArriba;
		lx1 = xDer;
		ly1 = yAbajo;
		if (ficha != null) {
			dibujarFicha(g, font, mx, my, parcial);
			return;
		}
		boolean libre = !eligiendo();
		int filasVisibles = Math.max(1, (yAbajo - yArriba) / FILA);
		int total = dias();
		desplazamiento = Mth.clamp(desplazamiento, 0, Math.max(0, total - filasVisibles));
		int xCeldas = xIzq + ICONO + 8;
		int celda = Math.min(FILA, (xDer - xCeldas) / LibroMobs.POR_DIA);
		float giro = (System.currentTimeMillis() % 12000L) / 12000f * 360f;
		for (int f = 0; f < filasVisibles; f++) {
			int dia = f + desplazamiento + 1;
			if (dia > total) break;
			int y = yArriba + f * FILA;
			// La ruleta y el texto del día.
			boolean sobreRuleta = libre && mx >= xIzq && mx < xIzq + ICONO && my >= y && my < y + ICONO;
			if (sobreRuleta && editor()) g.fill(xIzq - 1, y, xIzq + ICONO + 1, y + ICONO + 6, 0x25FFFFFF);
			com.mojang.blaze3d.systems.RenderSystem.enableBlend();
			g.blit(RULETA, xIzq, y + 1, ICONO, ICONO, 0, 0, 32, 32, 32, 32);
			com.mojang.blaze3d.systems.RenderSystem.disableBlend();
			String titulo = LibroMobs.tituloDia(dia);
			g.pose().pushPose();
			g.pose().translate(xIzq + ICONO / 2f, y + ICONO + 2, 0);
			float esc = Math.min(0.55f, (ICONO + 10) / (float) Math.max(1, font.width(titulo)));
			g.pose().scale(esc, esc, 1);
			g.drawCenteredString(font, titulo, 0, 0, 0xFFE8E8E8);
			g.pose().popPose();
			if (sobreRuleta) tooltip = Component.literal(titulo).withColor(0xFFE14A)
					.append(editor() ? Component.literal("  (click: cambiar el texto)").withColor(0x8C929C) : Component.empty());
			for (int c = 0; c < LibroMobs.POR_DIA; c++) {
				int x = xCeldas + c * celda;
				boolean encima = libre && mx >= x && mx < x + celda && my >= y && my < y + FILA - 2;
				if (eligiendo != null && eligiendo[0] == dia && eligiendo[1] == c) g.fill(x, y, x + celda, y + FILA - 2, 0x605AD8FF);
				else if (encima) g.fill(x, y, x + celda, y + FILA - 2, 0x25FFFFFF);
				String id = mobEn(dia, c);
				if (id.isEmpty()) {
					if (editor()) g.drawCenteredString(font, "+", x + celda / 2, y + FILA / 2 - 5, 0xFF3A4250);
					continue;
				}
				EntityType<?> tipo = tipoDe(id);
				if (tipo == null) continue;
				boolean visto = LibroMobs.VISTOS_CLIENTE.contains(id);
				if (visto || editor()) {
					dibujarMob(g, tipo, x + celda / 2f, y + (FILA - 2) / 2f, celda - 4, FILA - 6, giro + c * 40);
					if (!visto) {
						g.pose().pushPose();
						g.pose().translate(0, 0, 200);
						g.drawString(font, "?", x + celda - 7, y + 1, ROSA, true);
						g.pose().popPose();
					}
				} else {
					float e = (Math.min(celda, FILA) - 6) / 16f;
					g.pose().pushPose();
					g.pose().translate(x + celda / 2f - 8 * e, y + (FILA - 2) / 2f - 8 * e, 0);
					g.pose().scale(e, e, 1);
					g.renderItem(new ItemStack(ModItems.SIGNO_INTERROGACION), 0, 0);
					g.pose().popPose();
				}
				if (encima) {
					tooltip = visto || editor() ? tipo.getDescription().copy().withColor(visto ? 0xFFFFFF : 0xFF7AC8)
							.append(Component.literal("  (click: ver)").withColor(0x8C929C))
							: Component.literal("???").withColor(0xE8C547);
				}
			}
		}
		if (total == 0 || (!editor() && LibroMobs.DIAS_CLIENTE.isEmpty())) {
			g.drawCenteredString(font, "Todavía no hay mobs en el libro.", (xIzq + xDer) / 2, yArriba + 60, 0xFF8C929C);
		}
		// La barra para bajar.
		int bx = xDer + 4, by0 = yArriba, by1 = yArriba + filasVisibles * FILA;
		g.fill(bx, by0, bx + 2, by1, 0xFF2A313C);
		if (total > filasVisibles) {
			int largo = Math.max(12, (by1 - by0) * filasVisibles / total);
			int desde = by0 + (by1 - by0 - largo) * desplazamiento / Math.max(1, total - filasVisibles);
			g.fill(bx, desde, bx + 2, desde + largo, 0xFFC9CDD4);
		}
		if (editor()) {
			g.pose().pushPose();
			g.pose().translate((xIzq + xDer) / 2f, yAbajo + 2, 0);
			g.pose().scale(0.6f, 0.6f, 1);
			g.drawCenteredString(font, "Editor: casilla vacía: elegir mob · mob: su ficha · click derecho: vaciar · ruleta: texto del día", 0, 0, CELESTE);
			g.pose().popPose();
		}
		if (eligiendo != null) dibujarPanel(g, font, mx, my, parcial);
		if (diaTitulo > 0) dibujarTitulo(g, font, mx, my, parcial);
	}

	/** Un mob parado en el medio de (cx, cy), que entra en ancho × alto y gira despacio. */
	private void dibujarMob(GuiGraphics g, EntityType<?> tipo, float cx, float cy, float ancho, float alto, float giro) {
		if (!(modelo(tipo) instanceof LivingEntity vivo)) {
			g.drawCenteredString(Minecraft.getInstance().font, "?", (int) cx, (int) cy - 4, 0xFFFFFFFF);
			return;
		}
		float h = Math.max(0.3f, vivo.getBbHeight()), w = Math.max(0.3f, vivo.getBbWidth());
		float escala = Math.min(alto / h, ancho / (w * 1.4f));
		vivo.yBodyRot = 180f + giro;
		vivo.yBodyRotO = vivo.yBodyRot;
		vivo.setYRot(vivo.yBodyRot);
		vivo.yHeadRot = vivo.yBodyRot;
		vivo.yHeadRotO = vivo.yBodyRot;
		vivo.setXRot(0);
		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
		try {
			InventoryScreen.renderEntityInInventory(g, cx, cy, escala, new Vector3f(0, h / 2f, 0), pose, null, vivo);
		} catch (Exception ignorado) {
			// Algún mob raro que no se deja dibujar así: queda vacío.
		}
	}

	// --- La ficha del mob ---

	private void abrirFicha(String id, int dia, int casilla) {
		ficha = id;
		fichaCasilla = new int[]{dia, casilla};
		infoBox = null;
		if (editor()) {
			Font font = Minecraft.getInstance().font;
			infoBox = new EditBox(font, 0, 0, 100, 14, Component.literal("Información"));
			infoBox.setMaxLength(LibroMobs.MAX_INFO);
			infoBox.setValue(LibroMobs.INFO_CLIENTE.getOrDefault(id, ""));
			infoBox.setHint(Component.literal("Información... ( | : otra línea · {morado:palabras} : color )").withColor(0xFF8C929C));
		}
		Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
				net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1f));
	}

	/** Lo que sale con el mouse encima del mob: el nombre, la información y dónde aparece. */
	private List<FormattedCharSequence> infoDe(Font font, String id, EntityType<?> tipo) {
		List<FormattedCharSequence> l = new ArrayList<>();
		int ancho = 200;
		l.add(tipo.getDescription().copy().withColor(ROJO).getVisualOrderText());
		String info = LibroMobs.INFO_CLIENTE.get(id);
		if (info != null && !info.isBlank()) {
			for (String parte : info.split("\\|", -1)) {
				l.add(FormattedCharSequence.EMPTY);
				if (parte.isBlank()) continue;
				l.addAll(font.split(com.dedsafio4.catalogo.Misiones.descripcion(parte.strip(), Items.AIR), ancho));
			}
		} else if (editor()) {
			l.add(FormattedCharSequence.EMPTY);
			l.add(Component.literal("Sin información todavía.").withColor(0x8C929C).getVisualOrderText());
		}
		ResourceLocation rl = ResourceLocation.tryParse(id);
		String lugar = rl != null && rl.getNamespace().equals("dedsafio4") ? com.dedsafio4.bestias.AnuncioMob.LUGARES.get(rl.getPath()) : null;
		if (lugar != null) {
			l.add(FormattedCharSequence.EMPTY);
			l.addAll(font.split(Component.literal("Aparece en: ").withColor(ROSA)
					.append(Component.literal(lugar).withColor(0xFFFFFF)), ancho));
		}
		return l;
	}

	private void dibujarFicha(GuiGraphics g, Font font, int mx, int my, float parcial) {
		EntityType<?> tipo = tipoDe(ficha);
		if (tipo == null) {
			ficha = null;
			return;
		}
		int x0 = lx0, y0 = ly0, x1 = lx1, y1 = ly1;
		g.fill(x0, y0, x1, y1, 0xFFDADDE3);
		// El título: el nombre en rojo sobre la barra oscura.
		int barra = 30;
		g.fill(x0 + 6, y0 + 6, x1 - 6, y0 + 6 + barra, 0xFF16202C);
		g.fill(x0 + 6, y0 + 6 + barra, x1 - 6, y0 + 8 + barra, 0xFFFFFFFF);
		Component nombre = tipo.getDescription();
		float escala = Math.min(2f, (x1 - x0 - 40) / (float) Math.max(1, font.width(nombre)));
		g.pose().pushPose();
		g.pose().translate(x0 + 14, y0 + 6 + barra / 2f - 4 * escala, 0);
		g.pose().scale(escala, escala, 1);
		g.drawString(font, nombre, 0, 0, ROJO, false);
		g.pose().popPose();
		// Cerrar.
		boolean sobreCerrar = mx >= x1 - 20 && mx < x1 - 8 && my >= y0 + 8 && my < y0 + 20;
		g.drawString(font, "x", x1 - 16, y0 + 10, sobreCerrar ? 0xFFFF9090 : 0xFFFF6060, true);
		// El mob grande, girando.
		int abajoMob = y1 - (editor() ? 42 : 8);
		float cx = (x0 + x1) / 2f, cy = (y0 + barra + 12 + abajoMob) / 2f;
		float alto = (abajoMob - (y0 + barra + 14)) * 0.8f, ancho = (x1 - x0) * 0.45f;
		float giro = (System.currentTimeMillis() % 16000L) / 16000f * 360f;
		dibujarMob(g, tipo, cx, cy, ancho, alto, giro);
		if (mx >= cx - ancho / 2 && mx < cx + ancho / 2 && my >= cy - alto / 2 && my < cy + alto / 2) tooltipLineas = infoDe(font, ficha, tipo);
		else if (!editor()) {
			g.pose().pushPose();
			g.pose().translate(cx, abajoMob - 2, 0);
			g.pose().scale(0.6f, 0.6f, 1);
			g.drawCenteredString(font, "Pasa el mouse por el mob para ver su información", 0, 0, 0xFF6A7080);
			g.pose().popPose();
		}
		if (editor() && infoBox != null) {
			int by = y1 - 36;
			infoBox.setX(x0 + 8);
			infoBox.setY(by);
			infoBox.setWidth(x1 - x0 - 16 - 66);
			infoBox.render(g, mx, my, parcial);
			boton(g, font, mx, my, x1 - 8 - 60, by, 60, "Guardar", 0xFF2E6B3A);
			boton(g, font, mx, my, x0 + 8, by + 18, 90, "Cambiar el mob", 0xFF2A4E7A);
			g.pose().pushPose();
			g.pose().translate(x0 + 104, by + 21, 0);
			g.pose().scale(0.6f, 0.6f, 1);
			g.drawString(font, "| = otra línea   {morado:palabras} = color   (vacío = sin información)", 0, 0, 0xFF5A6070, false);
			g.pose().popPose();
		}
	}

	private static void boton(GuiGraphics g, Font font, int mx, int my, int x, int y, int w, String texto, int color) {
		boolean encima = mx >= x && mx < x + w && my >= y && my < y + 14;
		g.fill(x, y, x + w, y + 14, encima ? (color | 0x00303030) : color);
		g.drawCenteredString(font, texto, x + w / 2, y + 3, 0xFFFFFFFF);
	}

	private static boolean dentro(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void clickFicha(double mx, double my, int boton) {
		if (!dentro(mx, my, lx0, ly0, lx1 - lx0, ly1 - ly0) || dentro(mx, my, lx1 - 20, ly0 + 8, 12, 12)) {
			ficha = null;
			return;
		}
		if (!editor() || infoBox == null) return;
		int by = ly1 - 36;
		if (infoBox.isMouseOver(mx, my)) {
			infoBox.mouseClicked(mx, my, boton);
			infoBox.setFocused(true);
			return;
		}
		infoBox.setFocused(false);
		if (dentro(mx, my, lx1 - 8 - 60, by, 60, 14)) {
			ClientPlayNetworking.send(new LibroMobs.TextoPayload("mob", ficha, infoBox.getValue()));
			Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
					net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2f));
		} else if (dentro(mx, my, lx0 + 8, by + 18, 90, 14)) {
			int[] casilla = fichaCasilla;
			ficha = null;
			abrirPanel(casilla[0], casilla[1], lx0 - 40, ly0 - 20, lx1 - lx0 + 80, ly1 - ly0 + 40);
		}
	}

	// --- El texto del día (editores) ---

	private void abrirTitulo(int dia) {
		diaTitulo = dia;
		tituloBox = new EditBox(Minecraft.getInstance().font, 0, 0, 140, 14, Component.literal("Texto del día"));
		tituloBox.setMaxLength(LibroMobs.MAX_TITULO);
		String actual = LibroMobs.TITULOS_CLIENTE.get(dia);
		tituloBox.setValue(actual == null ? "" : actual);
		tituloBox.setHint(Component.literal("Día " + dia).withColor(0xFF8C929C));
		tituloBox.setFocused(true);
	}

	private int[] cajaTitulo() {
		int w = 180, h = 58;
		return new int[]{(lx0 + lx1) / 2 - w / 2, (ly0 + ly1) / 2 - h / 2, w, h};
	}

	private void dibujarTitulo(GuiGraphics g, Font font, int mx, int my, float parcial) {
		int[] c = cajaTitulo();
		g.pose().pushPose();
		g.pose().translate(0, 0, 300);
		g.fill(c[0], c[1], c[0] + c[2], c[1] + c[3], 0xFF141820);
		g.fill(c[0], c[1], c[0] + c[2], c[1] + 2, 0xFF5AD8FF);
		g.drawString(font, "Texto del Día " + diaTitulo, c[0] + 8, c[1] + 7, 0xFFFFFFFF, true);
		g.drawString(font, "x", c[0] + c[2] - 12, c[1] + 6, 0xFFFF7070, true);
		tituloBox.setX(c[0] + 8);
		tituloBox.setY(c[1] + 20);
		tituloBox.setWidth(c[2] - 16);
		tituloBox.render(g, mx, my, parcial);
		boton(g, font, mx, my, c[0] + c[2] - 8 - 60, c[1] + 38, 60, "Guardar", 0xFF2E6B3A);
		g.pose().pushPose();
		g.pose().translate(c[0] + 8, c[1] + 42, 0);
		g.pose().scale(0.6f, 0.6f, 1);
		g.drawString(font, "Vacío = \"Día " + diaTitulo + "\"", 0, 0, 0xFF8C929C, false);
		g.pose().popPose();
		g.pose().popPose();
	}

	private void clickTitulo(double mx, double my, int boton) {
		int[] c = cajaTitulo();
		if (!dentro(mx, my, c[0], c[1], c[2], c[3]) || dentro(mx, my, c[0] + c[2] - 14, c[1], 14, 16)) {
			diaTitulo = 0;
			return;
		}
		if (tituloBox.isMouseOver(mx, my)) {
			tituloBox.mouseClicked(mx, my, boton);
			tituloBox.setFocused(true);
			return;
		}
		if (dentro(mx, my, c[0] + c[2] - 8 - 60, c[1] + 38, 60, 14)) guardarTitulo();
	}

	private void guardarTitulo() {
		ClientPlayNetworking.send(new LibroMobs.TextoPayload("dia", String.valueOf(diaTitulo), tituloBox.getValue()));
		Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
				net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2f));
		diaTitulo = 0;
	}

	// --- El panel para elegir el mob (editores) ---

	private void abrirPanel(int dia, int casilla, int x0, int y0, int ancho, int alto) {
		eligiendo = new int[]{dia, casilla};
		pw = Math.min(ancho - 60, 260);
		ph = Math.min(alto - 40, 180);
		px = x0 + (ancho - pw) / 2;
		py = y0 + (alto - ph) / 2;
		buscador = new EditBox(Minecraft.getInstance().font, px + 8, py + 20, pw - 16 - 56, 14, Component.literal("Buscar"));
		buscador.setHint(Component.literal("Busca un mob...").withColor(0xFF8C929C));
		buscador.setMaxLength(40);
		buscador.setResponder(t -> {
			filtrar(t);
			despPanel = 0;
		});
		buscador.setFocused(true);
		filtrar("");
		despPanel = 0;
	}

	private void filtrar(String texto) {
		String q = texto.trim().toLowerCase(java.util.Locale.ROOT);
		List<EntityType<?>> l = new ArrayList<>(), otros = new ArrayList<>();
		for (EntityType<?> tipo : BuiltInRegistries.ENTITY_TYPE) {
			if (tipo.getCategory() == MobCategory.MISC || !tipo.canSummon()) continue;
			String nombre = tipo.getDescription().getString().toLowerCase(java.util.Locale.ROOT) + " " + EntityType.getKey(tipo);
			if (!q.isEmpty() && !nombre.contains(q)) continue;
			// Primero los del mod.
			(EntityType.getKey(tipo).getNamespace().equals("dedsafio4") ? l : otros).add(tipo);
		}
		l.addAll(otros);
		resultados = l;
	}

	private int filasPanel() {
		return Math.max(1, (ph - 44) / 11);
	}

	private void dibujarPanel(GuiGraphics g, Font font, int mx, int my, float parcial) {
		g.pose().pushPose();
		g.pose().translate(0, 0, 300);
		g.fill(px, py, px + pw, py + ph, 0xFF141820);
		g.fill(px, py, px + pw, py + 2, 0xFF5AD8FF);
		g.drawString(font, "Elige el mob del " + LibroMobs.tituloDia(eligiendo[0]), px + 8, py + 7, 0xFFFFFFFF, true);
		g.drawString(font, "x", px + pw - 12, py + 6, 0xFFFF7070, true);
		buscador.render(g, mx, my, parcial);
		int bx = px + pw - 8 - 50;
		boolean sobreVaciar = mx >= bx && mx < bx + 50 && my >= py + 20 && my < py + 34;
		g.fill(bx, py + 20, bx + 50, py + 34, sobreVaciar ? 0xFF8A3A3A : 0xFF5E2A2A);
		g.drawCenteredString(font, "Vaciar", bx + 25, py + 23, 0xFFFFFFFF);
		int filas = filasPanel(), ly = py + 40;
		despPanel = Mth.clamp(despPanel, 0, Math.max(0, resultados.size() - filas));
		for (int i = 0; i < filas; i++) {
			int n = i + despPanel;
			if (n >= resultados.size()) break;
			int y = ly + i * 11;
			boolean encima = mx >= px + 6 && mx < px + pw - 8 && my >= y && my < y + 11;
			if (encima) g.fill(px + 6, y, px + pw - 8, y + 11, 0x40FFFFFF);
			EntityType<?> tipo = resultados.get(n);
			boolean delMod = EntityType.getKey(tipo).getNamespace().equals("dedsafio4");
			g.drawString(font, tipo.getDescription(), px + 10, y + 2, delMod ? CELESTE : 0xFFD0D0D0, false);
		}
		if (resultados.isEmpty()) g.drawCenteredString(font, "No se encontró nada.", px + pw / 2, ly + 20, 0xFF8C929C);
		g.pose().popPose();
	}

	// --- Mouse y teclado ---

	/** Un click en la pestaña del libro. Devuelve true si lo usó. */
	boolean click(double mx, double my, int boton, int xIzq, int yArriba, int xDer, int yAbajo, int x0, int y0, int ancho, int alto) {
		if (eligiendo != null) {
			clickPanel(mx, my, boton);
			return true;
		}
		if (ficha != null) {
			clickFicha(mx, my, boton);
			return true;
		}
		if (diaTitulo > 0) {
			clickTitulo(mx, my, boton);
			return true;
		}
		int filasVisibles = Math.max(1, (yAbajo - yArriba) / FILA);
		int f = (int) Math.floor((my - yArriba) / FILA);
		if (f < 0 || f >= filasVisibles) return false;
		int dia = f + desplazamiento + 1;
		if (dia > dias()) return false;
		// La ruleta del día: los editores le cambian el texto.
		if (mx >= xIzq && mx < xIzq + ICONO) {
			if (editor() && boton == 0) {
				abrirTitulo(dia);
				return true;
			}
			return false;
		}
		int xCeldas = xIzq + ICONO + 8;
		int celda = Math.min(FILA, (xDer - xCeldas) / LibroMobs.POR_DIA);
		int c = (int) Math.floor((mx - xCeldas) / celda);
		if (mx < xCeldas || c < 0 || c >= LibroMobs.POR_DIA) return false;
		String id = mobEn(dia, c);
		if (editor() && boton == 1) {
			poner(dia, c, "");
			return true;
		}
		if (!id.isEmpty() && (editor() || LibroMobs.VISTOS_CLIENTE.contains(id)) && tipoDe(id) != null) {
			abrirFicha(id, dia, c);
			return true;
		}
		if (editor() && id.isEmpty()) {
			abrirPanel(dia, c, x0, y0, ancho, alto);
			return true;
		}
		return false;
	}

	private void clickPanel(double mx, double my, int boton) {
		if (mx < px || mx >= px + pw || my < py || my >= py + ph || (mx >= px + pw - 14 && my < py + 16)) {
			eligiendo = null;
			return;
		}
		if (buscador.isMouseOver(mx, my)) {
			buscador.mouseClicked(mx, my, boton);
			buscador.setFocused(true);
			return;
		}
		int bx = px + pw - 8 - 50;
		if (mx >= bx && mx < bx + 50 && my >= py + 20 && my < py + 34) {
			poner(eligiendo[0], eligiendo[1], "");
			return;
		}
		int ly = py + 40, i = (int) Math.floor((my - ly) / 11);
		if (my >= ly && i < filasPanel() && i + despPanel < resultados.size()) {
			poner(eligiendo[0], eligiendo[1], EntityType.getKey(resultados.get(i + despPanel)).toString());
		}
	}

	private void poner(int dia, int casilla, String mob) {
		ClientPlayNetworking.send(new LibroMobs.EditarPayload(dia, casilla, mob));
		Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
				net.minecraft.sounds.SoundEvents.ITEM_PICKUP, mob.isEmpty() ? 0.8f : 1.2f));
		eligiendo = null;
	}

	void scroll(double dy) {
		if (eligiendo != null) despPanel -= (int) Math.signum(dy);
		else if (ficha == null && diaTitulo == 0) desplazamiento -= (int) Math.signum(dy);
	}

	/** Teclas con algo abierto: Esc lo cierra (Enter guarda); lo demás va al cuadro de texto. */
	boolean tecla(int tecla, int scan, int mods) {
		boolean esc = tecla == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
		boolean enter = tecla == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || tecla == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
		if (eligiendo != null) {
			if (esc) eligiendo = null;
			else buscador.keyPressed(tecla, scan, mods);
			return true;
		}
		if (diaTitulo > 0) {
			if (esc) diaTitulo = 0;
			else if (enter) guardarTitulo();
			else tituloBox.keyPressed(tecla, scan, mods);
			return true;
		}
		if (ficha != null) {
			if (esc) ficha = null;
			else if (infoBox != null && infoBox.isFocused()) {
				if (enter) ClientPlayNetworking.send(new LibroMobs.TextoPayload("mob", ficha, infoBox.getValue()));
				else infoBox.keyPressed(tecla, scan, mods);
			}
			return true;
		}
		return false;
	}

	boolean letra(char c, int mods) {
		if (eligiendo != null) return buscador.charTyped(c, mods);
		if (diaTitulo > 0) return tituloBox.charTyped(c, mods);
		if (ficha != null && infoBox != null && infoBox.isFocused()) return infoBox.charTyped(c, mods);
		return false;
	}
}
