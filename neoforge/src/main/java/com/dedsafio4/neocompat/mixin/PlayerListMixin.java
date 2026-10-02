package com.dedsafio4.neocompat.mixin;

import com.dedsafio4.neocompat.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

/** ServerMessageEvents.ALLOW_GAME_MESSAGE de Fabric: un mensaje del juego para todos se puede frenar. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
	@Shadow
	@Final
	private MinecraftServer server;

	@Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$permitirMensaje(Component mensaje, Function<ServerPlayer, Component> porJugador, boolean arriba, CallbackInfo ci) {
		for (var o : ServerMessageEvents.ALLOW_GAME_MESSAGE.oyentes()) {
			if (!o.allowGameMessage(server, mensaje, arriba)) {
				ci.cancel();
				return;
			}
		}
	}
}
