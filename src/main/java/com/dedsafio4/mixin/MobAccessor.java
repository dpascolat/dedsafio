package com.dedsafio4.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Para cambiarle la navegación a un mob (los zombis que escalan usan la de las arañas). */
@Mixin(Mob.class)
public interface MobAccessor {
	@Mutable
	@Accessor("navigation")
	void dedsafio4$setNavigation(PathNavigation navegacion);
}
