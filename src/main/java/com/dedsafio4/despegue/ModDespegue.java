package com.dedsafio4.despegue;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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

	/** Fuerza la carga de la clase. */
	public static void registrar() {}
}
