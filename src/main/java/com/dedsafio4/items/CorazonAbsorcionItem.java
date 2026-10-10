package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Corazón de Absorción (el corazón dorado): al consumirlo te da Absorción V por 3 minutos (una barra completa de
 * corazones amarillos, +20). Se craftea con 8 bloques de oro alrededor de un Corazón del Limbo.
 */
public class CorazonAbsorcionItem extends Item {
	private static final int DURA = 3 * 60 * 20;

	public CorazonAbsorcionItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(0x55D9F0);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack pila, Level level, LivingEntity quien) {
		if (!level.isClientSide) {
			quien.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DURA, 4));
			level.playSound(null, quien.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
		}
		return super.finishUsingItem(pila, level, quien);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Absorción V (03:00)").withColor(0x5C8CFF));
		texto.add(Component.empty());
		texto.add(Component.literal("Al aplicarse:").withColor(0xC864E0));
		texto.add(Component.literal("Absorción máxima: +20").withColor(0x5C8CFF));
		texto.add(Component.empty());
		texto.add(Component.literal("Consúmelo para conseguir ").withColor(0xE8E8E8).append(Component.literal("una").withColor(0xFF6EC7)));
		texto.add(Component.literal("Barra Completa de Absorción").withColor(0xFF6EC7).append(Component.literal(".").withColor(0xE8E8E8)));
	}
}
