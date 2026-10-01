package com.dedsafio4.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Para poder cargar un Creeper sin que le caiga un rayo. */
@Mixin(Creeper.class)
public interface CreeperAccessor {
	@Accessor("DATA_IS_POWERED")
	static EntityDataAccessor<Boolean> dedsafio4$powered() {
		throw new AssertionError();
	}
}
