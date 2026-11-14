package com.dedsafio4.mixin;

import com.dedsafio4.items.ModItems;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Juntar dos Brazaletes en la mesa de crafteo no los arregla (su barra es el combustible). */
@Mixin(RepairItemRecipe.class)
public abstract class BrazaleteSinJuntarMixin {
	@Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinBrazalete(CraftingInput entrada, Level mundo, CallbackInfoReturnable<Boolean> cir) {
		for (int i = 0; i < entrada.size(); i++) {
			if (entrada.getItem(i).is(ModItems.BRAZALETE_INTERDIMENSIONAL)) {
				cir.setReturnValue(false);
				return;
			}
		}
	}
}
