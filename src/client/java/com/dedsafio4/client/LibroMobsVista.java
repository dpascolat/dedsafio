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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La pestaña del libro en la G: "Cambios & Mobs". Una fila por día: la ruleta adelante y los mobs de ese día,
 * girando despacio; los que todavía no se habilitaron con /mob salen como "?" de colores. Los editores tocan una
 * casilla para elegir el mob (click derecho la vacía) y ven siempre los mobs, aunque estén ocultos.
 */
final class LibroMobsVista {
	private static final ResourceLocation RULETA = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/catalogo/ruleta_icono.png");
	private static final int FILA = 34, ICONO = 26;

	/** Los mobs de mentira para dibujar (uno por tipo). */
	private final Map<EntityType<?>, Entity> modelos = new HashMap<>();
	private int desplazamiento;
	/** La casilla que un editor está eligiendo (día, casilla) o null; y el panel con su buscador. */
	private int[] eligiendo;
	private EditBox buscador;
	private List<EntityType<?>> resultados = List.of();
	private int despPanel;
	private int px, py, pw, ph;
	/** Lo que hay que mostrar con el mouse encima (lo dibuja la pantalla). */
	Component tooltip;

	private static boolean editor() {
		return Catalogo.PUEDE_EDITAR_CLIENTE;
	}

	boolean eligiendo() {
		return eligiendo != null;
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
			// La ruleta y el número del día.
			com.mojang.blaze3d.systems.RenderSystem.enableBlend();
			g.blit(RULETA, xIzq, y + 1, ICONO, ICONO, 0, 0, 32, 32, 32, 32);
			com.mojang.blaze3d.systems.RenderSystem.disableBlend();
			g.pose().pushPose();
			g.pose().translate(xIzq + ICONO / 2f, y + ICONO + 2, 0);
			g.pose().scale(0.55f, 0.55f, 1);
			g.drawCenteredString(font, "Día " + dia, 0, 0, 0xFFE8E8E8);
			g.pose().popPose();
			if (mx >= xIzq && mx < xIzq + ICONO && my >= y && my < y + ICONO) tooltip = Component.literal("Día " + dia).withColor(0xFFE14A);
			for (int c = 0; c < LibroMobs.POR_DIA; c++) {
				int x = xCeldas + c * celda;
				boolean encima = mx >= x && mx < x + celda && my >= y && my < y + FILA - 2 && eligiendo == null;
				if (eligiendo != null && eligiendo[0] == dia && eligiendo[1] == c) g.fill(x, y, x + celda, y + FILA - 2, 0x605AD8FF);
				else if (encima && editor()) g.fill(x, y, x + celda, y + FILA - 2, 0x25FFFFFF);
				String id = mobEn(dia, c);
				if (id.isEmpty()) {
					if (editor()) g.drawCenteredString(font, "+", x + celda / 2, y + FILA / 2 - 5, 0xFF3A4250);
					continue;
				}
				EntityType<?> tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(id)).orElse(null);
				if (tipo == null) continue;
				boolean visto = LibroMobs.VISTOS_CLIENTE.contains(id);
				if (visto || editor()) {
					dibujarMob(g, tipo, x + celda / 2f, y + (FILA - 2) / 2f, celda - 4, FILA - 6, giro + c * 40);
					if (!visto) {
						g.pose().pushPose();
						g.pose().translate(0, 0, 200);
						g.drawString(font, "?", x + celda - 7, y + 1, 0xFFFF7AC8, true);
						g.pose().popPose();
					}
				} else {
					float esc = (Math.min(celda, FILA) - 6) / 16f;
					g.pose().pushPose();
					g.pose().translate(x + celda / 2f - 8 * esc, y + (FILA - 2) / 2f - 8 * esc, 0);
					g.pose().scale(esc, esc, 1);
					g.renderItem(new ItemStack(ModItems.SIGNO_INTERROGACION), 0, 0);
					g.pose().popPose();
				}
				if (encima) {
					tooltip = visto || editor() ? tipo.getDescription().copy().withColor(visto ? 0xFFFFFF : 0xFF7AC8)
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
			g.drawCenteredString(font, "Editor: click en una casilla para elegir el mob · click derecho: vaciar", 0, 0, 0xFF8FD8FF);
			g.pose().popPose();
		}
		if (eligiendo != null) dibujarPanel(g, font, mx, my, parcial);
	}

	/** Un mob chiquito, parado en el medio de (cx, cy), que entra en ancho × alto y gira despacio. */
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
		g.drawString(font, "Elige el mob del Día " + eligiendo[0], px + 8, py + 7, 0xFFFFFFFF, true);
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
			g.drawString(font, tipo.getDescription(), px + 10, y + 2, delMod ? 0xFF8FD8FF : 0xFFD0D0D0, false);
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
		if (!editor()) return false;
		int filasVisibles = Math.max(1, (yAbajo - yArriba) / FILA);
		int xCeldas = xIzq + ICONO + 8;
		int celda = Math.min(FILA, (xDer - xCeldas) / LibroMobs.POR_DIA);
		int c = (int) Math.floor((mx - xCeldas) / celda), f = (int) Math.floor((my - yArriba) / FILA);
		if (mx < xCeldas || c < 0 || c >= LibroMobs.POR_DIA || f < 0 || f >= filasVisibles) return false;
		int dia = f + desplazamiento + 1;
		if (dia > dias()) return false;
		if (boton == 1) poner(dia, c, "");
		else abrirPanel(dia, c, x0, y0, ancho, alto);
		return true;
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
		else desplazamiento -= (int) Math.signum(dy);
	}

	/** Teclas con el panel abierto: Esc lo cierra; lo demás va al buscador. */
	boolean tecla(int tecla, int scan, int mods) {
		if (eligiendo == null) return false;
		if (tecla == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) eligiendo = null;
		else buscador.keyPressed(tecla, scan, mods);
		return true;
	}

	boolean letra(char c, int mods) {
		return eligiendo != null && buscador.charTyped(c, mods);
	}
}
