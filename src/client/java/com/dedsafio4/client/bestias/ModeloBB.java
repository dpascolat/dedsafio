package com.dedsafio4.client.bestias;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Un modelo de Blockbench en formato libre, leído de assets/dedsafio4/modelos/NOMBRE.json (lo arma bb2json.py a
 * partir del .bbmodel de Patricio): los grupos con su centro y su giro, los cubos con el pedazo de textura de cada
 * cara, y las animaciones (rotación y posición de cada grupo, con fotogramas lineales).
 * Se dibuja igual que en Blockbench: cada grupo y cada cubo giran sobre su centro (Z, después Y, después X), así
 * que sirve con cualquier ángulo (los modelos comunes de Minecraft solo giran de a 22,5°).
 */
public final class ModeloBB {
	private record Nodo(String nombre, int padre, float[] origen, float[] giro, List<Integer> hijos, List<Cubo> cubos) {}

	/** desde, hasta, centro y giro del cubo; por cada cara (norte, este, sur, oeste, arriba, abajo): u1, v1, u2, v2, giro de la textura (o null si no tiene). */
	private record Cubo(float[] desde, float[] hasta, float[] origen, float[] giro, float[][] caras) {}

	/** Fotogramas de un canal: {tiempo, x, y, z}. */
	private record Animacion(float largo, boolean bucle, Map<Integer, float[][]> rotacion, Map<Integer, float[][]> posicion) {}

	private final List<Nodo> nodos = new ArrayList<>();
	private final List<Cubo> sueltos = new ArrayList<>();
	private final Map<String, Animacion> animaciones = new HashMap<>();
	private final float ancho, alto;

	private ModeloBB(JsonObject j) {
		ancho = j.get("ancho").getAsFloat();
		alto = j.get("alto").getAsFloat();
		Map<String, Integer> porNombre = new HashMap<>();
		for (JsonElement e : j.getAsJsonArray("nodos")) {
			JsonObject o = e.getAsJsonObject();
			int padre = o.get("padre").getAsInt();
			Nodo n = new Nodo(o.get("nombre").getAsString(), padre, vector(o, "origen"), vector(o, "giro"), new ArrayList<>(), new ArrayList<>());
			porNombre.putIfAbsent(n.nombre(), nodos.size());
			if (padre >= 0) nodos.get(padre).hijos().add(nodos.size());
			nodos.add(n);
		}
		for (JsonElement e : j.getAsJsonArray("cubos")) {
			JsonObject o = e.getAsJsonObject();
			float[][] caras = new float[6][];
			JsonArray cs = o.getAsJsonArray("caras");
			for (int i = 0; i < 6; i++) if (!cs.get(i).isJsonNull()) caras[i] = numeros(cs.get(i).getAsJsonArray());
			Cubo c = new Cubo(vector(o, "desde"), vector(o, "hasta"), vector(o, "origen"), vector(o, "giro"), caras);
			int nodo = o.get("nodo").getAsInt();
			(nodo >= 0 ? nodos.get(nodo).cubos() : sueltos).add(c);
		}
		for (Map.Entry<String, JsonElement> e : j.getAsJsonObject("animaciones").entrySet()) {
			JsonObject o = e.getValue().getAsJsonObject();
			Map<Integer, float[][]> rot = new HashMap<>(), pos = new HashMap<>();
			for (Map.Entry<String, JsonElement> h : o.getAsJsonObject("huesos").entrySet()) {
				Integer nodo = porNombre.get(h.getKey());
				if (nodo == null) continue;
				JsonObject canales = h.getValue().getAsJsonObject();
				if (canales.has("rotation")) rot.put(nodo, fotogramas(canales.getAsJsonArray("rotation")));
				if (canales.has("position")) pos.put(nodo, fotogramas(canales.getAsJsonArray("position")));
			}
			animaciones.put(e.getKey(), new Animacion(o.get("largo").getAsFloat(), o.get("bucle").getAsBoolean(), rot, pos));
		}
	}

