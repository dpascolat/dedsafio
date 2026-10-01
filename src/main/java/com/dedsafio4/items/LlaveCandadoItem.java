package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Llave de Candado: con click derecho sobre un cofre con candado que es tuyo, le saca el candado.
 * Lo hace {@link com.dedsafio4.candados.Candados} (tiene que pasar antes de que se abra el cofre).
 */
public class LlaveCandadoItem extends Item {
	/** Los mismos colores que la Llave de Cobre. */
	private static final int CELESTE = 0x55C6E8, TEXTO = 0xC6C6C6, NARANJA = 0xF2B35A;

	public LlaveCandadoItem(Properties propiedades) {
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
		texto.add(parte("Cofre Protegido de tu propiedad", TEXTO));
		texto.add(parte("para desprotegerlo.", TEXTO));
	}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	private static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(color(rgb));
	}
}
