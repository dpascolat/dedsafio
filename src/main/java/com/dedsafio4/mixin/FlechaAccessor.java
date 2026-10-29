package com.dedsafio4.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Para que el Arco de Huesos Grande le dé a sus flechas el traspaso (como la Perforación de la ballesta). */
@Mixin(AbstractArrow.class)
public interface FlechaAccessor {
	@Invoker("setPierceLevel")
	void dedsafio4$setPierceLevel(byte nivel);
}
