package com.dedsafio4.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** /dropsmob quitar: el mob, al morir, no suelta ese objeto de su drop de siempre (ver DropsMob.frenar). */
@Mixin(Entity.class)
public abstract class EntityDropsMixin {
	@Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
			at = @At("HEAD"), cancellable = true)
	private void dedsafio4$frenarDrop(ItemStack pila, float altura, CallbackInfoReturnable<ItemEntity> cir) {
		if (com.dedsafio4.bestias.DropsMob.frenar((Entity) (Object) this, pila)) cir.setReturnValue(null);
	}
}
