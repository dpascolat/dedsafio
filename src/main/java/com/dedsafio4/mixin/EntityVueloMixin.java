package com.dedsafio4.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** /velocidad en un mob que vuela: va más rápido (o más lento) por el aire (ver AjustesMob.factorVuelo). */
@Mixin(Entity.class)
public abstract class EntityVueloMixin {
	@ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
	private Vec3 dedsafio4$vuelo(Vec3 movimiento, MoverType tipo) {
		if (tipo != MoverType.SELF || !((Object) this instanceof LivingEntity mob)) return movimiento;
		double factor = com.dedsafio4.bestias.AjustesMob.factorVuelo(mob);
		return factor == 1 ? movimiento : movimiento.scale(factor);
	}
}
