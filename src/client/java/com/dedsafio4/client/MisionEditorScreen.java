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
 * La Misión Principal tiene además el Premio: lo que manda Eón al entregar la Dedita de la Misión (hasta 18 objetos;
 * las deditas se ponen como monedas). A la derecha se ve cómo va a quedar.
 */
public class MisionEditorScreen extends Screen {
	private final Screen anterior;
	private final int indice;
	private final Misiones.Mision original;

	private boolean principal;
	private Item item;
	private EditBox dia, buscarItem, cantidad, deditas, texto;
	private List<Item> resultados = List.of();
	private Button tipo, manoPremio;
	private int xCampos, xVista, yResultados, yPremio;
	/** El premio de la Misión Principal. */
	private final List<ItemStack> premio = new java.util.ArrayList<>();
	private static final int COLUMNAS_PREMIO = 9;

	public MisionEditorScreen(Screen anterior, int indice, Misiones.Mision mision) {
		super(Component.literal(mision == null ? "Nueva misión" : "Editar misión"));
		this.anterior = anterior;
		this.indice = indice;
		this.original = mision;
		principal = mision != null && mision.principal();
		item = mision == null ? Items.DIAMOND : mision.item();
		if (mision != null) for (ItemStack o : mision.premio()) premio.add(o.copy());
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
		// El premio (solo Misión Principal): dos filas de casillas y "+ Mano" para agregar lo que tienes en la mano.
		yPremio = y;
		manoPremio = addRenderableWidget(Button.builder(Component.literal("+ Mano"), b -> {
			ItemStack mano = minecraft.player.getMainHandItem();
			if (!mano.isEmpty() && premio.size() < Misiones.MAX_PREMIO) premio.add(mano.copy());
		}).bounds(xCampos + COLUMNAS_PREMIO * 19 + 4, y, 50, 18).build());
		y += 54;
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
	}

	/** Suma (o resta, si es negativo) a una casilla del premio; si llega a 0, la saca. */
	private void cambiarPremio(int i, int cuanto) {
		ItemStack o = premio.get(i);
		int n = Math.min(o.getMaxStackSize(), o.getCount() + cuanto);
		if (n <= 0) premio.remove(i);
		else o.setCount(n);
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
		return new Misiones.Mision(original == null ? "" : original.id(), principal, Math.max(0, numero(dia, 1)),
				BuiltInRegistries.ITEM.getKey(item).toString(), Math.max(1, numero(cantidad, 1)), Math.max(0, numero(deditas, 0)),
				texto.getValue(), List.copyOf(premio));
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
					// Clic derecho: va al premio (si ya está al final, suma uno).
					Item r = resultados.get(i);
					if (!premio.isEmpty() && premio.get(premio.size() - 1).is(r)) cambiarPremio(premio.size() - 1, 1);
					else if (premio.size() < Misiones.MAX_PREMIO) premio.add(new ItemStack(r));
				} else {
					item = resultados.get(i);
				}
				return true;
			}
		}
		if (principal) {
			for (int i = 0; i < premio.size(); i++) {
				int x = xCampos + (i % COLUMNAS_PREMIO) * 19, y = yPremio + (i / COLUMNAS_PREMIO) * 19;
				if (mx < x || mx >= x + 18 || my < y || my >= y + 18) continue;
				cambiarPremio(i, boton == 1 ? -1 : hasShiftDown() ? 10 : 1);
				return true;
			}
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
		ItemStack encimaPremio = null;
		if (principal) {
			g.drawString(font, "Premio", izq, yPremio + 5, 0xFFC0C0C0);
			for (int i = 0; i < Misiones.MAX_PREMIO; i++) {
				int x = xCampos + (i % COLUMNAS_PREMIO) * 19, y = yPremio + (i / COLUMNAS_PREMIO) * 19;
				g.fill(x, y, x + 18, y + 18, 0xFF2E2E2E);
				g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF151515);
				if (i >= premio.size()) continue;
				g.renderItem(premio.get(i), x + 1, y + 1);
				g.renderItemDecorations(font, premio.get(i), x + 1, y + 1);
				if (mx >= x && mx < x + 18 && my >= y && my < y + 18) encimaPremio = premio.get(i);
			}
			g.pose().pushPose();
			g.pose().translate(xCampos, yPremio + 40, 0);
			g.pose().scale(0.75f, 0.75f, 1);
			g.drawString(font, "Clic: +1 (Shift +10) · Clic derecho: -1", 0, 0, 0xFF8C929C);
			g.drawString(font, "Clic derecho en un ítem buscado: agregarlo al premio", 0, 10, 0xFF8C929C);
			g.pose().popPose();
		}

		// El ítem elegido (al lado del día/tipo) y los resultados de la búsqueda.
		int xi = tipo.getX() + 158;
		g.fill(xi, tipo.getY() - 1, xi + 20, tipo.getY() + 19, 0xFF1A1C1F);
		g.renderItem(new ItemStack(item), xi + 2, tipo.getY() + 1);
		g.drawString(font, new ItemStack(item).getHoverName(), xi + 24, tipo.getY() + 5, 0xFF6FA8FF);
		ItemStack encimaDe = null;
		for (int i = 0; i < resultados.size(); i++) {
			int x = xCampos + i * 19;
			boolean encima = mx >= x && mx < x + 18 && my >= yResultados && my < yResultados + 18;
			g.fill(x, yResultados, x + 18, yResultados + 18, resultados.get(i) == item ? 0xFF3CCB5A : encima ? 0xFF6A6A6A : 0xFF2E2E2E);
			g.renderItem(new ItemStack(resultados.get(i)), x + 1, yResultados + 1);
			if (encima) encimaDe = new ItemStack(resultados.get(i));
		}

		// Cómo va a quedar.
		int wv = width - xVista - 12;
		if (wv >= 90) {
			g.fill(xVista - 6, 24, width - 6, height - 10, 0xC0101010);
			MisionesVista.detalle(g, font, armar(), 0, false, xVista, 32, wv);
		}
		if (encimaDe != null) g.renderTooltip(font, encimaDe, mx, my);
		else if (encimaPremio != null) g.renderTooltip(font, encimaPremio, mx, my);
	}
}
