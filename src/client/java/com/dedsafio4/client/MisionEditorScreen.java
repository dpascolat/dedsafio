package com.dedsafio4.client;

import com.dedsafio4.catalogo.Misiones;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

/**
 * El editor de una misión (para los editores del Catálogo). A la izquierda se elige: si es Misión Guía o Misión
 * Principal (y de qué día), el ítem a crear, cuántos, cuántas deditas da y el texto de abajo. Para pintar palabras:
 * se seleccionan en el texto y se toca un color (queda {morado:palabras}); "{item}" pone el nombre del ítem.
 * La Misión Principal tiene además el Premio: lo que manda Eón al entregar la Dedita de la Misión, en una grilla de
 * 3 filas de 9 (como un cofre). Acá se ve chiquito; "Editar premio" (o click en él) abre PremioEditorScreen, donde
 * se arma con tu inventario en el orden que quieras. A la derecha se ve cómo va a quedar.
 */
public class MisionEditorScreen extends Screen {
	private final Screen anterior;
	private final int indice;
	private final Misiones.Mision original;

	private boolean principal;
	private Item item;
	private EditBox dia, buscarItem, cantidad, deditas, texto;
	private List<Item> resultados = List.of();
	private Button tipo, manoPremio, botonObjetivo;
	/** craftear, obtener o romper (ver Misiones.Mision.OBJETIVOS). */
	private String objetivo;
	private int xCampos, xVista, yResultados, yPremio;
	private static final int COLUMNAS_PREMIO = 9, MINI = 12;
	/** El premio de la Misión Principal: cada casilla en su lugar (vacías incluidas). */
	private final ItemStack[] premio = new ItemStack[Misiones.MAX_PREMIO];

	public MisionEditorScreen(Screen anterior, int indice, Misiones.Mision mision) {
		super(Component.literal(mision == null ? "Nueva misión" : "Editar misión"));
		this.anterior = anterior;
		this.indice = indice;
		this.original = mision;
		principal = mision != null && mision.principal();
		objetivo = mision != null ? mision.objetivo() : "craftear";
		item = mision == null ? Items.DIAMOND : mision.item();
		java.util.Arrays.fill(premio, ItemStack.EMPTY);
		if (mision != null) {
			for (int i = 0; i < mision.premio().size() && i < premio.length; i++) premio[i] = mision.premio().get(i).copy();
		}
	}

	private static int numero(EditBox b, int siNo) {
		try {
			return Integer.parseInt(b.getValue().trim());
		} catch (NumberFormatException e) {
			return siNo;
		}
	}

	private EditBox caja(int x, int y, int w, int max, String valor, boolean soloNumeros) {
		EditBox b = new EditBox(font, x, y, w, 16, Component.empty());
		b.setMaxLength(max);
		if (soloNumeros) b.setFilter(t -> t.matches("\\d*"));
		b.setValue(valor);
		addRenderableWidget(b);
		return b;
	}