	public static ModeloBB cargar(String nombre) {
		String ruta = "/assets/dedsafio4/modelos/" + nombre + ".json";
		try (InputStream in = ModeloBB.class.getResourceAsStream(ruta)) {
			if (in == null) throw new IllegalStateException("No está el modelo " + ruta);
			return new ModeloBB(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
		} catch (java.io.IOException e) {
			throw new IllegalStateException("No se pudo leer el modelo " + ruta, e);
		}
	}

	private static float[] vector(JsonObject o, String clave) {
		return numeros(o.getAsJsonArray(clave));
	}

	private static float[] numeros(JsonArray a) {
		float[] r = new float[a.size()];
		for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	private static float[][] fotogramas(JsonArray a) {
		float[][] r = new float[a.size()][];
		for (int i = 0; i < r.length; i++) r[i] = numeros(a.get(i).getAsJsonArray());
		return r;
	}

	/** Un estado vacío para las animaciones: por cada grupo, giro extra (x, y, z) y corrimiento (x, y, z). */
	public float[][] estado() {
		return new float[nodos.size()][6];
	}

	/** El grupo con ese nombre (para moverlo a mano desde el renderer), o -1. */
	public int grupo(String nombre) {
		for (int i = 0; i < nodos.size(); i++) if (nodos.get(i).nombre().equals(nombre)) return i;
		return -1;
	}

	/** Suma al estado la animación en el segundo "t" (si es de una sola vez, se queda en el último fotograma), multiplicada por "peso". */
	public void animar(float[][] estado, String nombre, float t, float peso) {
		Animacion a = animaciones.get(nombre);
		if (a == null || peso == 0) return;
		if (a.bucle() && a.largo() > 0) t %= a.largo();
		for (Map.Entry<Integer, float[][]> e : a.rotacion().entrySet()) sumar(estado[e.getKey()], 0, e.getValue(), t, peso);
		for (Map.Entry<Integer, float[][]> e : a.posicion().entrySet()) sumar(estado[e.getKey()], 3, e.getValue(), t, peso);
	}

	public float largo(String nombre) {
		Animacion a = animaciones.get(nombre);
		return a == null ? 0 : a.largo();
	}

	private static void sumar(float[] destino, int desde, float[][] f, float t, float peso) {
		float[] v;
		if (t <= f[0][0]) v = f[0];
		else if (t >= f[f.length - 1][0]) v = f[f.length - 1];
		else {
			int i = 0;
			while (f[i + 1][0] < t) i++;
			float[] a = f[i], b = f[i + 1];
			float k = b[0] == a[0] ? 1 : (t - a[0]) / (b[0] - a[0]);
			v = new float[]{0, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k, a[3] + (b[3] - a[3]) * k};
		}
		destino[desde] += v[1] * peso;
		destino[desde + 1] += v[2] * peso;
		destino[desde + 2] += v[3] * peso;
	}

	/** Dibuja el modelo (en píxeles de Blockbench) con el estado de las animaciones (puede ser null). */
	public void dibujar(PoseStack pose, VertexConsumer vc, float[][] estado, int luz, int overlay, int color) {
		for (Cubo c : sueltos) cubo(pose, vc, c, luz, overlay, color);
		for (int i = 0; i < nodos.size(); i++) if (nodos.get(i).padre() < 0) grupo(pose, vc, i, estado, luz, overlay, color);
	}

	private void grupo(PoseStack pose, VertexConsumer vc, int i, float[][] estado, int luz, int overlay, int color) {
		Nodo n = nodos.get(i);
		float[] o = n.origen(), e = estado == null ? new float[6] : estado[i];
		pose.pushPose();
		// Como Blockbench: la animación resta el giro en X y en Y y suma el de Z; el corrimiento en X va al revés.
		pose.translate(-e[3], e[4], e[5]);
		pose.translate(o[0], o[1], o[2]);
		girar(pose, n.giro()[0] - e[0], n.giro()[1] - e[1], n.giro()[2] + e[2]);
		pose.translate(-o[0], -o[1], -o[2]);
		for (Cubo c : n.cubos()) cubo(pose, vc, c, luz, overlay, color);
		for (int h : n.hijos()) grupo(pose, vc, h, estado, luz, overlay, color);
		pose.popPose();
	}

	private static void girar(PoseStack pose, float x, float y, float z) {
		if (z != 0) pose.mulPose(Axis.ZP.rotationDegrees(z));
		if (y != 0) pose.mulPose(Axis.YP.rotationDegrees(y));
		if (x != 0) pose.mulPose(Axis.XP.rotationDegrees(x));
	}

	private void cubo(PoseStack pose, VertexConsumer vc, Cubo c, int luz, int overlay, int color) {
		pose.pushPose();
		float[] o = c.origen();
		pose.translate(o[0], o[1], o[2]);
		girar(pose, c.giro()[0], c.giro()[1], c.giro()[2]);
		pose.translate(-o[0], -o[1], -o[2]);
		float x0 = c.desde()[0], y0 = c.desde()[1], z0 = c.desde()[2], x1 = c.hasta()[0], y1 = c.hasta()[1], z1 = c.hasta()[2];
		PoseStack.Pose p = pose.last();
		float[][] f = c.caras();
		// Cada cara: sus 4 esquinas arriba-izquierda, arriba-derecha, abajo-derecha, abajo-izquierda (mirándola desde afuera).
		cara(vc, p, f[0], luz, overlay, color, 0, 0, -1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0);   // norte
		cara(vc, p, f[1], luz, overlay, color, 1, 0, 0, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1);    // este
		cara(vc, p, f[2], luz, overlay, color, 0, 0, 1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);    // sur
		cara(vc, p, f[3], luz, overlay, color, -1, 0, 0, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0);   // oeste
		cara(vc, p, f[4], luz, overlay, color, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);    // arriba
		cara(vc, p, f[5], luz, overlay, color, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);   // abajo
		pose.popPose();
	}

	private void cara(VertexConsumer vc, PoseStack.Pose p, float[] f, int luz, int overlay, int color, float nx, float ny, float nz,
					  float... q) {
		if (f == null) return;
		float u1 = f[0] / ancho, v1 = f[1] / alto, u2 = f[2] / ancho, v2 = f[3] / alto;
		float[][] uv = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
		// La textura girada (de a 90°): cada esquina toma la de la esquina anterior.
		int giro = Math.floorMod(Math.round(f[4] / 90f), 4);
		for (int k = 0; k < 4; k++) {
			float[] t = uv[Math.floorMod(k - giro, 4)];
			vc.addVertex(p, q[k * 3], q[k * 3 + 1], q[k * 3 + 2]).setColor(color).setUv(t[0], t[1])
					.setOverlay(overlay).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}
}
