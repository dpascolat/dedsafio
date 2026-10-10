package com.dedsafio4.mixin;

import com.dedsafio4.items.RecetaTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Los ítems de RecetaTooltip.conReceta() muestran su receta en la descripción. */
@Mixin(Item.class)
public abstract class RecetaEnDescripcionMixin {
	@Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$receta(ItemStack pila, CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		if (cir.getReturnValue().isEmpty() && RecetaTooltip.conReceta().contains((Item) (Object) this)) {
			cir.setReturnValue(Optional.of(new RecetaTooltip((Item) (Object) this)));
		}
	}
}
