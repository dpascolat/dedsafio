package com.dedsafio4.client.mixin;

import com.dedsafio4.baneos.ItemsBaneados;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En la barra de abajo, la casilla de un ítem baneado (/banear item) también tiene el fondo rojizo. */
@Mixin(Gui.class)
public abstract class HotbarBaneadaMixin {
	@Inject(method = "renderSlot", at = @At("HEAD"))
	private void dedsafio4$fondoRojizo(GuiGraphics g, int x, int y, DeltaTracker tiempo, Player jugador, ItemStack pila, int semilla,
									  CallbackInfo ci) {
		if (!pila.isEmpty() && ItemsBaneados.CLIENTE.contains(pila.getItem())) {
			g.fill(x, y, x + 16, y + 16, ItemsBaneados.ROJIZO);
		}
	}
}
