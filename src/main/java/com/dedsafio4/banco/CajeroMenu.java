package com.dedsafio4.banco;

import com.dedsafio4.items.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Ingresar dinero en el Cajero: se ponen deditas en la grilla de 3x3 y con "Ingresar" se suman al
 * saldo. Solo sirven la Dedita (1), la Dedita Verde (100) y la Dedita Roja (10.000); las demás
 * (Casino, Mercado Negro, ...) no entran en la grilla. Lo que quede al cerrar vuelve al jugador.
 */
public class CajeroMenu extends AbstractContainerMenu {
	/** Botón "Ingresar" (para clickMenuButton). */
	public static final int BOTON_INGRESAR = 0;

	// Posiciones (las usa también la pantalla).
	public static final int GRILLA_X = 22, GRILLA_Y = 20, CELDA = 22;
	public static final int INVENTARIO_Y = 102;

	private final Container grilla = new SimpleContainer(9);
	private final ContainerLevelAccess acceso;

	/** Del lado del cliente. */
	public CajeroMenu(int id, Inventory inventario) {
		this(id, inventario, ContainerLevelAccess.NULL);
	}

	public CajeroMenu(int id, Inventory inventario, ContainerLevelAccess acceso) {
		super(ModCajero.MENU, id);
		this.acceso = acceso;
		for (int fila = 0; fila < 3; fila++) {
			for (int col = 0; col < 3; col++) {
				addSlot(new Slot(grilla, fila * 3 + col, GRILLA_X + col * CELDA + 2, GRILLA_Y + fila * CELDA + 2) {
					@Override
					public boolean mayPlace(ItemStack pila) {
						return valor(pila) > 0;
					}
				});
			}
		}
		for (int fila = 0; fila < 3; fila++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventario, col + fila * 9 + 9, 8 + col * 18, INVENTARIO_Y + fila * 18));
			}
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventario, col, 8 + col * 18, INVENTARIO_Y + 58));
	}

	/** Cuántas deditas vale una sola de estas monedas (0 si el Cajero no la acepta). */
	public static long valor(ItemStack pila) {
		if (pila.is(ModItems.DEDITA)) return 1;
		if (pila.is(ModItems.DEDITA_VERDE)) return 100;
		if (pila.is(ModItems.DEDITA_ROJA)) return 10_000;
		return 0;
	}

	@Override
	public boolean clickMenuButton(Player jugador, int boton) {
		if (boton != BOTON_INGRESAR) return false;
		if (!(jugador instanceof ServerPlayer servidor)) return true;
		long total = 0;
		for (int i = 0; i < grilla.getContainerSize(); i++) {
			ItemStack pila = grilla.getItem(i);
			total += valor(pila) * pila.getCount();
		}
		if (total <= 0) {
			servidor.level().playSound(null, servidor.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.2f);
			return true;
		}
		for (int i = 0; i < grilla.getContainerSize(); i++) {
			if (valor(grilla.getItem(i)) > 0) grilla.setItem(i, ItemStack.EMPTY);
		}
		Banco.sumar(servidor, total);
		servidor.level().playSound(null, servidor.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.3f);
		broadcastChanges();
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player jugador, int indice) {
		Slot slot = slots.get(indice);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack pila = slot.getItem();
		ItemStack copia = pila.copy();
		if (indice < 9) {
			if (!moveItemStackTo(pila, 9, slots.size(), true)) return ItemStack.EMPTY;
		} else if (valor(pila) <= 0 || !moveItemStackTo(pila, 0, 9, false)) {
			return ItemStack.EMPTY;
		}
		if (pila.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return copia;
	}

	@Override
	public boolean stillValid(Player jugador) {
		return stillValid(acceso, jugador, ModCajero.CAJERO);
	}

	@Override
	public void removed(Player jugador) {
		super.removed(jugador);
		clearContainer(jugador, grilla);
	}
}
