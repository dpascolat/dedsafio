package com.dedsafio4.neocompat.fabric.api.event.lifecycle.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.server.MinecraftServer;

public final class ServerLifecycleEvents {
	private ServerLifecycleEvents() {}

	public interface ServerStarting {
		void onServerStarting(MinecraftServer server);
	}

	public interface ServerStopped {
		void onServerStopped(MinecraftServer server);
	}

	/** Antes de cargar los mundos (ServerAboutToStartEvent). */
	public static final Event<ServerStarting> SERVER_STARTING = new Event<>();
	/** Con el servidor ya apagado y los mundos guardados (ServerStoppedEvent). */
	public static final Event<ServerStopped> SERVER_STOPPED = new Event<>();
}
