package com.dedsafio4.client;

import com.dedsafio4.client.cajero.EstiloCajero;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;

/**
 * La grilla de 3×3 del crafteo de un ítem en su descripción, chiquita y con borde de arcoíris (como en las imágenes de
 * Patricio). Si un lugar acepta varios ítems, va mostrando uno por segundo. Sin receta, no dibuja nada.
 */
public class RecetaTooltipCliente implements ClientTooltipComponent {
	private static final int CELDA = 11, BORDE = 2, LADO = BORDE * 2 + CELDA * 3;
	private final Ingredient[] grilla = new Ingredient[9];
	private final boolean hay;

	public RecetaTooltipCliente(Item item) {
		boolean encontrada = false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			for (RecipeHolder<CraftingRecipe> r : mc.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
				if (!r.value().getResultItem(mc.level.registryAccess()).is(item)) continue;
				List<Ingredient> ingredientes = r.value().getIngredients();
				int ancho = r.value() instanceof ShapedRecipe forma ? forma.getWidth() : 3;
				for (int i = 0; i < ingredientes.size() && i < 9; i++) grilla[(i / ancho) * 3 + i % ancho] = ingredientes.get(i);
				encontrada = true;
				break;
			}
		}
		hay = encontrada;
	}

	@Override
	public int getHeight() {
		return hay ? LADO + 3 : 0;
	}

	@Override
	public int getWidth(Font font) {
		return hay ? LADO : 0;
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics g) {
		if (!hay) return;
		y += 1;
		float t = EstiloCajero.tiempo();
		g.fill(x, y, x + LADO, y + LADO, 0xFF101218);
		EstiloCajero.bordeArcoiris(g, x, y, LADO, LADO, t, t + 0.8f);
		int segundo = (int) (Util.getMillis() / 1000);
		for (int i = 0; i < 9; i++) {
			int cx = x + BORDE + (i % 3) * CELDA, cy = y + BORDE + (i / 3) * CELDA;
			g.fill(cx, cy, cx + CELDA - 1, cy + CELDA - 1, 0xFF2A2E38);
			Ingredient ing = grilla[i];
			if (ing == null || ing.isEmpty()) continue;
			ItemStack[] opciones = ing.getItems();
			if (opciones.length == 0) continue;
			g.pose().pushPose();
			g.pose().translate(cx + 0.5f, cy + 0.5f, 0);
			g.pose().scale(0.625f, 0.625f, 1);
			g.renderFakeItem(opciones[segundo % opciones.length], 0, 0);
			g.pose().popPose();
		}
	}
}
