package com.dedsafio4.mixin;

import com.dedsafio4.qumara.QumaraEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** El que tiene agarrado Qumara no se puede bajar con shift. */
@Mixin(Player.class)
public abstract class PlayerAgarreMixin {
	@Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$noSeBaja(CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player) (Object) this;
		if (self.getVehicle() instanceof QumaraEntity q && q.agarradoId() == self.getId()) cir.setReturnValue(false);
	}
}
