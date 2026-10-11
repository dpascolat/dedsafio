package com.dedsafio4.items;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * La receta de un ítem dibujada en su descripción, abajo del nombre: la grilla de 3×3 del crafteo con borde de
 * arcoíris (la dibuja el cliente, RecetaTooltipCliente, con la receta real de Minecraft).
 */
public record RecetaTooltip(Item item) implements TooltipComponent {
	/** Ítems de Minecraft que también muestran su receta (los del mod la muestran todos, si tienen). */
	public static Set<Item> conReceta() {
		return Set.of(Items.TRIDENT);
	}

	/** ¿Está abierto el Catálogo (la G)? Lo pone el cliente; la receta sólo se muestra ahí, no en el inventario. */
	public static java.util.function.BooleanSupplier enCatalogo = () -> false;

	/** ¿Muestra la receta? Todos los ítems del mod (si no tienen receta de mesa de crafteo, no se dibuja nada), sólo en la G. */
	public static boolean muestra(Item item) {
		if (!enCatalogo.getAsBoolean()) return false;
		return conReceta().contains(item)
				|| net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("dedsafio4");
	}
}
