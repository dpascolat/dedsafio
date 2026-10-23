package com.dedsafio4.gancho;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** El Gancho de Huesos como ítem: lleva su impulso (lo da /ganchito <distancia>; sin nada, 10 bloques). */
public class GanchoItem extends BlockItem {
	public static final int DISTANCIA_NORMAL = 10;

	public GanchoItem(Block bloque, Properties propiedades) {
		super(bloque, propiedades);
	}

	public static int distancia(ItemStack pila) {
		CompoundTag tag = pila.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.contains("distancia") ? tag.getInt("distancia") : DISTANCIA_NORMAL;
	}

	public static ItemStack con(int distancia) {
		ItemStack pila = new ItemStack(ModGancho.ITEM);
		CompoundTag tag = new CompoundTag();
		tag.putInt("distancia", distancia);
		pila.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return pila;
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Impulso: " + distancia(pila) + " bloques").withColor(0xE4DBBB));
		texto.add(Component.literal("Ponlo en el techo y engánchalo").withColor(0xC6CFD6));
		texto.add(Component.literal("con una caña de pescar.").withColor(0xC6CFD6));
	}
}
