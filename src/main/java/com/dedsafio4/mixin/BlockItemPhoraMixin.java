package com.dedsafio4.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cuando un jugador pone un bloque en Phora, se anota el lugar: ese bloque sí se puede romper después. */
@Mixin(BlockItem.class)
public abstract class BlockItemPhoraMixin {
	@Inject(method = "placeBlock", at = @At("RETURN"))
	private void dedsafio4$anotarEnPhora(BlockPlaceContext contexto, BlockState estado, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && contexto.getPlayer() != null) {
			com.dedsafio4.dimension.Phora.alPoner(contexto.getLevel(), contexto.getClickedPos(), estado);
		}
	}
}
