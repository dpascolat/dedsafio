package com.dedsafio4.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft no deja que la vida máxima pase de 1024. Qumara con "/boss 1 gente N" necesita más (N × 1000), así que
 * para la vida máxima el tope sube a un millón.
 */
@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {
	private static final double TOPE_VIDA = 1_000_000;

	@Shadow @Final private double minValue;

	@Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$masVida(double valor, CallbackInfoReturnable<Double> cir) {
		if ((Object) this != Attributes.MAX_HEALTH.value()) return;
		cir.setReturnValue(Double.isNaN(valor) ? minValue : Mth.clamp(valor, minValue, TOPE_VIDA));
	}
}
