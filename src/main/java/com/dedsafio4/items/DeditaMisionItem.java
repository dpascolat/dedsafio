package com.dedsafio4.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Dedita de Misión. La que da la Misión Principal lleva el día guardado: se llama "Dedita de la Misión (Día N)" y dice
 * dónde se canjea (el Pedestal de la Montaña, el bloque de Entrega de Misiones).
 */
public class DeditaMisionItem extends Item {
	public DeditaMisionItem(Properties propiedades) {
		super(propiedades);
	}

	/** El día de la Misión Principal que la dio (-1 si no tiene). */
	private static int dia(ItemStack pila) {
		CustomData datos = pila.get(DataComponents.CUSTOM_DATA);
		return datos == null || !datos.copyTag().contains("dia") ? -1 : datos.copyTag().getInt("dia");
	}

	@Override
	public Component getName(ItemStack pila) {
		int dia = dia(pila);
		if (dia < 0) return Component.translatable(getDescriptionId()).withColor(0x55D9F0);
		return Component.literal("Dedita de la Misión").withColor(0x55D9F0)
				.append(Component.literal(" (Día " + dia + ")").withColor(0xB79CFF));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		if (dia(pila) < 0) return;
		int blanco = 0xE8E8E8;
		texto.add(Component.empty());
		texto.add(Component.literal("Canjéalo en el ").withColor(blanco).append(Component.literal("Pedestal de").withColor(0xC3E84E)));
		texto.add(Component.literal("la Montaña").withColor(0xC3E84E).append(Component.literal(" para reclamar").withColor(blanco)));
		texto.add(Component.literal("la ").withColor(blanco).append(Component.literal("Misión Principal").withColor(0xF0D86A))
				.append(Component.literal(" del día.").withColor(blanco)));
	}
}
