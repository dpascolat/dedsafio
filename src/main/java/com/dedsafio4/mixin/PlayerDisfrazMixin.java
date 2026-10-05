package com.dedsafio4.mixin;

import com.dedsafio4.disfraz.Disfraces;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** El jugador transformado con /cambiarmob tiene el tamaño del mob (y los ojos a su altura). */
@Mixin(Player.class)
public abstract class PlayerDisfrazMixin {
	@Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$tamanoDelMob(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (pose != Pose.STANDING && pose != Pose.CROUCHING) return;
		EntityType<?> tipo = Disfraces.de((Player) (Object) this);
		if (tipo == null) return;
		EntityDimensions mob = tipo.getDimensions();
		cir.setReturnValue(pose == Pose.CROUCHING ? mob.scale(1f, 0.9f) : mob);
	}
}
