package com.dedsafio4.neocompat.fabric.api.client.message.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.network.chat.Component;

public final class ClientReceiveMessageEvents {
	private ClientReceiveMessageEvents() {}

	public interface Game {
		void onReceiveGameMessage(Component mensaje, boolean arriba);
	}

	public static final Event<Game> GAME = new Event<>();
}
