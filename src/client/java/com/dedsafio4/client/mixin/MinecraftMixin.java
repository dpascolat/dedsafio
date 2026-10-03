package com.dedsafio4.client.mixin;

import com.dedsafio4.client.XaeroBloqueo;
import com.dedsafio4.client.menu.MenuPrincipalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** Los menús de Xaero's Minimap (waypoints, ajustes...) no se abren para los que no son admin. */
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$bloquearMenusXaero(Screen pantalla, CallbackInfo ci) {
		if (XaeroBloqueo.esDeXaero(pantalla) && XaeroBloqueo.bloqueado()) ci.cancel();
	}

	/** La pausa (Esc) de Minecraft se cambia por la del Dedsafío (salvo después de M+Q; con F3+Esc no hay menú). */
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$pausa(Screen pantalla, CallbackInfo ci) {
		if (pantalla instanceof net.minecraft.client.gui.screens.PauseScreen pausa && pausa.showsPauseMenu()
				&& !MenuPrincipalScreen.modoNormal) {
			ci.cancel();
			((Minecraft) (Object) this).setScreen(new com.dedsafio4.client.menu.PausaScreen());
		}
	}

	/** El menú principal de Minecraft se cambia por el del Dedsafío (salvo después de M+Q). */
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$menuPrincipal(Screen pantalla, CallbackInfo ci) {
		if (pantalla != null && pantalla.getClass() == TitleScreen.class && !MenuPrincipalScreen.modoNormal) {
			ci.cancel();
			((Minecraft) (Object) this).setScreen(new MenuPrincipalScreen());
		}
	}
}
