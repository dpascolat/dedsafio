package com.dedsafio4.client.mixin;

import com.dedsafio4.client.EscopetaCliente;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Apuntando con la Escopeta Phora (clic derecho), la vista se acerca un poco. */
@Mixin(AbstractClientPlayer.class)
abstract class ZoomEscopetaMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$zoom(CallbackInfoReturnable<Float> cir) {
		if ((Object) this != net.minecraft.client.Minecraft.getInstance().player) return;
		float a = EscopetaCliente.apuntando(1f);
		if (a > 0) cir.setReturnValue(cir.getReturnValueF() * (1f - 0.18f * a));
	}
}
