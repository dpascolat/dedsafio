package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Pelos de Eburia: salen de cortar con Tijeras un Huevo Eburia que tenés en la mano secundaria. */
public class PeloEburiaItem extends Item {
	/** Los mismos colores que el Huevo Eburia. */
	private static final int CELESTE = 0x55D9F0, GRIS = 0xC6CFD6, NARANJA = 0xFFA23C;

	public PeloEburiaItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(parte("Utiliza unas ", GRIS).append(parte("Tijeras", NARANJA)).append(parte(" mientras", GRIS)));
		texto.add(parte("tienes el ", GRIS).append(parte("Huevo Eburia", CELESTE)).append(parte(" en la ", GRIS))
				.append(parte("Mano", NARANJA)));
		texto.add(parte("Secundaria", NARANJA).append(parte(" para conseguirlos.", GRIS)));
	}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	private static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(color(rgb));
	}
}
