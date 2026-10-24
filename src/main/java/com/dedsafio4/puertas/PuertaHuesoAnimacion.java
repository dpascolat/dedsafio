package com.dedsafio4.puertas;

import net.minecraft.util.Mth;

/**
 * Cómo se mueven las hojas de la Puerta de Hueso (sólo en el cliente), como en el diseño "¡Embestida!":
 * <ul>
 * <li>Abrir/cerrar: las hojas giran suave hasta abiertas (casi 100°) o cerradas.</li>
 * <li>Embestida 1 y 2: las hojas se abomban un poco hacia donde empujó (un golpe que se apaga) y todo tiembla.</li>
 * <li>Embestida 3: revientan. La hoja b sale disparada, rebota contra la pared y queda abierta; la hoja a se abre,
 * se arranca de las bisagras y cae acostada al piso (rebota una vez).</li>
 * </ul>
 * Los ángulos son en radianes; positivo = hacia donde empujó el Triceratops.
 */
public class PuertaHuesoAnimacion {
	private static final float APERTURA = Mth.PI * 0.55f;

	/** Ticks desde el último golpe (o -1) y qué tan fuerte fue. */
	private int golpeTicks = -1;
	private float fuerzaGolpe;

	private float apertura, aperturaAntes;

	/** Después de reventar: tiempo (segundos), ángulo y velocidad de cada hoja, y la caída de la hoja a. */
	private boolean reventada;
	private float t, a, va, b, vb, caida, vcaida;
	private float aAntes, bAntes, caidaAntes;
	private boolean rebotoCaida;

	public void golpe(int golpes) {
		if (golpes <= 0) return;
		if (golpes < PuertaHuesoEntity.EMBESTIDAS) {
			golpeTicks = 0;
			fuerzaGolpe = golpes == 1 ? 0.10f : 0.16f;
			return;
		}
		if (reventada) return;
		reventada = true;
		golpeTicks = 0;
		fuerzaGolpe = 0;
		t = 0;
		a = b = caida = 0;
		va = 14;
		vb = 16;
	}

	public void tick(boolean abierta) {
		aperturaAntes = apertura;
		apertura += ((abierta ? 1 : 0) - apertura) * 0.3f;
		if (golpeTicks >= 0 && ++golpeTicks > 40) golpeTicks = -1;
		if (!reventada) return;
		aAntes = a;
		bAntes = b;
		caidaAntes = caida;
		for (int i = 0; i < 10; i++) paso(0.005f);
	}

	private void paso(float dt) {
		t += dt;
		// Hoja b: un resorte hacia 1,62 rad con tope en 1,85 (la pared): rebota.
		vb += (18 * (1.62f - b) - 2.2f * vb) * dt;
		b += vb * dt;
		if (b > 1.85f) {
			b = 1.85f;
			vb = -0.38f * vb;
		}
		// Hoja a: se abre hasta 1 rad y a los 0,3 segundos se arranca y cae al piso.
		if (t < 0.3f) {
			va += (10 * (1.0f - a) - va) * dt;
			a += va * dt;
			if (a > 1.25f) {
				a = 1.25f;
				va = 0;
			}
		} else if (caida < Mth.HALF_PI || vcaida > 0) {
			vcaida += (14 * Mth.cos(caida) + 3) * dt;
			caida += vcaida * dt;
			if (caida >= Mth.HALF_PI) {
				caida = Mth.HALF_PI;
				vcaida = rebotoCaida ? 0 : -0.22f * vcaida;
				rebotoCaida = true;
			}
		}
	}

	/** Cuánto está abierta (0 a 1, para girar las hojas). */
	public float apertura(float parcial) {
		return Mth.lerp(parcial, aperturaAntes, apertura) * APERTURA;
	}

	/** Lo que se abomba cada hoja por un golpe (la b un poco menos). */
	public float abombado(float parcial) {
		if (golpeTicks < 0 || fuerzaGolpe == 0) return 0;
		float s = (golpeTicks + parcial) / 20f;
		if (s > 0.28f) return 0;
		return fuerzaGolpe * Mth.sin(Mth.PI * s / 0.28f) * (float) Math.exp(-6 * s);
	}

	/** Cuánto tiembla todo (en bloques, para mover la puerta un poquito al azar). */
	public float temblor(float parcial) {
		if (golpeTicks < 0) return 0;
		float s = (golpeTicks + parcial) / 20f;
		return (reventada ? 0.12f : 0.06f) * (float) Math.exp(-5 * s);
	}

	public boolean reventada() {
		return reventada;
	}

	public float hojaA(float parcial) {
		return Mth.lerp(parcial, aAntes, a);
	}

	public float hojaB(float parcial) {
		return Mth.lerp(parcial, bAntes, b);
	}

	/** Cuánto cayó la hoja a (0: parada, π/2: acostada en el piso). */
	public float caidaA(float parcial) {
		return Mth.lerp(parcial, caidaAntes, caida);
	}
}
