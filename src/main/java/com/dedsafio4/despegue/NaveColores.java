package com.dedsafio4.despegue;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.nave.PartesNave;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Los colores de la Nave Espacial Biplaza: en la mesa de crafteo, la nave con cualquier tinte de Minecraft sale
 * pintada de ese color (con el mismo combustible). El color va en el ítem (BASE_COLOR) y, al ponerla en el soporte,
 * pasa a la nave (NaveViajeEntity); al guardarla vuelve al ítem. Se dibuja teñida (NaveViajeRenderer, ParteNaveRenderer).
 */
public final class NaveColores {
	private NaveColores() {}

	/** Nave + un tinte = la nave de ese color. */
	public static class Tenir extends CustomRecipe {
		public Tenir(CraftingBookCategory categoria) {
			super(categoria);
		}

		@Override
		public boolean matches(CraftingInput entrada, Level level) {
			int naves = 0, tintes = 0;
			for (ItemStack pila : entrada.items()) {
				if (pila.isEmpty()) continue;
				if (pila.is(PartesNave.NAVE_BIPLAZA)) naves++;
				else if (pila.getItem() instanceof DyeItem) tintes++;
				else return false;
			}
			return naves == 1 && tintes == 1;
		}

		@Override
		public ItemStack assemble(CraftingInput entrada, HolderLookup.Provider registros) {
			ItemStack nave = ItemStack.EMPTY;
			DyeColor color = null;
			for (ItemStack pila : entrada.items()) {
				if (pila.is(PartesNave.NAVE_BIPLAZA)) nave = pila;
				else if (pila.getItem() instanceof DyeItem tinte) color = tinte.getDyeColor();
			}
			if (nave.isEmpty() || color == null) return ItemStack.EMPTY;
			ItemStack resultado = nave.copyWithCount(1);
			resultado.set(DataComponents.BASE_COLOR, color);
			return resultado;
		}

		@Override
		public boolean canCraftInDimensions(int ancho, int alto) {
			return ancho * alto >= 2;
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return SERIALIZADOR;
		}
	}

	public static final RecipeSerializer<Tenir> SERIALIZADOR = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "tenir_nave"), new SimpleCraftingRecipeSerializer<>(Tenir::new));

	/** El color de una nave (ítem), o -1 si es la de siempre. */
	public static int colorDe(ItemStack nave) {
		DyeColor c = nave.get(DataComponents.BASE_COLOR);
		return c == null ? -1 : c.getId();
	}

	/** Pinta el ítem de la nave con ese color (-1 = el de siempre). */
	public static void pintar(ItemStack nave, int color) {
		if (color < 0) nave.remove(DataComponents.BASE_COLOR);
		else nave.set(DataComponents.BASE_COLOR, DyeColor.byId(color));
	}

	/** La nave de siempre y una de cada color, para la pestaña de creativo. */
	public static List<ItemStack> todas() {
		List<ItemStack> l = new ArrayList<>();
		l.add(new ItemStack(PartesNave.NAVE_BIPLAZA));
		for (DyeColor c : DyeColor.values()) {
			ItemStack nave = new ItemStack(PartesNave.NAVE_BIPLAZA);
			nave.set(DataComponents.BASE_COLOR, c);
			l.add(nave);
		}
		return l;
	}

	/** Sólo carga la clase (registra lo de arriba). */
	public static void registrar() {}
}
