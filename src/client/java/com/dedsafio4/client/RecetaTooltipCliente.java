package com.dedsafio4.client;

import com.dedsafio4.client.cajero.EstiloCajero;
import com.dedsafio4.client.mixin.SmithingTransformAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Las recetas de un ítem en su descripción, chiquitas y con borde de arcoíris (como en las imágenes de Patricio):
 *   - la grilla de 3×3 de la mesa de crafteo (la del Diario de NavithLabs), el "=" y lo que se hace;
 *   - cocinando: lo que se mete → el horno (o ahumador, alto horno, fogata) → el ítem;
 *   - en la mesa de herrería: plantilla, base y agregado → la mesa → el ítem.
 * Si un lugar acepta varios ítems, va mostrando uno por segundo. Sin recetas, no dibuja nada.
 */
public class RecetaTooltipCliente implements ClientTooltipComponent {
	private static final int CELDA = 11, BORDE = 2, SEPARACION = 3;
	/** La grilla de la mesa de crafteo: el dibujo de 24×24 agrandado a 36, el "=" (9×5) y el resultado. */
	private static final int MESA = 36;
	private static final net.minecraft.resources.ResourceLocation MINI = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
			"dedsafio4", "textures/gui/diario/receta_mini.png"), IGUAL = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
			"dedsafio4", "textures/gui/diario/receta_igual.png");

	/** Un recuadro: columnas × filas de lugares; cada lugar, los ítems que acepta (null = vacío). La mesa lleva su resultado. */
	private record Seccion(int columnas, int filas, ItemStack[][] lugares, ItemStack resultado) {
		boolean mesa() { return resultado != null; }
		int ancho() { return mesa() ? MESA + 3 + 9 + 3 + 16 : BORDE * 2 + CELDA * columnas; }
		int alto() { return mesa() ? MESA : BORDE * 2 + CELDA * filas; }
	}

	private final List<Seccion> secciones;

	/** Las recetas ya buscadas (por mundo: si cambia el mundo, se vuelven a buscar). */
	private static final java.util.Map<Item, List<Seccion>> BUSCADAS = new java.util.HashMap<>();
	private static Object mundoBuscado;

	/** Cambiaron los crafteos (/crafteo): se vuelven a buscar. */
	public static void olvidar() {
		BUSCADAS.clear();
	}

	public RecetaTooltipCliente(Item item) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != mundoBuscado) {
			BUSCADAS.clear();
			mundoBuscado = mc.level;
		}
		secciones = mc.level == null ? List.of() : BUSCADAS.computeIfAbsent(item, i -> buscar(mc, i));
	}

	private static ItemStack[] de(Ingredient ing) {
		return ing == null || ing.isEmpty() ? null : ing.getItems();
	}

	private static List<Seccion> buscar(Minecraft mc, Item item) {
		List<Seccion> out = new ArrayList<>();
		var registros = mc.level.registryAccess();
		var recetas = mc.level.getRecipeManager();
		ItemStack resultado = new ItemStack(item);

		// Mesa de crafteo.
		for (RecipeHolder<CraftingRecipe> r : recetas.getAllRecipesFor(RecipeType.CRAFTING)) {
			if (!r.value().getResultItem(registros).is(item)) continue;
			List<Ingredient> ingredientes = r.value().getIngredients();
			if (ingredientes.isEmpty()) continue;
			ItemStack[][] g = new ItemStack[9][];
			int ancho = r.value() instanceof ShapedRecipe forma ? forma.getWidth() : 3;
			for (int i = 0; i < ingredientes.size() && i < 9; i++) g[(i / ancho) * 3 + i % ancho] = de(ingredientes.get(i));
			out.add(new Seccion(3, 3, g, r.value().getResultItem(registros)));
			break;
		}

		// Cocinando: todo lo que se puede meter para conseguirlo, en el mismo lugar (va cambiando).
		Object[][] hornos = {{RecipeType.SMELTING, Items.FURNACE}, {RecipeType.BLASTING, Items.BLAST_FURNACE},
				{RecipeType.SMOKING, Items.SMOKER}, {RecipeType.CAMPFIRE_COOKING, Items.CAMPFIRE}};
		for (Object[] h : hornos) {
			@SuppressWarnings("unchecked")
			RecipeType<AbstractCookingRecipe> tipo = (RecipeType<AbstractCookingRecipe>) h[0];
			List<ItemStack> entra = new ArrayList<>();
			for (RecipeHolder<AbstractCookingRecipe> r : recetas.getAllRecipesFor(tipo)) {
				if (!r.value().getResultItem(registros).is(item) || r.value().getIngredients().isEmpty()) continue;
				for (ItemStack s : r.value().getIngredients().get(0).getItems()) {
					if (entra.stream().noneMatch(e -> e.is(s.getItem()))) entra.add(s);
				}
			}
			if (entra.isEmpty()) continue;
			out.add(new Seccion(3, 1, new ItemStack[][]{entra.toArray(ItemStack[]::new),
					{new ItemStack((Item) h[1])}, {resultado}}, null));
			break;   // el horno alcanza (el alto horno y el ahumador hacen lo mismo, más rápido)
		}

		// Mesa de herrería.
		for (RecipeHolder<SmithingRecipe> r : recetas.getAllRecipesFor(RecipeType.SMITHING)) {
			if (!(r.value() instanceof SmithingTransformAccessor partes) || !r.value().getResultItem(registros).is(item)) continue;
			out.add(new Seccion(5, 1, new ItemStack[][]{de(partes.dedsafio4$plantilla()), de(partes.dedsafio4$base()),
					de(partes.dedsafio4$agregado()), {new ItemStack(Items.SMITHING_TABLE)}, {resultado}}, null));
			break;
		}
		return out;
	}

	@Override
	public int getHeight() {
		int h = 0;
		for (Seccion s : secciones) h += s.alto() + SEPARACION;
		return h;
	}

	@Override
	public int getWidth(Font font) {
		int w = 0;
		for (Seccion s : secciones) w = Math.max(w, s.ancho());
		return w;
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics g) {
		float t = EstiloCajero.tiempo();
		int segundo = (int) (Util.getMillis() / 1000);
		y += 1;
		for (Seccion s : secciones) {
			int w = s.ancho(), h = s.alto();
			if (s.mesa()) {
				mesa(g, s, x, y, segundo);
				y += h + SEPARACION;
				continue;
			}
			g.fill(x, y, x + w, y + h, 0xFF101218);
			EstiloCajero.bordeArcoiris(g, x, y, w, h, t, t + 0.8f);
			for (int i = 0; i < s.lugares().length; i++) {
				int cx = x + BORDE + (i % s.columnas()) * CELDA, cy = y + BORDE + (i / s.columnas()) * CELDA;
				g.fill(cx, cy, cx + CELDA - 1, cy + CELDA - 1, 0xFF2A2E38);
				ItemStack[] opciones = s.lugares()[i];
				if (opciones == null || opciones.length == 0) continue;
				g.pose().pushPose();
				g.pose().translate(cx + 0.5f, cy + 0.5f, 0);
				g.pose().scale(0.625f, 0.625f, 1);
				g.renderFakeItem(opciones[segundo % opciones.length], 0, 0);
				g.pose().popPose();
			}
			y += h + SEPARACION;
		}
	}

	/** La mesa de crafteo: la grilla del Diario (las casillas empiezan en 2,1 y van cada 7 de sus 24), "=" y el resultado. */
	private static void mesa(GuiGraphics g, Seccion s, int x, int y, int segundo) {
		float k = MESA / 24f;
		g.blit(MINI, x, y, MESA, MESA, 0, 0, 24, 24, 24, 24);
		for (int i = 0; i < 9; i++) {
			ItemStack[] opciones = s.lugares()[i];
			if (opciones == null || opciones.length == 0) continue;
			g.pose().pushPose();
			g.pose().translate(x + (2.1f + (i % 3) * 7f) * k, y + (2.1f + (i / 3) * 7f) * k, 0);
			g.pose().scale(5.8f * k / 16f, 5.8f * k / 16f, 1);
			g.renderFakeItem(opciones[segundo % opciones.length], 0, 0);
			g.pose().popPose();
		}
		g.blit(IGUAL, x + MESA + 3, y + (MESA - 5) / 2, 9, 5, 0, 0, 9, 5, 9, 5);
		g.renderFakeItem(s.resultado(), x + MESA + 3 + 9 + 3, y + (MESA - 16) / 2);
	}
}
