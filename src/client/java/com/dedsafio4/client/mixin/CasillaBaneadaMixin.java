package com.dedsafio4.client.mixin;

import com.dedsafio4.baneos.ItemsBaneados;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En los inventarios, la casilla de un ítem baneado (/banear item) tiene el fondo rojizo (el ítem se ve normal). */
@Mixin(AbstractContainerScreen.class)
public abstract class CasillaBaneadaMixin {
	@Inject(method = "renderSlot", at = @At("HEAD"))
	private void dedsafio4$fondoRojizo(GuiGraphics g, Slot casilla, CallbackInfo ci) {
		if (casilla.hasItem() && ItemsBaneados.CLIENTE.contains(casilla.getItem().getItem())) {
			g.fill(casilla.x, casilla.y, casilla.x + 16, casilla.y + 16, ItemsBaneados.ROJIZO);
		}
	}
}
