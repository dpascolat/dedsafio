package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import static com.dedsafio4.items.DescritoItem.parte;

/** Bioxita (antes Cristal Verde): sale del Mineral de Bioxita (con Pico de Diamante o mejor). */
public class BioxitaItem extends Item {
	private static final int VERDE = 0x4AA852, DORADO = 0xFFAA00;

	public BioxitaItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(parte("Utiliza un ", DescritoItem.GRIS).append(parte("Pico de Diamante", DORADO)));
		texto.add(parte("o superior sobre un ", DescritoItem.GRIS).append(parte("Mineral", VERDE)));
		texto.add(parte("de Bioxita", VERDE).append(parte(" para extraerla.", DescritoItem.GRIS)));
	}
}
