package com.dedsafio4.mixin;

import com.dedsafio4.cambios.Cambios;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * enderperlas 1: el golpe de caer al teletransportarse con una Perla del End ignora la armadura
 * y sus encantamientos (usa un tipo de daño propio del mod en lugar del daño de caída).
 */
@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {
	@WrapOperation(method = "onHit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/damagesource/DamageSources;fall()Lnet/minecraft/world/damagesource/DamageSource;"))
	private DamageSource dedsafio4$golpeSinArmadura(DamageSources fuentes, Operation<DamageSource> original) {
		Level level = ((ThrownEnderpearl) (Object) this).level();
		if (level.getServer() != null && Cambios.nivel(level.getServer(), Cambios.ENDERPERLAS) >= 1) {
			return Cambios.danioEnderperla(level);
		}
		return original.call(fuentes);
	}

	/** enderperlas 2: el golpe al teletransportarse pasa de 5 a 10. */
	@ModifyArg(method = "onHit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
			index = 1)
	private float dedsafio4$golpeMasFuerte(float danio) {
		Level level = ((ThrownEnderpearl) (Object) this).level();
		if (level.getServer() != null && Cambios.nivel(level.getServer(), Cambios.ENDERPERLAS) >= 2) return 10f;
		return danio;
	}
}
