package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/**
 * Espada Phora (diseño de Patricio, con la lava animada): 15 de daño por golpe (1 de la mano + 4 de netherita + 10) y
 * 1,6 de velocidad de ataque. No se quema. El nombre va en celeste.
 */
public class EspadaPhoraItem extends SwordItem {
	private static final int CELESTE = 0x55D9F0;

	public EspadaPhoraItem(Properties propiedades) {
		super(Tiers.NETHERITE, propiedades.fireResistant().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 10, -2.4f)));
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(CELESTE);
	}
}
