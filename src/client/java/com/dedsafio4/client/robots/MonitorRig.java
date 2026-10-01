package com.dedsafio4.client.robots;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * El robot del diseño "Monitor Mob" armado pieza por pieza como en el prototipo (Monitor Mob.html):
 * medidas en píxeles, Y hacia arriba y la pantalla mirando a +Z. Lo usan el Aldeano Robot (despierto,
 * flotando, 2 bloques de alto) y el bloque del robot dormido (plegado en 1 bloque).
 */
public final class MonitorRig {
	private static final int HUESO = 0xD6DABB, NEGRO = 0x141715, VERDE = 0x38D12F, LIMA = 0xA6DC45;
	private static final int TEAL = 0x1D6B5F, TEAL_OSCURO = 0x16524A, TEAL_LINEA = 0x2C8A78, BRILLO = 0xE6F2EE;
	private static final int APAGADA = 0x0C1210;
	private static final int LUZ_MAXIMA = 0xF000F0;
	private static final float E = 0.02f;

	/** Qué está haciendo, como los botones del prototipo. */
	public enum Animacion { QUIETO, RESPIRAR, CAMINAR, SALUDAR, BAILAR, MIRAR, DORMIDO }

	private static final class Caja {
		final float w, h, d, cx, cy, cz;
		final int color;
		final boolean brilla;
		boolean visible = true;
		float dy;

		Caja(float w, float h, float d, float cx, float cy, float cz, int color, boolean brilla) {
			this.w = w; this.h = h; this.d = d; this.cx = cx; this.cy = cy; this.cz = cz;
			this.color = 0xFF000000 | color;
			this.brilla = brilla;
		}
	}

	private static final class Nodo {
		/** Origen en píxeles absolutos del modelo (sólo para armarlo). */
		final float ox, oy, oz;
		float px, py, pz, rx, ry, rz, sx = 1, sy = 1, sz = 1;
		final List<Caja> cajas = new ArrayList<>();
		final List<Nodo> hijos = new ArrayList<>();

		Nodo(float ox, float oy, float oz) {
			this.ox = ox; this.oy = oy; this.oz = oz;
		}
	}

	private final Nodo raiz = new Nodo(0, 0, 0);
	private final Nodo cuerpo, torso, pivoteCabeza, cabeza;
	private final Nodo[] brazos = new Nodo[2];
	private final List<List<Nodo>> pinzas = new ArrayList<>();
	private final List<Caja> brillos = new ArrayList<>();
	private final Caja pantallaApagada, baseSoporte, zocalo;
	private final float cuerpoY, pivoteY, pivoteZ, zocaloY, baseY;
	private final float[] brazoY = new float[2];

	/** Escala y posición de la raíz (bloques por píxel, y bloques) como las calcula el prototipo. */
	private final float escalaDespierto, alturaDespierto, centroX, centroZ;
	private float escalaDormido = -1, alturaDormido;

