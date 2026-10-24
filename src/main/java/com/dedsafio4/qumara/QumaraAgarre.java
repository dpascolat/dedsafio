package com.dedsafio4.qumara;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * El "Atrapar" y "Sacar": dónde va el jugador agarrado en cada momento. Sigue al jugador de muestra de las
 * animaciones del modelo (atrapar, atrapar_sostener y sacar; las tablas salen de qumara_a_modelo.py), así queda
 * justo en la mano del brazo izquierdo. El servidor lo usa para mover al jugador. (La pinza y el alcance son para
 * saber si el brazo llega a alguien.)
 *
 * Todo en el espacio del cuerpo de Qumara (+Z = el frente, y = 0 el piso). Tiempos en segundos: 0–3,6 la
 * animación atrapar (el brazo va, lo agarra hacia los 1,6 s y lo sube delante de la cara); después lo sostiene
 * (atrapar_sostener, con el gas y el minijuego); al sacar, la animación sacar lo revolea y cae a los 1,35 s, y
 * queda tirado en una nube de gas.
 */
public final class QumaraAgarre {
	private QumaraAgarre() {}

	/** El brazo apunta al frente con este giro. */
	public static final float A0 = -Mth.PI / 2;
	/** Desde cuándo lo sostiene quieto (y empieza el minijuego), y cuánto tarda todo el "Sacar". */
	public static final float SOSTIENE = 3.6f, MINIJUEGO = 3.6f, SUELTA = SOSTIENE + 1.35f, FIN = SUELTA + 4.6f;
	/** Dónde cae el que tira (en el cuerpo, donde lo deja la animación sacar). */
	public static final Vec3 ATERRIZA = new Vec3(-26.035, 0, 23.409);
	/** Cuándo lo agarra la mano (antes se queda donde estaba; un momento después ya va con la mano). */
	private static final float AGARRA = 1.55f, AGARRADO = 1.75f;

	/** Dónde va el jugador (los pies) en la animación atrapar, cada 0,05 s (0 a 3,6), y en sacar (0 a 1,35). */
	private static final float[][] ATRAPAR = {
			{-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f},
			{-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f}, {-1.888f, 0.0f, 9.483f},
			{-1.888f, 0.0f, 9.511f}, {-1.888f, 0.0f, 9.574f}, {-1.888f, 0.0f, 9.667f}, {-1.888f, 0.0f, 9.786f}, {-1.888f, 0.0f, 9.925f}, {-1.888f, 0.0f, 10.079f},
			{-1.888f, 0.0f, 10.247f}, {-1.888f, 0.0f, 10.415f}, {-1.888f, 0.0f, 10.583f}, {-1.888f, 0.0f, 10.75f}, {-1.888f, 0.0f, 10.907f}, {-1.888f, 0.0f, 11.049f},
			{-1.888f, 0.0f, 11.171f}, {-1.888f, 0.0f, 11.268f}, {-1.888f, 0.0f, 11.337f}, {-1.888f, 0.0f, 11.37f}, {-1.888f, 0.0f, 11.371f}, {-1.888f, 0.0f, 11.371f},
			{-1.888f, 0.0f, 11.372f}, {-1.888f, 0.0f, 11.373f}, {-1.867f, 0.0f, 11.353f}, {-1.686f, 0.0f, 11.124f}, {-1.383f, 0.0f, 10.86f}, {-1.372f, 0.0f, 10.804f},
			{-1.362f, 0.0f, 10.804f}, {-1.362f, 0.0f, 10.804f}, {-1.362f, 0.0f, 10.804f}, {-1.362f, 0.0f, 10.804f}, {-1.362f, 0.0f, 10.804f}, {-1.371f, 0.0f, 10.911f},
			{-1.41f, 0.0f, 11.421f}, {-1.476f, 0.0f, 12.282f}, {-1.56f, 0.0f, 13.397f}, {-1.655f, 0.0f, 14.664f}, {-1.75f, 0.0f, 15.973f}, {-1.836f, 0.0f, 17.222f},
			{-1.899f, 1.898f, 18.32f}, {-1.93f, 4.058f, 19.196f}, {-1.918f, 6.284f, 19.811f}, {-1.855f, 8.485f, 20.15f}, {-1.735f, 10.573f, 20.233f}, {-1.553f, 12.47f, 20.105f},
			{-1.306f, 14.111f, 19.825f}, {-0.99f, 15.447f, 19.466f}, {-0.597f, 16.442f, 19.1f}, {-0.115f, 17.068f, 18.792f}, {0.43f, 17.32f, 18.586f}, {0.662f, 17.393f, 18.506f},
			{0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.506f},
			{0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.506f}, {0.662f, 17.393f, 18.505f},
			{0.662f, 17.393f, 18.505f},
	};
	private static final float[][] SACAR = {
			{0.662f, 17.393f, 18.505f}, {0.018f, 17.167f, 18.674f}, {-1.793f, 16.552f, 19.001f}, {-4.578f, 15.637f, 19.113f}, {-8.03f, 14.5f, 18.615f}, {-11.697f, 13.203f, 17.253f},
			{-15.06f, 11.809f, 15.04f}, {-17.703f, 10.397f, 12.272f}, {-19.447f, 9.076f, 9.43f}, {-20.379f, 7.988f, 7.022f}, {-20.827f, 7.164f, 5.184f}, {-20.892f, 7.28f, 5.409f},
			{-21.213f, 7.778f, 6.534f}, {-21.535f, 8.146f, 7.659f}, {-21.856f, 8.383f, 8.784f}, {-22.177f, 8.491f, 9.909f}, {-22.499f, 8.468f, 11.034f}, {-22.82f, 8.315f, 12.159f},
			{-23.142f, 8.032f, 13.284f}, {-23.463f, 7.619f, 14.409f}, {-23.785f, 7.075f, 15.534f}, {-24.106f, 6.401f, 16.659f}, {-24.428f, 5.597f, 17.784f}, {-24.749f, 4.663f, 18.909f},
			{-25.071f, 3.599f, 20.034f}, {-25.392f, 2.404f, 21.159f}, {-25.713f, 1.08f, 22.284f}, {-26.035f, 0.0f, 23.409f},
	};

