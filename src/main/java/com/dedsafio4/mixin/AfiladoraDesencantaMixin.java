package com.dedsafio4.mixin;

import com.dedsafio4.items.PerdicionEspada;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lo que sale de la afiladora queda marcado, para que las espadas con Perdición no se vuelvan a encantar solas. */
@Mixin(GrindstoneMenu.class)
public class AfiladoraDesencantaMixin {
	@Inject(method = "removeNonCursesFrom", at = @At("RETURN"))
	private void dedsafio4$marcar(ItemStack pila, CallbackInfoReturnable<ItemStack> cir) {
		PerdicionEspada.marcarDesencantada(cir.getReturnValue());
	}
}
