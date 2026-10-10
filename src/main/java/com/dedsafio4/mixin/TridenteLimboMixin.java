package com.dedsafio4.mixin;

import com.dedsafio4.dimension.Limbo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fuera del Limbo, pegar con el tridente no hace nada (el tridente solo sirve en el Limbo). */
@Mixin(Player.class)
public abstract class TridenteLimboMixin {
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$tridenteSoloEnElLimbo(Entity objetivo, CallbackInfo ci) {
		Player jugador = (Player) (Object) this;
		if (jugador.getMainHandItem().is(Items.TRIDENT) && !jugador.level().dimension().equals(Limbo.DIMENSION)) ci.cancel();
	}
}
