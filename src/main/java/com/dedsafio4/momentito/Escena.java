package com.dedsafio4.momentito;

import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Las cuentas de la escena del hacker (/momentito 1) que necesitan los dos lados: el cliente para dibujarla y
 * mover la cámara, y el servidor para cargar los chunks donde mira la cámara. Todo es en función del tiempo
 * (0 a 30 s) y de las medidas del campo de fuerza c = {largo (z), ancho (x), arriba, abajo}, en las medidas de
 * la escena (el héroe en el origen, y para arriba).
 */
public final class Escena {
	private Escena() {}

	public static final float TB0 = 8.5f, TB1 = 12f;
	/** La nave sale de la grieta del cielo rojo a los 2,5 s y baja hasta flotar a los 6 s. */
	public static final float SALE_GRIETA = 2.5f;

	public static float clamp(float x) {
		return Math.max(0, Math.min(1, x));
	}

	public static float seg(float t, float a, float b) {
		return clamp((t - a) / (b - a));
	}

	public static float sm(float s) {
		return s * s * (3 - 2 * s);
	}

	public static float lerp(float a, float b, float s) {
		return a + (b - a) * s;
	}

	public static float azar(float i, float k) {
		double x = Math.sin(i * 127.1 + k * 311.7) * 43758.5453;
		return (float) (x - Math.floor(x));
	}

	/** "Radio" del campo: la mitad de lo más largo (largo o ancho). */
	public static float radio(float[] c) {
		return Math.max(c[0], c[1]) / 2;
	}

	/** Altura a la que flota la nave: siempre arriba del campo de fuerza. */
	public static float alturaNave(float[] c) {
		return Math.max(40, c[2] + 30);
	}

	/** Donde la bomba choca contra el campo de fuerza: arriba del todo. */
	public static float alturaImpacto(float[] c) {
		return c[2] + 0.5f;
	}

	/** Dónde está la nave (sale de la grieta, baja, flota y al final sube al cielo). */
	public static Vector3f posNave(float t, float[] c) {
		float h = alturaNave(c);
		float llega = 1 - (float) Math.pow(1 - seg(t, SALE_GRIETA, 6), 3), sube = seg(t, 17, 21);
		return new Vector3f(10 * sube * sube,
				lerp(h + 600, h, llega) + Mth.sin(t * 1.6f) * 0.15f + (h + 400) * (float) Math.pow(sube, 2.2), 0);
	}

	/** Dónde está la bomba mientras cae. */
	public static Vector3f posBomba(float t, float[] c) {
		float cae = seg(t, TB0, TB1);
		float temblor = azar((float) Math.floor(t * 12), 9) > 0.8f ? 0.06f : 0;
		return new Vector3f(temblor, lerp(alturaNave(c) - 0.6f, alturaImpacto(c), cae * cae), 0);
	}

	/**
	 * La toma de cámara de cada momento (como el "director" del diseño): {x, y, z} de la cámara y {x, y, z}
	 * del punto al que mira.
	 */
	public static float[] toma(float t, float[] c) {
		float fr = radio(c);
		float w = Math.max(fr, 6), k = Math.max(1, Math.min(fr, c[2] * 2) / 12), imp = alturaImpacto(c), h = alturaNave(c);
		Vector3f n = posNave(t, c);
		// Primero se ve la grieta abriéndose arriba, después la nave saliendo de ella y bajando.
		if (t < SALE_GRIETA) return new float[]{6, 1.5f, 10, 0.5f, 200, 0.5f};
		if (t < 4.5f) return new float[]{6, 1.5f, 10, n.x, n.y, n.z};
		if (t < 6) return new float[]{4.5f, 1.2f, 7, 0, 2.2f + 0.8f * seg(t, 3, 4.5f), 0};
		if (t < 8.5f) return new float[]{n.x + 7, n.y + 2.5f, n.z + 11, n.x, n.y + 0.5f, n.z};
		if (t < 10) {
			Vector3f b = posBomba(t, c);
			return new float[]{b.x + 6, b.y + 2, b.z + 10, b.x, b.y, b.z};
		}
		if (t < 11.2f) return new float[]{3.2f, 1.6f, 4.8f, 0, 1.4f, 0};
		if (t < 12) {
			float e = sm(seg(t, 11.2f, 11.95f));
			return new float[]{lerp(3.2f, w * 1.3f, e), lerp(1.6f, w * 0.45f, e), lerp(4.8f, w * 1.9f, e), 0, lerp(1.4f, w * 0.45f, e), 0};
		}
		if (t < 14.5f) return new float[]{14 * k, imp + 5 * k, 22 * k, 0, imp - 2 * k, 0};
		if (t < 17) return new float[]{n.x + 7, n.y + 2.5f, n.z + 11, n.x, n.y + 0.5f, n.z};
		if (t < 21) return new float[]{h * 0.15f + 14, h - 6, 26, n.x, n.y, n.z};
		if (t < 24.5f) return new float[]{2.6f, 0.5f, 4.2f, 0, 2.6f, 0};
		if (t < 26.5f) return new float[]{w * 1.3f, w * 0.45f, w * 1.9f, 0, w * 0.3f, 0};
		return new float[]{12, 5, 16.5f, 0, 3.6f, 0};
	}

	/** Pasa un punto de la escena al mundo (la escena está girada para que el héroe mire al que la empezó). */
	public static double[] alMundo(double ex, double ey, double ez, float giro, float x, float y, float z) {
		Vector3f v = new Vector3f(x, y, z).rotateY((180 - giro) * Mth.DEG_TO_RAD);
		return new double[]{ex + v.x, ey + v.y, ez + v.z};
	}
}
