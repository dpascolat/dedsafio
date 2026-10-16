package com.dedsafio4.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Con shift-click en el resultado, Minecraft toca el sonido del trato tomando al comerciante como un bicho. El Puesto
 * de Intercambio no es un bicho: eso daba error justo después de darte lo comprado y antes de cobrarte (lo daba
 * gratis). Si el comerciante no es un bicho (el Puesto), no suena nada.
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
	@Shadow
	@Final
	private Merchant trader;

	@Inject(method = "playTradeSound", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sonidoSinBicho(CallbackInfo ci) {
		if (this.trader.isClientSide() || this.trader instanceof Entity) return;
		ci.cancel();
	}
}
