package com.dedsafio4.neocompat.fabric.api.client.screen.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;
import java.util.WeakHashMap;

public final class ScreenKeyboardEvents {
	private ScreenKeyboardEvents() {}

	public interface AfterKeyPress {
		void afterKeyPress(Screen pantalla, int tecla, int codigo, int modificadores);
	}

	public static final Map<Screen, Event<AfterKeyPress>> DESPUES_DE_TECLA = new WeakHashMap<>();

	public static Event<AfterKeyPress> afterKeyPress(Screen pantalla) {
		return DESPUES_DE_TECLA.computeIfAbsent(pantalla, p -> new Event<>());
	}
}
