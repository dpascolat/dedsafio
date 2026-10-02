package com.dedsafio4.neocompat.mixin;

import com.dedsafio4.cambios.Cambios;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Versión NeoForge de "enderperlas 1" (en Fabric: ThrownEnderpearlMixin#golpeSinArmadura): el golpe ignora la armadura. */
@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlNeoMixin {
	@ModifyArg(method = "onHit", require = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 0)
	private DamageSource dedsafio4$golpeSinArmaduraNeo(DamageSource fuente) {
		Level level = ((ThrownEnderpearl) (Object) this).level();
		if (level.getServer() != null && Cambios.nivel(level.getServer(), Cambios.ENDERPERLAS) >= 1) return Cambios.danioEnderperla(level);
		return fuente;
	}
}
