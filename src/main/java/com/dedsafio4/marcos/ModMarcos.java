package com.dedsafio4.marcos;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

/** El marco grande (4×4 bloques, invisible). */
public final class ModMarcos {
	private ModMarcos() {}

	public static final EntityType<MarcoGrandeEntity> MARCO_GRANDE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			id("marco_grande"), EntityType.Builder.<MarcoGrandeEntity>of(MarcoGrandeEntity::new, MobCategory.MISC)
					.sized(0.5f, 0.5f).eyeHeight(0f).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).build("marco_grande"));

	public static final Item MARCO_GRANDE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("marco_grande"),
			new MarcoGrandeItem(new Item.Properties()));

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Solo para que se carguen los registros al arrancar. */
	public static void registrar() {}
}
