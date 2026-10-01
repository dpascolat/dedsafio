package com.dedsafio4.client.mixin;

import com.dedsafio4.client.menu.MenuPrincipalScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Las listas de los menús (opciones de video, controles...) tampoco llevan textura de fondo: todo negro. */
@Mixin(AbstractSelectionList.class)
public abstract class ListaFondoMixin {
	@Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$listaNegra(GuiGraphics g, CallbackInfo ci) {
		if (MenuPrincipalScreen.fondoNegro()) ci.cancel();
	}
}
