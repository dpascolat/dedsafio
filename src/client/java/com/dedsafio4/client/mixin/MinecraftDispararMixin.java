package com.dedsafio4.client.mixin;

import com.dedsafio4.client.EscopetaCliente;
import com.dedsafio4.items.ModItems;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Con la Escopeta Phora en la mano, el clic izquierdo dispara (no pega ni rompe bloques). */
@Mixin(Minecraft.class)
abstract class MinecraftDispararMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$disparar(CallbackInfoReturnable<Boolean> cir) {
		if (EscopetaCliente.clicIzquierdo()) cir.setReturnValue(false);
	}

	/** Apuntando (usando el ítem), Minecraft tira los clics izquierdos: acá se toman antes para poder disparar. */
	@Inject(method = "handleKeybinds", at = @At("HEAD"))
	private void dedsafio4$dispararApuntando(CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;
		if (mc.player == null || !mc.player.isUsingItem() || !(mc.player.getUseItem().getItem() instanceof com.dedsafio4.items.EscopetaPhoraItem)) return;
		while (mc.options.keyAttack.consumeClick()) EscopetaCliente.clicIzquierdo();
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$noRomper(boolean apretado, CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;
		if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof com.dedsafio4.items.EscopetaPhoraItem) ci.cancel();
	}
}
