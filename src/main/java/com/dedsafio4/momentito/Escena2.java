package com.dedsafio4.momentito;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.dedsafio4.momentito.Escena.clamp;
import static com.dedsafio4.momentito.Escena.lerp;
import static com.dedsafio4.momentito.Escena.seg;
import static com.dedsafio4.momentito.Escena.sm;

/**
 * Las cuentas de /momentito 2 ("La escalera", 60 s), como en Escalera.html: el héroe sube una escalera de
 * piedra de 13 de ancho y 59 de alto (que construye el jugador), se detiene junto a los 5 compañeros caídos
 * (se arrodilla junto al tercero), arriba encuentra un esqueleto con el Cristal del Desierto, se agacha
 * llorando, lo agarra y lo levanta. Medidas en bloques; el héroe empieza 6 bloques antes del primer escalón
 * (z = 0) y la escalera sube hacia +z.
 */
public final class Escena2 {
	private Escena2() {}

	public static final float T = 60, P = 1 / 16f, STAIR_H = 59, SKZ = STAIR_H + 4.6f;

	/** Los compañeros caídos: x, z, lado (hacia dónde están acostados), y cuánto abren brazos y piernas. */
	public static final float[][] COMP = {{4, 9.25f, 1, 1}, {-4.5f, 20.25f, -1, -1}, {4.5f, 31.25f, 1, 0.6f}, {-3.5f, 42.25f, -1, 1}, {5.5f, 52.25f, 1, -0.8f}};
	/** El 3 tiene casco; el 2 y el 3, espada al lado. */
	public static final boolean[] CASCO = {false, false, true, false, false}, ESPADA = {false, true, true, false, false};

	private static final float[][] ZK = {{0, -6}, {4, -6}, {10, 7.6f}, {12.5f, 7.6f}, {17.5f, 18.6f}, {20, 18.6f}, {25, 30.6f}, {30.8f, 30.6f},
			{35.6f, 40.6f}, {37, 40.6f}, {41.5f, 50.6f}, {43, 50.6f}, {47.5f, 59.6f}, {50.6f, SKZ - 1.1f}, {60, SKZ - 1.1f}};
	private static final float[][] XK = {{21, 0}, {25.2f, 1.5f}, {30.8f, 1.5f}, {35, 0.2f}, {41.5f, 0}, {43, 0.6f}, {46, 0}, {48, 0}, {50.6f, 0.3f}};
	private static final float[][] PAUSAS = {{10, 12.5f, 0}, {17.5f, 20, 1}, {25, 30, 2}, {35, 37, 3}, {41.5f, 43, 4}};
	private static final Object[][] EXPR = {{0f, "normal"}, {3f, "decidido"}, {10.2f, "triste"}, {12.6f, "decidido"}, {17.8f, "triste"},
			{20.2f, "decidido"}, {25.6f, "triste"}, {27.2f, "llorando"}, {29.8f, "triste"}, {33f, "decidido"}, {35.3f, "triste"},
			{37.2f, "decidido"}, {41.8f, "triste"}, {43.2f, "decidido"}, {46.6f, "sorpresa"}, {48.6f, "triste"}, {51.6f, "llorando"},
			{53.4f, "triste"}, {55.4f, "asombro"}, {57.5f, "sorpresa"}, {58.8f, "decidido"}};

	public static float kf(float t, float[][] k) {
		if (t <= k[0][0]) return k[0][1];
		for (int i = 1; i < k.length; i++) {
			if (t <= k[i][0]) return lerp(k[i - 1][1], k[i][1], sm((t - k[i - 1][0]) / (k[i][0] - k[i - 1][0])));
		}
		return k[k.length - 1][1];
	}

	/** Altura de los pies sobre la escalera (escalones de medio bloque cada medio bloque). */
	public static float stairY(float z) {
		if (z < 0) return 0.5f * sm(clamp((z + 0.45f) / 0.45f));
		if (z >= STAIR_H) return STAIR_H;
		int i = (int) Math.floor(z * 2);
		float f = z * 2 - i;
		return Math.min(STAIR_H, 0.5f * (i + 1) + 0.5f * sm(clamp((f - 0.55f) / 0.45f)));
	}

	public static float treadTop(float z) {
		return 0.5f * ((float) Math.floor(z * 2) + 1);
	}

	/** Hacia dónde mira el compañero caído i (el punto que mira el héroe). */
	public static Vector3f mira(int i) {
		float[] c = COMP[i];
		return new Vector3f(c[0] - c[2] * 1.6f, treadTop(c[1]) + 0.25f, c[1]);
	}

