package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public interface HudRenderCallback {
	Event<HudRenderCallback> EVENT = new Event<>();

	void onHudRender(GuiGraphics g, DeltaTracker tiempo);
}
