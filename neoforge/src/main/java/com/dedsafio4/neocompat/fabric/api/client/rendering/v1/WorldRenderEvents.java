package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;

public final class WorldRenderEvents {
	private WorldRenderEvents() {}

	public interface Last {
		void onLast(WorldRenderContext contexto);
	}

	public interface AfterTranslucent {
		void afterTranslucent(WorldRenderContext contexto);
	}

	public interface AfterEntities {
		void afterEntities(WorldRenderContext contexto);
	}

	public static final Event<Last> LAST = new Event<>();
	public static final Event<AfterTranslucent> AFTER_TRANSLUCENT = new Event<>();
	public static final Event<AfterEntities> AFTER_ENTITIES = new Event<>();
}
