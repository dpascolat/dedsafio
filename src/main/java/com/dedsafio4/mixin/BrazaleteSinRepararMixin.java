package com.dedsafio4.mixin;

import com.dedsafio4.items.ModItems;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * El Brazalete Interdimensional no se arregla en el yunque (juntando dos, ni de ninguna forma): su barra es el
 * combustible, y eso solo lo carga la Batería de Dilitio.
 */
@Mixin(AnvilMenu.class)
public abstract class BrazaleteSinRepararMixin {
	@Inject(method = "createResult", at = @At("TAIL"))
	private void dedsafio4$sinBrazalete(CallbackInfo ci) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		if (menu.getSlot(0).getItem().is(ModItems.BRAZALETE_INTERDIMENSIONAL) || menu.getSlot(1).getItem().is(ModItems.BRAZALETE_INTERDIMENSIONAL)) {
			menu.getSlot(2).set(net.minecraft.world.item.ItemStack.EMPTY);
		}
	}
}
