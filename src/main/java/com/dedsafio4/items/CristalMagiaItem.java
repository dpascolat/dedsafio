package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Los Cristales de Magia (las gemas de Patricio): cada uno con el nombre de su color y la misma descripción, como en
 * sus imágenes. Todavía no tienen función.
 */
public class CristalMagiaItem extends Item {
	private final int color;

	public CristalMagiaItem(Properties propiedades, int color) {
		super(propiedades);
		this.color = color;
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(DescritoItem.color(color));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(DescritoItem.parte("Tipo de Magia de la Ruleta.", 0xE8E8E8));
		texto.add(DescritoItem.parte("Puede hallarse en los Cubos", 0xE8E8E8));
		texto.add(DescritoItem.parte("Phora del Espacio entre Dimensiones.", 0xE8E8E8));
	}
}
