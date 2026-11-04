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
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

/**
 * La Armadura del Espacio (blanca con detalles de colores, íconos de Patricio) y la Espada del Espacio. Por ahora
 * protegen y pegan igual que las de netherita y no se queman; sólo se consiguen en creativo o con /give.
 * La textura puesta (textures/models/armor/espacio_layer_1 y _2) es la de hierro pasada a los blancos de los íconos,
 * con un visor oscuro en el casco y los detalles de colores.
 */
public final class ArmaduraEspacio {
	private ArmaduraEspacio() {}

	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "espacio");
	/** El celeste claro del nombre. */
	public static final int COLOR = 0xB8E8FF;

	public static final Holder<ArmorMaterial> MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ID,
			new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8, ArmorItem.Type.LEGGINGS, 6,
					ArmorItem.Type.BOOTS, 3, ArmorItem.Type.BODY, 11), 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
					() -> Ingredient.of(Items.NETHERITE_INGOT), List.of(new ArmorMaterial.Layer(ID)), 3f, 0.1f));

	/** Una pieza de la armadura (con el nombre en celeste). */
	public static class Pieza extends ArmorItem {
		public Pieza(Type tipo, Properties propiedades) {
			super(MATERIAL, tipo, propiedades.durability(tipo.getDurability(37)).fireResistant());
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(getDescriptionId()).withColor(COLOR);
		}
	}

	/** La espada: como la de netherita (8 de daño, 1,6 de velocidad). */
	public static class Espada extends SwordItem {
		public Espada(Properties propiedades) {
			super(Tiers.NETHERITE, propiedades.fireResistant().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 3, -2.4f)));
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(getDescriptionId()).withColor(COLOR);
		}
	}
}
