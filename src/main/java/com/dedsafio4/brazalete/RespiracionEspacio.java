package com.dedsafio4.brazalete;

import com.dedsafio4.cambios.Cambios;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * En el Espacio (el Interdimensional y el de la nave) no hay aire: sin el Traje Espacial puesto (la J del Brazalete) se
 * pierde la respiración como bajo el agua con /cambio respiracion 1 (5 veces más rápido) y, sin aire, el daño por
 * ahogarse es 10 veces más. Con el traje se respira normal. Yendo en la nave (montado en algo) tampoco pasa nada.
 */
public final class RespiracionEspacio {
	private RespiracionEspacio() {}

	/** Aire que se pierde por tick (bajo el agua normal es 1) y el daño de cada golpe de ahogo (normal 2, acá x10). */
	private static final int AIRE_POR_TICK = 5;
	private static final float DANIO = 2f * 10;

	/** El aire de cada jugador que se está ahogando en el Espacio (Minecraft lo recupera solo fuera del agua). */
	private static final Map<UUID, Integer> AIRE = new HashMap<>();

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				if (!sinAire(jugador)) {
					AIRE.remove(jugador.getUUID());
					continue;
				}
				int aire = AIRE.getOrDefault(jugador.getUUID(), jugador.getAirSupply()) - AIRE_POR_TICK;
				if (aire <= -20) {
					aire = 0;
					// Si /cambio respiracion 1 está puesto, ese ya multiplica el ahogo x10: no se multiplica dos veces.
					float danio = Cambios.nivel(server, Cambios.RESPIRACION) >= 1 ? DANIO / 10 : DANIO;
					jugador.hurt(jugador.damageSources().drown(), danio);
				}
				AIRE.put(jugador.getUUID(), aire);
				jugador.setAirSupply(aire);
			}
		});
	}

	/** ¿Se está quedando sin aire? En el Espacio, sin el traje, sin ir montado y sin estar en creativo. */
	private static boolean sinAire(ServerPlayer jugador) {
		return Brazalete.enElEspacio(jugador) && !TrajeEspacial.puesto(jugador) && !jugador.isPassenger()
				&& !jugador.isCreative() && !jugador.isSpectator() && jugador.isAlive();
	}
}
