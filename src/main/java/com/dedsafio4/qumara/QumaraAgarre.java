package com.dedsafio4.qumara;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * El "Atrapar" y "Sacar" del diseño: dónde está la pinza del brazo y dónde va el jugador agarrado en cada
 * momento. Lo usan el servidor (para mover al jugador) y el cliente (para dibujar el brazo), así los dos
 * calculan exactamente lo mismo.
 *
 * Todo en el espacio del cuerpo de Qumara (+Z = el frente, y = 0 el piso). Tiempos en segundos, con los
 * del diseño (gT): 0–1,2 el brazo sale hacia el jugador; 1,2–1,6 lo agarra; 1,6–3,2 lo sube delante de la
 * cara; desde 3 le tira gas y lo sostiene. Al sacar: 5,3–6 toma impulso, 6–7 el jugador vuela en parábola
 * y queda tirado con una nube de gas hasta los 11,2; el brazo se esconde entre 6 y 7,6.
 */
public final class QumaraAgarre {
	private QumaraAgarre() {}

	/** El brazo apunta al frente con este giro. */
	public static final float A0 = -Mth.PI / 2;
	/** Desde cuándo lo sostiene quieto (y empieza el minijuego), y cuánto tarda todo el "Sacar". */
	public static final float SOSTIENE = 5.3f, MINIJUEGO = 3.2f, SUELTA = 7.0f, FIN = 11.6f;
	/** Dónde cae el que tira (en el cuerpo, como en el diseño). */
	public static final Vec3 ATERRIZA = new Vec3(8, 0, 24);
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
		if (g < 1.2f) return inicio;
		if (g < 6.0f) {
			Vec3 mano = pinza(brazo(g, alcance)).subtract(0, 1.5, 0);
			return g < 1.6f ? inicio.lerp(mano, suave((g - 1.2f) / 0.4f)) : mano;
		}
		if (g < SUELTA) {
			// Vuela en parábola (gravedad 30) y cae en 1 segundo.
			Vec3 desde = pinza(brazo(6.0f, alcance)).subtract(0, 1.5, 0);
			float k = g - 6.0f;
			double vy = aterriza.y - desde.y + 15;
			return new Vec3(Mth.lerp(k, desde.x, aterriza.x), desde.y + vy * k - 15 * k * k, Mth.lerp(k, desde.z, aterriza.z));
		}
		return aterriza;
	}
}
