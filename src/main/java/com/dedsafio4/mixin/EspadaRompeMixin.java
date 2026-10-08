package com.dedsafio4.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Con cualquier espada todos los bloques se rompen más rápido: tan rápido como con un pico (o hacha, o pala) del mismo
 * material (madera 2, piedra 4, hierro 6, diamante 8, netherite 9, oro 12). Lo que suelta el bloque no cambia: la piedra
 * sigue necesitando pico para soltar algo.
 */
@Mixin(ItemStack.class)
public abstract class EspadaRompeMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$espadaRapida(BlockState estado, CallbackInfoReturnable<Float> cir) {
		ItemStack pila = (ItemStack) (Object) this;
		if (!pila.is(net.minecraft.tags.ItemTags.SWORDS) || !(pila.getItem() instanceof TieredItem conMaterial)) return;
		float velocidad = conMaterial.getTier().getSpeed();
		if (velocidad > cir.getReturnValueF()) cir.setReturnValue(velocidad);
	}
}
