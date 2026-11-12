package com.dedsafio4.neocompat.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Como GuiTrajeMixin, para los métodos que tiene solo NeoForge: con el Traje Espacial puesto no se dibujan los
 * corazones, la armadura, la comida ni el aire (los reemplaza el visor del traje, TrajeHud).
 */
@Mixin(Gui.class)
abstract class GuiTrajeNeoMixin {
	@Inject(method = {"renderHealthLevel", "renderArmorLevel", "renderFoodLevel", "renderAirLevel"}, at = @At("HEAD"),
			cancellable = true, require = 0)
	private void dedsafio4$sinBarrasNeo(GuiGraphics g, CallbackInfo ci) {
		if (com.dedsafio4.client.TrajeHud.activo()) ci.cancel();
	}
}
