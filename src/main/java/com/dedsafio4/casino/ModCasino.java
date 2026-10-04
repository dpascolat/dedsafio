package com.dedsafio4.casino;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/** El Casino: la entidad y su huevo generador. */
public final class ModCasino {
	private ModCasino() {}

	/** Unos 1,4 bloques de ancho y 2,6 de alto (la palanca sobresale un poco). */
	public static final EntityType<CasinoEntity> CASINO = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("casino"),
			EntityType.Builder.of(CasinoEntity::new, MobCategory.MISC).sized(1.4f, 2.6f).clientTrackingRange(10)
					.build("casino"));

	public static final Item CASINO_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, id("casino_spawn_egg"),
			new SpawnEggItem(CASINO, 0x7A1020, 0xF2C230, new Item.Properties()));

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static void registrar() {
		FabricDefaultAttributeRegistry.register(CASINO, CasinoEntity.crearAtributos());
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(TablaPremiosPayload.TYPE, TablaPremiosPayload.CODEC);
	}
}
