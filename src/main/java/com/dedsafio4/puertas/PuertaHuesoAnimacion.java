package com.dedsafio4.puertas;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La reja de huesos (sólo en el cliente), como en el diseño "Puerta cárcel de hueso": 7 × 7 bloques de hueso, con los
 * postes girados 45° (se tocan por las puntas) entre una fila de bloques derechos abajo y otra arriba. Las 3 columnas
 * del medio son la puerta (gira sobre x = -24). Medidas en píxeles (16 = un bloque), y arriba, el frente +z.
 * <ul>
 * <li>Abrir/cerrar: la puerta gira suave hasta 0,6π.</li>
 * <li>Embestida 1 y 2: cada bloque se abomba hacia donde empujó (un resorte que se apaga), más en el medio.</li>
 * <li>Embestida 3: se desarma: cada bloque sale volando (primero los de la puerta, después los de los costados y por
 * último la fila de arriba), cae, rebota y queda acostado.</li>
 * </ul>
 */
public class PuertaHuesoAnimacion {
	public static final float B = 16, LADO_POSTE = (float) (16 / Math.sqrt(2));
	private static final float GRAVEDAD = 9.8f * 16, PUERTA_X0 = -24;

	/** Un bloque de la reja: su centro, tamaño, si es poste (girado 45°) o viga, su fila y si es de la puerta. */
	public static final class Bloque {
		public final float cx, cy, cz, w, h, d;
		public final boolean poste, puerta;
		public final int fila;

		Bloque(float cx, float cy, float cz, float w, float h, float d, boolean poste, boolean puerta, int fila) {
			this.cx = cx; this.cy = cy; this.cz = cz; this.w = w; this.h = h; this.d = d;
			this.poste = poste; this.puerta = puerta; this.fila = fila;
		}
	}

	public static final List<Bloque> BLOQUES = new ArrayList<>();

	static {
		seccion(-56, 2, false);
		seccion(-24, 3, true);
		seccion(24, 2, false);
	}

	private static void seccion(float x0, int n, boolean puerta) {
		for (int i = 0; i < n; i++) {
			float x = x0 + i * B + B / 2;
			BLOQUES.add(new Bloque(x, B / 2, 0, B, B, B, false, puerta, 0));
			BLOQUES.add(new Bloque(x, 6 * B + B / 2, 0, B, B, B, false, puerta, 6));
			for (int r = 1; r < 6; r++) BLOQUES.add(new Bloque(x, r * B + B / 2, 0, LADO_POSTE, B, LADO_POSTE, true, puerta, r));
		}
	}

	private static final float APERTURA = Mth.PI * 0.6f;

	private float apertura, aperturaAntes;
	private int golpeTicks = -1, golpes;
	private boolean desarmada;
	private float t;
	/** Por bloque: dónde está, cómo se mueve y cuánto falta para que se suelte. */
	private final float[][] estado = new float[BLOQUES.size()][];
	private final boolean[] suelto = new boolean[BLOQUES.size()], quieto = new boolean[BLOQUES.size()];

	public void golpe(int n, int lado) {
		if (n <= 0) return;
		if (n < PuertaHuesoEntity.EMBESTIDAS) {
			golpeTicks = 0;
			golpes = n;
			return;
		}
		if (desarmada) return;
		desarmada = true;
		golpeTicks = 0;
		t = 0;
		Random azar = new Random(1234);
		for (int i = 0; i < BLOQUES.size(); i++) {
			Bloque b = BLOQUES.get(i);
			float cerca = 1 - Math.min(1, Math.abs(b.cx) / 56f);
			float adelante = b.puerta ? 3.2f : 0.6f + 2.4f * cerca;
			float espera = b.puerta ? 0 : 0.6f * Math.min(1, (Math.abs(b.cx) - 24) / 32f);
			espera += 0.03f * b.fila + (b.fila == 6 ? 0.35f : 0);
			// x, y, z, rx, rz (posición en px y giro), vx, vy, vz, wx, wz (velocidades), espera, y los del tick anterior.
			float[] e = new float[16];
			e[5] = (b.cx / 56f) * (0.6f + azar.nextFloat() * 0.8f) * B;
			e[6] = (0.8f + azar.nextFloat() * 1.6f) * B;
			e[7] = adelante * lado * B;
			e[8] = (azar.nextFloat() - 0.5f) * 8 + 3 * lado;
			e[9] = (azar.nextFloat() - 0.5f) * 8;
			e[10] = espera;
			estado[i] = e;
		}
	}

