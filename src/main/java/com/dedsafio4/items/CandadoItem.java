package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import static com.dedsafio4.items.DescritoItem.color;
import static com.dedsafio4.items.DescritoItem.parte;

/** Candado: se le pone a un cofre y queda cerrado con un código. */
public class CandadoItem extends Item {
	// Los mismos colores que la Llave de Candado.
	private static final int CELESTE = 0x55C6E8, TEXTO = 0xC6C6C6, NARANJA = 0xF2B35A;

	public CandadoItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(parte("Haz ", TEXTO).append(parte("Click Derecho", NARANJA)).append(parte(" sobre un", TEXTO)));
		texto.add(parte("Cofre e introduce un código", TEXTO));
		texto.add(parte("para protegerlo.", TEXTO));
	}
}
