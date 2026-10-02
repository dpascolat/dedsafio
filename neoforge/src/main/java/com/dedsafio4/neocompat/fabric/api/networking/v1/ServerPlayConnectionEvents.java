package com.dedsafio4.neocompat.fabric.api.networking.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public final class ServerPlayConnectionEvents {
	private ServerPlayConnectionEvents() {}

	public interface Join {
		void onPlayReady(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server);
	}

	public interface Disconnect {
		void onPlayDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server);
	}

	public static final Event<Join> JOIN = new Event<>();
	public static final Event<Disconnect> DISCONNECT = new Event<>();
}
