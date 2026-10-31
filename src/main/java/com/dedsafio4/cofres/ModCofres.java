package com.dedsafio4.cofres;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** El cofre nuevo: el bloque, su inventario, el ítem y los sonidos de abrir y cerrar. */
public final class ModCofres {
	private ModCofres() {}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static final Block COFRE = Registry.register(BuiltInRegistries.BLOCK, id("cofre_protegido"),
			new CofreBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK)
					.strength(4f, 1200f)
					.requiresCorrectToolForDrops()
					.sound(SoundType.NETHERITE_BLOCK)
					.lightLevel(estado -> 4)
					.noOcclusion()));

	public static final Item COFRE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("cofre_protegido"),
			new BlockItem(COFRE, new Item.Properties().rarity(Rarity.EPIC)));

	public static final BlockEntityType<CofreBlockEntity> COFRE_ENTIDAD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, id("cofre_protegido"),
			FabricBlockEntityTypeBuilder.create(CofreBlockEntity::new, COFRE).build());

	/** El Cofre de Huesos (cofre doble con tapa de costillas). */
	public static final Block COFRE_HUESOS = Registry.register(BuiltInRegistries.BLOCK, id("cofre_huesos"),
			new CofreHuesosBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.SAND)
					.strength(2.5f)
					.sound(SoundType.BONE_BLOCK)
					.noOcclusion()));

	public static final Item COFRE_HUESOS_ITEM = Registry.register(BuiltInRegistries.ITEM, id("cofre_huesos"),
			new BlockItem(COFRE_HUESOS, new Item.Properties().rarity(Rarity.RARE)));

	public static final BlockEntityType<CofreBlockEntity> COFRE_HUESOS_ENTIDAD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, id("cofre_huesos"),
			FabricBlockEntityTypeBuilder.<CofreBlockEntity>create((pos, estado) -> new CofreBlockEntity(ModCofres.COFRE_HUESOS_ENTIDAD, pos, estado), COFRE_HUESOS).build());

	/** El Cofre de Gleba: carne, costillas, ojos y órganos. */
	public static final Block COFRE_GLEBA = Registry.register(BuiltInRegistries.BLOCK, id("cofre_gleba"),
			new CofreGlebaBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.CRIMSON_STEM)
					.strength(2.5f)
					.sound(SoundType.SLIME_BLOCK)
					.noOcclusion()));

	public static final Item COFRE_GLEBA_ITEM = Registry.register(BuiltInRegistries.ITEM, id("cofre_gleba"),
			new BlockItem(COFRE_GLEBA, new Item.Properties().rarity(Rarity.RARE)));

	public static final BlockEntityType<CofreBlockEntity> COFRE_GLEBA_ENTIDAD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, id("cofre_gleba"),
			FabricBlockEntityTypeBuilder.<CofreBlockEntity>create((pos, estado) -> new CofreBlockEntity(ModCofres.COFRE_GLEBA_ENTIDAD, pos, estado), COFRE_GLEBA).build());

	/** El Cofre del Limbo: roca oscura con grietas violetas. */
	public static final Block COFRE_LIMBO = Registry.register(BuiltInRegistries.BLOCK, id("cofre_limbo"),
			new CofreLimboBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK)
					.strength(2.5f)
					.sound(SoundType.SCULK)
					.lightLevel(estado -> 3)
					.noOcclusion()));

	public static final Item COFRE_LIMBO_ITEM = Registry.register(BuiltInRegistries.ITEM, id("cofre_limbo"),
			new BlockItem(COFRE_LIMBO, new Item.Properties().rarity(Rarity.RARE)));

	public static final BlockEntityType<CofreBlockEntity> COFRE_LIMBO_ENTIDAD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, id("cofre_limbo"),
			FabricBlockEntityTypeBuilder.<CofreBlockEntity>create((pos, estado) -> new CofreBlockEntity(ModCofres.COFRE_LIMBO_ENTIDAD, pos, estado), COFRE_LIMBO).build());

	public static final SoundEvent SONIDO_GLEBA_ABRIR = sonido("cofre_gleba_abrir");
	public static final SoundEvent SONIDO_GLEBA_CERRAR = sonido("cofre_gleba_cerrar");
	public static final SoundEvent SONIDO_LIMBO_ABRIR = sonido("cofre_limbo_abrir");
	public static final SoundEvent SONIDO_LIMBO_CERRAR = sonido("cofre_limbo_cerrar");

	/** Los sonidos que pasó el usuario: abrir (6 a 6,5 s del audio) y cerrar (4,9 a 5,2 s). */
	public static final SoundEvent SONIDO_ABRIR = sonido("cofre_abrir");
	public static final SoundEvent SONIDO_CERRAR = sonido("cofre_cerrar");

	private static SoundEvent sonido(String nombre) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id(nombre), SoundEvent.createVariableRangeEvent(id(nombre)));
	}

	/** Fuerza la carga de la clase. */
	public static void registrar() {}
}
