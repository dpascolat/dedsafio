package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.EnumMap;
import java.util.Map;

/**
 * Las Bolsas Primitivas de colores (una por cada tinte de Minecraft): hacen lo mismo que la Bolsa Primitiva y se
 * craftean con una Bolsa Primitiva (de cualquier color) y el tinte. Al teñirla, lo que tenía adentro se queda.
 */
public final class BolsasDeColores {
	private BolsasDeColores() {}

	public static final Map<DyeColor, Item> BOLSAS = new EnumMap<>(DyeColor.class);

	static {
		for (DyeColor color : DyeColor.values()) {
			BOLSAS.put(color, Registry.register(BuiltInRegistries.ITEM,
					ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "bolsa_primitiva_" + color.getName()),
					new BolsaItem(new Item.Properties().stacksTo(1))));
		}
	}

	/** El crafteo de teñir: como uno sin forma, pero el resultado se lleva lo que había adentro de la bolsa. */
	public static class Tenir extends ShapelessRecipe {
		public Tenir(ShapelessRecipe base) {
			super(base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
		}

		@Override
		public ItemStack assemble(CraftingInput entrada, HolderLookup.Provider registros) {
			ItemStack resultado = super.assemble(entrada, registros);
			for (int i = 0; i < entrada.size(); i++) {
				ItemStack pila = entrada.getItem(i);
				if (pila.getItem() instanceof BolsaItem && pila.has(DataComponents.CONTAINER)) {
					resultado.set(DataComponents.CONTAINER, pila.get(DataComponents.CONTAINER));
				}
			}
			return resultado;
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return SERIALIZADOR;
		}
	}

	public static final RecipeSerializer<Tenir> SERIALIZADOR = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "tenir_bolsa"), new RecipeSerializer<Tenir>() {
				private final MapCodec<Tenir> codec = RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(Tenir::new, t -> t);
				private final StreamCodec<RegistryFriendlyByteBuf, Tenir> stream =
						RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(Tenir::new, t -> t);

				@Override
				public MapCodec<Tenir> codec() {
					return codec;
				}

				@Override
				public StreamCodec<RegistryFriendlyByteBuf, Tenir> streamCodec() {
					return stream;
				}
			});

	public static void registrar() {}
}
