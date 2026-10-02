package com.dedsafio4.neocompat.mixin;

import com.dedsafio4.neocompat.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** PlayerBlockBreakEvents.AFTER de Fabric: después de que un jugador rompe un bloque. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
	@Shadow
	protected ServerLevel level;
	@Shadow
	@Final
	protected ServerPlayer player;

	@Unique
	private BlockState dedsafio4$estado;
	@Unique
	private BlockEntity dedsafio4$bloqueEntidad;

	@Inject(method = "destroyBlock", at = @At("HEAD"))
	private void dedsafio4$antes(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		dedsafio4$estado = level.getBlockState(pos);
		dedsafio4$bloqueEntidad = level.getBlockEntity(pos);
	}

	@Inject(method = "destroyBlock", at = @At("RETURN"))
	private void dedsafio4$despues(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		BlockState estado = dedsafio4$estado;
		BlockEntity bloqueEntidad = dedsafio4$bloqueEntidad;
		dedsafio4$estado = null;
		dedsafio4$bloqueEntidad = null;
		if (!cir.getReturnValueZ() || estado == null) return;
		for (var o : PlayerBlockBreakEvents.AFTER.oyentes()) o.afterBlockBreak(level, player, pos, estado, bloqueEntidad);
	}
}