	@Override
	protected void init() {
		// Si se vuelve a armar (cambió el tamaño), se guarda lo que ya estaba escrito.
		String vDia = dia == null ? String.valueOf(original == null ? 1 : Math.max(1, original.dia())) : dia.getValue();
		String vBuscar = buscarItem == null ? "" : buscarItem.getValue();
		String vCant = cantidad == null ? String.valueOf(original == null ? 1 : original.cantidad()) : cantidad.getValue();
		String vDed = deditas == null ? String.valueOf(original == null ? 0 : original.deditas()) : deditas.getValue();
		String vTexto = texto == null ? (original == null ? "Crea {item}." : original.texto()) : texto.getValue();

		int izq = 12;
		xCampos = izq + 58;
		int anchoIzq = Math.min(250, width / 2 - 20);
		xVista = izq + 58 + anchoIzq + 20;
		int y = 28;

		tipo = addRenderableWidget(Button.builder(Component.empty(), b -> {
			principal = !principal;
			actualizarTipo();
		}).bounds(xCampos, y, 150, 18).build());
		// Qué hay que hacer con el ítem: craftearlo, tenerlo o romperlo.
		botonObjetivo = addRenderableWidget(Button.builder(Component.empty(), b -> {
			var lista = Misiones.Mision.OBJETIVOS;
			objetivo = lista.get((lista.indexOf(objetivo) + 1) % lista.size());
			actualizarTipo();
		}).bounds(xCampos + 156, y, 120, 18).build());
		y += 22;
		dia = caja(xCampos, y, 50, 4, vDia, true);
		y += 22;
		buscarItem = caja(xCampos, y, anchoIzq - 52, 50, vBuscar, false);
		buscarItem.setHint(Component.literal("Buscar ítem...").withColor(0xFF8C929C));
		buscarItem.setResponder(t -> buscar());
		addRenderableWidget(Button.builder(Component.literal("Mano"), b -> {
			ItemStack mano = minecraft.player.getMainHandItem();
			if (!mano.isEmpty()) item = mano.getItem();
		}).bounds(xCampos + anchoIzq - 48, y - 1, 48, 18).build());
		y += 20;
		yResultados = y;
		y += 22;
		cantidad = caja(xCampos, y, 50, 4, vCant, true);
		deditas = caja(xCampos + 120, y, 60, 7, vDed, true);
		y += 22;
		texto = caja(xCampos, y, anchoIzq, Misiones.MAX_TEXTO, vTexto, false);
		y += 22;
		// Los colores: pintan lo que está seleccionado en el texto.
		int x = xCampos;
		for (Map.Entry<String, Integer> c : Misiones.COLORES.entrySet()) {
			String nombre = c.getKey();
			addRenderableWidget(Button.builder(Component.literal("■").withColor(c.getValue()), b -> pintar(nombre))
					.tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(nombre)))
					.bounds(x, y, 16, 16).build());
			x += 18;
		}
		addRenderableWidget(Button.builder(Component.literal("{item}"), b -> {
			texto.insertText("{item}");
			setFocused(texto);
		}).bounds(x + 4, y, 44, 16).build());
		y += 34;
		// El premio (solo Misión Principal): chiquito acá; "Editar premio" abre la pantalla para armarlo.
		yPremio = y;
		manoPremio = addRenderableWidget(Button.builder(Component.literal("Editar premio"), b -> abrirPremio())
				.bounds(xCampos + COLUMNAS_PREMIO * MINI + 6, y, 80, 18).build());
		y += 3 * MINI + 8;
		addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> guardar()).bounds(xCampos, y, 80, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> onClose()).bounds(xCampos + 86, y, 80, 20).build());
		actualizarTipo();
		buscar();
	}

	private void actualizarTipo() {
		tipo.setMessage(Component.literal(principal ? "★ Misión Principal" : "Misión Guía").withColor(principal ? 0xFFFF55 : 0x5ADCC8));
		dia.visible = principal;
		deditas.visible = !principal;
		manoPremio.visible = principal;
		botonObjetivo.setMessage(Component.literal(switch (objetivo) {
			case "obtener" -> "Objetivo: Obtener";
			case "romper" -> "Objetivo: Romper";
			default -> "Objetivo: Craftear";
		}).withColor(0xFFB347));
	}

	/** Lo pone en la primera casilla vacía del premio (o lo suma si ya está el mismo ítem). */
	private void agregarAlPremio(ItemStack pila) {
		for (int i = 0; i < premio.length; i++) {
			if (ItemStack.isSameItemSameComponents(premio[i], pila) && premio[i].getCount() < premio[i].getMaxStackSize()) {
				premio[i].setCount(Math.min(premio[i].getMaxStackSize(), premio[i].getCount() + pila.getCount()));
				return;
			}
		}
		for (int i = 0; i < premio.length; i++) {
			if (premio[i].isEmpty()) {
				premio[i] = pila;
				return;
			}
		}
	}

	private void abrirPremio() {
		minecraft.setScreen(new PremioEditorScreen(this, premio));
	}

	private void buscar() {
		String t = buscarItem.getValue();
		resultados = t.isBlank() ? List.of() : CatalogoScreen.filtrar(t, true);
		if (resultados.size() > 12) resultados = resultados.subList(0, 12);
	}

	/** Lo seleccionado en el texto queda {color:así}; si no hay nada seleccionado, pone {color:texto} para cambiar. */
	private void pintar(String color) {
		String sel = texto.getHighlighted();
		texto.insertText("{" + color + ":" + (sel.isEmpty() ? "texto" : sel) + "}");
		setFocused(texto);
	}

	private Misiones.Mision armar() {
		// Las casillas vacías del final no hacen falta; las del medio sí (para que cada cosa quede en su lugar).
		int hasta = premio.length;
		while (hasta > 0 && premio[hasta - 1].isEmpty()) hasta--;
		List<ItemStack> lista = new java.util.ArrayList<>();
		for (int i = 0; i < hasta; i++) lista.add(premio[i].copy());
		return new Misiones.Mision(original == null ? "" : original.id(), principal, Math.max(0, numero(dia, 1)),
				BuiltInRegistries.ITEM.getKey(item).toString(), Math.max(1, numero(cantidad, 1)), Math.max(0, numero(deditas, 0)),
				texto.getValue(), lista, objetivo);
	}

	private void guardar() {
		ClientPlayNetworking.send(new Misiones.EditarPayload("guardar", indice, armar()));
		onClose();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(anterior);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		for (int i = 0; i < resultados.size(); i++) {
			int x = xCampos + i * 19;
			if (mx >= x && mx < x + 18 && my >= yResultados && my < yResultados + 18) {
				if (boton == 1 && principal) {
					// Clic derecho: va al premio.
					agregarAlPremio(new ItemStack(resultados.get(i)));
				} else {
					item = resultados.get(i);
				}
				return true;
			}
		}
		if (principal && mx >= xCampos && mx < xCampos + COLUMNAS_PREMIO * MINI && my >= yPremio && my < yPremio + 3 * MINI) {
			abrirPremio();
			return true;
		}
		return super.mouseClicked(mx, my, boton);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		super.render(g, mx, my, parcial);
		g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
		int izq = 12;
		g.drawString(font, "Tipo", izq, tipo.getY() + 5, 0xFFC0C0C0);
		if (principal) g.drawString(font, "Día", izq, dia.getY() + 4, 0xFFC0C0C0);
		g.drawString(font, "Ítem", izq, buscarItem.getY() + 4, 0xFFC0C0C0);
		g.drawString(font, "Cantidad", izq, cantidad.getY() + 4, 0xFFC0C0C0);
		if (!principal) g.drawString(font, "Deditas", cantidad.getX() + 66, deditas.getY() + 4, 0xFFC0C0C0);
		g.drawString(font, "Texto", izq, texto.getY() + 4, 0xFFC0C0C0);
		g.drawString(font, "Colores", izq, texto.getY() + 26, 0xFFC0C0C0);
		g.drawString(font, "Selecciona palabras del texto y toca un color.", xCampos, texto.getY() + 42, 0xFF8C929C);

		// Cómo va a quedar (arriba a la derecha).
		int wv = width - xVista - 12;
		if (wv >= 90) {
			int fin = height - 10;
			g.fill(xVista - 6, 24, width - 6, fin, 0xC0101010);
			g.enableScissor(xVista - 6, 24, width - 6, fin);
			MisionesVista.detalle(g, font, armar(), 0, false, xVista, 32, wv);
			g.disableScissor();
		}

		ItemStack encimaDe = null;
		if (principal) {
			g.drawString(font, "Premio", izq, yPremio + 5, 0xFFC0C0C0);
			for (int i = 0; i < premio.length; i++) {
				int x = xCampos + (i % COLUMNAS_PREMIO) * MINI, y = yPremio + (i / COLUMNAS_PREMIO) * MINI;
				boolean encima = mx >= x && mx < x + MINI && my >= y && my < y + MINI;
				g.fill(x, y, x + MINI, y + MINI, encima ? 0xFF6A6A6A : 0xFF2E2E2E);
				g.fill(x + 1, y + 1, x + MINI - 1, y + MINI - 1, 0xFF151515);
				if (premio[i].isEmpty()) continue;
				g.pose().pushPose();
				g.pose().translate(x + 1, y + 1, 0);
				g.pose().scale((MINI - 2) / 16f, (MINI - 2) / 16f, 1);
				g.renderItem(premio[i], 0, 0);
				g.pose().popPose();
				if (encima) encimaDe = premio[i];
			}
		}

		// El ítem elegido (al lado del día/tipo) y los resultados de la búsqueda.
		int xi = tipo.getX() + 158;
		g.fill(xi, tipo.getY() - 1, xi + 20, tipo.getY() + 19, 0xFF1A1C1F);
		g.renderItem(new ItemStack(item), xi + 2, tipo.getY() + 1);
		g.drawString(font, new ItemStack(item).getHoverName(), xi + 24, tipo.getY() + 5, 0xFF6FA8FF);
		for (int i = 0; i < resultados.size(); i++) {
			int x = xCampos + i * 19;
			boolean encima = mx >= x && mx < x + 18 && my >= yResultados && my < yResultados + 18;
			g.fill(x, yResultados, x + 18, yResultados + 18, resultados.get(i) == item ? 0xFF3CCB5A : encima ? 0xFF6A6A6A : 0xFF2E2E2E);
			g.renderItem(new ItemStack(resultados.get(i)), x + 1, yResultados + 1);
			if (encima) encimaDe = new ItemStack(resultados.get(i));
		}

		if (encimaDe != null) g.renderTooltip(font, encimaDe, mx, my);
	}
}
