package com.dedsafio4.neocompat.fabric.api.event.lifecycle.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class ServerEntityEvents {
	private ServerEntityEvents() {}

	public interface Load {
		void onLoad(Entity entidad, ServerLevel mundo);
	}

	public static final Event<Load> ENTITY_LOAD = new Event<>();
}
