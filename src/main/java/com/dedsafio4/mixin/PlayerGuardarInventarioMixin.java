package com.dedsafio4.mixin;

import com.dedsafio4.items.FrutaGuardianaItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Con la Fruta Guardiana comida, al morir no se caen las cosas ni la experiencia (como el keepInventory). */
@Mixin(Player.class)
public abstract class PlayerGuardarInventarioMixin {
	@Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$noTirarCosas(CallbackInfo ci) {
		if (FrutaGuardianaItem.protegido((Player) (Object) this)) ci.cancel();
	}

	@Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$noTirarExperiencia(CallbackInfoReturnable<Integer> cir) {
		if (FrutaGuardianaItem.protegido((Player) (Object) this)) cir.setReturnValue(0);
	}
}
