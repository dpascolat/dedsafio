package com.dedsafio4.mixin;

import com.dedsafio4.bestias.MutacionFantasma;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Con /mutacion pantome: cuando un fantasma le pega a un jugador, le mezcla el inventario (ver MutacionFantasma). */
@Mixin(Mob.class)
public abstract class MobGolpeFantasmaMixin {
	@Inject(method = "doHurtTarget", at = @At("RETURN"))
	private void dedsafio4$mezclarInventario(Entity objetivo, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && (Object) this instanceof Phantom && objetivo instanceof ServerPlayer jugador) {
			MutacionFantasma.alPegar(jugador);
		}
	}
}
