package com.dedsafio4.mixin;

import com.dedsafio4.candados.Candados;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.ChestBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Un cofre nuevo no se junta con uno que tiene candado (si no, el candado quedaba en la otra mitad). */
@Mixin(ChestBlock.class)
public class ChestBlockMixin {
	@Inject(method = "candidatePartnerFacing", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$noJuntarConCandado(BlockPlaceContext contexto, Direction direccion,
											  CallbackInfoReturnable<Direction> cir) {
		if (cir.getReturnValue() == null) return;
		if (Candados.tieneCandado(contexto.getLevel(), contexto.getClickedPos().relative(direccion))) {
			cir.setReturnValue(null);
		}
	}
}
