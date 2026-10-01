package com.dedsafio4.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class EffectRenderingInventoryScreenMixin {
	/** Al costado del inventario se ven los efectos, menos el Rojizo, el Negro Puro y los del minimapa. */
	@org.spongepowered.asm.mixin.injection.Redirect(method = "renderEffects", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"))
	private java.util.Collection<net.minecraft.world.effect.MobEffectInstance> dedsafio4$efectosVisibles(net.minecraft.client.player.LocalPlayer jugador) {
		return com.dedsafio4.client.EfectosPantalla.visibles(jugador.getActiveEffects());
	}
}
