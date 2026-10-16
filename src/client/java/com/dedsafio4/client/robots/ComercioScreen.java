package com.dedsafio4.client.robots;

import com.dedsafio4.catalogo.Catalogo;
import com.dedsafio4.client.cajero.EstiloCajero;
import com.dedsafio4.items.ModItems;
import com.dedsafio4.robots.OfertasPhora;
import com.dedsafio4.robots.ReponerComercio;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;

/**
 * El comercio con el estilo del Dedsafío (reemplaza al de Minecraft): a la izquierda la barra de nivel, la Dedita
 * para reponer (solo con el Aldeano) y la lista de comercios; a la derecha el nivel, las casillas para pagar y lo que
 * se compra, y el inventario. Las casillas están donde las pone Minecraft (MerchantMenu); acá solo cambia el dibujo.
 * Los ítems del mod que la G tiene ocultos se ven como el signo de pregunta.
 */
public class ComercioScreen extends AbstractContainerScreen<MerchantMenu> {
	private static final int FILAS = 7, FILA_X = 5, FILA_Y = 18, FILA_W = 88, FILA_H = 20;
	private static final int BARRA_X = 5, BARRA_Y = 5, BARRA_W = 76, BOTON_X = 83, BOTON_Y = 2, BOTON = 13;
	private static final int NARANJA = 0xFFE07A1E, VERDE = 0xFF3CCB5A;

	/** Lo que cuesta reponer a este Aldeano (lo manda el servidor; -1: no es un Aldeano, sin botón). */
	public static volatile int costoReponer = -1;
	/** Para cada comercio, en qué momento (System.currentTimeMillis) vuelve si está agotado (0: no está agotado). */
	private static volatile long[] vuelve = new long[0];

	public static void recibir(ReponerComercio.CostoPayload p) {
		costoReponer = p.costo();
		long ahora = System.currentTimeMillis();
		long[] v = new long[p.segundos().size()];
		for (int i = 0; i < v.length; i++) v[i] = p.segundos().get(i) < 0 ? 0 : ahora + p.segundos().get(i) * 1000L;
		vuelve = v;
	}

	private int elegido = -1, desplazamiento;

	public ComercioScreen(MerchantMenu menu, Inventory inventario, Component titulo) {
		super(menu, inventario, titulo);
		imageWidth = 276;
		imageHeight = 166;
	}

	@Override
	public void removed() {
		super.removed();
		costoReponer = -1;
		vuelve = new long[0];
	}

	/** Si es un ítem del mod que la G tiene oculto, se muestra el signo de pregunta. */
	public static ItemStack aLaVista(ItemStack pila) {
		if (pila.isEmpty() || pila.is(ModItems.SIGNO_INTERROGACION)) return pila;
		if (!BuiltInRegistries.ITEM.getKey(pila.getItem()).getNamespace().equals("dedsafio4")) return pila;
		return Catalogo.ocultoEnCliente(pila.getItem()) ? new ItemStack(ModItems.SIGNO_INTERROGACION, pila.getCount()) : pila;
	}

	private static boolean bloqueada(MerchantOffer o) {
		return o.getCostB().is(Items.BARRIER);
	}

	private int filasTotales() {
		return menu.getOffers().size();
	}

	// --- Dibujo ---

	@Override
	protected void renderBg(GuiGraphics g, float delta, int mx, int my) {
		int l = leftPos, t = topPos;
		// El fondo y el marco.
		g.fill(l, t, l + imageWidth, t + imageHeight, 0xFF0B0D12);
		g.fill(l + 1, t + 1, l + imageWidth - 1, t + imageHeight - 1, 0xFF262B35);
		g.fill(l + 2, t + 2, l + imageWidth - 2, t + imageHeight - 2, 0xFF12151C);
		g.fill(l + 2, t + 1, l + 40, t + 2, NARANJA);
		g.fill(l + imageWidth - 40, t + imageHeight - 2, l + imageWidth - 2, t + imageHeight - 1, NARANJA);
		float tiempo = EstiloCajero.tiempo();
		for (int i = 0; i < 48; i += 3) {
			g.fill(l + 114 + i, t + imageHeight - 2, l + 116 + i, t + imageHeight, EstiloCajero.arcoiris(tiempo * 2 + i / 48f));
		}
		// El cuadro de arriba a la derecha (nivel y casillas del comercio).
		g.fill(l + 103, t + 4, l + imageWidth - 4, t + 70, 0xFF2A2F3A);
		g.fill(l + 104, t + 5, l + imageWidth - 5, t + 69, 0xFF181C24);
		for (int[] c : new int[][]{{136, 37}, {162, 37}, {220, 37}}) casilla(g, l + c[0], t + c[1], c[0] == 220 ? 0xFF6A5A20 : 0xFF3A404C);
		g.drawString(font, "->", l + 194, t + 42, 0xFFC0C6D0, false);
		// El inventario.
		for (int i = 0; i < 36; i++) {
			int x = l + 108 + i % 9 * 18, y = t + (i < 27 ? 84 + i / 9 * 18 : 142);
			casilla(g, x, y, 0xFF2E323C);
		}
	}

