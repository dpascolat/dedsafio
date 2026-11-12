package com.dedsafio4.client.mixin;

import com.dedsafio4.client.TrajeHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Con el Traje Espacial puesto, el visor (TrajeHud) reemplaza la barra de ítems, los corazones, la comida, la armadura,
 * el aire y la experiencia de Minecraft. En NeoForge los corazones y lo demás van por otros métodos: GuiTrajeNeoMixin.
 */
@Mixin(Gui.class)
abstract class GuiTrajeMixin {
	@Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinBarra(GuiGraphics g, DeltaTracker tiempo, CallbackInfo ci) {
		if (TrajeHud.activo()) ci.cancel();
	}

	@Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinVida(GuiGraphics g, CallbackInfo ci) {
		if (TrajeHud.activo()) ci.cancel();
	}

	@Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinExperiencia(GuiGraphics g, int x, CallbackInfo ci) {
		if (TrajeHud.activo()) ci.cancel();
	}

	@Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinNivel(GuiGraphics g, DeltaTracker tiempo, CallbackInfo ci) {
		if (TrajeHud.activo()) ci.cancel();
	}
}
