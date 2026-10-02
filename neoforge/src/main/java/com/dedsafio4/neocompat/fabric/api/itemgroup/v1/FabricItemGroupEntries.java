package com.dedsafio4.neocompat.fabric.api.itemgroup.v1;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.ArrayList;
import java.util.function.Predicate;

/** Lo que el mod usa de las entradas de una pestaña: sacar cosas (de la pestaña o de la búsqueda). */
public final class FabricItemGroupEntries {
	private final BuildCreativeModeTabContentsEvent evento;

	public FabricItemGroupEntries(BuildCreativeModeTabContentsEvent evento) {
		this.evento = evento;
	}

	public interface Pilas {
		boolean removeIf(Predicate<? super ItemStack> condicion);
	}

	public Pilas getDisplayStacks() {
		return condicion -> quitar(evento.getParentEntries(), condicion, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
	}

	public Pilas getSearchTabStacks() {
		return condicion -> quitar(evento.getSearchEntries(), condicion, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
	}

	private boolean quitar(Iterable<ItemStack> pilas, Predicate<? super ItemStack> condicion, CreativeModeTab.TabVisibility donde) {
		boolean alguna = false;
		for (ItemStack pila : new ArrayList<ItemStack>((java.util.Collection<ItemStack>) pilas)) {
			if (condicion.test(pila)) {
				evento.remove(pila, donde);
				alguna = true;
			}
		}
		return alguna;
	}

	public void accept(net.minecraft.world.level.ItemLike item) {
		evento.accept(item);
	}

	public void accept(ItemStack pila) {
		evento.accept(pila);
	}
}
