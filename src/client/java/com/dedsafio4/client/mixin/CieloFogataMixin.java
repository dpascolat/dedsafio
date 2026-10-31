package com.dedsafio4.client.mixin;

import com.dedsafio4.client.fogata.FogataRitual;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Durante el ritual de la Fogata el cielo se oscurece y salen las estrellas. */
@Mixin(ClientLevel.class)
abstract class CieloFogataMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$oscuro(Vec3 pos, float parcial, CallbackInfoReturnable<Vec3> cir) {
		float oscuro = FogataRitual.a(parcial);
		if (oscuro > 0) cir.setReturnValue(cir.getReturnValue().scale(1 - oscuro));
	}

	@Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$estrellas(float parcial, CallbackInfoReturnable<Float> cir) {
		float estrellas = FogataRitual.b(parcial);
		if (estrellas > cir.getReturnValueF()) cir.setReturnValue(estrellas);
	}
}
