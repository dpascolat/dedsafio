package com.dedsafio4.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/**
 * Las espadas que vienen con Perdición (la Glebanoide y la del Limbo) se encantan solas si les falta. La que pasó por
 * la afiladora queda marcada para que eso no vuelva a pasar: el jugador la desencantó a propósito.
 */
public final class PerdicionEspada {
	private static final String MARCA = "dedsafio4_desencantada";

	private PerdicionEspada() {
	}

	public static void ponerSiFalta(ItemStack pila, Level mundo, ResourceKey<Enchantment> clave) {
		if (pila.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(MARCA)) return;
		mundo.registryAccess().registry(Registries.ENCHANTMENT).flatMap(r -> r.getHolder(clave)).ifPresent(perdicion -> {
			if (pila.getEnchantments().getLevel(perdicion) < 1) pila.enchant(perdicion, 1);
		});
	}

	public static void marcarDesencantada(ItemStack pila) {
		if (pila.getItem() instanceof EspadaGlebanoideItem || pila.getItem() instanceof EspadaLimboItem) {
			CustomData.update(DataComponents.CUSTOM_DATA, pila, datos -> datos.putBoolean(MARCA, true));
		}
	}
}
