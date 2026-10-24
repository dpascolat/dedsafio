package com.dedsafio4.client.qumara;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.qumara.QumaraAgarre;
import com.dedsafio4.qumara.QumaraEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qumara, la Flor Mutante: el modelo nuevo de Blockbench (qumara/modelo.json, pasado del .bbmodel; una sola textura,
 * textures/entity/qumara/qumara.png), dibujado a casi 38 bloques de alto. Raíces, un tronco de 7 remolinos que se
 * mece, dos brazos con cuchillas, la cabeza con mandíbula y lengua, la flor de 5 pétalos detrás y la enredadera con
 * los bichos. Animaciones: quieta, giro (la animación "barrido" del modelo: se echa para atrás y da una vuelta entera
 * con los brazos estirados, con anillos de onda, estelas y polvo), grito,
 * transformación (de pimpollo cerrado mirando al cielo a Qumara), debilitada (celeste y marchita), derrotada (se
 * desploma y queda) y atrapar/sacar (el brazo derecho va a buscar al jugador y lo sube a la cara). El brazo se estira
 * hasta donde el servidor pone la pinza (QumaraAgarre), así se ve justo donde está el jugador. El giro del cuerpo
 * coincide con QumaraEntity.yawBrazo (donde pega el servidor).
 */
public class QumaraRenderer extends EntityRenderer<QumaraEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/qumara/qumara.png");
	private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "qumara/modelo.json");
	/** Bloques por píxel del modelo: la boca queda a unos 24 bloques y la flor llega a casi 38. */
	private static final float K = 0.3375f;
	/** El hombro derecho y el punto que agarra (entre las cuchillas), en píxeles del modelo. */
	private static final Vector3f HOMBRO = new Vector3f(8.8f, 41.44f, 1.6f), MANO = new Vector3f(64f, 30f, 11f);

	private static final class Nodo {
		final String nombre;
		final float ox, oy, oz;
		final float[] caras, brillan;
		final List<Nodo> hijos = new ArrayList<>();
		float rx, ry, rz, s = 1;
		boolean visible = true;
		/** Un giro más (los pétalos se doblan sobre su propio eje) y una matriz (el brazo que se estira). */
		final Quaternionf extra = new Quaternionf();
		Matrix4f matriz;
		/** Lo que pone la animación del modelo (el giro). */
		final Vector3f animPos = new Vector3f(), animEsc = new Vector3f(1, 1, 1);
		final Quaternionf animRot = new Quaternionf();

		Nodo(String nombre, float ox, float oy, float oz, float[] caras, float[] brillan) {
			this.nombre = nombre;
			this.ox = ox; this.oy = oy; this.oz = oz;
			this.caras = caras;
			this.brillan = brillan;
		}

		void reiniciar() {
			rx = ry = rz = 0;
			s = 1;
			visible = true;
			extra.identity();
			matriz = null;
			animPos.zero();
			animEsc.set(1, 1, 1);
			animRot.identity();
		}
	}

	private final List<Nodo> todos = new ArrayList<>();
	private final Map<String, Nodo> porNombre = new HashMap<>();
	private Nodo raiz;
	/** Para cada pétalo, el eje sobre el que se dobla hacia adelante. */
	private final List<Vector3f> ejesPetalo = new ArrayList<>();

	/** Una animación del modelo: por hueso, sus cuadros de rotación (t, x, y, z, w), posición y escala (t, x, y, z). */
	private record Canales(float[][] rot, float[][] pos, float[][] esc) {}
	private final Map<String, Canales> giro = new HashMap<>();
	private float largoGiro = 1.8f;

	public QumaraRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 8f;
	}

	private void cargar() {
		if (raiz != null) return;
		List<Vector3f> absolutos = new ArrayList<>();
		try (Reader lector = Minecraft.getInstance().getResourceManager().openAsReader(MODELO)) {
			JsonObject m = JsonParser.parseReader(lector).getAsJsonObject();
			for (JsonElement e : m.getAsJsonArray("huesos")) {
				JsonObject h = e.getAsJsonObject();
				JsonArray o = h.getAsJsonArray("o");
				Nodo n = new Nodo(h.get("n").getAsString(), o.get(0).getAsFloat(), o.get(1).getAsFloat(), o.get(2).getAsFloat(),
						numeros(h.getAsJsonArray("q")), numeros(h.getAsJsonArray("g")));
				int padre = h.get("p").getAsInt();
				Vector3f abs = new Vector3f(n.ox, n.oy, n.oz);
				if (padre >= 0) {
					todos.get(padre).hijos.add(n);
					abs.add(absolutos.get(padre));
				} else {
					raiz = n;
				}
				todos.add(n);
				absolutos.add(abs);
				porNombre.put(n.nombre, n);
			}
			if (m.has("animaciones") && m.getAsJsonObject("animaciones").has("barrido")) {
				JsonObject a = m.getAsJsonObject("animaciones").getAsJsonObject("barrido");
				largoGiro = a.get("largo").getAsFloat();
				for (var e : a.getAsJsonObject("huesos").entrySet()) {
					JsonObject c = e.getValue().getAsJsonObject();
					giro.put(e.getKey(), new Canales(cuadros(c, "rot"), cuadros(c, "pos"), cuadros(c, "esc")));
				}
			}
		} catch (Exception ex) {
			Dedsafio4.LOGGER.error("No se pudo cargar el modelo de Qumara", ex);
			raiz = new Nodo("vacio", 0, 0, 0, new float[0], new float[0]);
			return;
		}
		// El eje de cada pétalo: de costado a la dirección en que sale desde el centro de la flor.
		Vector3f centro = absolutos.get(todos.indexOf(nodo("bloom")));
		for (int k = 0; k < 5; k++) {
			Vector3f d = new Vector3f(absolutos.get(todos.indexOf(nodo("petal_" + k + "_tilt")))).sub(centro);
			d.z = 0;
			if (d.lengthSquared() < 1.0E-4f) d.set(0, 1, 0);
			d.normalize();
			ejesPetalo.add(new Vector3f(d.y, -d.x, 0));
		}
	}

	private static float[][] cuadros(JsonObject c, String canal) {
		if (!c.has(canal)) return null;
		JsonArray a = c.getAsJsonArray(canal);
		float[][] r = new float[a.size()][];
		for (int i = 0; i < r.length; i++) r[i] = numeros(a.get(i).getAsJsonArray());
		return r;
	}

	/** El cuadro de antes y el de después de ese momento, y cuánto se avanzó entre los dos. */
	private static int cuadro(float[][] l, float t) {
		int i = 0;
		while (i + 1 < l.length && l[i + 1][0] <= t) i++;
		return i;
	}

	/** Pone la animación del giro en el momento t (segundos). */
	private void ponerGiro(float t) {
		for (var e : giro.entrySet()) {
			Nodo n = porNombre.get(e.getKey());
			if (n == null) continue;
			Canales c = e.getValue();
			if (c.rot() != null) {
				int i = cuadro(c.rot(), t);
				float[] a = c.rot()[i], b = c.rot()[Math.min(i + 1, c.rot().length - 1)];
				float k = b[0] > a[0] ? Mth.clamp((t - a[0]) / (b[0] - a[0]), 0, 1) : 0;
				n.animRot.set(a[1], a[2], a[3], a[4]).slerp(new Quaternionf(b[1], b[2], b[3], b[4]), k);
			}
			if (c.pos() != null) n.animPos.set(interpolar(c.pos(), t));
			if (c.esc() != null) n.animEsc.set(interpolar(c.esc(), t));
		}
	}

	private static Vector3f interpolar(float[][] l, float t) {
		int i = cuadro(l, t);
		float[] a = l[i], b = l[Math.min(i + 1, l.length - 1)];
		float k = b[0] > a[0] ? Mth.clamp((t - a[0]) / (b[0] - a[0]), 0, 1) : 0;
		return new Vector3f(Mth.lerp(k, a[1], b[1]), Mth.lerp(k, a[2], b[2]), Mth.lerp(k, a[3], b[3]));
	}

	private static float[] numeros(JsonArray a) {
		float[] r = new float[a.size()];
		for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	private Nodo nodo(String nombre) {
		Nodo n = porNombre.get(nombre);
		if (n == null) {
			n = new Nodo(nombre, 0, 0, 0, new float[0], new float[0]);   // por si falta: no hace nada
			porNombre.put(nombre, n);
		}
		return n;
	}

	private static float suave(float x) {
		return QumaraEntity.suave(x);
	}

	// Tintes (color de todo el cuerpo): celeste al debilitarse, marchito al ser derrotada.
	private float tinteR = 1, tinteG = 1, tinteB = 1;
	private boolean ojosApagados;

	/** Dobla el pétalo k hacia adelante (positivo) o hacia atrás (negativo). */
	private void doblar(int k, float angulo) {
		Vector3f eje = ejesPetalo.get(k);
		nodo("petal_" + k).extra.set(new Quaternionf().rotationAxis(angulo, eje.x, eje.y, eje.z));
	}

	/**
	 * Estira el brazo derecho para que la pinza llegue a ese punto (en bloques, en el cuerpo de Qumara). k: cuánto
	 * (0 = el brazo en su lugar, 1 = justo en el punto).
	 */
	private void brazoHacia(Vec3 punto, float k) {
		if (k <= 0.001f) return;
		Vector3f u = new Vector3f(MANO).sub(HOMBRO);
		Vector3f v = new Vector3f((float) punto.x / K, (float) punto.y / K, (float) punto.z / K).sub(HOMBRO);
		float largo = u.length(), hasta = Math.max(v.length(), 1);
		Quaternionf giro = new Quaternionf().rotationTo(new Vector3f(u).normalize(), new Vector3f(v).normalize());
		giro = new Quaternionf().slerp(giro, k);
		float s = Mth.clamp(Mth.lerp(k, 1, hasta / largo), 0.05f, 4f);
		Vector3f d = new Vector3f(u).normalize().rotate(giro);
		// Estirar a lo largo de d: I + (s - 1) d dᵀ.
		Matrix4f estirar = new Matrix4f(
				1 + (s - 1) * d.x * d.x, (s - 1) * d.y * d.x, (s - 1) * d.z * d.x, 0,
				(s - 1) * d.x * d.y, 1 + (s - 1) * d.y * d.y, (s - 1) * d.z * d.y, 0,
				(s - 1) * d.x * d.z, (s - 1) * d.y * d.z, 1 + (s - 1) * d.z * d.z, 0,
				0, 0, 0, 1);
		nodo("arm_right").matriz = estirar.rotate(giro);
	}

	/** Pone la pose del momento. */
	private void animar(QumaraEntity q, float parcial) {
		for (Nodo n : todos) n.reiniciar();
		float t = (q.level().getGameTime() % 72000L + parcial) / 20f;
		Nodo cabeza = nodo("head"), mandibula = nodo("mandible"), lengua = nodo("tongue"), flor = nodo("bloom");
		Nodo brazoI = nodo("arm_left"), brazoD = nodo("arm_right"), enredadera = nodo("vine");
		// Quieta: el tronco se mece, la cabeza mira un poco, la boca se abre y cierra, todo se mueve un poquito.
		for (int i = 0; i < 7; i++) {
			Nodo r = nodo("trunk_swirl_" + i);
			r.rz = Mth.sin(t * 0.9f - i * 0.3f) * 0.014f;
			r.rx = Mth.sin(t * 0.7f - i * 0.25f) * 0.009f;
		}
		cabeza.rx = -0.05f + Mth.sin(t * 1.1f) * 0.04f;
		cabeza.ry = Mth.sin(t * 0.5f) * 0.1f;
		float m0 = 0.1f + Math.max(0, Mth.sin(t * 1.7f)) * 0.12f;
		mandibula.rx = m0;
		float[] pliegue = new float[5];
		for (int k = 0; k < 5; k++) pliegue[k] = Mth.sin(t * 1.4f + k * 0.8f) * 0.05f;
		lengua.visible = false;
		for (int i = 0; i < 7; i++) {
			Nodo v = nodo("vine_j" + i);
			v.rz = Mth.sin(t * 1.6f - i * 0.7f) * 0.05f;
			v.rx = Mth.sin(t * 1.1f - i * 0.6f) * 0.04f;
		}
		for (int i = 0; i < 3; i++) {
			nodo("frill_l_" + i).ry = Mth.sin(t * 2 + i) * 0.12f;
			nodo("frill_r_" + i).ry = -Mth.sin(t * 2 + i + 0.5f) * 0.12f;
		}
		brazoI.rz = Mth.sin(t * 0.8f) * 0.04f;
		brazoD.rz = -Mth.sin(t * 0.8f + 1) * 0.04f;
		float rapidez = 2.2f;
		for (int j = 1; j < 4; j++) {
			nodo("arm_left_j" + j).rz = Mth.sin(t * 1.2f - j * 0.5f) * 0.03f;
			nodo("arm_right_j" + j).rz = -Mth.sin(t * 1.2f - j * 0.5f + 1) * 0.03f;
		}
		for (int i = 0; i < 6; i++) nodo("bicho_cola_" + (char) ('a' + i)).ry = Mth.sin(t * 2.5f + i) * 0.3f;

		// Giro (barrido): la animación del modelo, con los anillos de onda, las estelas y el polvo solo mientras gira.
		float aT = q.tBarrido(parcial) / 20f;
		boolean girando = aT >= 0 && aT < largoGiro && !q.derrotada();
		nodo("barrido_fx").visible = girando;
		if (girando) {
			ponerGiro(aT);
			nodo("shock_ring_a").visible = nodo("shock_ring_b").visible = aT >= 0.7f && aT < 1.45f;
			nodo("streaks").visible = aT >= 0.45f && aT < 1.25f;
			for (int i = 0; i < 12; i++) nodo("dust_" + i).visible = aT >= 0.5f && aT < 1.2f;
			rapidez = 10;
		}
		// Grito: la cabeza para arriba, la boca bien abierta, la lengua afuera, los pétalos para atrás y los brazos arriba.
		float sT = q.tGrito(parcial) / 20f;
		if (sT >= 0 && sT < 3.2f) {
			float r = suave(sT / 0.4f) * (1 - suave((sT - 2.4f) / 0.7f));
			float sx = sT > 0.3f && sT < 2.5f ? Mth.sin(sT * 70) * 0.03f : 0;
			for (int i = 0; i < 7; i++) nodo("trunk_swirl_" + i).rx += -r * 0.035f + sx * 0.14f;
			cabeza.rx += -r * 0.35f + sx;
			cabeza.rz = sx * 1.5f;
			mandibula.rx = m0 * (1 - r) + r * 1.1f + sx * 3;
			float tr = suave((sT - 0.25f) / 0.35f) * (1 - suave((sT - 2.3f) / 0.4f));
			lengua.visible = tr > 0.02f;
			lengua.s = Math.max(0.01f, tr);
			for (int i = 0; i < 7; i++) nodo("tongue_j" + i).rx = Mth.sin(sT * 20 - i) * 0.1f * tr;
			for (int k = 0; k < 5; k++) pliegue[k] = pliegue[k] * (1 - r) - r * 0.45f + Mth.sin(sT * 50 + k) * 0.06f * r;
			brazoI.rz += -0.5f * r;
			brazoD.rz += 0.5f * r;
			for (int i = 0; i < 3; i++) {
				nodo("frill_l_" + i).ry += -0.4f * r;
				nodo("frill_r_" + i).ry += 0.4f * r;
			}
			rapidez += 6 * r;
		}
		// Transformación: un pimpollo cerrado mirando al cielo que se abre en Qumara. Antes de nacer queda así.
		if (q.esRosa() || q.tNacer(parcial) < QumaraEntity.TICKS_NACER) {
			float trT = q.esRosa() ? 0 : q.tNacer(parcial) / 20f;
			float g = suave((trT - 2.4f) / 2);
			float temblor = trT > 0.9f && trT < 4.6f ? Mth.sin(trT * 45) * 0.02f * (0.5f + suave((trT - 0.9f) / 2)) : 0;
			for (int i = 0; i < 7; i++) nodo("trunk_swirl_" + i).rz += temblor * (i + 1) * 0.3f;
			cabeza.rx = -1.35f * (1 - g) + cabeza.rx * g + temblor;
			cabeza.ry *= g;
			mandibula.rx *= g;
			for (int k = 0; k < 5; k++) {
				float o = suave((trT - 1.8f - (4 - k) * 0.15f) / 1.1f);
				pliegue[k] = 1.35f * (1 - o) + pliegue[k] * o + (trT < 1.8f ? Mth.sin(t * 1.2f + k) * 0.02f : 0);
			}
			// Lo de "mutante" (la mandíbula, las aletas de los costados, los brazos y la enredadera) aparece al abrirse.
			Nodo[] crecen = {mandibula, nodo("frill_l_0"), nodo("frill_r_0"), nodo("frill_l_1"), nodo("frill_r_1"), nodo("frill_l_2"),
					nodo("frill_r_2"), brazoI, brazoD, enredadera};
			for (int i = 0; i < crecen.length; i++) {
				float f = suave((trT - 3.0f - i * 0.08f) / 0.6f);
				crecen[i].s = Math.max(0.001f, f);
				crecen[i].visible = f > 0.01f;
			}
			brazoD.matriz = null;
			lengua.visible = false;
		}
		// Debilitada: se marchita y se pone celeste.
		float wk = Mth.lerp(parcial, q.debilVistoAntes, q.debilVisto);
		if (wk > 0.001f) {
			float br = Mth.sin(t * 1.2f) * 0.03f * wk;
			for (int i = 0; i < 7; i++) {
				Nodo r = nodo("trunk_swirl_" + i);
				r.rx += wk * 0.07f + br * 0.7f;
				r.rz *= 1 - wk * 0.7f;
			}
			cabeza.rx += wk * 0.75f;
			cabeza.ry *= 1 - wk * 0.8f;
			mandibula.rx = mandibula.rx * (1 - wk) + wk * 0.45f;
			for (int k = 0; k < 5; k++) pliegue[k] = pliegue[k] * (1 - wk) + wk * (0.75f + Mth.sin(k * 1.7f) * 0.15f);
			brazoI.rz += 0.6f * wk;
			brazoD.rz += -0.6f * wk;
			for (int i = 0; i < 7; i++) nodo("vine_j" + i).rx += wk * 0.12f;
			rapidez *= 1 - wk * 0.8f;
		}
		// Derrotada: se estremece, se desploma hacia adelante con un rebote y queda así.
		float dk = 0;
		if (q.derrotada()) {
			float dT = q.tDerrota(parcial) / 20f;
			float x = Mth.clamp((dT - 0.6f) / 2.2f, 0, 1), c1 = 1.4f;
			dk = x <= 0 ? 0 : 1 + (c1 + 1) * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
			float estremece = dT < 0.9f ? Mth.sin(dT * 60) * 0.04f * (1 - dT / 0.9f) : 0;
			float[] doblez = {0.25f, 0.25f, 0.24f, 0.21f, 0.18f, 0.16f, 0.14f};
			for (int i = 0; i < 7; i++) {
				Nodo r = nodo("trunk_swirl_" + i);
				r.rx = r.rx * (1 - dk) + doblez[i] * dk + estremece;
				r.rz = r.rz * (1 - dk) + (i < 3 ? 0.06f : 0.02f) * dk;
			}
			cabeza.rx = cabeza.rx * (1 - dk) + 0.6f * dk + estremece * 2;
			cabeza.ry *= 1 - dk;
			cabeza.rz = cabeza.rz * (1 - dk) + 0.15f * dk;
			mandibula.rx = mandibula.rx * (1 - dk) + 0.6f * dk;
			for (int k = 0; k < 5; k++) pliegue[k] = pliegue[k] * (1 - dk) + dk * (1.05f + Mth.sin(k * 2.3f) * 0.2f);
			brazoI.rz = brazoI.rz * (1 - dk) + 0.9f * dk;
			brazoD.rz = brazoD.rz * (1 - dk) - 0.9f * dk;
			brazoD.matriz = null;
			lengua.visible = false;
			rapidez *= 1 - dk;
		}
		// Atrapar / Sacar: el brazo derecho va a buscar al jugador, lo sube a la cara, le tira gas y lo tira lejos.
		if (q.agarrando()) {
			float g = q.tiempoAgarre(parcial), tS = q.tSacar(parcial), tA = q.tAgarre(parcial);
			QumaraAgarre.Pose p = QumaraAgarre.brazo(g, q.poseAlcance());
			brazoHacia(QumaraAgarre.pinza(Math.max(p.e(), 0.15f), p.yaw(), p.c()), suave(p.e() / 0.4f));
			// La mano envuelve la cabeza del agarrado: en primera persona no lo deja ver nada, así que para él no se dibuja.
			Minecraft mc = Minecraft.getInstance();
			nodo("arm_right_hand").visible = !(mc.player != null && mc.player.getId() == q.agarradoId() && mc.options.getCameraType().isFirstPerson());
			boolean quieto = tS < 0 && tA >= QumaraAgarre.SOSTIENE;
			if (quieto) cabeza.rx += Mth.sin(t * 1.5f) * 0.03f;
			// Boca bien abierta mientras tira gas (se cierra al sacarlo).
			if (tA > 3.0f) {
				float k = suave((tA - 3.0f) / 0.4f) * (tS < 0 ? 1 : 1 - suave(tS / 0.4f)) * (1 + Mth.sin(t * 9) * 0.06f);
				mandibula.rx = mandibula.rx * (1 - k) + 1.0f * k;
				cabeza.rx += 0.12f * k;
			}
		}
		// Las cuchillas de las manos y los pétalos.
		for (String lado : new String[]{"left", "right"}) {
			for (int k = 0; k < 4; k++) {
				for (int i = 0; i < 5; i++) {
					nodo("arm_" + lado + "_blade_" + k + "_j" + i).ry = Mth.sin(t * rapidez + k - i * 0.6f) * 0.08f;
				}
			}
		}
		for (int k = 0; k < 5; k++) doblar(k, pliegue[k]);
		// La flor va detrás de la cabeza: la sigue.
		flor.rx = cabeza.rx;
		flor.ry = cabeza.ry;
		flor.rz = cabeza.rz;
		// Colores: blanco → celeste (#8fd8ff) al debilitarse; → marchito (#8a7a66) al ser derrotada.
		float cel = wk * 0.8f, mar = Math.min(1, Math.max(0, dk)) * 0.6f;
		tinteR = Mth.lerp(mar, Mth.lerp(cel, 1, 0x8f / 255f), 0x8a / 255f);
		tinteG = Mth.lerp(mar, Mth.lerp(cel, 1, 0xd8 / 255f), 0x7a / 255f);
		tinteB = Mth.lerp(mar, 1, 0x66 / 255f);
		ojosApagados = q.derrotada();
	}

	// --- Dibujo ---

	@Override
	public void render(QumaraEntity q, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		cargar();
		animar(q, parcial);
		int overlay = OverlayTexture.pack(0f, q.hurtTime > 0 && !q.derrotada());
		int color = 0xFF000000 | (int) (255 * Math.min(1, tinteR)) << 16 | (int) (255 * Math.min(1, tinteG)) << 8 | (int) (255 * Math.min(1, tinteB));
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, q.yBodyRotO, q.yBodyRot)));
		pose.scale(K, K, K);
		dibujar(pose, vc, raiz, luz, ojosApagados ? luz : LightTexture.FULL_BRIGHT, overlay, color);
		pose.popPose();
		super.render(q, yaw, parcial, pose, buffers, luz);
	}

	private void dibujar(PoseStack pose, VertexConsumer vc, Nodo n, int luz, int luzOjos, int overlay, int color) {
		if (!n.visible) return;
		pose.pushPose();
		pose.translate(n.ox + n.animPos.x, n.oy + n.animPos.y, n.oz + n.animPos.z);
		if (n.matriz != null) {
			pose.last().pose().mul(n.matriz);
			pose.last().normal().mul(new Matrix3f(n.matriz).invert().transpose());
		}
		if (n.rx != 0) pose.mulPose(Axis.XP.rotation(n.rx));
		if (n.ry != 0) pose.mulPose(Axis.YP.rotation(n.ry));
		if (n.rz != 0) pose.mulPose(Axis.ZP.rotation(n.rz));
		pose.mulPose(n.extra);
		pose.mulPose(n.animRot);
		if (n.s != 1 || n.animEsc.x != 1 || n.animEsc.y != 1 || n.animEsc.z != 1) pose.scale(n.s * n.animEsc.x, n.s * n.animEsc.y, n.s * n.animEsc.z);
		caras(pose.last(), vc, n.caras, luz, overlay, color);
		caras(pose.last(), vc, n.brillan, luzOjos, overlay, color);
		for (Nodo h : n.hijos) dibujar(pose, vc, h, luz, luzOjos, overlay, color);
		pose.popPose();
	}

	private static void caras(PoseStack.Pose p, VertexConsumer vc, float[] q, int luz, int overlay, int color) {
		for (int k = 0; k + 23 <= q.length; k += 23) {
			for (int v = 0; v < 4; v++) {
				int o = k + v * 5;
				vc.addVertex(p, q[o], q[o + 1], q[o + 2]).setColor(color).setUv(q[o + 3], q[o + 4])
						.setOverlay(overlay).setLight(luz).setNormal(p, q[k + 20], q[k + 21], q[k + 22]);
			}
		}
	}

	/** Mide mucho más que su caja: se dibuja mientras cualquier parte esté a la vista (el brazo llega lejos). */
	@Override
	public boolean shouldRender(QumaraEntity q, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(q.getBoundingBox().inflate(34, 10, 34));
	}

	@Override
	public ResourceLocation getTextureLocation(QumaraEntity q) {
		return TEXTURA;
	}
}
