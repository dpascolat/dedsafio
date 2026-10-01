package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Tela Primitiva: un tejido de extrema resistencia hecho a partir de los Huevos Eburia. */
public class TelaPrimitivaItem extends Item {
	/** Los mismos colores que el Huevo Eburia. */
	private static final int CELESTE = 0x55D9F0, GRIS = 0xC6CFD6;

	public TelaPrimitivaItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(parte("Tejido de extrema resistencia,", GRIS));
		texto.add(parte("obtenido a partir de los ", GRIS).append(parte("Huevos", CELESTE)));
		texto.add(parte("Eburia", CELESTE).append(parte(".", GRIS)));
	}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	private static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(color(rgb));
	}
}