	public MonitorRig() {
		cuerpo = articulacion(raiz, 0, 7, 0);
		cuerpo.py -= 4;
		torso = articulacion(cuerpo, 0, 11, 0);
		torso.sx = 1.3f; torso.sy = 1.8f; torso.sz = 1.3f;
		caja(torso, NEGRO, false, 11, 3, 7, -5.5f, 4, -3.5f);
		caja(torso, NEGRO, false, 13, 8, 9, -6.5f, 7, -4.5f);
		float fz = 4.5f + E;
		float y = 4;
		float[][] marcas = {
				{1.5f, 3, -5.5f, 7}, {3, 1, -5.5f, 7}, {1.5f, 3, 4, 7}, {3, 1, 2.5f, 7},
				{2.5f, 3, -5, 3.5f}, {2.5f, 3, 2.5f, 3.5f},
		};
		for (float[] m : marcas) caja(torso, VERDE, true, m[0], m[1], E, m[2], m[3] + y, fz - E);
		float[][] espiral = {{3, 1, -1.5f, 9}, {1, 4, -1.5f, 5}, {3, 1, -1.5f, 5}, {1, 2, 0.5f, 6}, {1, 1, -0.5f, 7}};
		for (float[] m : espiral) caja(torso, HUESO, false, m[0], m[1], E, m[2], m[3] + y, fz - E);
		caja(torso, VERDE, true, 8, 1, E, -4, 9 + y, -4.5f - E);

		// Brazos: pivote en el hombro, con dos pinzas cada uno.
		for (int i = 0; i < 2; i++) {
			int s = i == 0 ? -1 : 1;
			float x0 = s < 0 ? -12.5f : 8.5f;
			Nodo brazo = articulacion(cuerpo, x0 + 2, 12.5f + y, 0);
			float ay = y + 3;
			float ax = s < 0 ? -11.8f : 9.3f;
			caja(brazo, NEGRO, false, 3, 3, 3.5f, ax - 0.25f, 7.5f + ay, -1.75f);
			caja(brazo, HUESO, false, 2.5f, 5, 2.5f, ax, 2.5f + ay, -1.25f);
			caja(brazo, NEGRO, false, 3, 1.2f, 3, ax - 0.25f, 1.4f + ay, -1.5f);
			List<Nodo> par = new ArrayList<>();
			for (int f : new int[]{1, -1}) {
				float pz = f > 0 ? 0.6f : -0.6f;
				Nodo pinza = articulacion(brazo, ax + 1.25f, 1.4f + ay, pz);
				float z0 = f > 0 ? 0.4f : -1.2f;
				caja(pinza, HUESO, false, 2.2f, 4.2f, 0.9f, ax + 0.15f, -2.8f + ay, z0);
				caja(pinza, NEGRO, false, 2.2f, 1, 1.4f, ax + 0.15f, -2.8f + ay, f > 0 ? z0 - 1 : z0 + 0.5f);
				par.add(pinza);
			}
			pinzas.add(par);
			brazos[i] = brazo;
			brazoY[i] = brazo.py;
		}

		// Soporte (cable) y la cabeza monitor.
		float top = 18.2f, hp = top + 11;
		baseSoporte = caja(cuerpo, NEGRO, false, 6, 1, 6, -3, top, -7);
		pivoteCabeza = articulacion(cuerpo, 0, top + 1, -4);
		caja(pivoteCabeza, NEGRO, false, 5, hp + 5 - top - 1, 5, -2.5f, top + 1, -6.5f);
		caja(pivoteCabeza, HUESO, false, 6, 1, 6, -3, top + 2, -7);
		caja(pivoteCabeza, HUESO, false, 6, 1, 6, -3, top + 7, -7);
		caja(pivoteCabeza, HUESO, false, 6, 1, 6, -3, top + 12, -7);
		zocalo = caja(cuerpo, HUESO, false, 7, 1.5f, 7, -3.5f, top - 0.5f, -7.5f);
		caja(pivoteCabeza, HUESO, false, 5, 2, 4.4f, -2.5f, hp + 3.5f, -7.2f);
		cabeza = articulacion(pivoteCabeza, 0, hp, 0.5f);
		cabeza.rx = -0.38f;
		cabeza.sx = cabeza.sy = cabeza.sz = 1.35f;
		float sz = 2;
		cajaCabeza(HUESO, false, 16, 12, 3, -8, 0, -1, hp);
		cajaCabeza(TEAL, true, 13, 9, 0.3f, -6.5f, 1.5f, sz - 0.3f + E, hp);
		cajaCabeza(TEAL_OSCURO, true, 10, 6, 0.3f, -5, 3, sz + E, hp);
		cajaCabeza(TEAL_LINEA, true, 7, 0.8f, 0.2f, -3.5f, 7, sz + 0.3f + E, hp);
		cajaCabeza(TEAL_LINEA, true, 7, 0.8f, 0.2f, -3.5f, 5.5f, sz + 0.3f + E, hp);
		cajaCabeza(TEAL_LINEA, true, 5, 0.8f, 0.2f, -2.5f, 4, sz + 0.3f + E, hp);
		brillos.add(cajaCabeza(BRILLO, true, 2, 1, 0.2f, -5.2f, 2.4f, sz + 0.3f + E, hp));
		brillos.add(cajaCabeza(BRILLO, true, 2, 1, 0.2f, 3.2f, 2.4f, sz + 0.3f + E, hp));
		cajaCabeza(NEGRO, false, 14, 10, 2, -7, 1, -3, hp);
		pantallaApagada = cajaCabeza(APAGADA, false, 13.2f, 9.2f, 0.9f, -6.6f, 1.4f, sz - 0.3f + E * 2, hp);
		pantallaApagada.visible = false;
		float bz = -3 - E * 2;
		cajaCabeza(LIMA, true, 2, 10, E, -6, 1, bz, hp);
		cajaCabeza(VERDE, true, 6, 1.5f, E, -2, 7.5f, bz, hp);
		cajaCabeza(VERDE, true, 1.5f, 4, E, -2, 4, bz, hp);
		cajaCabeza(VERDE, true, 4, 1.5f, E, -2, 3, bz, hp);
		cajaCabeza(VERDE, true, 1.5f, 3, E, 3, 6, bz, hp);
		cajaCabeza(VERDE, true, 3, 1.5f, E, 3, 4.5f, bz, hp);

		cuerpoY = cuerpo.py;
		pivoteY = pivoteCabeza.py;
		pivoteZ = pivoteCabeza.pz;
		zocaloY = 0;
		baseY = 0;

		// Como el prototipo: se escala para que despierto mida 2 bloques y se centra en X/Z.
		float[] caja = limites();
		escalaDespierto = 2f / (caja[4] - caja[1]);
		centroX = -(caja[0] + caja[3]) / 2 * escalaDespierto;
		centroZ = -(caja[2] + caja[5]) / 2 * escalaDespierto;
		alturaDespierto = -caja[1] * escalaDespierto;
	}

