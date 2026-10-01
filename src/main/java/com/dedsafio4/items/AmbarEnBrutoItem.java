package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Ámbar en Bruto: lo que sale al picar el Mineral de Ámbar. Cocinándolo se obtiene Ámbar. */
public class AmbarEnBrutoItem extends Item {
	private static final int DORADO = 0xF5C542, GRIS = 0xC6CFD6, NARANJA = 0xFFA23C;

	public AmbarEnBrutoItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId())
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(DORADO)));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		Style gris = Style.EMPTY.withColor(TextColor.fromRgb(GRIS));
		Style naranja = Style.EMPTY.withColor(TextColor.fromRgb(NARANJA));
		Style dorado = Style.EMPTY.withColor(TextColor.fromRgb(DORADO));
		texto.add(Component.empty());
		texto.add(Component.literal("Utiliza un ").withStyle(gris)
				.append(Component.literal("Pico de Diamante").withStyle(naranja)));
		texto.add(Component.literal("o superior sobre un ").withStyle(gris)
				.append(Component.literal("Mineral").withStyle(naranja)));
		texto.add(Component.literal("de Ámbar").withStyle(naranja)
				.append(Component.literal(" para extraerlo.").withStyle(gris)));
		texto.add(Component.empty());
		texto.add(Component.literal("Cocínalo para obtener ").withStyle(gris)
				.append(Component.literal("Ámbar").withStyle(dorado))
				.append(Component.literal(".").withStyle(gris)));
	}
}
