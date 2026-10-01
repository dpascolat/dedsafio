package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Semilla de Huevo Eburia: se planta en los troncos (como los granos de cacao) y da el Huevo Eburia. */
public class SemillaEburiaItem extends ItemNameBlockItem {
	/** Los mismos colores que el Huevo Eburia. */
	private static final int CELESTE = 0x55D9F0, GRIS = 0xC6CFD6, AMARILLO = 0xFFD24A, NARANJA = 0xFFA23C;

	public SemillaEburiaItem(Block bloque, Properties propiedades) {
		super(bloque, propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(parte("Puedes plantarla en ", GRIS).append(parte("Troncos", CELESTE)));
		texto.add(parte("de Abedúl", CELESTE).append(parte(" y ", GRIS)).append(parte("Roble de Quiu", CELESTE)));
		texto.add(parte("para hacerla crecer.", GRIS));
		texto.add(Component.empty());
		texto.add(parte("⚠ Atención: ", AMARILLO).append(parte("Al alcanzar su punto", GRIS)));
		texto.add(parte("máximo de crecimiento debes", GRIS));
		texto.add(parte("recogerlo lo antes posible.", GRIS));
		texto.add(parte("Después de un tiempo se pudrirá", GRIS));
		texto.add(parte("si no lo cosechas.", GRIS));
		texto.add(Component.empty());
		texto.add(parte("Utiliza un ", GRIS).append(parte("Hacha", NARANJA)).append(parte(" mientras tienes", GRIS)));
		texto.add(parte("un ", GRIS).append(parte("Huevo Eburia", CELESTE)).append(parte(" en la ", GRIS))
				.append(parte("Mano", NARANJA)));
		texto.add(parte("Secundaria", NARANJA).append(parte(" para extraerla.", GRIS)));
	}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	private static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(color(rgb));
	}
}
