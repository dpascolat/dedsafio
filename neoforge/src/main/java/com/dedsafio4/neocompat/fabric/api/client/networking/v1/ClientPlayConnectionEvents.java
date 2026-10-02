package com.dedsafio4.neocompat.fabric.api.client.networking.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class ClientPlayConnectionEvents {
	private ClientPlayConnectionEvents() {}

	public interface Join {
		void onPlayReady(ClientPacketListener handler, com.dedsafio4.neocompat.fabric.api.networking.v1.PacketSender sender, Minecraft client);
	}

	public interface Disconnect {
		void onPlayDisconnect(ClientPacketListener handler, Minecraft client);
	}

	public static final Event<Join> JOIN = new Event<>();
	public static final Event<Disconnect> DISCONNECT = new Event<>();
}