	private static Vec3 tabla(float[][] t, float seg) {
		float i = Mth.clamp(seg / 0.05f, 0, t.length - 1);
		int a = (int) i, b = Math.min(a + 1, t.length - 1);
		float k = i - a;
		return new Vec3(Mth.lerp(k, t[a][0], t[b][0]), Mth.lerp(k, t[a][1], t[b][1]), Mth.lerp(k, t[a][2], t[b][2]));
	}
	/** Punto 13 bloques delante de la boca, a donde lo sube. */
	private static final Vec3 CARA = new Vec3(0, 23, 16);

	/** Pose del brazo: extensión (escala de todo el brazo), giro y curva de cada segmento. */
	public record Pose(float e, float yaw, float c) {}

	/** Dónde queda la pinza (el punto que agarra) con esa pose. */
	public static Vec3 pinza(float e, float yaw, float c) {
		double cos = Math.cos(c), sin = Math.sin(c);
		double qx = 3.7 + 2.6, qy = 0;
		for (int i = 6; i >= 0; i--) {
			double nx = qx * cos - qy * sin, ny = qx * sin + qy * cos;
			qx = nx + (i >= 1 ? 3.7 : 0);
			qy = ny;
		}
		qx += 3;   // la base del brazo sale del costado del tallo
		double x = qx * e, y = qy * e;
		return new Vec3(x * Math.cos(yaw), 6 + y, -x * Math.sin(yaw));
	}

	public static Vec3 pinza(Pose p) {
		return pinza(p.e(), p.yaw(), p.c());
	}

	/** La pose (con ese giro) que deja la pinza lo más cerca posible del punto. */
	public static Pose buscar(Vec3 punto, float yaw) {
		Pose mejor = new Pose(1, yaw, 0);
		double d = Double.MAX_VALUE;
		for (float c = -0.4f; c <= 0.7f; c += 0.01f) {
			for (float e = 0.15f; e <= 1.001f; e += 0.01f) {
				double dd = pinza(e, yaw, c).distanceToSqr(punto);
				if (dd < d) {
					d = dd;
					mejor = new Pose(e, yaw, c);
				}
			}
		}
		return mejor;
	}

	/** La pose para agarrar a alguien parado en ese lugar (los pies, en el cuerpo): la pinza a la altura del pecho. */
	public static Pose alcance(Vec3 pies) {
		float yaw = (float) Math.atan2(-pies.z, pies.x);
		return buscar(pies.add(0, 1.5, 0), yaw);
	}

	private static Pose cara;

	/** Delante de la cara (siempre la misma). */
	public static Pose cara() {
		if (cara == null) cara = buscar(CARA, A0);
		return cara;
	}

	private static float suave(float x) {
		return QumaraEntity.suave(x);
	}

	/** El tiempo del diseño: mientras lo sostiene se queda en 5,3; al sacar sigue desde ahí. */
	public static float tiempo(float tAgarre, float tSacar) {
		return tSacar >= 0 ? SOSTIENE + tSacar : Math.min(tAgarre, SOSTIENE);
	}

	/** El giro más corto de a hacia b. */
	private static float girar(float a, float b, float k) {
		return a + Mth.wrapDegrees((b - a) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * k;
	}

	/** La pose del brazo en ese momento (e = 0: escondido). */
	public static Pose brazo(float g, Pose alcance) {
		Pose cara = cara();
		if (g < 1.2f) {
			float k = suave(g / 1.2f);
			return new Pose(k * alcance.e(), alcance.yaw(), Mth.lerp(k, 0.85f, alcance.c()));
		}
		if (g < 1.6f) return alcance;
		if (g < 3.2f) {
			float k = suave((g - 1.6f) / 1.6f);
			return new Pose(Mth.lerp(k, alcance.e(), cara.e()), girar(alcance.yaw(), A0, k), Mth.lerp(k, alcance.c(), cara.c()));
		}
		if (g < 5.4f) return cara;
		if (g < 6.0f) {
			float k = suave((g - 5.4f) / 0.6f);
			return new Pose(Mth.lerp(k, cara.e(), 1), A0 + 0.9f * Mth.sin(k * Mth.PI) - 0.5f * k, Mth.lerp(k, cara.c(), 0.1f));
		}
		float k = suave((g - 6.0f) / 1.6f);
		return new Pose(1 - k, A0 - 0.5f * (1 - k), Mth.lerp(k, 0.1f, 0.85f));
	}

	/**
	 * Dónde van los pies del agarrado (en el cuerpo). inicio: donde estaba al agarrarlo; aterriza: donde cae
	 * (y = la altura del piso ahí).
	 */
	public static Vec3 pies(float g, Pose alcance, Vec3 inicio, Vec3 aterriza) {
		if (g < AGARRA) return inicio;
		if (g <= SOSTIENE) {
			Vec3 mano = tabla(ATRAPAR, g);
			return g < AGARRADO ? inicio.lerp(mano, suave((g - AGARRA) / (AGARRADO - AGARRA))) : mano;
		}
		float s = g - SOSTIENE;
		if (s < SUELTA - SOSTIENE) {
			// Lo revolea; al final baja hasta la altura del piso donde cae.
			Vec3 p = tabla(SACAR, s);
			return p.add(0, aterriza.y * suave(s / (SUELTA - SOSTIENE)), 0);
		}
		return aterriza;
	}
}
