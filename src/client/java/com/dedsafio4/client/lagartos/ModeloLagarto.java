package com.dedsafio4.client.lagartos;

import com.dedsafio4.Dedsafio4;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Un modelo de los lagartos (assets/dedsafio4/lagartos/<nombre>.json, sacado del diseño en HTML con
 * lagartos_extraer.mjs y lagartos_a_modelo.py): grupos (huesos) con sus cajas ya hechas caras, cada cara de un
 * material de color (las escamas, la panza y la cresta cambian con la variante). Se dibuja con una textura blanca y
 * el color en cada vértice; los ojos y las brasas brillan. Las medidas son en píxeles.
 */
public final class ModeloLagarto {
	public static final ResourceLocation BLANCO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/momentito/blanco.png");

	/** Un grupo del modelo: su lugar y giro de reposo, y lo que le pone la animación. */
	public static final class Hueso {
		public final String nombre;
		final float ox, oy, oz, r0x, r0y, r0z, s0x, s0y, s0z;
		final boolean visible0;
		final float[] caras;
		final List<Hueso> hijos = new ArrayList<>();
		public Hueso padre;
		public float x, y, z, rx, ry, rz, sx, sy, sz;
		public boolean visible;

		Hueso(String nombre, float[] o, float[] r, float[] s, boolean visible, float[] caras) {
			this.nombre = nombre;
			ox = o[0]; oy = o[1]; oz = o[2];
			r0x = r[0]; r0y = r[1]; r0z = r[2];
			s0x = s[0]; s0y = s[1]; s0z = s[2];
			this.visible0 = visible;
			this.caras = caras;
			reiniciar();
		}

		void reiniciar() {
			x = ox; y = oy; z = oz;
			rx = r0x; ry = r0y; rz = r0z;
			sx = s0x; sy = s0y; sz = s0z;
			visible = visible0;
		}

		public void giro(float x, float y, float z) {
			rx = x; ry = y; rz = z;
		}
	}

	private static final Map<String, ModeloLagarto> CARGADOS = new HashMap<>();

	public final List<Hueso> huesos = new ArrayList<>();
	private final Map<String, Hueso> porNombre = new HashMap<>();
	private int[] colores = new int[0];
	private boolean[] brillan = new boolean[0];
	private String[] nombresMat = new String[0];
	private final List<Map<String, Integer>> variantes = new ArrayList<>();

	public static ModeloLagarto de(String nombre) {
		return CARGADOS.computeIfAbsent(nombre, ModeloLagarto::new);
	}

	private ModeloLagarto(String nombre) {
		ResourceLocation archivo = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "lagartos/" + nombre + ".json");
		try (Reader lector = Minecraft.getInstance().getResourceManager().openAsReader(archivo)) {
			JsonObject m = JsonParser.parseReader(lector).getAsJsonObject();
			JsonArray mats = m.getAsJsonArray("materiales");
			colores = new int[mats.size()];
			brillan = new boolean[mats.size()];
			nombresMat = new String[mats.size()];
			for (int i = 0; i < mats.size(); i++) {
				JsonObject o = mats.get(i).getAsJsonObject();
				colores[i] = o.get("c").getAsInt();
				brillan[i] = o.get("e").getAsBoolean();
				nombresMat[i] = o.get("n").getAsString();
			}
			for (var e : m.getAsJsonObject("variantes").entrySet()) {
				Map<String, Integer> v = new HashMap<>();
				for (var c : e.getValue().getAsJsonObject().entrySet()) v.put(c.getKey(), c.getValue().getAsInt());
				variantes.add(v);
			}
			for (JsonElement e : m.getAsJsonArray("huesos")) {
				JsonObject h = e.getAsJsonObject();
				Hueso hueso = new Hueso(h.get("n").getAsString(), numeros(h.getAsJsonArray("o")), numeros(h.getAsJsonArray("r")),
						numeros(h.getAsJsonArray("s")), h.get("v").getAsBoolean(), numeros(h.getAsJsonArray("q")));
				int p = h.get("p").getAsInt();
				if (p >= 0) {
					hueso.padre = huesos.get(p);
					hueso.padre.hijos.add(hueso);
				}
				huesos.add(hueso);
				// Los dos antebrazos se llaman igual: se distinguen por el brazo.
				String clave = hueso.nombre;
				if (clave.equals("antebrazo") && hueso.padre != null) clave = hueso.padre.nombre + "/antebrazo";
				porNombre.putIfAbsent(clave, hueso);
			}
		} catch (Exception ex) {
			Dedsafio4.LOGGER.error("No se pudo cargar el modelo del lagarto " + nombre, ex);
			huesos.add(new Hueso("vacio", new float[3], new float[3], new float[]{1, 1, 1}, true, new float[0]));
		}
	}

	private static float[] numeros(JsonArray a) {
		float[] r = new float[a.size()];
		for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	/** El hueso con ese nombre (los antebrazos: "brazo_izq/antebrazo" y "brazo_der/antebrazo"). */
	public Hueso hueso(String nombre) {
		Hueso h = porNombre.get(nombre);
		if (h == null) {
			h = new Hueso(nombre, new float[3], new float[3], new float[]{1, 1, 1}, true, new float[0]);   // por si falta: no hace nada
			porNombre.put(nombre, h);
		}
		return h;
	}

	/** Todo a como está en reposo. */
	public void reiniciar() {
		for (Hueso h : huesos) h.reiniciar();
	}

	/** Dibuja desde ese hueso (o todo, con la raíz), con los colores de esa variante. */
	public void dibujar(Hueso desde, PoseStack pose, VertexConsumer vc, int luz, int overlay, int variante, boolean ojosApagados) {
		Map<String, Integer> v = variantes.isEmpty() ? Map.of() : variantes.get(Math.floorMod(variante, variantes.size()));
		int[] cols = new int[colores.length];
		for (int i = 0; i < cols.length; i++) cols[i] = 0xFF000000 | v.getOrDefault(nombresMat[i], colores[i]);
		dibujar(desde, pose, vc, luz, overlay, cols, ojosApagados);
	}

	public Hueso raiz() {
		return huesos.get(0);
	}

	private void dibujar(Hueso h, PoseStack pose, VertexConsumer vc, int luz, int overlay, int[] cols, boolean ojosApagados) {
		if (!h.visible) return;
		pose.pushPose();
		pose.translate(h.x, h.y, h.z);
		if (h.rx != 0) pose.mulPose(Axis.XP.rotation(h.rx));
		if (h.ry != 0) pose.mulPose(Axis.YP.rotation(h.ry));
		if (h.rz != 0) pose.mulPose(Axis.ZP.rotation(h.rz));
		if (h.sx != 1 || h.sy != 1 || h.sz != 1) pose.scale(h.sx, h.sy, h.sz);
		PoseStack.Pose p = pose.last();
		float[] q = h.caras;
		for (int k = 0; k + 16 <= q.length; k += 16) {
			int mat = (int) q[k + 15];
			int l = brillan[mat] && !ojosApagados ? LightTexture.FULL_BRIGHT : luz;
			for (int i = 0; i < 4; i++) {
				vc.addVertex(p, q[k + i * 3], q[k + i * 3 + 1], q[k + i * 3 + 2]).setColor(cols[mat]).setUv(0.5f, 0.5f)
						.setOverlay(overlay).setLight(l).setNormal(p, q[k + 12], q[k + 13], q[k + 14]);
			}
		}
		for (Hueso hijo : h.hijos) dibujar(hijo, pose, vc, luz, overlay, cols, ojosApagados);
		pose.popPose();
	}
}
