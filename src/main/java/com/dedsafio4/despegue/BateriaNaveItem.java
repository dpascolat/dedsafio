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
 * Batería de Nave: click derecho sobre la Nave Espacial Biplaza (o usarla sentado adentro) le llena el tanque
 * al 100% de una.
 */
public class BateriaNaveItem extends Item {
	public BateriaNaveItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(0x6CF07A);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		if (!(jugador.getVehicle() instanceof NaveViajeEntity nave)) return InteractionResultHolder.pass(pila);
		if (!level.isClientSide) nave.cargarBateria(jugador, pila);
		return InteractionResultHolder.sidedSuccess(pila, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Carga la nave al ").withColor(0xC6CFD6).append(Component.literal("100%").withColor(0x6CF07A))
				.append(Component.literal(" de una.").withColor(0xC6CFD6)));
		texto.add(Component.literal("Click derecho sobre la nave").withColor(0xC6CFD6));
		texto.add(Component.literal("(o úsala sentado adentro).").withColor(0xC6CFD6));
	}
}
