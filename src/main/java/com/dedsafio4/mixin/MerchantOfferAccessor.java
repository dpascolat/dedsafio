package com.dedsafio4.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Para que la demanda de los comercios del Aldeano Phora pueda bajar (Minecraft no trae cómo cambiarla). */
@Mixin(MerchantOffer.class)
public interface MerchantOfferAccessor {
	@Accessor("demand")
	void dedsafio4$setDemanda(int demanda);
}