	public static float heroYaw(float t) {
		float h = 0.35f, vx = kf(t + h, XK) - kf(t - h, XK), vz = kf(t + h, ZK) - kf(t - h, ZK);
		float mv = (float) Math.hypot(vx, vz), cabeza = mv > 1e-4f ? (float) Math.atan2(vx, vz) : 0;
		float y = cabeza * clamp(mv / 0.6f) * 0.8f;
		float cara = sm(seg(t, 25.0f, 26.1f)) * (1 - sm(seg(t, 30.1f, 31.1f)));
		return lerp(y, (float) Math.PI / 2, cara);
	}

	public static String expresion(float t) {
		String e = "normal";
		for (Object[] x : EXPR) if (t >= (Float) x[0]) e = (String) x[1];
		boolean parpadeo = (t % 3.7f) < 0.13f && !e.equals("llorando") && !e.equals("sorpresa");
		return parpadeo ? "parpadeo" : e;
	}

	/** Todo lo que hace falta para dibujar un cuadro de la escena (giros en radianes, orden X, Y, Z). */
	public static final class Pose {
		public final Vector3f heroe = new Vector3f();
		public float heroeGiro;
		public final float[] torso = new float[3], cabeza = new float[3], brazoD = new float[3], brazoI = new float[3],
				piernaD = new float[3], piernaI = new float[3];
		public String expresion;
		public final Vector3f amuleto = new Vector3f();
		public final float[] amuletoGiro = new float[3];
		public float esqueletoBrazo, pulso, cerca, levanta;
	}

	private static Matrix4f rot(Matrix4f m, float[] r) {
		return m.rotateX(r[0]).rotateY(r[1]).rotateZ(r[2]);
	}

	/** Dónde está la mano izquierda del esqueleto (el amuleto apoyado en el piso al lado). */
	private static Vector3f manoEsqueleto(float brazoZ) {
		Matrix4f m = new Matrix4f().translate(1.05f, STAIR_H + 2.2f * P, SKZ).rotateY((float) Math.PI / 2).rotateX((float) -Math.PI / 2)
				.translate(5 * P, 22 * P, 0).rotateZ(brazoZ);
		return m.transformPosition(new Vector3f(0, -11 * P, 0));
	}

