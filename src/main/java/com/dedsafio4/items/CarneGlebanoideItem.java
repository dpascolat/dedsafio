package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * Carne Glebanoide: al comerla se te va la Levitación y caés despacio durante 1 segundo (Caída Lenta).
 * Cocinada se convierte en Oro en Bruto (recetas de horno, ahumador y fogata).
 */
public class CarneGlebanoideItem extends DescritoItem {
	/** Cuánto dura la Caída Lenta (1 segundo). */
	private static final int CAIDA_LENTA = 20;

	public CarneGlebanoideItem(Properties propiedades, Consumer<List<Component>> descripcion) {
		super(propiedades, descripcion);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack item, Level mundo, LivingEntity quien) {
		ItemStack resto = super.finishUsingItem(item, mundo, quien);
		if (!mundo.isClientSide) {
			quien.removeEffect(MobEffects.LEVITATION);
			quien.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, CAIDA_LENTA, 0));
		}
		return resto;
	}
}
