package com.dedsafio4.mixin;

import com.dedsafio4.misiones.Misiones;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	/** Se llama cuando un jugador saca un ítem de la mesa de crafteo (también con shift-click). */
	@Inject(method = "onCraftedBy", at = @At("TAIL"))
	private void dedsafio4$misionesDeFabricar(Level level, Player jugador, int cantidad, CallbackInfo ci) {
		if (jugador instanceof ServerPlayer servidor) {
			Misiones.alFabricar(servidor, (ItemStack) (Object) this);
		}
	}
}
