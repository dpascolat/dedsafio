package com.dedsafio4.client.mixin;

import com.dedsafio4.client.AlmaCliente;
import com.dedsafio4.client.CatalogoScreen;
import com.dedsafio4.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * En el inventario, a la derecha del libro de recetas, se ve el Alma (si la tenés) o el Sin Alma (si no).
 * Entre el libro y el alma queda lugar para el ojo, que se agrega después. Con clic en el alma se abre la G.
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
		if (!dedsafio4$almaVisible()) return;
		ItemStack alma = new ItemStack(AlmaCliente.tieneAlma ? ModItems.ALMA : ModItems.SIN_ALMA);
		g.renderItem(alma, leftPos + ALMA_X, topPos + ALMA_Y);
		if (dedsafio4$sobreAlma(mouseX, mouseY) && menu.getCarried().isEmpty()) {
			g.renderComponentTooltip(font, List.of(
					Component.literal(AlmaCliente.tieneAlma ? "Tenés alma" : "No tenés alma"),
					Component.literal("Clic para abrir la G").withColor(0xC6CFD6)), mouseX, mouseY);
		}
	}

	/** Clic en el alma: abre el catálogo (la G), igual que la tecla. */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$clicAlma(double mouseX, double mouseY, int boton, CallbackInfoReturnable<Boolean> cir) {
		if (boton != 0 || !dedsafio4$almaVisible() || !dedsafio4$sobreAlma(mouseX, mouseY) || !menu.getCarried().isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
		mc.setScreen(new CatalogoScreen());
		cir.setReturnValue(true);
	}

	/** Con la pantalla angosta y el libro abierto, el inventario no se ve: el alma tampoco. */
	private boolean dedsafio4$almaVisible() {
		return !(recipeBookComponent.isVisible() && widthTooNarrow);
	}

	private boolean dedsafio4$sobreAlma(double mouseX, double mouseY) {
		int x = leftPos + ALMA_X, y = topPos + ALMA_Y;
		return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
	}
}
