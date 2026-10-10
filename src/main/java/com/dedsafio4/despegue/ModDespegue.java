package com.dedsafio4.despegue;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** El Soporte de Nave y la nave que despega hacia la Dimensión de los Órganos. */
public final class ModDespegue {
	private ModDespegue() {}

	private static BlockBehaviour.Properties metal(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(3f, 6f).sound(SoundType.METAL)
				.requiresCorrectToolForDrops();
	}

	/** El Soporte de Nave (2×2 bloques): ahí se coloca la Nave Espacial Biplaza. */
	public static final Block SOPORTE_NAVE = bloque("soporte_nave", new SoporteNaveBlock(metal(MapColor.METAL).noOcclusion()));
	public static final Item SOPORTE_NAVE_ITEM = item("soporte_nave", SOPORTE_NAVE);
	public static final net.minecraft.world.level.block.entity.BlockEntityType<SoporteNaveBlockEntity> SOPORTE_NAVE_ENTIDAD =
			Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("soporte_nave"),
					net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder
							.create(SoporteNaveBlockEntity::new, SOPORTE_NAVE).build());

	/** Combustible para la Nave Espacial Biplaza: cada uno carga un 10% del tanque. */
	public static final Item COMBUSTIBLE = Registry.register(BuiltInRegistries.ITEM, id("combustible"),
			new CombustibleItem(new Item.Properties().stacksTo(16)));
	/** Tanque de Dilitio: llena el tanque de la nave al 100% de una. */
	public static final Item TANQUE_DILITIO = Registry.register(BuiltInRegistries.ITEM, id("tanque_dilitio"),
			new TanqueDilitioItem(new Item.Properties().stacksTo(16)));

	/** La nave parada: unos 4 bloques de alto (el modelo, de punta a motor). */
	public static final EntityType<NaveViajeEntity> NAVE_VIAJE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			id("nave_viaje"),
			EntityType.Builder.<NaveViajeEntity>of(NaveViajeEntity::new, MobCategory.MISC).sized(1.6f, 4.2f)
					.clientTrackingRange(16).updateInterval(1).build("nave_viaje"));

	private static Block bloque(String nombre, Block bloque) {
		return Registry.register(BuiltInRegistries.BLOCK, id(nombre), bloque);
	}

	private static Item item(String nombre, Block bloque) {
		return Registry.register(BuiltInRegistries.ITEM, id(nombre), new BlockItem(bloque, new Item.Properties()));
	}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** El Espacio: una dimensión vacía, con cielo negro y estrellas, por donde viaja la nave entre los dos planetas. */
	public static final ResourceKey<Level> ESPACIO = ResourceKey.create(Registries.DIMENSION, id("espacio"));

	public static void registrar() {
		// Nadie se queda en el Espacio sin la nave (por ejemplo si se desconectó en pleno viaje): vuelve al Overworld.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(mundo -> {
			if (!mundo.dimension().equals(ESPACIO) || mundo.getGameTime() % 20 != 0) return;
			ServerLevel overworld = mundo.getServer().overworld();
			for (ServerPlayer jugador : java.util.List.copyOf(mundo.players())) {
				if (jugador.getVehicle() instanceof NaveViajeEntity) continue;
				net.minecraft.core.BlockPos lugar = com.dedsafio4.dimension.Portales.lugarSeguro(overworld, jugador.blockPosition());
				jugador.teleportTo(overworld, lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, jugador.getYRot(), jugador.getXRot());
			}
		});
	}
}
