package com.dedsafio4.crafteo;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;

/**
 * La mesa de /crafteo: la grilla de 3×3, el resultado y el inventario. Las casillas de la grilla y del resultado son
 * "de muestra": copian lo que tienes en el mouse (no se gasta nada) y con la mano vacía se vacían. Al poner un ítem
 * en el resultado con la grilla vacía, aparece el crafteo que tiene ahora.
 */
public class CrafteoMenu extends AbstractContainerMenu {
	// Posiciones (las usa también la pantalla).
	public static final int ANCHO = 176, ALTO = 196;
	public static final int GRILLA_X = 30, GRILLA_Y = 28, RESULTADO_X = 124, RESULTADO_Y = 46;
	public static final int INVENTARIO_X = 8, INVENTARIO_Y = 114;
	public static final int RESULTADO = 9;

	private final SimpleContainer casillas = new SimpleContainer(10);

	public CrafteoMenu(int id, Inventory inventario) {
		super(Crafteos.MENU, id);
		for (int i = 0; i < 9; i++) addSlot(new Muestra(casillas, i, GRILLA_X + i % 3 * 18, GRILLA_Y + i / 3 * 18));
		addSlot(new Muestra(casillas, RESULTADO, RESULTADO_X, RESULTADO_Y));
		for (int fila = 0; fila < 3; fila++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventario, col + fila * 9 + 9, INVENTARIO_X + col * 18, INVENTARIO_Y + fila * 18));
			}
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventario, col, INVENTARIO_X + col * 18, INVENTARIO_Y + 58));
	}

	/** Una casilla de muestra: no se puede sacar ni poner de verdad (lo maneja clicked). */
	static final class Muestra extends Slot {
		Muestra(SimpleContainer c, int i, int x, int y) {
			super(c, i, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack pila) {
			return false;
		}

		@Override
		public boolean mayPickup(Player jugador) {
			return false;
		}
	}

	ItemStack casilla(int i) {
		return casillas.getItem(i);
	}

	ItemStack resultado() {
		return casillas.getItem(RESULTADO);
	}

	@Override
	public void clicked(int indice, int boton, ClickType tipo, Player jugador) {
		if (indice < 0 || indice > RESULTADO) {
			super.clicked(indice, boton, tipo, jugador);
			return;
		}
		if (tipo != ClickType.PICKUP && tipo != ClickType.QUICK_MOVE) return;
		ItemStack mano = getCarried();
		if (mano.isEmpty()) {
			// Mano vacía: click vacía la casilla; en el resultado, click derecho le suma uno a la cantidad.
			if (indice == RESULTADO && boton == 1 && !resultado().isEmpty()) {
				ItemStack r = resultado().copy();
				r.setCount(Math.min(r.getMaxStackSize(), r.getCount() + 1));
				casillas.setItem(RESULTADO, r);
			} else {
				casillas.setItem(indice, ItemStack.EMPTY);
			}
			return;
		}
		if (indice == RESULTADO) {
			casillas.setItem(RESULTADO, mano.copyWithCount(boton == 1 ? 1 : mano.getCount()));
			if (grillaVacia()) mostrarActual(jugador, mano);
		} else {
			casillas.setItem(indice, mano.copyWithCount(1));
		}
	}

	private boolean grillaVacia() {
		for (int i = 0; i < 9; i++) if (!casillas.getItem(i).isEmpty()) return false;
		return true;
	}

	/** Pone en la grilla el crafteo que tiene ahora ese ítem (si tiene). */
	private void mostrarActual(Player jugador, ItemStack item) {
		var registros = jugador.level().registryAccess();
		for (RecipeHolder<CraftingRecipe> r : jugador.level().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
			if (!r.value().getResultItem(registros).is(item.getItem())) continue;
			List<Ingredient> ingredientes = r.value().getIngredients();
			if (ingredientes.isEmpty()) continue;
			int ancho = r.value() instanceof ShapedRecipe s ? s.getWidth() : 3;
			for (int i = 0; i < ingredientes.size() && i < 9; i++) {
				int casilla = r.value() instanceof ShapedRecipe ? (i / ancho) * 3 + i % ancho : i;
				ItemStack[] opciones = ingredientes.get(i).getItems();
				if (opciones.length > 0 && casilla < 9) casillas.setItem(casilla, opciones[0].copyWithCount(1));
			}
			return;
		}
	}

	@Override
	public ItemStack quickMoveStack(Player jugador, int indice) {
		// Shift-click en el inventario: copia el ítem en la primera casilla vacía de la grilla.
		if (indice > RESULTADO) {
			ItemStack pila = slots.get(indice).getItem();
			if (pila.isEmpty()) return ItemStack.EMPTY;
			for (int i = 0; i < 9; i++) {
				if (casillas.getItem(i).isEmpty()) {
					casillas.setItem(i, pila.copyWithCount(1));
					break;
				}
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player jugador) {
		return true;
	}
}
