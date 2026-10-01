package com.dedsafio4.mixin;

import com.dedsafio4.cambios.Cambios;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
	/** Cambio "golem 1": poner la calabaza (o la lámpara) ya no crea Gólems de Hierro ni de Nieve. */
	@Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinGolems(Level level, BlockPos pos, CallbackInfo ci) {
		if (level.getServer() != null && Cambios.nivel(level.getServer(), Cambios.GOLEM) >= 1) {
			ci.cancel();
		}
	}
}
