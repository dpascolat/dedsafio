package com.dedsafio4.boveda;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Pantalla de la Bóveda: arriba una fila de 9 casillas donde se ponen deditas físicas (Dedita, Verde
 * y Roja) para depositarlas; abajo el inventario. Lo que quede en las casillas al cerrar vuelve al jugador.
 */
public class BovedaMenu extends AbstractContainerMenu {
	// Posiciones (las usa también la pantalla).
	public static final int ANCHO = 210;
	public static final int SLOTS_X = (ANCHO - 9 * 18) / 2 + 1, CASILLAS_Y = 9;
	public static final int INVENTARIO_Y = 98;

	private final Container casillas = new SimpleContainer(9);
	private final BlockPos pos;

	/** Del lado del cliente. */
	public BovedaMenu(int id, Inventory inventario) {
		this(id, inventario, BlockPos.ZERO);
	}

	public BovedaMenu(int id, Inventory inventario, BlockPos pos) {
		super(ModBoveda.MENU, id);
		this.pos = pos;
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(casillas, col, SLOTS_X + col * 18, CASILLAS_Y) {
				@Override
				public boolean mayPlace(ItemStack pila) {
					return ModBoveda.valor(pila) > 0;
				}
			});
		}
		for (int fila = 0; fila < 3; fila++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventario, col + fila * 9 + 9, SLOTS_X + col * 18, INVENTARIO_Y + fila * 18));
			}
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventario, col, SLOTS_X + col * 18, INVENTARIO_Y + 58));
	}

	/** Posición del bloque principal de la Bóveda. */
	public BlockPos pos() {
		return pos;
	}

	/** Cuántas deditas suman las monedas puestas en las casillas. */
	long valorCasillas() {
		long total = 0;
		for (int i = 0; i < casillas.getContainerSize(); i++) {
			ItemStack pila = casillas.getItem(i);
			total += ModBoveda.valor(pila) * pila.getCount();
		}
		return total;
	}

	void vaciarCasillas() {
		for (int i = 0; i < casillas.getContainerSize(); i++) {
			if (ModBoveda.valor(casillas.getItem(i)) > 0) casillas.setItem(i, ItemStack.EMPTY);
		}
		broadcastChanges();
	}

	@Override
	public ItemStack quickMoveStack(Player jugador, int indice) {
		Slot slot = slots.get(indice);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack pila = slot.getItem();
		ItemStack copia = pila.copy();
		if (indice < 9) {
			if (!moveItemStackTo(pila, 9, slots.size(), true)) return ItemStack.EMPTY;
		} else if (ModBoveda.valor(pila) <= 0 || !moveItemStackTo(pila, 0, 9, false)) {
			return ItemStack.EMPTY;
		}
		if (pila.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return copia;
	}

	@Override
	public boolean stillValid(Player jugador) {
		return jugador.level().getBlockState(pos).is(ModBoveda.BOVEDA)
				&& jugador.distanceToSqr(pos.above().getCenter()) <= 64;
	}

	@Override
	public void removed(Player jugador) {
		super.removed(jugador);
		clearContainer(jugador, casillas);
	}
}