	private static Nodo articulacion(Nodo padre, float px, float py, float pz) {
		Nodo n = new Nodo(px, py, pz);
		n.px = px - padre.ox;
		n.py = py - padre.oy;
		n.pz = pz - padre.oz;
		padre.hijos.add(n);
		return n;
	}

	/** Tamaño (w, h, d) y esquina mínima (x, y, z) en píxeles absolutos, como box() del prototipo. */
	private static Caja caja(Nodo padre, int color, boolean brilla, float w, float h, float d, float x, float y, float z) {
		Caja c = new Caja(w, h, d, x + w / 2 - padre.ox, y + h / 2 - padre.oy, z + d / 2 - padre.oz, color, brilla);
		padre.cajas.add(c);
		return c;
	}

	private Caja cajaCabeza(int color, boolean brilla, float w, float h, float d, float x, float y, float z, float hp) {
		return caja(cabeza, color, brilla, w, h, d, x, y + hp, z + 0.5f);
	}

	/**
	 * Deja el robot en la pose de ese momento (t en segundos). "cabezaGira" y "cabezaSube" son hacia
	 * dónde mira (en radianes) y "camina" de 0 a 1 mezcla la animación de caminar.
	 */
	public void posar(Animacion anim, float t, float camina, float cabezaGira, float cabezaSube) {
		float bodyY = 0, bodyRz = 0, bodyRy = 0, headX = 0, headZ = 0, headY = 0;
		float aLx = 0, aRx = 0, aLz = 0, aRz = 0;
		switch (anim) {
			case RESPIRAR -> {
				bodyY = Mth.sin(t * 2) * 0.25f; headX = Mth.sin(t * 2 - 0.6f) * 0.04f;
				aLz = -Mth.sin(t * 2) * 0.04f; aRz = Mth.sin(t * 2) * 0.04f;
			}
			case CAMINAR -> {
				float w = t * 6;
				aLx = -Mth.sin(w) * 0.5f; aRx = Mth.sin(w) * 0.5f;
				bodyY = Math.abs(Mth.sin(w)) * 0.6f; bodyRz = Mth.sin(w) * 0.04f; headX = Mth.sin(w * 2) * 0.03f;
			}
			case SALUDAR -> {
				aRz = 2.4f + Mth.sin(t * 8) * 0.35f; aLz = -0.05f; headZ = -0.12f; headY = 0.15f; bodyY = Mth.sin(t * 2) * 0.2f;
			}
			case BAILAR -> {
				float w = t * 5;
				bodyY = Math.abs(Mth.sin(w)) * 1.2f; bodyRz = Mth.sin(w) * 0.12f; bodyRy = Mth.sin(w * 0.5f) * 0.35f;
				aLz = -1.2f - Mth.sin(w) * 0.6f; aRz = 1.2f - Mth.sin(w) * 0.6f; headZ = Mth.sin(w) * 0.15f;
			}
			case MIRAR -> {
				headY = Mth.sin(t * 1.2f) * 0.6f; headZ = Mth.sin(t * 0.6f) * 0.12f; bodyY = Mth.sin(t * 2) * 0.2f;
			}
			default -> {}
		}
		// Caminar se mezcla encima de lo que esté haciendo.
		if (camina > 0 && anim != Animacion.DORMIDO) {
			float w = t * 6;
			aLx = mezcla(aLx, -Mth.sin(w) * 0.5f, camina);
			aRx = mezcla(aRx, Mth.sin(w) * 0.5f, camina);
			bodyY = mezcla(bodyY, Math.abs(Mth.sin(w)) * 0.6f, camina);
			bodyRz = mezcla(bodyRz, Mth.sin(w) * 0.04f, camina);
		}
		cuerpo.py = cuerpoY + bodyY;
		cuerpo.rx = 0; cuerpo.ry = bodyRy; cuerpo.rz = bodyRz;
		pivoteCabeza.rx = headX + cabezaSube;
		pivoteCabeza.ry = headY + cabezaGira;
		pivoteCabeza.rz = headZ;
		brazos[0].rx = aLx; brazos[0].rz = aLz; brazos[0].ry = 0;
		brazos[1].rx = aRx; brazos[1].rz = aRz; brazos[1].ry = 0;
		float abre = anim == Animacion.QUIETO ? 0.35f
				: (anim == Animacion.SALUDAR || anim == Animacion.BAILAR) ? 0.3f + Math.abs(Mth.sin(t * 8)) * 0.4f
				: 0.3f + (float) Math.pow(Math.max(0, Mth.sin(t * 1.3f)), 8) * 0.5f;
		for (List<Nodo> par : pinzas) {
			for (int i = 0; i < par.size(); i++) par.get(i).rx = -(i == 0 ? 1 : -1) * abre;
		}
		boolean parpadea = anim != Animacion.QUIETO && (t % 3.2f) < 0.12f;
		boolean dormido = anim == Animacion.DORMIDO;
		for (Caja b : brillos) b.visible = !parpadea && !dormido;
		pantallaApagada.visible = dormido;

		if (dormido) {
			// Se pliega en un bloque: el torso se achica, la cabeza cae hacia adelante con la pantalla apagada.
			float respira = Mth.sin(t * 1.2f) * 0.02f;
			torso.sy = 0.8f + respira;
			float baja = 4 * (1.8f - torso.sy);
			pivoteCabeza.py = pivoteY - baja - 1;
			pivoteCabeza.pz = pivoteZ - 2;
			zocalo.dy = -baja;
			baseSoporte.dy = -baja;
			pivoteCabeza.rz += Mth.sin(t * 0.8f) * 0.03f;
			pivoteCabeza.rx += 0.95f;
			for (int i = 0; i < 2; i++) {
				brazos[i].rx = 0; brazos[i].rz = 0;
				brazos[i].py = brazoY[i] - 5.5f;
				brazos[i].sx = brazos[i].sy = brazos[i].sz = 0.6f;
				for (Nodo p : pinzas.get(i)) p.rx = 0;
			}
			cuerpo.py = cuerpoY;
			if (escalaDormido < 0) {
				float[] c = limites();
				escalaDormido = 1f / (c[4] - c[1]);
				alturaDormido = -c[1] * escalaDormido;
			}
		} else {
			torso.sy = 1.8f;
			pivoteCabeza.py = pivoteY;
			pivoteCabeza.pz = pivoteZ;
			zocalo.dy = zocaloY;
			baseSoporte.dy = baseY;
			for (int i = 0; i < 2; i++) {
				brazos[i].py = brazoY[i];
				brazos[i].sx = brazos[i].sy = brazos[i].sz = 1;
			}
		}
	}

