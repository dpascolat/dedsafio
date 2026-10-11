package com.dedsafio4.subastas;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * El menú del /ah (la pantalla la dibuja SubastasScreen). Arriba, una grilla de 9×5 que muestra según la pestaña:
 *   0 Comprar     todas las ventas (clic: comprar; si es tuya, la sacas de la venta y va a Recuperar)
 *   1 Vender      una casilla donde pones lo que quieres vender (el precio se escribe en la pantalla)
 *   2 Mis ventas  lo que tienes a la venta (clic: lo sacas y va a Recuperar)
 *   3 Recuperar   lo que sacaste de la venta (clic: vuelve a tu inventario)
 * Con páginas («« 1 / N »»). Abajo, tu inventario. La grilla no es de nadie: nada se puede sacar ni poner.
 */
public class MenuSubastas extends AbstractContainerMenu {
	// Posiciones (las usa también la pantalla).
	public static final int ANCHO = 190, ALTO = 236;
	public static final int GRILLA_X = 14, GRILLA_Y = 24, COLUMNAS = 9, FILAS = 5, POR_PAGINA = COLUMNAS * FILAS;
	public static final int PAGINAS_Y = GRILLA_Y + FILAS * 18 + 4;
	public static final int INVENTARIO_X = 15, INVENTARIO_Y = 152;
	/** La casilla para vender (en la pestaña Vender), al medio de la grilla. */
	public static final int VENTA_X = GRILLA_X + 4 * 18 + 1, VENTA_Y = GRILLA_Y + 18 + 1;
	public static final int COMPRAR = 0, VENDER = 1, MIS_VENTAS = 2, RECUPERAR = 3;
	/** Los botones (clickMenuButton): 0..3 las pestañas; estos, las páginas. */
	public static final int ANTERIOR = 10, SIGUIENTE = 11;

	private final Player jugador;
	private final SimpleContainer vidriera = new SimpleContainer(POR_PAGINA);
	private final SimpleContainer venta = new SimpleContainer(1);
	/** pestaña, página y cuántas páginas hay (se mandan solos a la pantalla). */
	private final ContainerData datos;
	/** Qué venta hay en cada casillero de esta página (en Recuperar: el número de objeto guardado). */
	private final UUID[] idPorCasillero = new UUID[POR_PAGINA];
	private final int[] indicePorCasillero = new int[POR_PAGINA];

	/** Del lado del cliente. */
	public MenuSubastas(int id, Inventory inventario) {
		this(id, inventario, new SimpleContainerData(3));
	}

