package com.dedsafio4.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** /dañomob en un creeper: su explosión hace ese daño justo (ver AjustesMob); y la armadura del Limbo (ver ArmaduraLimbo). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAjustesMixin {
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float dedsafio4$danoExplosion(float danio, DamageSource fuente) {
		return com.dedsafio4.bestias.AjustesMob.danoExplosion((LivingEntity) (Object) this, fuente, danio);
	}

	/** La armadura del Limbo: menos daño de las criaturas del Limbo (y con el set completo, dentro del Limbo). */
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float dedsafio4$armaduraLimbo(float danio, DamageSource fuente) {
		return com.dedsafio4.items.ArmaduraLimbo.reducir((LivingEntity) (Object) this, fuente, danio);
	}
}
