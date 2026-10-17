package com.dedsafio4.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * La protección del spawn no deja que los que no son OP toquen bloques cerca del spawn. Los bloques del mod que son
 * para usar (la Entrega de Misiones, el Bloque de Baneados, el Puesto de Intercambio, el Mensajero, la Vitrina, el
 * Cajero y la Bóveda) se pueden usar igual, sin OP. Romper o poner bloques sigue protegido.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class UsoSinOpMixin {
	@WrapOperation(method = "handleUseItemOn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;)Z"))
	private boolean dedsafio4$usoSinOp(ServerLevel level, Player jugador, BlockPos pos, Operation<Boolean> original) {
		if (original.call(level, jugador, pos)) return true;
		Block bloque = level.getBlockState(pos).getBlock();
		return bloque instanceof com.dedsafio4.bloques.EntregaMisionBlock
				|| bloque == com.dedsafio4.baneos.Baneos.BLOQUE
				|| bloque == com.dedsafio4.intercambio.ModIntercambio.PUESTO
				|| bloque instanceof com.dedsafio4.bloques.MensajeroBlock
				|| bloque instanceof com.dedsafio4.subastas.VitrinaBlock
				|| bloque instanceof com.dedsafio4.banco.CajeroBlock
				|| bloque instanceof com.dedsafio4.boveda.BovedaBlock;
	}
}
