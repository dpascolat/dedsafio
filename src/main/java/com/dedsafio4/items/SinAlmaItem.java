package com.dedsafio4.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Sin Alma y Alma: con click derecho abren el catálogo (la G), igual que la tecla. */
public class SinAlmaItem extends Item {
	/** Lo pone el cliente al arrancar: abre la pantalla de la G (el servidor no abre pantallas). */
	public static Runnable abrirCatalogo;

	public SinAlmaItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level mundo, Player jugador, InteractionHand mano) {
		if (mundo.isClientSide && abrirCatalogo != null) abrirCatalogo.run();
		return InteractionResultHolder.sidedSuccess(jugador.getItemInHand(mano), mundo.isClientSide);
	}
}
