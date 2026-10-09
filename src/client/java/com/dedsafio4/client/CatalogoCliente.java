package com.dedsafio4.client;

import com.dedsafio4.catalogo.Catalogo;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** La tecla G abre el Catálogo, y el servidor avisa qué ítems están ocultos. */
public final class CatalogoCliente {
	private CatalogoCliente() {}

	public static final KeyMapping ABRIR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.dedsafio4.catalogo", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.dedsafio4"));

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (ABRIR.consumeClick()) {
				if (mc.screen == null && mc.player != null) mc.setScreen(new CatalogoScreen());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio4.catalogo.Misiones.Payload.TYPE, (payload, context) -> {
			int[] p = new int[payload.progreso().size()];
			for (int i = 0; i < p.length; i++) p[i] = payload.progreso().get(i);
			com.dedsafio4.catalogo.Misiones.PROGRESO_CLIENTE = p;
		});
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio4.catalogo.Misiones.ListaPayload.TYPE, (payload, context) ->
				com.dedsafio4.catalogo.Misiones.LISTA_CLIENTE = List.copyOf(payload.misiones()));
		ClientPlayNetworking.registerGlobalReceiver(Catalogo.Payload.TYPE, (payload, context) -> {
			Catalogo.OCULTOS_CLIENTE.clear();
			Catalogo.OCULTOS_CLIENTE.addAll(payload.ocultos());
			Catalogo.recibirDiseno(payload.celdas());
			Catalogo.PUEDE_EDITAR_CLIENTE = payload.puedeEditar();
		});
	}
}
