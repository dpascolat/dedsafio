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

	/** Los sonidos que pasó el usuario: abrir (6 a 6,5 s del audio) y cerrar (4,9 a 5,2 s). */
	public static final SoundEvent SONIDO_ABRIR = sonido("cofre_abrir");
	public static final SoundEvent SONIDO_CERRAR = sonido("cofre_cerrar");

	private static SoundEvent sonido(String nombre) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id(nombre), SoundEvent.createVariableRangeEvent(id(nombre)));
	}

	/** Fuerza la carga de la clase. */
	public static void registrar() {}
}
