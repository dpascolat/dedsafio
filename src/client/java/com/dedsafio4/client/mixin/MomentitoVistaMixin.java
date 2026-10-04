package com.dedsafio4.client.mixin;

import com.dedsafio4.client.momentito.MomentitoCamara;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Durante /momentito 1 se ve mucho más lejos (el campo de fuerza puede medir 1500 bloques). */
@Mixin(GameRenderer.class)
public abstract class MomentitoVistaMixin {
	@Inject(method = "getDepthFar", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$verLejos(CallbackInfoReturnable<Float> cir) {
		if (MomentitoCamara.activa()) cir.setReturnValue(20000f);
	}
}
