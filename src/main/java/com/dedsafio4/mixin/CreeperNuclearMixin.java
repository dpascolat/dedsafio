package com.dedsafio4.mixin;

import com.dedsafio4.bestias.CreeperNuclearEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cuando el Creeper Nuclear explota, además de la explosión sale su onda de 40 bloques (ver CreeperNuclearEntity.onda).
 * El Creeper de Papel no explota: hace su papeleo (CreeperPapelEntity.papeleo).
 */
@Mixin(Creeper.class)
public abstract class CreeperNuclearMixin {
	@Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$onda(CallbackInfo ci) {
		if ((Object) this instanceof com.dedsafio4.bestias.CreeperPapelEntity papel && papel.level() instanceof ServerLevel m) {
			papel.papeleo(m);
			ci.cancel();
			return;
		}
		if ((Object) this instanceof CreeperNuclearEntity nuclear && nuclear.level() instanceof ServerLevel mundo) nuclear.onda(mundo);
	}
}
