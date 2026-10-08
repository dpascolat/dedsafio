package com.dedsafio4.subastas;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** La Vitrina del /ah (bloque invisible que muestra lo que está a la venta). */
public final class ModVitrina {
	private ModVitrina() {}

	public static final Block VITRINA = Registry.register(BuiltInRegistries.BLOCK, id("vitrina"),
			new VitrinaBlock(BlockBehaviour.Properties.of().strength(-1f, 3_600_000f).noLootTable().noOcclusion()
					.noCollission().sound(SoundType.GLASS).pushReaction(PushReaction.BLOCK)));
	public static final Item VITRINA_ITEM = Registry.register(BuiltInRegistries.ITEM, id("vitrina"),
			new BlockItem(VITRINA, new Item.Properties()));
	public static final BlockEntityType<VitrinaBlockEntity> ENTIDAD = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("vitrina"), FabricBlockEntityTypeBuilder.create(VitrinaBlockEntity::new, VITRINA).build());

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Sólo carga la clase (registra lo de arriba). */
	public static void registrar() {}
}
