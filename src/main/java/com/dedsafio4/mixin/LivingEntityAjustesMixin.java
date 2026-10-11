package com.dedsafio4.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** /dañomob en un creeper: su explosión hace ese daño justo (ver AjustesMob). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAjustesMixin {
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float dedsafio4$danoExplosion(float danio, DamageSource fuente) {
		return com.dedsafio4.bestias.AjustesMob.danoExplosion((LivingEntity) (Object) this, fuente, danio);
	}
}
