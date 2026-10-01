package com.dedsafio4;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reemplaza los mensajes amarillos de entrada y salida por
 * "(icono) X se ha conectado." en verde y "(icono) X se ha desconectado." en gris.
 */
public final class MensajesConexion {
	private MensajesConexion() {}

	private static final ResourceLocation FUENTE_ICONOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos");
	private static final String ICONO_CONECTADO = "";

	public static void registrar() {
		ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) ->
				!(message.getContents() instanceof TranslatableContents t
						&& (t.getKey().startsWith("multiplayer.player.joined") || t.getKey().equals("multiplayer.player.left"))));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				server.getPlayerList().broadcastSystemMessage(
						mensaje(handler.player, " se ha conectado.", ChatFormatting.GREEN), false));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				server.getPlayerList().broadcastSystemMessage(
						mensaje(handler.player, " se ha desconectado.", ChatFormatting.GRAY), false));
	}

	private static Component mensaje(ServerPlayer jugador, String accion, ChatFormatting color) {
		return Component.empty()
				.append(Component.literal(ICONO_CONECTADO).withStyle(Style.EMPTY.withFont(FUENTE_ICONOS)))
				.append(Component.literal(" " + jugador.getGameProfile().getName() + accion).withStyle(color));
	}
}
