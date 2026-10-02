package com.dedsafio4.neocompat.fabric.api.client.screen.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;
import java.util.WeakHashMap;

/** Como en Fabric: los eventos de cada pantalla se borran cada vez que la pantalla se arma de nuevo. */
public final class ScreenEvents {
	private ScreenEvents() {}

	public interface BeforeInit {
		void beforeInit(Minecraft client, Screen pantalla, int ancho, int alto);
	}

	public interface AfterInit {
		void afterInit(Minecraft client, Screen pantalla, int ancho, int alto);
	}

	public interface AfterRender {
		void afterRender(Screen pantalla, GuiGraphics g, int mouseX, int mouseY, float parcial);
	}

	public static final Event<BeforeInit> BEFORE_INIT = new Event<>();
	public static final Event<AfterInit> AFTER_INIT = new Event<>();

	public static final Map<Screen, Event<AfterRender>> DESPUES_DE_DIBUJAR = new WeakHashMap<>();

	public static Event<AfterRender> afterRender(Screen pantalla) {
		return DESPUES_DE_DIBUJAR.computeIfAbsent(pantalla, p -> new Event<>());
	}
}
