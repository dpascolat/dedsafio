package com.dedsafio4.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Espada Glebanoide: 11 de daño y 1,6 de velocidad, y ya viene con Perdición de Gleba (33% más de daño a las criaturas
 * Glebanoides, las de la Dimensión de los Órganos, que se llama Gleba). El encantamiento está en
 * data/dedsafio4/enchantment/perdicion_de_gleba.json y las criaturas en el tag dedsafio4:glebanoides.
 */
public class EspadaGlebanoideItem extends SwordItem {
	public static final ResourceKey<Enchantment> PERDICION_DE_GLEBA = ResourceKey.create(Registries.ENCHANTMENT,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "perdicion_de_gleba"));

	public EspadaGlebanoideItem(Properties propiedades) {
		// Netherite (4) + 6 = 11 de daño (con el 1 de la mano); 1,6 de velocidad.
		super(Tiers.NETHERITE, propiedades.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.4f)));
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(0xE07AE6);
	}

	/** Apenas está en el inventario de alguien, ya está encantada con Perdición de Gleba. */
	@Override
	public void inventoryTick(ItemStack pila, Level mundo, Entity duenio, int casilla, boolean enMano) {
		super.inventoryTick(pila, mundo, duenio, casilla, enMano);
		if (mundo.isClientSide) return;
		mundo.registryAccess().registry(Registries.ENCHANTMENT).flatMap(r -> r.getHolder(PERDICION_DE_GLEBA)).ifPresent(perdicion -> {
			if (pila.getEnchantments().getLevel(perdicion) < 1) pila.enchant(perdicion, 1);
		});
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Hace ").withColor(0xE8E8E8).append(Component.literal("33% más de daño").withColor(0xF0D86A)));
		texto.add(Component.literal("a criaturas Glebanoides.").withColor(0xE8E8E8));
		texto.add(Component.empty());
	}
}
