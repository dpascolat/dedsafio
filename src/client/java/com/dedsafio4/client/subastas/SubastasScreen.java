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
 * La casa de subastas (/ah): el marco metálico con luces del Cajero, la grilla de 9×5 con bordes de arcoíris,
 * «« página »» abajo, tu inventario y, a la derecha, las pestañas Comprar / Vender / Mis ventas / Recuperar.
 * En Vender: la casilla para poner lo que vendes, el precio y el botón para ponerlo a la venta.
 */
public class SubastasScreen extends AbstractContainerScreen<MenuSubastas> {
	private static final String[] PESTANAS = {"Comprar", "Vender", "Mis ventas", "Recuperar"};
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
		precio.setHint(Component.literal("precio en deditas").withColor(0xFF5A6070));
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
		if (menu.pestana() != MenuSubastas.VENDER && my >= y && my < y + 16) {
			int izq = leftPos + MenuSubastas.GRILLA_X, der = izq + MenuSubastas.COLUMNAS * 18;
			if (mx >= izq && mx < izq + 40 && menu.pagina() > 0) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MenuSubastas.ANTERIOR);
				return true;
			}
			if (mx >= der - 40 && mx < der && menu.pagina() < menu.paginas() - 1) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MenuSubastas.SIGUIENTE);
				return true;
			}
		}
		return super.mouseClicked(mx, my, boton);
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
		EstiloCajero.marco(g, font, l, t, imageWidth, imageHeight, "Subastas");
		float tiempo = EstiloCajero.tiempo();
		int gx = l + MenuSubastas.GRILLA_X, gy = t + MenuSubastas.GRILLA_Y;
		int anchoGrilla = MenuSubastas.COLUMNAS * 18, altoGrilla = MenuSubastas.FILAS * 18;
		boolean vender = menu.pestana() == MenuSubastas.VENDER;
		// La grilla (o, en Vender, la casilla sola).
		EstiloCajero.bordeArcoiris(g, gx - 2, gy - 2, anchoGrilla + 4, altoGrilla + 4, tiempo, tiempo + 0.9f);
		g.fill(gx - 1, gy - 1, gx + anchoGrilla + 1, gy + altoGrilla + 1, 0xFF0B0D13);
		if (vender) {
			int vx = l + MenuSubastas.VENTA_X - 1, vy = t + MenuSubastas.VENTA_Y - 1;
			g.drawCenteredString(font, "Pon acá lo que quieres vender", gx + anchoGrilla / 2, gy + 5, 0xFFB8C0CC);
			EstiloCajero.bordeArcoiris(g, vx - 2, vy - 2, 22, 22, tiempo, tiempo + 0.5f);
			g.fill(vx - 1, vy - 1, vx + 19, vy + 19, 0xFF1A1E28);
			g.fill(vx, vy, vx + 18, vy + 18, 0xFF2A2F3B);
		} else {
			for (int fila = 0; fila < MenuSubastas.FILAS; fila++) {
				for (int col = 0; col < MenuSubastas.COLUMNAS; col++) {
					int x = gx + col * 18, y = gy + fila * 18;
					g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1C2130);
					g.fill(x + 1, y + 1, x + 17, y + 2, 0xFF262C3D);
				}
			}
			// Las páginas: «« 1 / N »».
			int py = t + MenuSubastas.PAGINAS_Y;
			boton(g, gx, py, 40, "<<<", menu.pagina() > 0, mouseX, mouseY);
			boton(g, gx + anchoGrilla - 40, py, 40, ">>>", menu.pagina() < menu.paginas() - 1, mouseX, mouseY);
			g.drawCenteredString(font, (menu.pagina() + 1) + " / " + menu.paginas(), gx + anchoGrilla / 2, py + 4, 0xFFFFFFFF);
		}
		// El inventario.
		int ix = l + MenuSubastas.INVENTARIO_X - 1, iy = t + MenuSubastas.INVENTARIO_Y - 1;
		for (int fila = 0; fila < 4; fila++) {
			int y = iy + fila * 18 + (fila == 3 ? 4 : 0);
			for (int col = 0; col < 9; col++) {
				int x = ix + col * 18;
				g.fill(x, y, x + 18, y + 18, 0xFF0B0D13);
				g.fill(x + 1, y + 1, x + 17, y + 17, fila == 3 ? 0xFF2A2F3B : 0xFF1C2130);
			}
		}
		EstiloCajero.bordeArcoiris(g, ix - 1, iy + 3 * 18 + 3, 9 * 18 + 2, 20, tiempo + 0.3f, tiempo + 1.2f);
	}

	/** Un botón de página: cartelito oscuro con borde de arcoíris (apagado si no hay a dónde ir). */
	private void boton(GuiGraphics g, int x, int y, int ancho, String texto, boolean activo, int mx, int my) {
		boolean encima = activo && mx >= x && mx < x + ancho && my >= y && my < y + 16;
		float tiempo = EstiloCajero.tiempo();
		if (activo) EstiloCajero.bordeArcoiris(g, x, y, ancho, 16, tiempo, tiempo + 0.4f);
		else g.fill(x, y, x + ancho, y + 16, 0xFF3A3F4A);
		g.fill(x + 1, y + 1, x + ancho - 1, y + 15, encima ? 0xFF2C3446 : 0xFF151923);
		g.drawCenteredString(font, texto, x + ancho / 2, y + 4, activo ? 0xFFFFFFFF : 0xFF5A6070);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		renderTooltip(g, mouseX, mouseY);
	}
}
