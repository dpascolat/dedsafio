package com.dedsafio4.neocompat.fabric.api.client.keybinding.v1;

import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.List;

public final class KeyBindingHelper {
	private KeyBindingHelper() {}

	public static final List<KeyMapping> TECLAS = new ArrayList<>();

	public static KeyMapping registerKeyBinding(KeyMapping tecla) {
		TECLAS.add(tecla);
		return tecla;
	}
}
