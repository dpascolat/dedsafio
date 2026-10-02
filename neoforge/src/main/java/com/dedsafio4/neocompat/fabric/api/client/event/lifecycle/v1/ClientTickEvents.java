package com.dedsafio4.neocompat.fabric.api.client.event.lifecycle.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.client.Minecraft;

public final class ClientTickEvents {
	private ClientTickEvents() {}

	public interface StartTick {
		void onStartTick(Minecraft client);
	}

	public interface EndTick {
		void onEndTick(Minecraft client);
	}

	public static final Event<StartTick> START_CLIENT_TICK = new Event<>();
	public static final Event<EndTick> END_CLIENT_TICK = new Event<>();
}
