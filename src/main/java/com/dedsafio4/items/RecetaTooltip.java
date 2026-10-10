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
	/** Los ítems que muestran su receta en la descripción. */
	public static Set<Item> conReceta() {
		return Set.of(Items.TRIDENT, ModItems.TENEDOR_SIMPLE, ModItems.CORAZON_DORADO, ModItems.ESPADA_GLEBANOIDE);
	}
}
