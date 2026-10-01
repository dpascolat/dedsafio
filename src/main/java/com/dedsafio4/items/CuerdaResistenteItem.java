package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Cuerda Resistente: fibra de alta resistencia y durabilidad extrema, para diferentes creaciones. */
public class CuerdaResistenteItem extends Item {
	/** Los mismos colores que la Tela Primitiva. */
	private static final int CELESTE = 0x55D9F0, GRIS = 0xC6CFD6;

	public CuerdaResistenteItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Fibra de alta resistencia y").withStyle(color(GRIS)));
		texto.add(Component.literal("durabilidad extrema. Muy útil").withStyle(color(GRIS)));
		texto.add(Component.literal("para diferentes creaciones.").withStyle(color(GRIS)));
	}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}
}