	public void tick(boolean abierta) {
		aperturaAntes = apertura;
		apertura += ((abierta ? 1 : 0) - apertura) * 0.3f;
		if (golpeTicks >= 0 && ++golpeTicks > 60) golpeTicks = -1;
		if (!desarmada) return;
		for (float[] e : estado) {
			e[11] = e[0]; e[12] = e[1]; e[13] = e[2]; e[14] = e[3]; e[15] = e[4];
		}
		for (int k = 0; k < 10; k++) paso(0.005f);
	}

	private void paso(float dt) {
		t += dt;
		for (int i = 0; i < BLOQUES.size(); i++) {
			Bloque b = BLOQUES.get(i);
			float[] e = estado[i];
			if (t < e[10] || quieto[i]) continue;
			suelto[i] = true;
			e[6] -= GRAVEDAD * dt;
			e[0] += e[5] * dt;
			e[1] += e[6] * dt;
			e[2] += e[7] * dt;
			e[3] += e[8] * dt;
			e[4] += e[9] * dt;
			// El piso: rebota un poco, frena y al final queda acostado (girado de a 90°).
			float base = b.cy + e[1] - b.h / 2;
			if (base < 0) {
				e[1] -= base;
				if (Math.abs(e[6]) < 0.5f * B) {
					quieto[i] = true;
					e[3] = Math.round(e[3] / (Mth.PI / 2)) * (Mth.PI / 2);
					e[4] = Math.round(e[4] / (Mth.PI / 2)) * (Mth.PI / 2);
					continue;
				}
				e[6] = -e[6] * 0.3f;
				e[5] *= 0.55f;
				e[7] *= 0.55f;
				e[8] *= 0.5f;
				e[9] *= 0.5f;
			}
		}
	}

	/** Cuánto está abierta la puerta (radianes). */
	public float apertura(float parcial) {
		return Mth.lerp(parcial, aperturaAntes, apertura) * APERTURA;
	}

	/** Lo que se abomba ese bloque por un golpe (px hacia donde empujó). */
	public float abombado(Bloque b, float parcial) {
		if (golpeTicks < 0 || desarmada) return 0;
		float x = (golpeTicks + parcial) / 20f;
		float resorte = (golpes + 1) * 3.2f * (float) Math.exp(-5 * x) * Mth.cos(24 * x);
		float peso = (float) Math.exp(-(b.cx * b.cx) / (2 * 30 * 30)) * (float) Math.exp(-((b.cy - 45) * (b.cy - 45)) / (2 * 40 * 40));
		return resorte * peso * (b.poste ? 1 : 0.35f);
	}

	/** Cuánto tiembla toda la reja (px). */
	public float temblor(float parcial) {
		if (golpeTicks < 0) return 0;
		float s = (golpeTicks + parcial) / 20f;
		return (desarmada ? 2.5f : 1.2f) * (float) Math.exp(-6 * s);
	}

	public boolean desarmada() {
		return desarmada;
	}

	/** Del bloque i ya suelto: cuánto se movió (px) y giró (x, z), en ese momento. Null si sigue en su lugar. */
	public float[] desarme(int i, float parcial) {
		if (!desarmada || !suelto[i]) return null;
		float[] e = estado[i];
		return new float[]{Mth.lerp(parcial, e[11], e[0]), Mth.lerp(parcial, e[12], e[1]), Mth.lerp(parcial, e[13], e[2]),
				Mth.lerp(parcial, e[14], e[3]), Mth.lerp(parcial, e[15], e[4])};
	}

	public static float puertaX0() {
		return PUERTA_X0;
	}
}
