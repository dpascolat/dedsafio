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
 * Espada del Limbo: como una de netherite (13 de daño, 1,6 de velocidad, 2031 de durabilidad) y viene encantada con
 * Perdición del Limbo (data/dedsafio4/enchantment/perdicion_del_limbo.json): 33% más de daño a las criaturas del
 * Limbo (el tag dedsafio4:criaturas_limbo). Se consigue en los Cofres del Limbo.
 */
public class EspadaLimboItem extends SwordItem {
	public static final ResourceKey<Enchantment> PERDICION = ResourceKey.create(Registries.ENCHANTMENT,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "perdicion_del_limbo"));
	private static final int TEXTO = 0xE8E8E8, DORADO = 0xF7D36E, CELESTE = 0x7FE6F2, VIOLETA = 0xD16AFC;

	public EspadaLimboItem(Properties propiedades) {
		super(Tiers.NETHERITE, propiedades.fireResistant()
				.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 8, -2.4f)));   // 1 + 8 + 4 = 13 de daño
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId(pila)).withColor(VIOLETA);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Hace ").withColor(TEXTO).append(Component.literal("33% más de daño").withColor(DORADO)));
		texto.add(Component.literal("a criaturas del Limbo.").withColor(TEXTO));
		texto.add(Component.empty());
		texto.add(Component.literal("Consíguela en los Cofres del").withColor(TEXTO));
		texto.add(Component.literal("Limbo con una ").withColor(TEXTO)
				.append(Component.literal("Probabilidad").withColor(CELESTE)).append(Component.literal(".").withColor(TEXTO)));
		texto.add(Component.empty());
	}

	/** Viene encantada: si a alguna le falta Perdición del Limbo (recién sacada de un cofre, del creativo...), se le pone. Salvo si pasó por la afiladora. */
	@Override
	public void inventoryTick(ItemStack pila, Level mundo, Entity entidad, int lugar, boolean enMano) {
		if (mundo.isClientSide) return;
		PerdicionEspada.ponerSiFalta(pila, mundo, PERDICION);
	}
}
