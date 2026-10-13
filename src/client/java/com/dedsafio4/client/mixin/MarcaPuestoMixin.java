package com.dedsafio4.client.mixin;

import com.dedsafio4.intercambio.ModIntercambio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Como con la Barrera: en creativo, con el Puesto de Intercambio en la mano, se ve su marca donde hay uno. */
@Mixin(ClientLevel.class)
public abstract class MarcaPuestoMixin {
	@Inject(method = "getMarkerParticleTarget", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$marcaPuesto(CallbackInfoReturnable<Block> cir) {
		if (cir.getReturnValue() != null) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.CREATIVE
				&& mc.player.getMainHandItem().is(ModIntercambio.PUESTO_ITEM)) {
			cir.setReturnValue(ModIntercambio.PUESTO);
		}
	}
}