	public static Pose pose(float t) {
		Pose p = new Pose();
		float z = kf(t, ZK), x = kf(t, XK);
		float speed = Math.abs(kf(t + 0.05f, ZK) - kf(t - 0.05f, ZK)) / 0.1f;
		boolean subiendo = z > -0.3f && z < STAIR_H;
		float speedX = Math.abs(kf(t + 0.05f, XK) - kf(t - 0.05f, XK)) / 0.1f;
		float amp = clamp((float) Math.hypot(speed, speedX) / 1.8f) * (subiendo ? 0.72f : 0.58f);
		float respira = (float) Math.sin(t * 2.1f);
		float rodilla = sm(seg(t, 25.9f, 27)) * (1 - sm(seg(t, 29.2f, 30.3f)));
		float agacha = sm(seg(t, 50.4f, 51.8f)) * (1 - sm(seg(t, 55.6f, 56.8f)));
		float bajo = Math.max(rodilla, agacha);
		float giro = heroYaw(t), giroRapido = Math.abs(heroYaw(t + 0.1f) - heroYaw(t - 0.1f)) / 0.2f;
		float pasito = clamp(giroRapido / 1.2f) * (1 - bajo) * 0.35f;
		float fase = z * (float) Math.PI * 1.25f + x * (float) Math.PI * 1.25f;
		float pasoGiro = pasito > 0.01f ? (float) Math.sin(t * 9) * pasito : 0;
		float sw = (float) Math.sin(fase) * amp + pasoGiro, sw2 = (float) Math.sin(fase - 0.5f) * amp + pasoGiro * 0.6f;
		p.heroe.set(x + (float) Math.sin(fase) * 0.03f * amp,
				stairY(z) - (0.36f + 0.12f * agacha) * bajo + Math.abs((float) Math.cos(fase)) * 0.05f * amp + Math.abs(pasoGiro) * 0.06f + 0.006f * respira, z);
		p.heroeGiro = giro + (float) Math.sin(fase) * 0.05f * amp;
		p.torso[0] = 0.1f * amp * (subiendo ? 1 : 0.4f) + 0.42f * rodilla + 0.62f * agacha
				+ 0.3f * agacha * kf(t, new float[][]{{53, 0}, {54.3f, 1}, {55.2f, 1}, {55.9f, 0}}) + 0.015f * respira;
		p.torso[1] = -sw * 0.12f;
		p.torso[2] = (float) Math.sin(fase) * 0.03f * amp;
		float alza = subiendo ? 0.28f * amp : 0;
		p.piernaD[0] = sw - 1.3f * bajo - alza * Math.max(0, (float) Math.sin(fase));
		p.piernaD[2] = 0.04f * bajo;
		p.piernaI[0] = -sw + 1.0f * bajo - alza * Math.max(0, -(float) Math.sin(fase));
		p.piernaI[2] = -0.04f * bajo;

		float yaw = 0, pitch = subiendo ? -0.16f + 0.03f * respira : 0.02f * respira;
		for (float[] pausa : PAUSAS) {
			float w = sm(seg(t, pausa[0] - 1.2f, pausa[0] + 0.6f)) * (1 - sm(seg(t, pausa[1] - 0.8f, pausa[1] + 0.6f)));
			if (w <= 0) continue;
			Vector3f c = mira((int) pausa[2]);
			float ox = x, oy = p.heroe.y + 1.6f, oz = z;
			float yy = (float) Math.atan2(c.x - ox, c.z - oz) - p.heroeGiro;
			float dist = (float) Math.hypot(c.x - x, c.z - z);
			yaw = lerp(yaw, Math.max(-1.3f, Math.min(1.3f, yy)), w);
			pitch = lerp(pitch, (float) Math.atan2(oy - c.y, dist) * 0.9f, w);
		}
		pitch += 0.35f * sm(seg(t, 27.4f, 28.4f)) * (1 - sm(seg(t, 29.2f, 29.9f)));
		yaw += rodilla * (float) Math.sin(t * 1.3f) * 0.12f * sm(seg(t, 27.8f, 28.8f));
		float mirarArriba = sm(seg(t, 46, 47.8f));
		if (mirarArriba > 0) {
			float oy = p.heroe.y + 1.5f;
			pitch = lerp(pitch, (float) Math.atan2(oy - (STAIR_H + 0.15f), Math.max(0.5f, SKZ - z)) - p.torso[0] * 0.6f, mirarArriba);
			yaw = lerp(yaw, -0.25f * sm(seg(t, 51.8f, 52.8f)) * (1 - sm(seg(t, 53.4f, 54.2f))), mirarArriba);
		}
		float asombro = sm(seg(t, 56.5f, 57.5f));
		pitch = lerp(pitch, -0.45f, asombro);
		p.cabeza[0] = pitch - p.torso[0];
		p.cabeza[1] = yaw;
		p.cabeza[2] = (float) Math.sin(t * 0.9f) * 0.02f;
		p.expresion = expresion(t);

		float toca = kf(t, new float[][]{{27, 0}, {27.8f, 1}, {28.8f, 1}, {29.4f, 0}});
		float alcanza = kf(t, new float[][]{{53, 0}, {54.3f, 1}, {55.2f, 1}, {56, 0}});
		float sostiene = sm(seg(t, 55.2f, 56)) * (1 - sm(seg(t, 56.2f, 57.2f)));
		p.levanta = sm(seg(t, 56.2f, 57.6f)) * (1 - sm(seg(t, 59.3f, 60)));
		float apoya = agacha * (1 - alcanza);
		p.brazoI[0] = sw2 * 0.9f + 0.1f * rodilla - 0.55f * apoya - 0.03f * respira;
		p.brazoI[2] = -0.1f * apoya;
		float baseRX = -sw2 * 0.9f - 0.95f * toca - 1.15f * sostiene - 2.9f * p.levanta + 0.03f * respira;
		p.brazoD[0] = baseRX;
		p.brazoD[2] = -0.1f * p.levanta;

		// El amuleto: en el piso junto a la mano del esqueleto, hasta que el héroe lo agarra.
		float agarrado = sm(seg(t, 54.75f, 55.05f));
		p.esqueletoBrazo = lerp(0.75f, 0.6f, sm(seg(t, 54.7f, 55.4f)));
		Vector3f piso = manoEsqueleto(p.esqueletoBrazo);
		piso.y = STAIR_H + 0.03f;
		Matrix4f torso = new Matrix4f().translate(p.heroe).rotateY(p.heroeGiro).translate(0, 12 * P, 0);
		rot(torso, p.torso);
		if (alcanza > 0) {
			// Estira el brazo hacia el amuleto (IK): la dirección en el espacio del torso.
			Vector3f objetivo = new Vector3f(piso).add(0, 0.05f, 0);
			Vector3f d = new Matrix4f(torso).invert().transformPosition(objetivo).sub(-6 * P, 10 * P, 0).normalize();
			float phi = (float) Math.asin(clamp((d.x + 1) / 2) * 2 - 1), th = (float) Math.atan2(-d.z, -d.y);
			float aprieta = (float) Math.sin(clamp((t - 54.6f) / 0.5f) * Math.PI) * 0.12f;
			p.brazoD[0] = lerp(baseRX, th - aprieta, alcanza);
			p.brazoD[2] = lerp(-0.1f * p.levanta, phi, alcanza);
		}
		Matrix4f mano = new Matrix4f(torso).translate(-6 * P, 10 * P, 0);
		rot(mano, p.brazoD);
		Vector3f enMano = mano.transformPosition(new Vector3f(0, -10 * P, 0));
		p.amuleto.set(piso).lerp(enMano, agarrado);
		float desde = Math.max(0, t - 55);
		float balanceo = agarrado * ((float) Math.sin(desde * 5) * 0.5f * (float) Math.exp(-desde * 1.6f) + (float) Math.sin(t * 2.1f) * 0.06f);
		p.amuletoGiro[0] = lerp((float) -Math.PI / 2, balanceo * 0.4f, agarrado);
		p.amuletoGiro[1] = lerp(0.4f, 0.4f + desde * 0.5f, agarrado);
		p.amuletoGiro[2] = balanceo;
		p.pulso = 0.5f + 0.5f * (float) Math.sin(t * 3);
		p.cerca = sm(seg(t, 47, 52));
		return p;
	}

