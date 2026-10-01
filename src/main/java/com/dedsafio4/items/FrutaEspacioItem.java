package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ChorusFruitItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Fruta del Espacio entre Dimensiones: al comerla te teletransporta a un lugar cercano al azar,
 * igual que la fruta coral (de ahí hereda el salto).
 */
public class FrutaEspacioItem extends ChorusFruitItem {
	private static final int VIOLETA = 0xC77DF0, GRIS = 0xC6C6C6;

	public FrutaEspacioItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(VIOLETA));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Al consumirla, te teletransportará").withStyle(color(GRIS)));
		texto.add(Component.literal("a una posición aleatoria cercana.").withStyle(color(GRIS)));
		texto.add(Component.empty());
		texto.add(Component.literal("Puedes conseguirla en el Espacio").withStyle(color(GRIS)));
		texto.add(Component.literal("entre Dimensiones o dentro de los").withStyle(color(GRIS)));
		texto.add(Component.literal("Cubos Phora.").withStyle(color(GRIS)));
	}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}
}
