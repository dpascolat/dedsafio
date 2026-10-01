package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

/** Una semilla que planta un bloque, con el nombre en celeste y su descripción (como {@link DescritoItem}). */
public class SemillaDescritaItem extends ItemNameBlockItem {
	private final Consumer<List<Component>> descripcion;

	public SemillaDescritaItem(Block bloque, Properties propiedades, Consumer<List<Component>> descripcion) {
		super(bloque, propiedades);
		this.descripcion = descripcion;
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(DescritoItem.color(DescritoItem.CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		descripcion.accept(texto);
	}
}
