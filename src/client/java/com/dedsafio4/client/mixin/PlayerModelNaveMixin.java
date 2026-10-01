package com.dedsafio4.client.mixin;

import com.dedsafio4.despegue.NaveViajeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En la nave la cabeza del jugador queda derecha (mirando al cielo) aunque mueva la cámara. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelNaveMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void dedsafio4$cabezaQuieta(LivingEntity jugador, float limbSwing, float limbSwingAmount, float edad,
									   float giroCabeza, float inclinacionCabeza, CallbackInfo ci) {
		if (!(jugador.getVehicle() instanceof NaveViajeEntity)) return;
		HumanoidModel<?> modelo = (HumanoidModel<?>) (Object) this;
		modelo.head.xRot = 0f;
		modelo.head.yRot = 0f;
		modelo.head.zRot = 0f;
		modelo.hat.copyFrom(modelo.head);
	}
}
