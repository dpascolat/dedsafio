package com.dedsafio4.bloques;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBloques {
	private ModBloques() {}

	/** Se rompe a la misma velocidad que la arena (dureza 0,5) y también se cae. */
	public static final Block CACA = registrar("caca", new CacaBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)
					.mapColor(MapColor.TERRACOTTA_BROWN)
					.strength(0.5f)
					.sound(SoundType.MUD)
					.noOcclusion()));

	public static final Item CACA_ITEM = registrarItem("caca", CACA);

	// Árboles de la dimensión nueva: iguales al roble y al abeto, pero con la corteza más clara.
	public static final Block ROBLE_CLARO_LOG = registrar("roble_claro_log",
			new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
	public static final Block ABETO_CLARO_LOG = registrar("abeto_claro_log",
			new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG)));

	/** Si la tocás te lleva a la otra dimensión. Brilla y no frena al que pasa. */
	public static final Block AGUA_PORTAL = registrar("agua_portal", new com.dedsafio4.dimension.AguaPortalBlock(
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.WATER)
					.strength(0.4f)
					.noCollission()
					.noOcclusion()
					.lightLevel(estado -> 10)
					.sound(SoundType.EMPTY)
					.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

	/** La fruta que crece en los troncos de la sabana. */
	public static final Block HUEVO_EBURIA = registrar("huevo_eburia",
			new com.dedsafio4.eburia.FrutaTroncoBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.SAND)
					.strength(0.2f, 3f)
					.sound(SoundType.WOOD)
					.randomTicks()
					.noOcclusion()
					.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY),
					com.dedsafio4.eburia.FrutaTroncoBlock.FORMAS_HUEVO));

	/** Arándano Nocturno: crece pegado al costado del tronco, como el cacao. */
	public static final Block ARANDANO_NOCTURNO = registrar("arandano_nocturno",
			new com.dedsafio4.eburia.FrutaTroncoBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLUE)
					.strength(0.2f, 3f)
					.sound(SoundType.WOOD)
					.randomTicks()
					.noOcclusion()
					.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY),
					com.dedsafio4.eburia.FrutaTroncoBlock.FORMAS_ARANDANO));

	/** Fruta Solaria: crece pegada al costado del tronco, como el cacao. */
	public static final Block FRUTA_SOLARIA = registrar("fruta_solaria",
			new com.dedsafio4.eburia.FrutaTroncoBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.GOLD)
					.strength(0.2f, 3f)
					.sound(SoundType.WOOD)
					.randomTicks()
					.noOcclusion()
					.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

	/** Baya Uvina: crece pegada al costado del tronco, como el cacao. */
	public static final Block BAYA_UVINA = registrar("baya_uvina",
			new com.dedsafio4.eburia.FrutaTroncoBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_PURPLE)
					.strength(0.2f, 3f)
					.sound(SoundType.WOOD)
					.randomTicks()
					.noOcclusion()
					.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY),
					com.dedsafio4.eburia.FrutaTroncoBlock.FORMAS_UVINA));

	/** El mineral de la dimensión nueva: hace falta pico de diamante. */
	public static final Block MINERAL_DE_AMBAR = registrar("mineral_de_ambar",
			new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)));

	public static final Item MINERAL_DE_AMBAR_ITEM = registrarItem("mineral_de_ambar", MINERAL_DE_AMBAR);

	/**
	 * Mineral de gemas verdes: hace falta pico de hierro, como la esmeralda. Suelta Cristal Verde (más con
	 * Fortuna) y experiencia; con Toque de Seda, el bloque.
	 */
	public static final Block MINERAL_VERDE = registrar("mineral_verde",
			new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7),
					BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)));

	public static final Item MINERAL_VERDE_ITEM = registrarItem("mineral_verde", MINERAL_VERDE);

	public static final Item AGUA_PORTAL_ITEM = registrarItem("agua_portal", AGUA_PORTAL);

	public static final Item ROBLE_CLARO_LOG_ITEM = registrarItem("roble_claro_log", ROBLE_CLARO_LOG);
	public static final Item ABETO_CLARO_LOG_ITEM = registrarItem("abeto_claro_log", ABETO_CLARO_LOG);

	/** Bloques de colores con textura animada (se mueven). */
	public static final Block BLOQUE_CELESTE = registrar("bloque_celeste", bloqueDeColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_BLUE));
	public static final Block BLOQUE_AMARILLO = registrar("bloque_amarillo", bloqueDeColor(net.minecraft.world.level.material.MapColor.COLOR_YELLOW));
	public static final Block BLOQUE_VERDE = registrar("bloque_verde", bloqueDeColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_GREEN));
	public static final Block BLOQUE_ROJO = registrar("bloque_rojo", bloqueDeColor(net.minecraft.world.level.material.MapColor.COLOR_RED));
	public static final Item BLOQUE_CELESTE_ITEM = registrarItem("bloque_celeste", BLOQUE_CELESTE);
	public static final Item BLOQUE_AMARILLO_ITEM = registrarItem("bloque_amarillo", BLOQUE_AMARILLO);
	public static final Item BLOQUE_VERDE_ITEM = registrarItem("bloque_verde", BLOQUE_VERDE);
	public static final Item BLOQUE_ROJO_ITEM = registrarItem("bloque_rojo", BLOQUE_ROJO);

	/** Bloque de Píxeles: sólo una cruz negra en cada cara, el resto se ve a través. */
	public static final Block BLOQUE_PIXELES = registrar("bloque_pixeles", new Block(BlockBehaviour.Properties.of()
			.mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK).strength(1.5f, 6f)
			.sound(net.minecraft.world.level.block.SoundType.STONE).noOcclusion()
			.isSuffocating((estado, mundo, pos) -> false).isViewBlocking((estado, mundo, pos) -> false)));
	public static final Item BLOQUE_PIXELES_ITEM = registrarItem("bloque_pixeles", BLOQUE_PIXELES);

	/**
	 * Bloque de Dilitio: con Pico de Diamante o mejor suelta Dilitio (con Toque de Seda, el bloque).
	 * La textura es un cubo desplegado de 64x64, una cara por lado.
	 */
	public static final Block BLOQUE_DILITIO = registrar("bloque_dilitio", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GREEN).strength(3f, 3f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
	public static final Item BLOQUE_DILITIO_ITEM = Registry.register(BuiltInRegistries.ITEM,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "bloque_dilitio"),
			new BlockItem(BLOQUE_DILITIO, new Item.Properties()) {
				@Override
				public net.minecraft.network.chat.Component getName(net.minecraft.world.item.ItemStack pila) {
					return net.minecraft.network.chat.Component.translatable(getDescriptionId())
							.withStyle(com.dedsafio4.items.DescritoItem.color(0x4AA852));
				}

				@Override
				public void appendHoverText(net.minecraft.world.item.ItemStack pila, TooltipContext contexto,
											java.util.List<net.minecraft.network.chat.Component> texto,
											net.minecraft.world.item.TooltipFlag bandera) {
					com.dedsafio4.items.DescritoItem.bloqueDilitio(texto);
				}
			});

	// Dimensión de los Órganos: todo el piso es carne. Blandos como la tierra, salvo el Músculo de abajo.
	public static final Block CARNE_ROSA = registrar("carne_rosa", bloqueDeCarne(MapColor.COLOR_PINK));
	public static final Block CARNE_ROJA = registrar("carne_roja", bloqueDeCarne(MapColor.COLOR_RED));
	public static final Block CARNE_CON_VENAS = registrar("carne_con_venas", bloqueDeCarne(MapColor.COLOR_PINK));
	/** Lo que hay debajo de la carne, como la piedra del Overworld: hace falta pico. */
	public static final Block MUSCULO = registrar("musculo", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.CRIMSON_NYLIUM).strength(1.5f, 6f).sound(SoundType.WART_BLOCK)
			.requiresCorrectToolForDrops()));
	public static final Item CARNE_ROSA_ITEM = registrarItem("carne_rosa", CARNE_ROSA);
	public static final Item CARNE_ROJA_ITEM = registrarItem("carne_roja", CARNE_ROJA);
	public static final Item CARNE_CON_VENAS_ITEM = registrarItem("carne_con_venas", CARNE_CON_VENAS);
	public static final Item MUSCULO_ITEM = registrarItem("musculo", MUSCULO);
	/** Bloque de Carne: un trozo chiquito (más bajo que una losa y más angosto). */
	public static final Block BLOQUE_CARNE = registrar("bloque_carne", new BloqueCarneBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.CRIMSON_NYLIUM).strength(0.6f).sound(SoundType.MUD).noOcclusion()));
	public static final Item BLOQUE_CARNE_ITEM = registrarItem("bloque_carne", BLOQUE_CARNE);
	/** Bloque de Carne Rosa: igual, pero rosita clarito. */
	public static final Block BLOQUE_CARNE_ROSA = registrar("bloque_carne_rosa", new BloqueCarneBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PINK).strength(0.6f).sound(SoundType.MUD).noOcclusion()));
	public static final Item BLOQUE_CARNE_ROSA_ITEM = registrarItem("bloque_carne_rosa", BLOQUE_CARNE_ROSA);
	/** Dientes: tres dientes incrustados en el piso. */
	public static final Block DIENTES = registrar("dientes", new DientesBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.SAND).strength(1f, 3f).sound(SoundType.BONE_BLOCK).noOcclusion()
			.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
	public static final Item DIENTES_ITEM = registrarItem("dientes", DIENTES);

	// Los árboles de la Dimensión de los Órganos: gelatina que tiembla (la textura se mueve). Se pican con pico.
	public static final Block GELATINA_ROJA = registrar("gelatina_roja", bloqueDeGelatina(MapColor.COLOR_RED));
	public static final Block GELATINA_ROSA = registrar("gelatina_rosa", bloqueDeGelatina(MapColor.COLOR_PINK));
	public static final Item GELATINA_ROJA_ITEM = registrarItem("gelatina_roja", GELATINA_ROJA);
	public static final Item GELATINA_ROSA_ITEM = registrarItem("gelatina_rosa", GELATINA_ROSA);

	/** Pelos Glebanoides (id pasto_rosa): como el pasto de Minecraft, sobre la carne de la Dimensión de los Órganos. */
	public static final Block PASTO_ROSA = registrar("pasto_rosa", new com.dedsafio4.dimension.PastoRosaBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).replaceable().noCollission().instabreak()
					.sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).ignitedByLava()
					.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
	public static final Item PASTO_ROSA_ITEM = Registry.register(BuiltInRegistries.ITEM,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "pasto_rosa"),
			new com.dedsafio4.items.BloqueDescritoItem(PASTO_ROSA, new Item.Properties(), com.dedsafio4.items.DescritoItem::pelosGlebanoides));

	private static Block bloqueDeGelatina(MapColor color) {
		return new Block(BlockBehaviour.Properties.of().mapColor(color).strength(1.5f, 3f).sound(SoundType.SLIME_BLOCK)
				.requiresCorrectToolForDrops());
	}

	private static Block bloqueDeCarne(MapColor color) {
		return new Block(BlockBehaviour.Properties.of().mapColor(color).strength(0.6f).sound(SoundType.MUD));
	}

	/** Brillan: dan luz y se ven siempre a pleno color, aunque sea de noche o estén en una cueva. */
	private static Block bloqueDeColor(net.minecraft.world.level.material.MapColor color) {
		return new Block(BlockBehaviour.Properties.of().mapColor(color).strength(1.5f, 6f)
				.sound(net.minecraft.world.level.block.SoundType.STONE)
				.lightLevel(estado -> 12)
				.emissiveRendering((estado, mundo, pos) -> true));
	}

	private static Block registrar(String nombre, Block bloque) {
		return Registry.register(BuiltInRegistries.BLOCK,
				ResourceKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre)),
				bloque);
	}

	private static Item registrarItem(String nombre, Block bloque) {
		return Registry.register(BuiltInRegistries.ITEM,
				ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre),
				new BlockItem(bloque, new Item.Properties()));
	}

	/** Fuerza la carga de la clase. */
	public static void registrar() {}
}
