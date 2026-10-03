package com.dedsafio4.client.bulag;

import com.dedsafio4.bulag.Bulag;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.Set;
import java.util.UUID;

/** Bulag 2 en el cliente: quién tiene el cráneo y la pantalla de cuentas. */
public final class BulagCliente {
	private BulagCliente() {}

	private static Set<UUID> craneos = Set.of();

	public static boolean tieneCraneo(UUID jugador) {
		return craneos.contains(jugador);
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(Bulag.CraneosPayload.TYPE, (payload, context) ->
				craneos = Set.copyOf(payload.jugadores()));
		ClientPlayNetworking.registerGlobalReceiver(Bulag.PantallaPayload.TYPE, (payload, context) -> {
			Minecraft mc = context.client();
			if (!payload.abierta()) {
				if (mc.screen instanceof BulagScreen) mc.setScreen(null);
			} else if (mc.screen instanceof BulagScreen pantalla) {
				pantalla.actualizar(payload);
			} else {
				mc.setScreen(new BulagScreen(payload));
			}
		});
	}
}
