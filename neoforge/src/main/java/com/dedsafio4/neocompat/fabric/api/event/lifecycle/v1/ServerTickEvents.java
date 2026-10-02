package com.dedsafio4.neocompat.fabric.api.event.lifecycle.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class ServerTickEvents {
	private ServerTickEvents() {}

	public interface StartTick {
		void onStartTick(MinecraftServer server);
	}

	public interface EndTick {
		void onEndTick(MinecraftServer server);
	}

	public interface StartWorldTick {
		void onStartTick(ServerLevel mundo);
	}

	public interface EndWorldTick {
		void onEndTick(ServerLevel mundo);
	}

	public static final Event<StartTick> START_SERVER_TICK = new Event<>();
	public static final Event<EndTick> END_SERVER_TICK = new Event<>();
	public static final Event<StartWorldTick> START_WORLD_TICK = new Event<>();
	public static final Event<EndWorldTick> END_WORLD_TICK = new Event<>();
}