	/** La toma de cámara (el "director" del diseño): {x, y, z} de la cámara y {x, y, z} del punto que mira. */
	public static float[] toma(float t) {
		Vector3f hp = pose(t).heroe;
		if (t < 4) {
			float e = sm(seg(t, 0, 4));
			return new float[]{lerp(46, 30, e), lerp(8, 14, e), lerp(-22, -18, e), 0, lerp(26, 20, e), 26};
		}
		if (t < 10) return new float[]{hp.x + 2.2f, hp.y + 2.6f, hp.z - 5.5f, hp.x, hp.y + 1.7f, hp.z + 3};
		if (t < 12.5f) return compa(0, hp, 3.5f, 1.5f);
		if (t < 17.5f) return new float[]{hp.x + 13, hp.y + 4, hp.z - 4, hp.x, hp.y + 1.2f, hp.z + 2};
		if (t < 20) return compa(1, hp, -3.5f, 1.5f);
		if (t < 25) return new float[]{hp.x - 2, hp.y + 2.6f, hp.z - 5.5f, hp.x, hp.y + 1.7f, hp.z + 3};
		if (t < 30) {
			float[] c = COMP[2];
			float top = treadTop(c[1]);
			Vector3f m = mira(2);
			return new float[]{c[0] + 3.2f, top + 3.3f, c[1] - 1.2f, (hp.x + m.x) / 2, top + 0.5f, c[1]};
		}
		if (t < 35) return new float[]{hp.x - 13, hp.y + 4, hp.z - 4, hp.x, hp.y + 1.2f, hp.z + 2};
		if (t < 37) return compa(3, hp, -3.5f, 1.5f);
		if (t < 43) return new float[]{hp.x + 2.2f, hp.y + 2.6f, hp.z - 5.5f, hp.x, hp.y + 1.7f, hp.z + 3};
		if (t < 47.5f) return new float[]{3.5f, STAIR_H + 2.6f, STAIR_H + 2.5f, hp.x, hp.y + 1.2f, hp.z};
		if (t < 50.4f) return new float[]{hp.x + 1.4f, hp.y + 2.4f, hp.z - 3.6f, 0, STAIR_H + 0.2f, SKZ};
		if (t < 52.6f) {
			float e = sm(seg(t, 50.4f, 52.6f));
			return new float[]{lerp(3.6f, 2.4f, e), STAIR_H + lerp(2.2f, 1.5f, e), SKZ - 0.4f, -0.1f, STAIR_H + lerp(0.8f, 0.6f, e), SKZ - 0.55f};
		}
		if (t < 55.4f) return new float[]{-1.6f, STAIR_H + 1.1f, SKZ - 0.2f, 0.15f, STAIR_H + 0.45f, SKZ - 0.85f};
		float e = sm(seg(t, 55.4f, 60));
		return new float[]{lerp(1.4f, 3.2f, e), STAIR_H + lerp(1.1f, 1.6f, e), hp.z + lerp(2, 2.8f, e), hp.x, hp.y + lerp(1.4f, 2.2f, e), hp.z};
	}

	private static float[] compa(int i, Vector3f hp, float dx, float dz) {
		float[] c = COMP[i];
		float top = treadTop(c[1]);
		Vector3f m = mira(i);
		return new float[]{c[0] + dx, top + 4, c[1] + dz, (hp.x + m.x) / 2, top + 0.6f, (hp.z + c[1]) / 2};
	}
}
