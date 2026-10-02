package com.dedsafio4.neocompat.fabric.api.message.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class ServerMessageEvents {
	private ServerMessageEvents() {}

	public interface AllowGameMessage {
		boolean allowGameMessage(MinecraftServer server, Component mensaje, boolean arriba);
	}

	public static final Event<AllowGameMessage> ALLOW_GAME_MESSAGE = new Event<>();
}
