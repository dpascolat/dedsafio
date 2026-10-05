package com.dedsafio4.client.disfraz;

import com.dedsafio4.Dedsafio4;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dibuja los modelos del Skin Pack Dedsafío (de Customizable Player Models) en lugar del jugador. Como en ese mod,
 * las 6 piezas principales (cabeza, cuerpo, brazos y piernas) se mueven con la animación normal del jugador
 * (caminar, mirar, pegar, agacharse...) corridas lo que diga el modelo, y los cubos cuelgan de ellas. Encima van
 * las animaciones del modelo: la de siempre ("global") y la de la pose (caminar, correr, agacharse...).
 */
public final class SkinsCliente {
	private SkinsCliente() {}

	private static final ResourceLocation BLANCO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/momentito/blanco.png");

	/** Una pieza: dónde gira, cuánto está girada (grados), si se ve, su color (cubos sin textura) y sus caras. */
	private record Pieza(float[] pos, float[] rot, boolean visible, int color, boolean brilla, float[] caras, int[] hijos) {}

	/** Lo que cambia una animación en una pieza en un cuadro. */
	private record Cambio(float[] pos, float[] rot, float[] escala, boolean visible) {}

	private record Animacion(int duracion, boolean suma, boolean vuelta, List<Map<Integer, Cambio>> cuadros) {}

	private record Modelo(ResourceLocation textura, List<Pieza> piezas, Map<String, Animacion> anims) {}

	private static final Map<String, Modelo> MODELOS = new HashMap<>();
	/** Por jugador: en qué pose está y desde cuándo (para que la animación de la pose empiece desde el principio). */
	private static final Map<UUID, Object[]> POSES = new HashMap<>();

