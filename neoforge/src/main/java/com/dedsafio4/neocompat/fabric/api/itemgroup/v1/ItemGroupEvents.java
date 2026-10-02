package com.dedsafio4.neocompat.fabric.api.itemgroup.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import java.util.HashMap;
import java.util.Map;

public final class ItemGroupEvents {
	private ItemGroupEvents() {}

	public interface ModifyEntries {
		void modifyEntries(FabricItemGroupEntries entradas);
	}

	public static final Map<ResourceKey<CreativeModeTab>, Event<ModifyEntries>> POR_PESTANIA = new HashMap<>();

	public static Event<ModifyEntries> modifyEntriesEvent(ResourceKey<CreativeModeTab> pestania) {
		return POR_PESTANIA.computeIfAbsent(pestania, k -> new Event<>());
	}
}
