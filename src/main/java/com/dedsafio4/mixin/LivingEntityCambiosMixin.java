package com.dedsafio4.mixin;

import com.dedsafio4.cambios.Cambios;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Los cambios de dificultad que van adentro de LivingEntity (el resto está en CambiosDificultad). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityCambiosMixin {
	/** invisibilidad 1: la invisibilidad no esconde de los mobs. */
	@Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$sinInvisibilidad(Entity quienMira, CallbackInfoReturnable<Double> cir) {
		LivingEntity yo = (LivingEntity) (Object) this;
		if (yo.isInvisible() && yo.level() instanceof ServerLevel mundo && Cambios.nivel(mundo.getServer(), Cambios.INVISIBILIDAD) >= 1) {
			cir.setReturnValue(1.0);
		}
	}

	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float dedsafio4$danioCambios(float danio, DamageSource fuente) {
		return com.dedsafio4.cambios.CambiosDificultad.danioJugador((LivingEntity) (Object) this, fuente, danio);
	}

	/** manzanas 1: comer no da Absorción (solo las Manzanas de Oro la dan). */
	@Redirect(method = "addEatEffect", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
	private boolean dedsafio4$sinAbsorcion(LivingEntity yo, MobEffectInstance efecto) {
		if (efecto.is(MobEffects.ABSORPTION) && yo.level() instanceof ServerLevel mundo && Cambios.nivel(mundo.getServer(), Cambios.MANZANAS) >= 1) return false;
		return yo.addEffect(efecto);
	}
}