	private static Modelo modelo(String nombre) {
		if (MODELOS.containsKey(nombre)) return MODELOS.get(nombre);
		Modelo m = null;
		ResourceLocation ruta = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "skins/" + nombre + ".json");
		try (Reader lector = Minecraft.getInstance().getResourceManager().openAsReader(ruta)) {
			m = leer(nombre, JsonParser.parseReader(lector).getAsJsonObject());
		} catch (Exception e) {
			Dedsafio4.LOGGER.error("No se pudo cargar la skin " + nombre, e);
		}
		MODELOS.put(nombre, m);
		return m;
	}

	private static float[] numeros(JsonElement e) {
		JsonArray a = e.getAsJsonArray();
		float[] r = new float[a.size()];
		for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	private static Modelo leer(String nombre, JsonObject o) {
		List<Pieza> piezas = new ArrayList<>();
		for (JsonElement e : o.getAsJsonArray("piezas")) {
			JsonObject p = e.getAsJsonObject();
			JsonArray h = p.getAsJsonArray("h");
			int[] hijos = new int[h.size()];
			for (int i = 0; i < hijos.length; i++) hijos[i] = h.get(i).getAsInt();
			piezas.add(new Pieza(numeros(p.get("p")), numeros(p.get("r")), p.get("v").getAsBoolean(),
					p.has("c") ? p.get("c").getAsInt() : -1, p.has("g") && p.get("g").getAsBoolean(),
					p.has("q") ? numeros(p.get("q")) : new float[0], hijos));
		}
		Map<String, Animacion> anims = new HashMap<>();
		for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("anims").entrySet()) {
			JsonObject a = e.getValue().getAsJsonObject();
			List<Map<Integer, Cambio>> cuadros = new ArrayList<>();
			for (JsonElement c : a.getAsJsonArray("cuadros")) {
				Map<Integer, Cambio> cuadro = new HashMap<>();
				for (Map.Entry<String, JsonElement> cc : c.getAsJsonObject().entrySet()) {
					JsonObject v = cc.getValue().getAsJsonObject();
					cuadro.put(Integer.parseInt(cc.getKey()), new Cambio(numeros(v.get("p")), numeros(v.get("r")),
							numeros(v.get("s")), v.get("v").getAsBoolean()));
				}
				cuadros.add(cuadro);
			}
			anims.put(e.getKey(), new Animacion(Math.max(1, a.get("dur").getAsInt()), a.get("suma").getAsBoolean(),
					a.get("vuelta").getAsBoolean(), cuadros));
		}
		return new Modelo(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/skins/" + nombre + ".png"),
				piezas, anims);
	}

	// ---------------------------------------------------------------- Animaciones

	/** La pose del modelo según lo que hace el jugador (como las poses de Customizable Player Models). */
	private static String pose(AbstractClientPlayer j, Modelo m) {
		boolean mueve = j.walkAnimation.speed() > 0.1f;
		if (j.isCrouching()) return mueve && m.anims.containsKey("sneak_walk") ? "sneak_walk" : "sneaking";
		if (j.isSprinting() && mueve && m.anims.containsKey("running")) return "running";
		if (mueve) return "walking";
		return "standing";
	}

	private static float giro(float a, float b, float f) {
		float d = ((b - a) % 360 + 540) % 360 - 180;
		return a + d * f;
	}

	/** Suma (o pone) lo que dice la animación en este momento a las piezas. */
	private static void aplicar(Animacion a, long ms, float[][] pos, float[][] rot, float[][] esc, boolean[] vis) {
		int n = a.cuadros().size();
		if (n == 0) return;
		float t = (ms % a.duracion()) / (float) a.duracion();
		int i0, i1;
		float f;
		if (n == 1) {
			i0 = i1 = 0;
			f = 0;
		} else if (a.vuelta()) {
			float ft = t * n;
			i0 = Math.min(n - 1, (int) ft);
			i1 = (i0 + 1) % n;
			f = ft - i0;
		} else {
			float ft = t * (n - 1);
			i0 = Math.min(n - 1, (int) ft);
			i1 = Math.min(n - 1, i0 + 1);
			f = ft - i0;
		}
		Map<Integer, Cambio> c0 = a.cuadros().get(i0), c1 = a.cuadros().get(i1);
		for (Map.Entry<Integer, Cambio> e : c0.entrySet()) {
			int k = e.getKey();
			if (k < 0 || k >= pos.length) continue;
			Cambio x = e.getValue(), y = c1.getOrDefault(k, x);
			for (int d = 0; d < 3; d++) {
				float p = Mth.lerp(f, x.pos()[d], y.pos()[d]), r = giro(x.rot()[d], y.rot()[d], f);
				if (a.suma()) {
					pos[k][d] += p;
					rot[k][d] += r;
				} else {
					pos[k][d] = p;
					rot[k][d] = r;
				}
				esc[k][d] *= Mth.lerp(f, x.escala()[d], y.escala()[d]);
			}
			if (k >= 6) vis[k] = x.visible();   // en las 6 principales, "visible" es la pieza del jugador normal
		}
	}

	// ---------------------------------------------------------------- Dibujo

	/**
	 * Dibuja el modelo con la pose ya armada (el PoseStack ya está como el del jugador, después de girarlo y dar
	 * vuelta, y el modelo de jugador ya tiene su animación). Devuelve false si no hay modelo con ese nombre.
	 */
	public static boolean dibujar(String nombre, HumanoidModel<?> jugadorModelo, AbstractClientPlayer jugador, PoseStack pose,
								  MultiBufferSource buffers, int luz, int overlay) {
		Modelo m = modelo(nombre);
		if (m == null) return false;
		int n = m.piezas().size();
		float[][] pos = new float[n][], rot = new float[n][], esc = new float[n][];
		boolean[] vis = new boolean[n];
		for (int i = 0; i < n; i++) {
			Pieza p = m.piezas().get(i);
			pos[i] = p.pos().clone();
			rot[i] = p.rot().clone();
			esc[i] = new float[]{1, 1, 1};
			vis[i] = p.visible();
		}
		// Animaciones: la de siempre y la de la pose.
		String ahora = pose(jugador, m);
		Object[] antes = POSES.get(jugador.getUUID());
		if (antes == null || !antes[0].equals(ahora)) POSES.put(jugador.getUUID(), antes = new Object[]{ahora, Util.getMillis()});
		long desde = Util.getMillis() - (Long) antes[1];
		Animacion global = m.anims().get("global"), dePose = m.anims().get(ahora);
		if (global != null) aplicar(global, Util.getMillis(), pos, rot, esc, vis);
		if (dePose != null) aplicar(dePose, desde, pos, rot, esc, vis);

		ModelPart[] partes = {jugadorModelo.head, jugadorModelo.body, jugadorModelo.leftArm, jugadorModelo.rightArm,
				jugadorModelo.leftLeg, jugadorModelo.rightLeg};
		for (int i = 0; i < 6 && i < n; i++) {
			ModelPart mp = partes[i];
			pose.pushPose();
			pose.translate((mp.x + pos[i][0]) / 16f, (mp.y + pos[i][1]) / 16f, (mp.z + pos[i][2]) / 16f);
			pose.mulPose(new Quaternionf().rotationZYX(mp.zRot + rot[i][2] * Mth.DEG_TO_RAD,
					mp.yRot + rot[i][1] * Mth.DEG_TO_RAD, mp.xRot + rot[i][0] * Mth.DEG_TO_RAD));
			pose.scale(esc[i][0], esc[i][1], esc[i][2]);
			for (int h : m.piezas().get(i).hijos()) pieza(m, h, pos, rot, esc, vis, pose, buffers, luz, overlay);
			pose.popPose();
		}
		return true;
	}

	private static void pieza(Modelo m, int i, float[][] pos, float[][] rot, float[][] esc, boolean[] vis, PoseStack pose,
							  MultiBufferSource buffers, int luz, int overlay) {
		Pieza p = m.piezas().get(i);
		pose.pushPose();
		pose.translate(pos[i][0] / 16f, pos[i][1] / 16f, pos[i][2] / 16f);
		pose.mulPose(new Quaternionf().rotationZYX(rot[i][2] * Mth.DEG_TO_RAD, rot[i][1] * Mth.DEG_TO_RAD, rot[i][0] * Mth.DEG_TO_RAD));
		pose.scale(Math.max(esc[i][0], 0.01f), Math.max(esc[i][1], 0.01f), Math.max(esc[i][2], 0.01f));
		if (vis[i] && p.caras().length > 0) {
			boolean conColor = p.color() >= 0;
			ResourceLocation tex = conColor ? BLANCO : m.textura();
			VertexConsumer vc = buffers.getBuffer(p.brilla() ? RenderType.eyes(tex) : RenderType.entityTranslucent(tex));
			int r = conColor ? p.color() >> 16 & 255 : 255, g = conColor ? p.color() >> 8 & 255 : 255, b = conColor ? p.color() & 255 : 255;
			int l = p.brilla() ? LightTexture.FULL_BRIGHT : luz;
			PoseStack.Pose ultimo = pose.last();
			float[] q = p.caras();
			for (int k = 0; k + 23 <= q.length; k += 23) {
				float nx = q[k + 20], ny = q[k + 21], nz = q[k + 22];
				for (int v = 0; v < 4; v++) {
					int o = k + v * 5;
					vc.addVertex(ultimo, q[o] / 16f, q[o + 1] / 16f, q[o + 2] / 16f).setColor(r, g, b, 255)
							.setUv(q[o + 3], q[o + 4]).setOverlay(overlay).setLight(l).setNormal(ultimo, nx, ny, nz);
				}
			}
		}
		for (int h : p.hijos()) pieza(m, h, pos, rot, esc, vis, pose, buffers, luz, overlay);
		pose.popPose();
	}
}
