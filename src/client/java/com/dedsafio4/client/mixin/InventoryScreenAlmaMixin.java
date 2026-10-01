package com.dedsafio4.client.mixin;

import com.dedsafio4.client.AlmaCliente;
import com.dedsafio4.items.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * En el inventario, a la derecha del libro de recetas, se ve el Alma (si la tenés) o el Sin Alma (si no).
 * Entre el libro y el alma queda lugar para el ojo, que se agrega después.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenAlmaMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
	/** Dónde va el ícono, contando desde la esquina del inventario (el libro de recetas está en 104, 61). */
	private static final int ALMA_X = 152, ALMA_Y = 62;

	@Shadow @Final private RecipeBookComponent recipeBookComponent;
	@Shadow private boolean widthTooNarrow;

	private InventoryScreenAlmaMixin(InventoryMenu menu, Inventory inventario, Component titulo) {
		super(menu, inventario, titulo);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void dedsafio4$dibujarAlma(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		// Con la pantalla angosta y el libro abierto, el inventario no se ve: el alma tampoco.
		if (recipeBookComponent.isVisible() && widthTooNarrow) return;
		ItemStack alma = new ItemStack(AlmaCliente.tieneAlma ? ModItems.ALMA : ModItems.SIN_ALMA);
		int x = leftPos + ALMA_X, y = topPos + ALMA_Y;
		g.renderItem(alma, x, y);
		if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16 && menu.getCarried().isEmpty()) {
			g.renderTooltip(font, Component.literal(AlmaCliente.tieneAlma ? "Tenés alma" : "No tenés alma"), mouseX, mouseY);
		}
	}
}
