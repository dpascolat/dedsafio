package com.dedsafio4.items;

import com.dedsafio4.subastas.Puja;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tarjeta de Subasta: hace falta tenerla para pujar. Con click derecho puja lo justo (la puja actual más el mínimo,
 * o el mínimo si nadie pujó), igual que el Bloque de Puja. Espera 1 segundo entre pujas, para no pujar dos veces
 * sin querer dejando apretado el botón.
 */
public class TarjetaSubastaItem extends DescritoItem {
	private static final int ESPERA = 20;

	public TarjetaSubastaItem(Properties propiedades) {
		super(propiedades, DescritoItem::tarjetaSubasta);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		if (jugador instanceof ServerPlayer servidor && Puja.pujar(servidor, Puja.datos(servidor.server).siguiente())) {
			jugador.getCooldowns().addCooldown(this, ESPERA);
		}
		return InteractionResultHolder.sidedSuccess(pila, level.isClientSide);
	}
}
