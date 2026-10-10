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
 * La armadura Glebanoide (sin encantamientos; cada pieza da medio corazón extra mientras la tienes puesta):
 *   Pechera     9 de armadura, 3 de resistencia, 592 de durabilidad, reducción base ~36%
 *   Pantalones  7 de armadura, 3 de resistencia, 1055 de durabilidad, reducción base ~28%
 *   Botas       4 de armadura, 3 de resistencia, 981 de durabilidad, reducción base ~16%
 * Las texturas puestas en el cuerpo (textures/models/armor/glebanoide_layer_1 y _2) son las de diamante pintadas con
 * los rojos de los íconos.
 */
public final class ArmaduraGlebanoide {
	private ArmaduraGlebanoide() {}

	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "glebanoide");

	public static final Holder<ArmorMaterial> MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ID,
			new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 9, ArmorItem.Type.LEGGINGS, 7,
					ArmorItem.Type.BOOTS, 4, ArmorItem.Type.BODY, 11), 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
					() -> Ingredient.of(ModItems.CAPARAZON), List.of(new ArmorMaterial.Layer(ID)), 3f, 0f));

	/** Una pieza: nombre celeste, medio corazón extra y su reducción base en la descripción. */
	public static class Pieza extends ArmorItem {
		private final ResourceLocation corazon;
		private final EquipmentSlotGroup lugar;
		private final int reduccion;

		public Pieza(Type tipo, int durabilidad, int reduccion, Properties propiedades) {
			super(MATERIAL, tipo, propiedades.durability(durabilidad));
			this.reduccion = reduccion;
			this.lugar = EquipmentSlotGroup.bySlot(tipo.getSlot());
			this.corazon = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "glebanoide_corazon_" + tipo.getName());
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(getDescriptionId()).withColor(0x55D9F0);
		}

		/** La armadura de siempre más medio corazón de vida máxima. */
		@Override
		public ItemAttributeModifiers getDefaultAttributeModifiers() {
			return super.getDefaultAttributeModifiers().withModifierAdded(Attributes.MAX_HEALTH,
					new AttributeModifier(corazon, 1.0, AttributeModifier.Operation.ADD_VALUE), lugar);
		}

		@Override
		public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
			texto.add(Component.empty());
			texto.add(Component.literal("♥ Otorga 0.5 Corazones Extra.").withColor(0x7CFC6A));
			texto.add(Component.literal("⛨ Reducción Base: ~" + reduccion + "%").withColor(0x5CE1E6));
		}
	}
}
