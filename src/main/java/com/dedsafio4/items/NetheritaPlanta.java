package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

/**
 * Netherita con recubrimiento de planta: la armadura nueva. Protege igual que la de netherita
 * (casco 3 de armadura, 3 de resistencia, resistencia al empuje, 407 de durabilidad, no se quema).
 * La textura puesta es provisoria (la de netherita en verde) hasta que llegue la del diseño.
 */
public final class NetheritaPlanta {
	private NetheritaPlanta() {}

	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "netherita_planta");
	/** El verde claro del nombre. */
	public static final int VERDE = 0xBEF264;

	public static final Holder<ArmorMaterial> MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ID,
			new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8, ArmorItem.Type.LEGGINGS, 6,
					ArmorItem.Type.BOOTS, 3, ArmorItem.Type.BODY, 11), 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
					() -> Ingredient.of(Items.NETHERITE_INGOT), List.of(new ArmorMaterial.Layer(ID)), 3f, 0.1f));

	/** Una pieza de la armadura (con el nombre en verde). */
	public static class Pieza extends ArmorItem {
		public Pieza(Type tipo, Properties propiedades) {
			super(MATERIAL, tipo, propiedades.durability(tipo.getDurability(37)).fireResistant());
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(this.getDescriptionId()).withColor(VERDE);
		}
	}
}
