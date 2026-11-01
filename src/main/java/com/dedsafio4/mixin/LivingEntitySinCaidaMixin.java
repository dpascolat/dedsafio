package com.dedsafio4.mixin;

import com.dedsafio4.salidalimbo.SalidaLimbo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** El que llega por la Salida del Limbo no recibe daño de caída hasta tocar el piso. */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySinCaidaMixin {
	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinCaida(float distancia, float multiplicador, DamageSource fuente, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ServerPlayer jugador && SalidaLimbo.sinCaida(jugador)) cir.setReturnValue(false);
	}
}
