package com.dedsafio4.client.subastas;

import com.dedsafio4.client.cajero.EstiloCajero;
import com.dedsafio4.subastas.MenuSubastas;
import com.dedsafio4.subastas.Subastas;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * La casa de subastas (/ah), con el fondo que dibujó Patricio (textures/gui/subastas.png): la grilla de 9×5,
 * «« página »» abajo, tu inventario y, a la derecha, las pestañas Comprar / Vender / Mis ventas / Recuperar.
 * En Vender: la casilla para poner lo que vendes, el precio y el botón para ponerlo a la venta.
 */
public class SubastasScreen extends AbstractContainerScreen<MenuSubastas> {
	private static final String[] PESTANAS = {"Comprar", "Vender", "Mis ventas", "Recuperar"};
	private static final net.minecraft.resources.ResourceLocation FONDO = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
			com.dedsafio4.Dedsafio4.MOD_ID, "textures/gui/subastas.png");
	private static final int FONDO_ANCHO = 720, FONDO_ALTO = 984;
	private final Button[] pestanas = new Button[4];
	private EditBox precio;
	private Button ponerALaVenta;

	public SubastasScreen(MenuSubastas menu, Inventory inventario, Component titulo) {
		super(menu, inventario, titulo);
		imageWidth = MenuSubastas.ANCHO;
		imageHeight = MenuSubastas.ALTO;
		inventoryLabelY = 10000;   // sin los textos de Minecraft
		titleLabelY = 10000;
	}

	@Override
	protected void init() {
		super.init();
		int l = leftPos, t = topPos;
		for (int i = 0; i < 4; i++) {
			int boton = i;
			pestanas[i] = addRenderableWidget(Button.builder(Component.literal(PESTANAS[i]),
					b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, boton))
					.bounds(l + imageWidth + 4, t + 16 + i * 24, 66, 20).build());
		}
		// Vender: el precio y el botón (debajo de la casilla).
		precio = new EditBox(font, l + MenuSubastas.GRILLA_X + 2 * 18, t + MenuSubastas.VENTA_Y + 26, 5 * 18, 14, Component.literal("Precio"));
		precio.setMaxLength(10);
		precio.setFilter(s -> s.chars().allMatch(Character::isDigit));
		precio.setHint(Component.literal("precio por unidad").withColor(0xFF5A6070));
		addRenderableWidget(precio);
		ponerALaVenta = addRenderableWidget(Button.builder(Component.literal("Poner a la venta"), b -> {
			if (precio.getValue().isEmpty()) return;
			ClientPlayNetworking.send(new Subastas.PrecioPayload(Long.parseLong(precio.getValue())));
			precio.setValue("");
		}).bounds(l + MenuSubastas.GRILLA_X + 2 * 18 - 4, t + MenuSubastas.VENTA_Y + 44, 5 * 18 + 8, 18).build());
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		boolean vender = menu.pestana() == MenuSubastas.VENDER;
		precio.visible = vender;
		ponerALaVenta.visible = vender;
		for (int i = 0; i < 4; i++) pestanas[i].active = menu.pestana() != i;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		int y = topPos + MenuSubastas.PAGINAS_Y;
		if (menu.pestana() != MenuSubastas.VENDER && my >= y && my < y + MenuSubastas.PAGINAS_ALTO) {
			if (dentro(mx, leftPos + MenuSubastas.BOTON_IZQ_X) && menu.pagina() > 0) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MenuSubastas.ANTERIOR);
				return true;
			}
			if (dentro(mx, leftPos + MenuSubastas.BOTON_DER_X) && menu.pagina() < menu.paginas() - 1) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MenuSubastas.SIGUIENTE);
				return true;
			}
		}
		return super.mouseClicked(mx, my, boton);
	}

	private static boolean dentro(double mx, int x) {
		return mx >= x && mx < x + MenuSubastas.BOTON_ANCHO;
	}

	@Override
	public boolean keyPressed(int tecla, int codigo, int modificadores) {
		// Mientras escribes el precio, la E no cierra la pantalla.
		if (precio.isFocused() && precio.visible && tecla != 256) return precio.keyPressed(tecla, codigo, modificadores) || true;
		return super.keyPressed(tecla, codigo, modificadores);
	}

	@Override
	protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
		int l = leftPos, t = topPos;
		// El fondo dibujado por Patricio (guardado a 4 veces el tamaño, para que se vea nítido).
		g.blit(FONDO, l, t, imageWidth, imageHeight, 0, 0, FONDO_ANCHO, FONDO_ALTO, FONDO_ANCHO, FONDO_ALTO);
		g.drawCenteredString(font, "Subastas", l + 92, t + 8, 0xFFFFFFFF);
		int py = t + MenuSubastas.PAGINAS_Y;
		if (menu.pestana() == MenuSubastas.VENDER) {
			// En Vender se tapa la grilla: sólo la casilla para vender, el precio y el botón.
			float tiempo = EstiloCajero.tiempo();
			g.fill(l + 11, t + 25, l + 173, t + 123, 0xF2151923);
			g.drawCenteredString(font, "Pon acá lo que quieres vender", l + 92, t + 31, 0xFFB8C0CC);
			int vx = l + MenuSubastas.VENTA_X - 1, vy = t + MenuSubastas.VENTA_Y - 1;
			EstiloCajero.bordeArcoiris(g, vx - 2, vy - 2, 22, 22, tiempo, tiempo + 0.5f);
			g.fill(vx - 1, vy - 1, vx + 19, vy + 19, 0xFF1A1E28);
			g.fill(vx, vy, vx + 18, vy + 18, 0xFF2A2F3B);
			apagar(g, l + MenuSubastas.BOTON_IZQ_X, py);
			apagar(g, l + MenuSubastas.BOTON_DER_X, py);
		} else {
			// Las páginas: los botones ya están dibujados; se apagan si no hay a dónde ir y se iluminan al pasar.
			boton(g, l + MenuSubastas.BOTON_IZQ_X, py, menu.pagina() > 0, mouseX, mouseY);
			boton(g, l + MenuSubastas.BOTON_DER_X, py, menu.pagina() < menu.paginas() - 1, mouseX, mouseY);
			g.drawCenteredString(font, (menu.pagina() + 1) + " / " + menu.paginas(), l + MenuSubastas.PAGINA_TEXTO_X, py + 4, 0xFF2A2F3B);
		}
	}

	private void boton(GuiGraphics g, int x, int y, boolean activo, int mx, int my) {
		if (!activo) apagar(g, x, y);
		else if (mx >= x && mx < x + MenuSubastas.BOTON_ANCHO && my >= y && my < y + MenuSubastas.PAGINAS_ALTO) {
			g.fill(x, y, x + MenuSubastas.BOTON_ANCHO, y + MenuSubastas.PAGINAS_ALTO, 0x40FFFFFF);
		}
	}

	private static void apagar(GuiGraphics g, int x, int y) {
		g.fill(x, y, x + MenuSubastas.BOTON_ANCHO, y + MenuSubastas.PAGINAS_ALTO, 0x90000000);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		renderTooltip(g, mouseX, mouseY);
	}
}
