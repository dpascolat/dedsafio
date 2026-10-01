package com.dedsafio4.client.mixin;

import com.dedsafio4.client.XaeroBloqueo;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
	/** Las teclas de Xaero's Minimap no responden para los que no son admin. */
	@Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$bloquearClick(CallbackInfoReturnable<Boolean> cir) {
		if (XaeroBloqueo.esDeXaero((KeyMapping) (Object) this) && XaeroBloqueo.bloqueado()) cir.setReturnValue(false);
	}

	@Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$bloquearApretada(CallbackInfoReturnable<Boolean> cir) {
		if (XaeroBloqueo.esDeXaero((KeyMapping) (Object) this) && XaeroBloqueo.bloqueado()) cir.setReturnValue(false);
	}
}
