package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Chip Phora: sirve para crear Cofres Protegidos y se encuentra en los Cubos Phora. */
public class ChipPhoraItem extends Item {
	/** Celeste del nombre y gris azulado de la descripción, como en la imagen. */
	private static final int CELESTE = 0x55C6E8, DESCRIPCION = 0xC6CFD6;

	public ChipPhoraItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId(pila))
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(CELESTE)));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		Style estilo = Style.EMPTY.withColor(TextColor.fromRgb(DESCRIPCION));
		texto.add(Component.literal("Utilízalo para crear").withStyle(estilo));
		texto.add(Component.literal("Cofres Protegidos.").withStyle(estilo));
		texto.add(Component.empty());
		texto.add(Component.literal("Se encuentra en los").withStyle(estilo));
		texto.add(Component.literal("Cubos Phora.").withStyle(estilo));
	}
}
