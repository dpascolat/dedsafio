package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Arco de Huesos Grande (el arco del Reptisaurio arquero): dispara como un arco normal, pero cada flecha traspasa a
 * los enemigos y le pega hasta a 3 (como la Perforación II de la ballesta). Se repara con huesos.
 */
public class ArcoHuesosGrandeItem extends BowItem {
	/** Traspasa a 2, así impacta hasta 3 enemigos. */
	private static final byte TRASPASA = 2;

	public ArcoHuesosGrandeItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected Projectile createProjectile(Level mundo, LivingEntity tirador, ItemStack arco, ItemStack flecha, boolean critico) {
		Projectile proyectil = super.createProjectile(mundo, tirador, arco, flecha, critico);
		if (proyectil instanceof AbstractArrow f && f.getPierceLevel() < TRASPASA) {
			((com.dedsafio4.mixin.FlechaAccessor) f).dedsafio4$setPierceLevel(TRASPASA);
		}
		return proyectil;
	}

	@Override
	public boolean isValidRepairItem(ItemStack arco, ItemStack material) {
		return material.is(Items.BONE) || material.is(Items.BONE_BLOCK);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(DescritoItem.color(DescritoItem.CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(DescritoItem.parte("Dispara flechas e impacta", DescritoItem.GRIS));
		texto.add(DescritoItem.parte("hasta 3 enemigos.", DescritoItem.VIOLETA));
	}
}
