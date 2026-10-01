package com.dedsafio4.robots;

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
import net.minecraft.world.level.material.MapColor;

/** El bloque del Aldeano Robot dormido. */
public final class ModRobots {
	private ModRobots() {}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static final Block ROBOT_DORMIDO = Registry.register(BuiltInRegistries.BLOCK, id("robot_dormido"),
			new RobotDormidoBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK)
					.strength(3f, 6f)
					.sound(SoundType.NETHERITE_BLOCK)
					.noOcclusion()));

	public static final Item ROBOT_DORMIDO_ITEM = Registry.register(BuiltInRegistries.ITEM, id("robot_dormido"),
			new BlockItem(ROBOT_DORMIDO, new Item.Properties()));

	public static final BlockEntityType<RobotDormidoBlockEntity> ROBOT_DORMIDO_ENTIDAD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, id("robot_dormido"),
			FabricBlockEntityTypeBuilder.create(RobotDormidoBlockEntity::new, ROBOT_DORMIDO).build());

	/** Fuerza la carga de la clase. */
	public static void registrar() {}
}
