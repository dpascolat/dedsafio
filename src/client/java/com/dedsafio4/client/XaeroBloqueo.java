package com.dedsafio4.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Para los jugadores que no son admin, Xaero's Minimap queda bloqueado: no pueden usar sus teclas
 * (crear waypoints, waypoint instantáneo, agrandar el mapa...) ni abrir sus menús.
 * El mini mapa en sí lo esconde el servidor con el código de Xaero (ver BloqueoMinimapa).
 * Los admins (permiso 2, los que pueden usar comandos) lo usan normalmente.
 */
public final class XaeroBloqueo {
	private XaeroBloqueo() {}

	/** Lo manda el servidor: si los admins pueden usar el minimapa (si no, nadie). */
	public static volatile boolean adminsLoUsan = false;

	public static boolean bloqueado() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && !(adminsLoUsan && mc.player.hasPermissions(2));
	}

	/** Las del mapa grande (Xaero's World Map, la M) sí andan: solo se bloquea lo del mini mapa. */
	public static boolean esDeXaero(KeyMapping tecla) {
		if (tecla.getCategory() != null && tecla.getCategory().toLowerCase().contains("world map")) return false;
		return contieneXaero(tecla.getName()) || contieneXaero(tecla.getCategory());
	}

	public static boolean esDeXaero(Screen pantalla) {
		if (pantalla == null) return false;
		String clase = pantalla.getClass().getName();
		return clase.startsWith("xaero.") && !clase.startsWith("xaero.map.");
	}

	private static boolean contieneXaero(String texto) {
		return texto != null && texto.toLowerCase().contains("xaero");
	}
}