	private static float mezcla(float a, float b, float cuanto) {
		return a + (b - a) * cuanto;
	}

	/**
	 * Dibuja el robot parado en el origen del PoseStack (en bloques, Y arriba, frente +Z).
	 * Despierto flota medio bloque y sube y baja; dormido queda apoyado y mide 1 bloque.
	 */
	public void dibujar(PoseStack pose, VertexConsumer vc, int luz, int overlay, int tinte, boolean dormido, float t) {
		pose.pushPose();
		if (dormido) {
			pose.translate(centroX, alturaDormido, centroZ);
			pose.scale(escalaDormido, escalaDormido, escalaDormido);
		} else {
			pose.translate(centroX, alturaDespierto + 0.5f + Mth.sin(t * 1.6f) * 0.08f, centroZ);
			pose.scale(escalaDespierto, escalaDespierto, escalaDespierto);
		}
		dibujar(raiz, pose, vc, luz, overlay, tinte);
		pose.popPose();
	}

	private void dibujar(Nodo n, PoseStack pose, VertexConsumer vc, int luz, int overlay, int tinte) {
		pose.pushPose();
		aplicar(n, pose);
		PoseStack.Pose p = pose.last();
		for (Caja c : n.cajas) if (c.visible) dibujarCaja(p, vc, c, c.brilla ? LUZ_MAXIMA : luz, overlay, tinte);
		for (Nodo h : n.hijos) dibujar(h, pose, vc, luz, overlay, tinte);
		pose.popPose();
	}

