package com.dedsafio4.client.mixin;

import com.dedsafio4.client.qumara.AtraerCliente;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mientras Qumara te atrae no podés saltar (el espacio sirve para resistirte). */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySaltoMixin {
	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinSalto(CallbackInfo ci) {
		if ((Object) this == Minecraft.getInstance().player && AtraerCliente.atraido()) ci.cancel();
	}
}
