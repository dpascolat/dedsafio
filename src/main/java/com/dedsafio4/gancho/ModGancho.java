package com.dedsafio4.gancho;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** El Gancho de Huesos (ver Ganchos). */
public final class ModGancho {
	private ModGancho() {}

	public static final Block BLOQUE = Registry.register(BuiltInRegistries.BLOCK, id("gancho_huesos"),
			new GanchoBlock(BlockBehaviour.Properties.of().strength(1f).noLootTable().noOcclusion().noCollission()
					.sound(SoundType.BONE_BLOCK).pushReaction(PushReaction.DESTROY)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, id("gancho_huesos"),
			new GanchoItem(BLOQUE, new Item.Properties()));
	public static final BlockEntityType<GanchoBlockEntity> ENTIDAD = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("gancho_huesos"), FabricBlockEntityTypeBuilder.create(GanchoBlockEntity::new, BLOQUE).build());

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Carga la clase (registra lo de arriba) y los eventos de la caña. */
	public static void registrar() {
		Ganchos.registrar();
	}
}
