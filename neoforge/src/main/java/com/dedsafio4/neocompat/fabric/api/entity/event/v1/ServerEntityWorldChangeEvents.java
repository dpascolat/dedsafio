package com.dedsafio4.neocompat.fabric.api.entity.event.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ServerEntityWorldChangeEvents {
	private ServerEntityWorldChangeEvents() {}

	public interface AfterPlayerChange {
		void afterChangeWorld(ServerPlayer jugador, ServerLevel antes, ServerLevel despues);
	}

	public static final Event<AfterPlayerChange> AFTER_PLAYER_CHANGE_WORLD = new Event<>();
}
