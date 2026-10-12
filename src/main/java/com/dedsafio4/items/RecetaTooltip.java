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

	/**
	 * ¿Muestra la receta? Todos los ítems (los del mod y los de Minecraft), sólo en la G. Si no tienen receta de mesa
	 * de crafteo, de horno ni de mesa de herrería, no se dibuja nada.
	 */
	public static boolean muestra(Item item) {
		return enCatalogo.getAsBoolean();
	}
}
