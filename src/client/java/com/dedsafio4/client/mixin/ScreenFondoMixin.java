package com.dedsafio4.client.mixin;

import com.dedsafio4.client.menu.MenuPrincipalScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fuera de un mundo (opciones, lista de servidores...), el fondo de los menús es negro en vez del panorama. */
@Mixin(Screen.class)
public abstract class ScreenFondoMixin {
	@Shadow public int width;
	@Shadow public int height;

	@Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$panoramaNegro(GuiGraphics g, float parcial, CallbackInfo ci) {
		if (MenuPrincipalScreen.fondoNegro()) {
			g.fill(0, 0, width, height, 0xFF000000);
			ci.cancel();
		}
	}

	@Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinDesenfoque(float parcial, CallbackInfo ci) {
		if (MenuPrincipalScreen.fondoNegro()) ci.cancel();
	}

	@Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinTextura(GuiGraphics g, int x, int y, int ancho, int alto, CallbackInfo ci) {
		if (MenuPrincipalScreen.fondoNegro()) ci.cancel();
	}
}
