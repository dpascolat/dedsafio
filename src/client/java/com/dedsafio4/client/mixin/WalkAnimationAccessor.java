package com.dedsafio4.client.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Para copiar cómo camina el jugador al mob de /cambiarmob. */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationAccessor {
	@Accessor("speedOld")
	float dedsafio4$getSpeedOld();

	@Accessor("speedOld")
	void dedsafio4$setSpeedOld(float valor);

	@Accessor("speed")
	float dedsafio4$getSpeed();

	@Accessor("speed")
	void dedsafio4$setSpeed(float valor);

	@Accessor("position")
	float dedsafio4$getPosition();

	@Accessor("position")
	void dedsafio4$setPosition(float valor);
}
