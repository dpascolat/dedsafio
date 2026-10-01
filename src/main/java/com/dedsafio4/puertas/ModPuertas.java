package com.dedsafio4.puertas;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;
import java.util.function.Supplier;

/** Las puertas de 3x3 con llave: la Rosa y la Verde, cada una con su Tarjeta de Acceso Phora. */
public final class ModPuertas {
	private ModPuertas() {}

	public static final Block PUERTA_ROSA = bloque("puerta_rosa", () -> ModItems.TARJETA_PUERTA_ROSA, "Tarjeta de Acceso Phora rosa");
	public static final Block PUERTA_VERDE = bloque("puerta_verde", () -> ModItems.TARJETA_PUERTA_VERDE, "Tarjeta de Acceso Phora verde");
	public static final Item PUERTA_ROSA_ITEM = item("puerta_rosa", PUERTA_ROSA, "Tarjeta de Acceso Phora rosa", 0x55D9F0);
	public static final Item PUERTA_VERDE_ITEM = item("puerta_verde", PUERTA_VERDE, "Tarjeta de Acceso Phora verde", 0x55D9F0);

	public static final BlockEntityType<PuertaBlockEntity> PUERTA_ENTIDAD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, id("puerta"),
			FabricBlockEntityTypeBuilder.create(PuertaBlockEntity::new, PUERTA_ROSA, PUERTA_VERDE).build());

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** No se rompe (como la bedrock): solo se abre con su llave. */
	private static Block bloque(String nombre, Supplier<Item> llave, String nombreLlave) {
		return Registry.register(BuiltInRegistries.BLOCK, id(nombre), new PuertaBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_BLACK).strength(-1f, 3_600_000f).noLootTable().sound(SoundType.METAL)
				.noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(estado -> 4)
				.isSuffocating((e, m, p) -> false).isViewBlocking((e, m, p) -> false), llave, nombreLlave));
	}

	private static Item item(String nombre, Block bloque, String nombreLlave, int color) {
		return Registry.register(BuiltInRegistries.ITEM, id(nombre), new BlockItem(bloque, new Item.Properties()) {
			@Override
			public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
				texto.add(Component.literal("Puerta de 3x3. Se abre usando la").withColor(0xC6CFD6));
				texto.add(Component.literal(nombreLlave).withColor(color)
						.append(Component.literal(" (la llave se gasta).").withColor(0xC6CFD6)));
			}
		});
	}

	public static void registrar() {}
}
