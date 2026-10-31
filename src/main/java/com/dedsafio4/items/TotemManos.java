package com.dedsafio4.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Qué tótem se gasta si tienes uno en cada mano: siempre el de la mano principal. */
public final class TotemManos {
	private TotemManos() {}

	private static boolean esDelMod(ItemStack pila) {
		return pila.is(ModItems.TOTEM_FRERICO) || pila.is(ModItems.TOTEM_GLEBANOIDE) || pila.is(ModItems.TOTEM_YUNQUE) || pila.is(ModItems.TOTEM_LIMBO);
	}

	private static boolean esTotem(ItemStack pila) {
		return pila.is(Items.TOTEM_OF_UNDYING) || esDelMod(pila);
	}

	/** Si es ese tótem el que le toca gastarse. */
	public static boolean leToca(Player jugador, Item totem) {
		ItemStack principal = jugador.getMainHandItem();
		return (esTotem(principal) ? principal : jugador.getOffhandItem()).is(totem);
	}

	/** El de Minecraft en la otra mano no se gasta si en la principal hay uno del mod: ese lo gasta su evento. */
	public static boolean saltearMinecraft(Player jugador) {
		return esDelMod(jugador.getMainHandItem()) && jugador.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
	}
}
