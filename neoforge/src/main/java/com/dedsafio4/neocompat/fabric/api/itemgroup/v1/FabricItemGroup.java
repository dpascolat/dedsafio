package com.dedsafio4.neocompat.fabric.api.itemgroup.v1;

import net.minecraft.world.item.CreativeModeTab;

public final class FabricItemGroup {
	private FabricItemGroup() {}

	public static CreativeModeTab.Builder builder() {
		return CreativeModeTab.builder();
	}
}
