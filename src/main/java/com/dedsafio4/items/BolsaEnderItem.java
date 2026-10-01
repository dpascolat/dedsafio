package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Bolsa de Ender: con click derecho abre tu Cofre de Ender (lo mismo que tenés guardado en cualquier
 * Cofre de Ender), estés donde estés.
 */
public class BolsaEnderItem extends Item {
	private static final Component TITULO = Component.translatable("container.enderchest");

	public BolsaEnderItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		if (!level.isClientSide) {
			var cofre = jugador.getEnderChestInventory();
			jugador.openMenu(new SimpleMenuProvider((id, inventario, quien) -> ChestMenu.threeRows(id, inventario, cofre), TITULO));
			level.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.6f, 1.2f);
		}
		return InteractionResultHolder.sidedSuccess(pila, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Click derecho para abrir tu").withColor(0xC6CFD6));
		texto.add(Component.literal("Cofre de Ender desde cualquier lado.").withColor(0xC6CFD6));
	}
}
