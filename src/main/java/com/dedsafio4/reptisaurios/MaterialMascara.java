package com.dedsafio4.reptisaurios;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

import java.util.List;
import java.util.Map;

/** Material de la Máscara Anti-Esporas (protege poco: lo suyo es el filtro). */
public final class MaterialMascara {
	private MaterialMascara() {}

	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "mascara");

	public static final Holder<ArmorMaterial> MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ID,
			new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 2), 9, SoundEvents.ARMOR_EQUIP_IRON,
					() -> net.minecraft.world.item.crafting.Ingredient.of(ItemTags.PLANKS), 
					List.of(new ArmorMaterial.Layer(ID)), 0f, 0f));
}
