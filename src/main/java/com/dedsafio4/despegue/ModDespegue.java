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

/** La Plataforma de Despegue (sus tres bloques) y la nave que despega hacia la Dimensión de los Órganos. */
public final class ModDespegue {
	private ModDespegue() {}

	private static BlockBehaviour.Properties metal(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(3f, 6f).sound(SoundType.METAL)
				.requiresCorrectToolForDrops();
	}

	/** El centro: se pone éste y arma el resto solo. */
	public static final Block PLATAFORMA_DESPEGUE = bloque("plataforma_despegue", new PlataformaBlock(metal(MapColor.METAL)));
	public static final Block PLATAFORMA_METAL = bloque("plataforma_metal", new Block(metal(MapColor.METAL)));
	public static final Block PLATAFORMA_BORDE = bloque("plataforma_borde", new Block(metal(MapColor.COLOR_YELLOW)));

	public static final Item PLATAFORMA_DESPEGUE_ITEM = item("plataforma_despegue", PLATAFORMA_DESPEGUE);
	public static final Item PLATAFORMA_METAL_ITEM = item("plataforma_metal", PLATAFORMA_METAL);
	public static final Item PLATAFORMA_BORDE_ITEM = item("plataforma_borde", PLATAFORMA_BORDE);

	/** Combustible para la Nave Espacial Biplaza: cada uno carga un 10% del tanque. */
	public static final Item COMBUSTIBLE = Registry.register(BuiltInRegistries.ITEM, id("combustible"),
			new CombustibleItem(new Item.Properties().stacksTo(16)));

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
