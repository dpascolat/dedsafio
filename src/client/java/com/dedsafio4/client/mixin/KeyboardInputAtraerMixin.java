package com.dedsafio4.client.mixin;

import com.dedsafio4.client.qumara.AtraerCliente;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mientras Qumara te atrae y tenés el espacio apretado, no podés caminar (la cámara sí). */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputAtraerMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void dedsafio4$quietoResistiendo(boolean agachado, float velocidadAgachado, CallbackInfo ci) {
		if (!AtraerCliente.sinCaminar()) return;
		Input entrada = (Input) (Object) this;
		entrada.forwardImpulse = 0;
		entrada.leftImpulse = 0;
		entrada.up = entrada.down = entrada.left = entrada.right = false;
	}
}
