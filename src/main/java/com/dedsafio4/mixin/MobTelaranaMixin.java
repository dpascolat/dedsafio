package com.dedsafio4.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** aranas_telas 1: las arañas dejan una telaraña al golpearte (ver CambiosDificultad). */
@Mixin(Mob.class)
public abstract class MobTelaranaMixin {
	@Inject(method = "doHurtTarget", at = @At("RETURN"))
	private void dedsafio4$telarana(Entity objetivo, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && (Object) this instanceof Spider arana && objetivo instanceof LivingEntity vivo) {
			com.dedsafio4.cambios.CambiosDificultad.aranaPego(arana, vivo);
		}
	}
}