	/** Igual que three.js: posición, rotación en orden XYZ y escala. */
	private static void aplicar(Nodo n, PoseStack pose) {
		pose.translate(n.px, n.py, n.pz);
		if (n.rx != 0) pose.mulPose(Axis.XP.rotation(n.rx));
		if (n.ry != 0) pose.mulPose(Axis.YP.rotation(n.ry));
		if (n.rz != 0) pose.mulPose(Axis.ZP.rotation(n.rz));
		if (n.sx != 1 || n.sy != 1 || n.sz != 1) pose.scale(n.sx, n.sy, n.sz);
	}

	/** Los límites de todo el modelo en píxeles: {minX, minY, minZ, maxX, maxY, maxZ}. */
	private float[] limites() {
		float[] r = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		PoseStack pose = new PoseStack();
		limites(raiz, pose, r);
		return r;
	}

	private void limites(Nodo n, PoseStack pose, float[] r) {
		pose.pushPose();
		aplicar(n, pose);
		Matrix4f m = pose.last().pose();
		Vector3f v = new Vector3f();
		for (Caja c : n.cajas) {
			for (int i = 0; i < 8; i++) {
				v.set(c.cx + ((i & 1) == 0 ? -c.w : c.w) / 2, c.cy + c.dy + ((i & 2) == 0 ? -c.h : c.h) / 2,
						c.cz + ((i & 4) == 0 ? -c.d : c.d) / 2);
				m.transformPosition(v);
				r[0] = Math.min(r[0], v.x); r[1] = Math.min(r[1], v.y); r[2] = Math.min(r[2], v.z);
				r[3] = Math.max(r[3], v.x); r[4] = Math.max(r[4], v.y); r[5] = Math.max(r[5], v.z);
			}
		}
		for (Nodo h : n.hijos) limites(h, pose, r);
		pose.popPose();
	}

	private static final float[][] NORMALES = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};

	private static void dibujarCaja(PoseStack.Pose p, VertexConsumer vc, Caja c, int luz, int overlay, int tinte) {
		float x0 = c.cx - c.w / 2, x1 = c.cx + c.w / 2;
		float y0 = c.cy + c.dy - c.h / 2, y1 = c.cy + c.dy + c.h / 2;
		float z0 = c.cz - c.d / 2, z1 = c.cz + c.d / 2;
		int color = FastColor.ARGB32.multiply(c.color, tinte);
		float[][][] esquinas = {
				{{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}},
				{{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}},
				{{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}},
				{{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}},
				{{x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}},
				{{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}},
		};
		for (int cara = 0; cara < 6; cara++) {
			float[] n = NORMALES[cara];
			for (float[] v : esquinas[cara]) {
				vc.addVertex(p, v[0], v[1], v[2]).setColor(color).setUv(0.5f, 0.5f)
						.setOverlay(overlay).setLight(luz).setNormal(p, n[0], n[1], n[2]);
			}
		}
	}
}
