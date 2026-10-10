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
 * Tanque de Dilitio: click derecho sobre la Nave Espacial Biplaza (o usarla sentado adentro) le llena el tanque
 * al 100% de una.
 */
public class TanqueDilitioItem extends Item {
	public TanqueDilitioItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(0x6FD3F5);
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
		int blanco = 0xE8E8E8, celeste = 0x6FD3F5;
		texto.add(Component.empty());
		texto.add(Component.literal("Contenedor de alto rendimiento").withColor(blanco));
		texto.add(Component.literal("alimentado principalmente por").withColor(blanco));
		texto.add(Component.literal("Baterías de Dilitio").withColor(celeste).append(Component.literal(". Esencial").withColor(blanco)));
		texto.add(Component.literal("para la propulsión y vuelo").withColor(blanco));
		texto.add(Component.literal("de naves.").withColor(blanco));
		texto.add(Component.empty());
		texto.add(Component.literal("Este Tanque sirve para ir y").withColor(blanco));
		texto.add(Component.literal("venir de un planeta.").withColor(blanco));
	}
}
