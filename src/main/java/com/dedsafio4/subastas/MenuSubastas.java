package com.dedsafio4.subastas;

import com.dedsafio4.banco.Banco;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * El menú del /ah: un cofre grande de 6 filas. Las 5 de arriba muestran las ventas (45 por página); la de abajo tiene
 * página anterior, el saldo y página siguiente. Nada se puede sacar del menú: tocar una venta la compra (o la saca de
 * la venta si es tuya).
 */
public class MenuSubastas extends ChestMenu {
	private static final int POR_PAGINA = 45, ANTERIOR = 45, SALDO = 49, SIGUIENTE = 53;

	private final ServerPlayer jugador;
	private final SimpleContainer cofre;
	private int pagina;
	/** Qué venta hay en cada casillero de esta página. */
	private final UUID[] idPorCasillero = new UUID[POR_PAGINA];

	public MenuSubastas(int id, Inventory inventario, ServerPlayer jugador, int pagina) {
		this(id, inventario, jugador, pagina, new SimpleContainer(54));
	}

	private MenuSubastas(int id, Inventory inventario, ServerPlayer jugador, int pagina, SimpleContainer cofre) {
		super(MenuType.GENERIC_9x6, id, inventario, cofre, 6);
		this.jugador = jugador;
		this.cofre = cofre;
		this.pagina = pagina;
		armar();
	}

	private void armar() {
		cofre.clearContent();
		java.util.Arrays.fill(idPorCasillero, null);
		List<SubastasData.Venta> ventas = Subastas.data(jugador.server).ventas();
		int paginas = Math.max(1, (ventas.size() + POR_PAGINA - 1) / POR_PAGINA);
		pagina = Math.max(0, Math.min(pagina, paginas - 1));
		for (int i = 0; i < POR_PAGINA; i++) {
			int indice = pagina * POR_PAGINA + i;
			if (indice >= ventas.size()) break;
			SubastasData.Venta v = ventas.get(indice);
			idPorCasillero[i] = v.id();
			cofre.setItem(i, mostrar(v));
		}
		ItemStack vidrio = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		vidrio.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		for (int i = POR_PAGINA; i < 54; i++) cofre.setItem(i, vidrio.copy());
		if (pagina > 0) cofre.setItem(ANTERIOR, boton(Items.ARROW, "← Página anterior"));
		if (pagina < paginas - 1) cofre.setItem(SIGUIENTE, boton(Items.ARROW, "Página siguiente →"));
		ItemStack saldo = boton(Items.GOLD_NUGGET, "Tus deditas: " + Banco.saldo(jugador));
		saldo.set(DataComponents.LORE, new ItemLore(List.of(
				gris("Página " + (pagina + 1) + " de " + paginas),
				gris("Para vender: /ah vender <precio>"),
				gris("(vende lo que tenés en la mano)"))));
		cofre.setItem(SALDO, saldo);
		broadcastChanges();
	}

	/** La venta como se ve en el menú: el ítem con el precio y el vendedor abajo. */
	private ItemStack mostrar(SubastasData.Venta v) {
		ItemStack muestra = v.item().copy();
		List<Component> lineas = new ArrayList<>();
		ItemLore anterior = muestra.get(DataComponents.LORE);
		if (anterior != null) lineas.addAll(anterior.lines());
		lineas.add(Component.empty());
		lineas.add(Component.literal("Precio: " + v.precio() + " deditas").withStyle(s -> s.withColor(ChatFormatting.GOLD).withItalic(false)));
		lineas.add(gris("Vende: " + v.nombreVendedor()));
		lineas.add(v.vendedor().equals(jugador.getUUID())
				? Component.literal("Clic para sacarlo de la venta").withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false))
				: Component.literal("Clic para comprar").withStyle(s -> s.withColor(ChatFormatting.GREEN).withItalic(false)));
		muestra.set(DataComponents.LORE, new ItemLore(lineas));
		return muestra;
	}

	private static ItemStack boton(net.minecraft.world.item.Item item, String nombre) {
		ItemStack pila = new ItemStack(item);
		pila.set(DataComponents.CUSTOM_NAME, Component.literal(nombre).withStyle(s -> s.withColor(ChatFormatting.WHITE).withItalic(false)));
		return pila;
	}

	private static Component gris(String texto) {
		return Component.literal(texto).withStyle(s -> s.withColor(ChatFormatting.GRAY).withItalic(false));
	}

	/** Nada se mueve: cada clic es un botón. */
	@Override
	public void clicked(int casillero, int boton, ClickType tipo, Player quien) {
		if (casillero >= 0 && casillero < POR_PAGINA && idPorCasillero[casillero] != null && tipo == ClickType.PICKUP) {
			Subastas.tocar(jugador, idPorCasillero[casillero]);
			armar();
		} else if (casillero == ANTERIOR && pagina > 0) {
			pagina--;
			armar();
		} else if (casillero == SIGUIENTE) {
			pagina++;
			armar();
		}
		setCarried(ItemStack.EMPTY);
		sendAllDataToRemote();
	}

	@Override
	public ItemStack quickMoveStack(Player quien, int casillero) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canDragTo(net.minecraft.world.inventory.Slot casillero) {
		return false;
	}

	@Override
	public void removed(Player quien) {
		super.removed(quien);
		cofre.clearContent();   // lo que se ve no es de nadie: que no se devuelva al cerrar
	}
}
