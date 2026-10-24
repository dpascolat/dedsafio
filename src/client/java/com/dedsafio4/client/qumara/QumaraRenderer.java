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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qumara, la Flor Mutante: el modelo de Blockbench (qumara/modelo.json, pasado del .bbmodel con qumara_a_modelo.py;
 * una sola textura, textures/entity/qumara/qumara.png), a casi 38 bloques de alto, con las animaciones del modelo:
 * reposo, gritar, transformar (de pimpollo a Qumara), barrido (el giro), atrapar / atrapar_sostener / sacar (el
 * agarre: el jugador de verdad va donde está el de muestra, ver QumaraAgarre), debilitar y derrotar. El jugador de
 * muestra y su gas no se dibujan (el gas del agarre lo hace el servidor). Debilitada se tiñe de celeste y derrotada
 * de marrón, y los ojos brillan salvo derrotada.
 */
public class QumaraRenderer extends EntityRenderer<QumaraEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/qumara/qumara.png");
	private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "qumara/modelo.json");
	/** Bloques por píxel del modelo: la boca queda a unos 24 bloques y la flor llega a casi 38. */
	private static final float K = 0.3375f;

	private static final class Nodo {
		final String nombre;
		final float ox, oy, oz;
		final float[] caras, brillan;
		final List<Nodo> hijos = new ArrayList<>();
		boolean visible = true;
		final Vector3f pos = new Vector3f(), esc = new Vector3f(1, 1, 1);
		final Quaternionf rot = new Quaternionf();

		Nodo(String nombre, float ox, float oy, float oz, float[] caras, float[] brillan) {
			this.nombre = nombre;
			this.ox = ox; this.oy = oy; this.oz = oz;
			this.caras = caras;
			this.brillan = brillan;
		}

		void reiniciar() {
			visible = true;
			pos.zero();
			esc.set(1, 1, 1);
			rot.identity();
		}
	}

	/** Una animación: por hueso, sus cuadros de rotación (t, x, y, z, w), posición y escala (t, x, y, z). */
	private record Canales(float[][] rot, float[][] pos, float[][] esc) {}
	private record Animacion(float largo, boolean vuelta, Map<String, Canales> huesos) {}

	private final List<Nodo> todos = new ArrayList<>();
	private final Map<String, Nodo> porNombre = new HashMap<>();
	private final Map<String, Animacion> animaciones = new HashMap<>();
	private Nodo raiz;

	public QumaraRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 8f;
	}

	private void cargar() {
		if (raiz != null) return;
		try (Reader lector = Minecraft.getInstance().getResourceManager().openAsReader(MODELO)) {
			JsonObject m = JsonParser.parseReader(lector).getAsJsonObject();
			for (JsonElement e : m.getAsJsonArray("huesos")) {
				JsonObject h = e.getAsJsonObject();
				JsonArray o = h.getAsJsonArray("o");
				Nodo n = new Nodo(h.get("n").getAsString(), o.get(0).getAsFloat(), o.get(1).getAsFloat(), o.get(2).getAsFloat(),
						numeros(h.getAsJsonArray("q")), numeros(h.getAsJsonArray("g")));
				int padre = h.get("p").getAsInt();
				if (padre >= 0) todos.get(padre).hijos.add(n);
				else raiz = n;
				todos.add(n);
				porNombre.put(n.nombre, n);
			}
			for (var e : m.getAsJsonObject("animaciones").entrySet()) {
				JsonObject a = e.getValue().getAsJsonObject();
				Map<String, Canales> huesos = new HashMap<>();
				for (var h : a.getAsJsonObject("huesos").entrySet()) {
					JsonObject c = h.getValue().getAsJsonObject();
					huesos.put(h.getKey(), new Canales(cuadros(c, "rot"), cuadros(c, "pos"), cuadros(c, "esc")));
				}
				animaciones.put(e.getKey(), new Animacion(a.get("largo").getAsFloat(), a.get("vuelta").getAsBoolean(), huesos));
			}
		} catch (Exception ex) {
			Dedsafio4.LOGGER.error("No se pudo cargar el modelo de Qumara", ex);
			raiz = new Nodo("vacio", 0, 0, 0, new float[0], new float[0]);
		}
	}

	private static float[] numeros(JsonArray a) {
		float[] r = new float[a.size()];
		for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	private static float[][] cuadros(JsonObject c, String canal) {
		if (!c.has(canal)) return null;
		JsonArray a = c.getAsJsonArray(canal);
		float[][] r = new float[a.size()][];
		for (int i = 0; i < r.length; i++) r[i] = numeros(a.get(i).getAsJsonArray());
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

	/** El cuadro de antes de ese momento (el de después es el siguiente). */
	private static int cuadro(float[][] l, float t) {
		int i = 0;
		while (i + 1 < l.length && l[i + 1][0] <= t) i++;
		return i;
	}

	private static float avance(float[] a, float[] b, float t) {
		return b[0] > a[0] ? Mth.clamp((t - a[0]) / (b[0] - a[0]), 0, 1) : 0;
	}

	/** Pone esa animación en el momento t (segundos; si da vueltas, se repite; si no, se queda en el final). */
	private void poner(String nombre, float t) {
		Animacion a = animaciones.get(nombre);
		if (a == null) return;
		t = a.vuelta() ? t % a.largo() : Math.min(t, a.largo());
		for (var e : a.huesos().entrySet()) {
			Nodo n = porNombre.get(e.getKey());
			if (n == null) continue;
			Canales c = e.getValue();
			if (c.rot() != null) {
				int i = cuadro(c.rot(), t);
				float[] x = c.rot()[i], y = c.rot()[Math.min(i + 1, c.rot().length - 1)];
				n.rot.set(x[1], x[2], x[3], x[4]).slerp(new Quaternionf(y[1], y[2], y[3], y[4]), avance(x, y, t));
			}
			if (c.pos() != null) n.pos.set(interpolar(c.pos(), t));
			if (c.esc() != null) n.esc.set(interpolar(c.esc(), t));
		}
	}

	private static Vector3f interpolar(float[][] l, float t) {
		int i = cuadro(l, t);
		float[] a = l[i], b = l[Math.min(i + 1, l.length - 1)];
		float k = avance(a, b, t);
		return new Vector3f(Mth.lerp(k, a[1], b[1]), Mth.lerp(k, a[2], b[2]), Mth.lerp(k, a[3], b[3]));
	}

	// Tintes (color de todo el cuerpo): celeste al debilitarse, marchito al ser derrotada.
	private float tinteR = 1, tinteG = 1, tinteB = 1;

	/** Elige qué animación va según lo que está haciendo Qumara. */
	private void animar(QumaraEntity q, float parcial) {
		for (Nodo n : todos) n.reiniciar();
		float t = (q.level().getGameTime() % 72000L + parcial) / 20f;
		float aT = q.tBarrido(parcial) / 20f, sT = q.tGrito(parcial) / 20f;
		boolean naciendo = q.esRosa() || q.tNacer(parcial) < QumaraEntity.TICKS_NACER;
		String fx = null;
		if (naciendo) {
			float tr = q.esRosa() ? 0 : q.tNacer(parcial) / 20f;
			poner("transformar", tr);
			fx = tr > 4.15f && tr < 6.25f ? "petalos" : null;
		} else if (q.derrotada()) {
			poner("derrotar", q.tDerrota(parcial) / 20f);
		} else if (q.agarrando()) {
			float tS = q.tSacar(parcial), tA = q.tAgarre(parcial);
			if (tS >= 0) poner(tS < 1.8f ? "sacar" : "reposo", tS < 1.8f ? tS : t);
			else if (tA < QumaraAgarre.SOSTIENE) poner("atrapar", tA);
			else poner("atrapar_sostener", tA - QumaraAgarre.SOSTIENE);
		} else if (aT >= 0 && aT < 1.8f) {
			poner("barrido", aT);
			fx = "giro";
		} else if (sT >= 0 && sT < 2f) {
			poner("gritar", sT);
		} else if (q.debil()) {
			poner("debilitar", t);
		} else {
			poner("reposo", t);
		}
		// El jugador de muestra y el gas del modelo no van (el jugador es el de verdad y el gas lo pone el servidor).
		nodo("player").visible = false;
		nodo("gas").visible = false;
		// Los efectos del giro y los pétalos que vuelan al transformarse, solo mientras pasan.
		nodo("barrido_fx").visible = "giro".equals(fx);
		if ("giro".equals(fx)) {
			nodo("shock_ring_a").visible = nodo("shock_ring_b").visible = aT >= 0.7f && aT < 1.45f;
			nodo("streaks").visible = aT >= 0.45f && aT < 1.25f;
			for (int i = 0; i < 12; i++) nodo("dust_" + i).visible = aT >= 0.5f && aT < 1.2f;
		}
		for (int i = 0; i < 16; i++) nodo("petal_debris_" + i).visible = "petalos".equals(fx);
		// Agarrando: la mano tapa la cabeza del agarrado; en primera persona no lo deja ver nada, así que para él no va.
		if (q.agarrando()) {
			Minecraft mc = Minecraft.getInstance();
			nodo("arm_left_hand").visible = !(mc.player != null && mc.player.getId() == q.agarradoId() && mc.options.getCameraType().isFirstPerson());
		}
		// Colores: blanco → celeste (#8fd8ff) al debilitarse; → marchito (#8a7a66) al ser derrotada.
		float wk = Mth.lerp(parcial, q.debilVistoAntes, q.debilVisto);
		float cel = wk * 0.8f, mar = q.derrotada() ? Mth.clamp((q.tDerrota(parcial) / 20f - 0.6f) / 2.2f, 0, 1) * 0.6f : 0;
		tinteR = Mth.lerp(mar, Mth.lerp(cel, 1, 0x8f / 255f), 0x8a / 255f);
		tinteG = Mth.lerp(mar, Mth.lerp(cel, 1, 0xd8 / 255f), 0x7a / 255f);
		tinteB = Mth.lerp(mar, 1, 0x66 / 255f);
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
		dibujar(pose, vc, raiz, luz, q.derrotada() ? luz : LightTexture.FULL_BRIGHT, overlay, color);
		pose.popPose();
		super.render(q, yaw, parcial, pose, buffers, luz);
	}

	private void dibujar(PoseStack pose, VertexConsumer vc, Nodo n, int luz, int luzOjos, int overlay, int color) {
		if (!n.visible) return;
		pose.pushPose();
		pose.translate(n.ox + n.pos.x, n.oy + n.pos.y, n.oz + n.pos.z);
		pose.mulPose(n.rot);
		if (n.esc.x != 1 || n.esc.y != 1 || n.esc.z != 1) pose.scale(n.esc.x, n.esc.y, n.esc.z);
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

	/** Mide mucho más que su caja: se dibuja mientras cualquier parte esté a la vista (los brazos llegan lejos). */
	@Override
	public boolean shouldRender(QumaraEntity q, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(q.getBoundingBox().inflate(34, 10, 34));
	}

	@Override
	public ResourceLocation getTextureLocation(QumaraEntity q) {
		return TEXTURA;
	}
}
