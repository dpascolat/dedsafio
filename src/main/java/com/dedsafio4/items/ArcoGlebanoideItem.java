package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Arco Glebanoide: dispara como un arco normal, pero sus flechas rebotan en los enemigos cercanos
 * (el rebote está en ArcoReboteMixin).
 */
public class ArcoGlebanoideItem extends BowItem {
	public ArcoGlebanoideItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(DescritoItem.color(DescritoItem.CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(DescritoItem.parte("Dispara flechas y las hace", DescritoItem.GRIS));
		texto.add(DescritoItem.parte("rebotar en enemigos cercanos.", DescritoItem.VIOLETA));
	}
}
