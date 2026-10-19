package com.dedsafio4.client.mixin;

import com.dedsafio4.client.despegue.TunelPantalla;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mientras se ve el túnel de /tptp la pantalla queda negra del todo: el chat no se dibuja encima. */
@Mixin(ChatComponent.class)
public abstract class ChatTunelMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinChat(CallbackInfo ci) {
		if (TunelPantalla.activo()) ci.cancel();
	}
}
