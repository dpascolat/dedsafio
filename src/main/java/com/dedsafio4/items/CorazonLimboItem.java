package com.dedsafio4.items;

import com.dedsafio4.dimension.Limbo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Corazón del Limbo (el corazón violeta): al consumirlo, la barra de salud vuelve a ser de 10 corazones (20): te
 * devuelve los corazones que te sacaron para siempre el agua del Limbo o el Fantasma Negro, y te saca los corazones
 * extra que tengas (los de comer Corazones). Te deja con la vida llena.
 */
public class CorazonLimboItem extends Item {
	public CorazonLimboItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(0x55D9F0);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack pila, Level level, LivingEntity quien) {
		if (!level.isClientSide && quien instanceof ServerPlayer jugador) {
			Limbo.devolverCorazones(jugador);
			CorazonItem.sacarExtra(jugador);
			jugador.setHealth(jugador.getMaxHealth());
			level.playSound(null, jugador.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.4f);
			jugador.displayClientMessage(Component.literal("Tu Barra de Salud volvió al máximo (20).").withColor(0xF0D86A), true);
		}
		return super.finishUsingItem(pila, level, quien);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Consúmelo para ").withColor(0xE8E8E8).append(Component.literal("reestablecer").withColor(0xF0D86A)));
		texto.add(Component.literal("tu Barra de Salud al máximo (20).").withColor(0xF0D86A));
		texto.add(Component.empty());
		texto.add(Component.literal("⚠ Atención: ").withColor(0xF0D86A).append(Component.literal("Si tienes Corazones").withColor(0xFF7A7A)));
		texto.add(Component.literal("Extra, los PERDERÁS al consumirlo.").withColor(0xFF7A7A));
	}
}