	private static void casilla(GuiGraphics g, int x, int y, int borde) {
		g.fill(x - 1, y - 1, x + 17, y + 17, borde);
		g.fill(x, y, x + 16, y + 16, 0xFF0E1016);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		Component titulo = this.title;
		if (menu.showProgressBar()) {
			int nivel = menu.getTraderLevel();
			titulo = Component.literal(nivel >= OfertasPhora.NIVEL_MAXIMO ? "Nivel máximo" : "Nivel " + nivel);
		}
		g.drawString(font, titulo, 188 - font.width(titulo) / 2, 12, 0xFFFFFFFF, false);
		g.drawString(font, "Inventario", 108, 73, 0xFFC0C6D0, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		int l = leftPos, t = topPos;
		List<Component> tooltip = null;
		ItemStack tooltipItem = ItemStack.EMPTY;

		// La barra de nivel.
		if (menu.showProgressBar()) {
			int nivel = menu.getTraderLevel(), xp = menu.getTraderXp();
			float lleno = 1f;
			if (nivel < OfertasPhora.NIVEL_MAXIMO && nivel + 1 < OfertasPhora.XP_NIVEL.length) {
				int desde = OfertasPhora.XP_NIVEL[Math.max(1, nivel)], hasta = OfertasPhora.XP_NIVEL[nivel + 1];
				lleno = Mth.clamp((xp - desde) / (float) Math.max(1, hasta - desde), 0f, 1f);
			}
			g.fill(l + BARRA_X - 1, t + BARRA_Y - 1, l + BARRA_X + BARRA_W + 1, t + BARRA_Y + 8, NARANJA);
			g.fill(l + BARRA_X, t + BARRA_Y, l + BARRA_X + BARRA_W, t + BARRA_Y + 7, 0xFF0E1016);
			g.fill(l + BARRA_X + 1, t + BARRA_Y + 1, l + BARRA_X + 1 + (int) ((BARRA_W - 2) * lleno), t + BARRA_Y + 6, VERDE);
			if (mx >= l + BARRA_X && mx < l + BARRA_X + BARRA_W && my >= t + BARRA_Y - 1 && my < t + BARRA_Y + 8) {
				tooltip = List.of(Component.literal(nivel >= OfertasPhora.NIVEL_MAXIMO ? "Nivel máximo" : "Nivel " + nivel + " · " + xp + " de experiencia"));
			}
		}
		// La Dedita para reponer.
		int costo = costoReponer;
		if (costo >= 0) {
			int bx = l + BOTON_X, by = t + BOTON_Y;
			boolean encima = mx >= bx && mx < bx + BOTON && my >= by && my < by + BOTON;
			g.fill(bx, by, bx + BOTON, by + BOTON, encima ? 0xFFFFD84A : 0xFF3A404C);
			g.fill(bx + 1, by + 1, bx + BOTON - 1, by + BOTON - 1, 0xFF181C24);
			g.blit(EstiloCajero.ICONO_DEDITA, bx + 2, by + 2, 0, 0, 9, 9, 9, 9);
			g.pose().pushPose();
			g.pose().translate(bx + BOTON - 1, by + BOTON - 4, 200);
			g.pose().scale(0.5f, 0.5f, 1);
			String n = String.valueOf(costo);
			g.drawString(font, n, -font.width(n), 0, 0xFFFFFFFF, true);
			g.pose().popPose();
			if (encima) {
				tooltip = List.of(Component.literal("Reponer los comercios").withColor(0xFFD84A),
						Component.literal("Cuesta " + costo + (costo == 1 ? " Dedita" : " Deditas") + " (tienes " + deditas() + ")").withColor(0xC0C6D0),
						Component.literal("Cada vez cuesta una más.").withColor(0x8C929C));
			}
		}

		// La lista de comercios.
		MerchantOffers ofertas = menu.getOffers();
		desplazamiento = Mth.clamp(desplazamiento, 0, Math.max(0, ofertas.size() - FILAS));
		for (int f = 0; f < FILAS && f + desplazamiento < ofertas.size(); f++) {
			int k = f + desplazamiento;
			MerchantOffer o = ofertas.get(k);
			int x = l + FILA_X, y = t + FILA_Y + f * FILA_H;
			boolean encima = mx >= x && mx < x + FILA_W && my >= y && my < y + FILA_H - 1;
			g.fill(x, y, x + FILA_W, y + FILA_H - 1, k == elegido ? 0xFFE6E6E6 : encima ? 0xFF4A5060 : 0xFF2E3440);
			g.fill(x + 1, y + 1, x + FILA_W - 1, y + FILA_H - 2, k == elegido ? 0xFFF4F4F4 : 0xFF1A1E26);
			ItemStack pide = aLaVista(o.getCostA()), base = o.getBaseCostA(), da = aLaVista(o.getResult());
			g.renderItem(pide, x + 3, y + 1);
			// Lo que pedía antes (tachado) y lo que pide ahora, en verde.
			g.pose().pushPose();
			g.pose().translate(0, 0, 200);
			String antes = String.valueOf(base.getCount()), ahora = String.valueOf(o.getCostA().getCount());
			int ax = x + 20 - font.width(antes), tachado = k == elegido ? 0xFF505050 : 0xFFE0E0E0;
			g.drawString(font, antes, ax, y + 1, tachado, k != elegido);
			g.fill(ax - 1, y + 4, x + 20, y + 5, tachado);
			g.drawString(font, ahora, x + 20 - font.width(ahora), y + 10, k == elegido ? 0xFF1E9E3A : 0xFF55FF55, k != elegido);
			g.pose().popPose();
			if (!o.getCostB().isEmpty() && !bloqueada(o)) g.renderItem(aLaVista(o.getCostB()), x + 24, y + 1);
			g.drawString(font, ">", x + 44, y + 6, k == elegido ? 0xFF404040 : 0xFFA0A6B0, false);
			g.renderItem(da, x + 56, y + 1);
			g.renderItemDecorations(font, da, x + 56, y + 1);
			if (o.isOutOfStock()) g.drawString(font, "X", x + 78, y + 6, 0xFFFF4040, true);
			if (encima) {
				if (bloqueada(o)) {
					tooltip = List.of(Component.literal("Se desbloquea en el Nivel " + o.getCostB().getCount()).withColor(0xFF7070));
				} else if (mx < x + 22) {
					tooltipItem = pide;
				} else if (mx >= x + 54 && mx < x + 74) {
					// Lo que se compra, con la eficiencia y, si está agotado, cuánto falta para que vuelva.
					List<Component> lineas = new ArrayList<>(getTooltipFromItem(minecraft, da));
					if (menu.showProgressBar()) {
						lineas.add(Component.empty());
						lineas.add(Component.literal("Eficiencia: " + eficiencia(o) + "%").withColor(0xFFFF55));
						if (o.isOutOfStock()) lineas.add(Component.literal(agotado(k)).withColor(0xFF5555));
					}
					tooltip = lineas;
				} else if (o.isOutOfStock()) {
					tooltip = List.of(Component.literal(agotado(k)).withColor(0xFF5555));
				}
			}
		}
		// La barra para bajar, si hay más de 7.
		if (ofertas.size() > FILAS) {
			int bx = l + FILA_X + FILA_W + 2, alto = FILAS * FILA_H - 1;
			g.fill(bx, t + FILA_Y, bx + 3, t + FILA_Y + alto, 0xFF2E3440);
			int largo = Math.max(12, alto * FILAS / ofertas.size());
			int desde = t + FILA_Y + (alto - largo) * desplazamiento / Math.max(1, ofertas.size() - FILAS);
			g.fill(bx, desde, bx + 3, desde + largo, 0xFFC9CDD4);
		}

		if (tooltip != null) g.renderComponentTooltip(font, tooltip, mx, my);
		else if (!tooltipItem.isEmpty()) g.renderTooltip(font, tooltipItem, mx, my);
		else renderTooltip(g, mx, my);
	}

	/**
	 * Qué tan conveniente es el comercio ahora: lo que pedía al principio dividido lo que pide ahora. Si se usa
	 * mucho, al reponer pide más y baja; con descuento puede pasar del 100%.
	 */
	private static int eficiencia(MerchantOffer o) {
		int ahora = Math.max(1, o.getCostA().getCount());
		return Math.round(100f * o.getBaseCostA().getCount() / ahora);
	}

	/** "Agotado - disponible en mm:ss". */
	private static String agotado(int k) {
		long[] v = vuelve;
		if (k >= v.length || v[k] == 0) return "Agotado";
		long seg = Math.max(0, (v[k] - System.currentTimeMillis() + 999) / 1000);
		return String.format("Agotado - disponible en %02d:%02d", seg / 60, seg % 60);
	}

	private int deditas() {
		int n = 0;
		Inventory inv = minecraft.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ModItems.DEDITA)) n += inv.getItem(i).getCount();
		return n;
	}

	// --- Mouse ---

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		int l = leftPos, t = topPos;
		if (costoReponer >= 0 && mx >= l + BOTON_X && mx < l + BOTON_X + BOTON && my >= t + BOTON_Y && my < t + BOTON_Y + BOTON) {
			ClientPlayNetworking.send(new ReponerComercio.ReponerPayload());
			return true;
		}
		for (int f = 0; f < FILAS && f + desplazamiento < menu.getOffers().size(); f++) {
			int x = l + FILA_X, y = t + FILA_Y + f * FILA_H;
			if (mx >= x && mx < x + FILA_W && my >= y && my < y + FILA_H - 1) {
				elegido = f + desplazamiento;
				// Como el de Minecraft: elige el comercio y pone lo que hace falta en las casillas.
				menu.setSelectionHint(elegido);
				menu.tryMoveItems(elegido);
				minecraft.getConnection().send(new ServerboundSelectTradePacket(elegido));
				return true;
			}
		}
		return super.mouseClicked(mx, my, boton);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		if (filasTotales() > FILAS) {
			desplazamiento = Mth.clamp(desplazamiento - (int) Math.signum(dy), 0, filasTotales() - FILAS);
			return true;
		}
		return super.mouseScrolled(mx, my, dx, dy);
	}

}
