package com.dedsafio4.despegue;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Combustible para la Nave Espacial Biplaza: click derecho sobre la nave (o usarlo sentado adentro) carga
 * un 10% del tanque. Cada viaje gasta un 50%.
 */
public class CombustibleItem extends Item {
	public CombustibleItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		if (!(jugador.getVehicle() instanceof NaveViajeEntity nave)) return InteractionResultHolder.pass(pila);
		if (!level.isClientSide) nave.cargar(jugador, pila);
		return InteractionResultHolder.sidedSuccess(pila, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Carga un " + NaveViajeEntity.COMBUSTIBLE_CARGA + "% del tanque de la nave.").withColor(0xC6CFD6));
		texto.add(Component.literal("Cada viaje gasta un " + NaveViajeEntity.COMBUSTIBLE_VIAJE + "%.").withColor(0xC6CFD6));
	}
}
