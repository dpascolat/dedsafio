package com.dedsafio4.neocompat.fabric.api.entity.event.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.server.level.ServerPlayer;

public final class ServerPlayerEvents {
	private ServerPlayerEvents() {}

	public interface CopyFrom {
		void copyFromPlayer(ServerPlayer viejo, ServerPlayer nuevo, boolean vivo);
	}

	public interface AfterRespawn {
		void afterRespawn(ServerPlayer viejo, ServerPlayer nuevo, boolean vivo);
	}

	public static final Event<CopyFrom> COPY_FROM = new Event<>();
	public static final Event<AfterRespawn> AFTER_RESPAWN = new Event<>();
}
