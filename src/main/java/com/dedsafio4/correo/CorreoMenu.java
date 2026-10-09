package com.dedsafio4.correo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * El menú de un mensaje: la grilla de 9×3 del mensaje (como un cofre) y el inventario del jugador.
 * carta == -1: escribiendo uno nuevo; se ponen objetos en la grilla (si se cierra sin mandarlo, vuelven al jugador).
 * Si no: leyendo ese mensaje; de la grilla solo se sacan objetos, y lo que queda se guarda al cerrar.
 */
public class CorreoMenu extends AbstractContainerMenu {
	// Posiciones (las usa también la pantalla).
	public static final int ANCHO = 190, ALTO = 262;
	public static final int GRILLA_X = 14, GRILLA_Y = 102, CELDA_W = 18, CELDA_H = 18, COLUMNAS = 9, FILAS = 3;
	public static final int INVENTARIO_X = 15, INVENTARIO_Y = 180;

	final int carta;
	private final SimpleContainer grilla = new SimpleContainer(Correo.CASILLAS);

	/** Del lado del cliente. */
	public CorreoMenu(int id, Inventory inventario) {
		this(id, inventario, Correo.ABIERTA_CLIENTE, List.of());
	}

	public CorreoMenu(int id, Inventory inventario, int carta, List<ItemStack> objetos) {
		super(Correo.MENU, id);
		this.carta = carta;
		for (int i = 0; i < objetos.size() && i < Correo.CASILLAS; i++) grilla.setItem(i, objetos.get(i).copy());
		boolean leyendo = carta >= 0;
		for (int fila = 0; fila < FILAS; fila++) {
			for (int col = 0; col < COLUMNAS; col++) {
				addSlot(new Slot(grilla, fila * COLUMNAS + col, GRILLA_X + col * CELDA_W + 1, GRILLA_Y + fila * CELDA_H + 1) {
					@Override
					public boolean mayPlace(ItemStack pila) {
						return !leyendo;
					}
				});
			}
		}
		for (int fila = 0; fila < 3; fila++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventario, col + fila * 9 + 9, INVENTARIO_X + col * 18, INVENTARIO_Y + fila * 18));
			}
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventario, col, INVENTARIO_X + col * 18, INVENTARIO_Y + 58));
	}

	public boolean leyendo() {
		return carta >= 0;
	}

	public int carta() {
		return carta;
	}

	/** Los objetos que hay en la grilla (sin los huecos). */
	List<ItemStack> objetos() {
		List<ItemStack> l = new ArrayList<>();
		for (int i = 0; i < grilla.getContainerSize(); i++) if (!grilla.getItem(i).isEmpty()) l.add(grilla.getItem(i).copy());
		return l;
	}

	void vaciar() {
		grilla.clearContent();
	}

	@Override
	public ItemStack quickMoveStack(Player jugador, int indice) {
		Slot slot = slots.get(indice);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack pila = slot.getItem(), antes = pila.copy();
		if (indice < Correo.CASILLAS) {
			if (!moveItemStackTo(pila, Correo.CASILLAS, slots.size(), true)) return ItemStack.EMPTY;
		} else {
			if (leyendo() || !moveItemStackTo(pila, 0, Correo.CASILLAS, false)) return ItemStack.EMPTY;
		}
		if (pila.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return antes;
	}

	@Override
	public boolean stillValid(Player jugador) {
		return true;
	}

	@Override
	public void removed(Player jugador) {
		super.removed(jugador);
		if (!(jugador instanceof ServerPlayer servidor)) return;
		if (leyendo()) {
			Correo.guardarRestantes(servidor, carta, objetos());
		} else {
			// No lo mandó: los objetos vuelven.
			clearContainer(jugador, grilla);
		}
	}
}
