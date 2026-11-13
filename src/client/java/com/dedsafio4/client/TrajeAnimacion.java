package com.dedsafio4.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La animación del Traje Espacial al apretar J: las piezas llegan volando y se encajan solas (primero las piernas,
 * después la pechera, los brazos y al final el casco). Al sacarlo, se van volando al revés. La dibuja TrajeLayer, y en
 * primera persona TrajeHud baja el visor sobre la pantalla.
 */
public final class TrajeAnimacion {
	private TrajeAnimacion() {}

	/** Cuánto tarda en ponerse (1,4 s) y en sacarse (0,7 s). */
	public static final long PONER_MS = 1400, SACAR_MS = 700;

	/** Cuándo empezó y si se estaba poniendo (true) o sacando (false). */
	private record Cambio(long desde, boolean poniendo) {}

	private static final Map<UUID, Cambio> CAMBIOS = new ConcurrentHashMap<>();

	public static void empezar(UUID jugador, boolean poniendo) {
		CAMBIOS.put(jugador, new Cambio(System.currentTimeMillis(), poniendo));
	}

	/**
	 * Cuánto está puesto el traje (0 = nada, 1 = entero), o -1 si no hay animación (está entero o no está).
	 * Mientras se saca va de 1 a 0.
	 */
	public static float avance(UUID jugador) {
		Cambio c = CAMBIOS.get(jugador);
		if (c == null) return -1;
		long pasaron = System.currentTimeMillis() - c.desde();
		long dura = c.poniendo() ? PONER_MS : SACAR_MS;
		if (pasaron >= dura) {
			CAMBIOS.remove(jugador);
			return -1;
		}
		float a = pasaron / (float) dura;
		return c.poniendo() ? a : 1 - a;
	}

	/** ¿Se está sacando ahora? (el traje ya no figura puesto, pero hay que dibujarlo yéndose) */
	public static boolean sacando(UUID jugador) {
		Cambio c = CAMBIOS.get(jugador);
		return c != null && !c.poniendo() && avance(jugador) >= 0;
	}

	/**
	 * Cuánto llegó una pieza que empieza a moverse en {@code desde} y termina de encajar en {@code hasta} (de 0 a 1),
	 * suavizado para que frene al llegar.
	 */
	public static float pieza(float avance, float desde, float hasta) {
		float q = Math.max(0, Math.min(1, (avance - desde) / (hasta - desde)));
		float r = 1 - q;
		return 1 - r * r * r;
	}
}
