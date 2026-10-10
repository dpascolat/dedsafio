package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

/**
 * La armadura Glebanoide. Por ahora la Pechera: 9 de armadura, 3 de resistencia de armadura, 592 de durabilidad (como
 * la de netherita) y medio corazón extra mientras la tienes puesta. Sin encantamientos. La textura puesta en el cuerpo
 * (textures/models/armor/glebanoide_layer_1.png) es la de diamante pintada con los rojos del ícono.
 */
public final class ArmaduraGlebanoide {
	private ArmaduraGlebanoide() {}

	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "glebanoide");
	private static final ResourceLocation CORAZON_EXTRA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "pechera_glebanoide_corazon");

	public static final Holder<ArmorMaterial> MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ID,
			new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 9, ArmorItem.Type.LEGGINGS, 6,
					ArmorItem.Type.BOOTS, 3, ArmorItem.Type.BODY, 11), 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
					() -> Ingredient.of(ModItems.CAPARAZON), List.of(new ArmorMaterial.Layer(ID)), 3f, 0f));

	/** La Pechera Glebanoide (nombre celeste, medio corazón extra). */
	public static class Pechera extends ArmorItem {
		public Pechera(Properties propiedades) {
			super(MATERIAL, Type.CHESTPLATE, propiedades.durability(Type.CHESTPLATE.getDurability(37)));
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(getDescriptionId()).withColor(0x55D9F0);
		}

		/** La armadura de siempre más medio corazón de vida máxima (en el torso). */
		@Override
		public ItemAttributeModifiers getDefaultAttributeModifiers() {
			return super.getDefaultAttributeModifiers().withModifierAdded(Attributes.MAX_HEALTH,
					new AttributeModifier(CORAZON_EXTRA, 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST);
		}

		@Override
		public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
			texto.add(Component.empty());
			texto.add(Component.literal("♥ Otorga 0.5 Corazones Extra.").withColor(0x7CFC6A));
			texto.add(Component.literal("⛨ Reducción Base: ~36%").withColor(0x5CE1E6));
		}
	}
}
