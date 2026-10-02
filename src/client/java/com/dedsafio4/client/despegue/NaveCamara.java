package com.dedsafio4.client.despegue;

import com.dedsafio4.despegue.NaveViajeEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Al subirte a la nave (y otra vez cuando despega) la cámara queda mirando para arriba, al cielo, como el personaje
 * acostado. Después la podés mover libre.
 */
public final class NaveCamara {
	private NaveCamara() {}

	private static NaveViajeEntity naveAntes;
	private static int estadoAntes = -1;

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			NaveViajeEntity nave = mc.player != null && mc.player.getVehicle() instanceof NaveViajeEntity n ? n : null;
			int estado = nave == null ? -1 : nave.estado();
			boolean recienSubido = nave != null && nave != naveAntes;
			boolean despega = nave != null && estado == NaveViajeEntity.DESPEGANDO && estadoAntes != NaveViajeEntity.DESPEGANDO;
			if (recienSubido || despega) mirarArriba(mc);
			naveAntes = nave;
			estadoAntes = estado;
		});
	}

	private static void mirarArriba(Minecraft mc) {
		mc.player.setXRot(-90f);
		mc.player.xRotO = -90f;
	}
}
