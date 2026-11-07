package com.dedsafio4.client.mixin;

import com.dedsafio4.client.EscopetaCliente;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Con la Escopeta Phora en la mano, la mira de Minecraft se cambia por la de la escopeta (ver EscopetaCliente.mira). */
@Mixin(Gui.class)
abstract class MiraEscopetaMixin {
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$miraEscopeta(GuiGraphics g, DeltaTracker tiempo, CallbackInfo ci) {
		if (EscopetaCliente.mira(g, tiempo.getGameTimeDeltaPartialTick(false))) ci.cancel();
	}
}