	private MenuSubastas(int id, Inventory inventario, ContainerData datos) {
		super(Subastas.MENU, id);
		this.jugador = inventario.player;
		this.datos = datos;
		for (int fila = 0; fila < FILAS; fila++) {
			for (int col = 0; col < COLUMNAS; col++) {
				addSlot(new Slot(vidriera, fila * COLUMNAS + col, GRILLA_X + col * 18 + 1, GRILLA_Y + fila * 18 + 1) {
					@Override
					public boolean mayPlace(ItemStack pila) {
						return false;
					}

					@Override
					public boolean mayPickup(Player quien) {
						return false;
					}

					@Override
					public boolean isActive() {
						return pestana() != VENDER;
					}
				});
			}
		}
		addSlot(new Slot(venta, 0, VENTA_X, VENTA_Y) {
			@Override
			public boolean isActive() {
				return pestana() == VENDER;
			}
		});
		for (int fila = 0; fila < 3; fila++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventario, col + fila * 9 + 9, INVENTARIO_X + col * 18, INVENTARIO_Y + fila * 18));
			}
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventario, col, INVENTARIO_X + col * 18, INVENTARIO_Y + 58));
		addDataSlots(datos);
	}

	/** Del lado del servidor. */
	public static MenuSubastas abrir(int id, Inventory inventario) {
		MenuSubastas menu = new MenuSubastas(id, inventario, new SimpleContainerData(3));
		menu.armar();
		return menu;
	}

	public int pestana() {
		return datos.get(0);
	}

	public int pagina() {
		return datos.get(1);
	}

	public int paginas() {
		return Math.max(1, datos.get(2));
	}

	/** Llena la grilla según la pestaña y la página (en el servidor). */
	void armar() {
		if (!(jugador instanceof ServerPlayer sp)) return;
		vidriera.clearContent();
		java.util.Arrays.fill(idPorCasillero, null);
		java.util.Arrays.fill(indicePorCasillero, -1);
		SubastasData data = Subastas.data(sp.server);
		List<ItemStack> mostrar = new ArrayList<>();
		List<UUID> ids = new ArrayList<>();
		switch (pestana()) {
			case COMPRAR, MIS_VENTAS -> {
				for (SubastasData.Venta v : data.ventas()) {
					if (pestana() == MIS_VENTAS && !v.vendedor().equals(sp.getUUID())) continue;
					mostrar.add(mostrar(v, sp));
					ids.add(v.id());
				}
			}
			case RECUPERAR -> {
				for (ItemStack o : data.recuperar(sp.getUUID())) {
					ItemStack muestra = o.copy();
					agregarLineas(muestra, List.of(Component.literal("Clic para recuperarlo").withStyle(s -> s.withColor(ChatFormatting.GREEN).withItalic(false))));
					mostrar.add(muestra);
					ids.add(null);
				}
			}
			default -> {
			}
		}
		int paginas = Math.max(1, (mostrar.size() + POR_PAGINA - 1) / POR_PAGINA);
		int pagina = Math.max(0, Math.min(datos.get(1), paginas - 1));
		for (int i = 0; i < POR_PAGINA; i++) {
			int indice = pagina * POR_PAGINA + i;
			if (indice >= mostrar.size()) break;
			vidriera.setItem(i, mostrar.get(indice));
			idPorCasillero[i] = ids.get(indice);
			indicePorCasillero[i] = indice;
		}
		datos.set(1, pagina);
		datos.set(2, paginas);
		broadcastChanges();
	}

	/** La venta como se ve: el ítem con el precio, quién lo vende y qué pasa al tocarlo. */
	private static ItemStack mostrar(SubastasData.Venta v, ServerPlayer quien) {
		ItemStack muestra = v.item().copy();
		boolean mia = v.vendedor().equals(quien.getUUID());
		agregarLineas(muestra, List.of(
				Component.empty(),
				Component.literal("Precio: " + v.precio() + " deditas").withStyle(s -> s.withColor(ChatFormatting.GOLD).withItalic(false)),
				Component.literal("Vende: " + v.nombreVendedor()).withStyle(s -> s.withColor(ChatFormatting.GRAY).withItalic(false)),
				mia ? Component.literal("Clic para sacarlo de la venta").withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false))
						: Component.literal("Clic para comprar").withStyle(s -> s.withColor(ChatFormatting.GREEN).withItalic(false))));
		return muestra;
	}

	private static void agregarLineas(ItemStack pila, List<Component> extra) {
		List<Component> lineas = new ArrayList<>();
		ItemLore antes = pila.get(DataComponents.LORE);
		if (antes != null) lineas.addAll(antes.lines());
		lineas.addAll(extra);
		pila.set(DataComponents.LORE, new ItemLore(lineas));
	}

	@Override
	public boolean clickMenuButton(Player quien, int boton) {
		if (boton >= COMPRAR && boton <= RECUPERAR) {
			if (pestana() == VENDER && boton != VENDER) devolverVenta();
			datos.set(0, boton);
			datos.set(1, 0);
		} else if (boton == ANTERIOR) {
			datos.set(1, Math.max(0, pagina() - 1));
		} else if (boton == SIGUIENTE) {
			datos.set(1, pagina() + 1);
		} else {
			return false;
		}
		armar();
		return true;
	}

	@Override
	public void clicked(int casillero, int boton, ClickType tipo, Player quien) {
		if (casillero >= 0 && casillero < POR_PAGINA) {
			// La grilla es solo para mirar: cada clic es un botón (lo hace el servidor).
			if (quien instanceof ServerPlayer sp && tipo == ClickType.PICKUP && pestana() != VENDER) {
				if (pestana() == RECUPERAR) {
					if (indicePorCasillero[casillero] >= 0) Subastas.recuperar(sp, indicePorCasillero[casillero]);
				} else if (idPorCasillero[casillero] != null) {
					Subastas.tocar(sp, idPorCasillero[casillero]);
				}
				armar();
				sendAllDataToRemote();
			}
			return;
		}
		super.clicked(casillero, boton, tipo, quien);
	}

	/** Lo que está en la casilla para vender, a la venta por ese precio (en el servidor). */
	void venderDesdeCasilla(long precio) {
		if (!(jugador instanceof ServerPlayer sp) || pestana() != VENDER) return;
		ItemStack item = venta.getItem(0);
		if (item.isEmpty()) {
			sp.displayClientMessage(Component.literal("Pon en la casilla lo que quieres vender.").withStyle(ChatFormatting.RED), true);
			return;
		}
		if (Subastas.vender(sp, item.copy(), precio)) {
			venta.setItem(0, ItemStack.EMPTY);
			datos.set(0, MIS_VENTAS);
			datos.set(1, 0);
			armar();
		}
		sendAllDataToRemote();
	}

	/** Lo que quedó en la casilla para vender vuelve al inventario. */
	private void devolverVenta() {
		if (!venta.getItem(0).isEmpty()) clearContainer(jugador, venta);
	}

	@Override
	public ItemStack quickMoveStack(Player quien, int indice) {
		Slot slot = slots.get(indice);
		if (indice < POR_PAGINA || !slot.hasItem()) return ItemStack.EMPTY;
		ItemStack pila = slot.getItem(), antes = pila.copy();
		int casillaVenta = POR_PAGINA, inicioInventario = POR_PAGINA + 1;
		if (indice == casillaVenta) {
			if (!moveItemStackTo(pila, inicioInventario, slots.size(), true)) return ItemStack.EMPTY;
		} else {
			if (pestana() != VENDER || !moveItemStackTo(pila, casillaVenta, casillaVenta + 1, false)) return ItemStack.EMPTY;
		}
		if (pila.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return antes;
	}

	@Override
	public boolean canDragTo(Slot casillero) {
		return casillero.container != vidriera;
	}

	@Override
	public boolean stillValid(Player quien) {
		return true;
	}

	@Override
	public void removed(Player quien) {
		super.removed(quien);
		vidriera.clearContent();   // lo que se ve no es de nadie: que no se devuelva al cerrar
		if (quien instanceof ServerPlayer) clearContainer(quien, venta);
	}
}
