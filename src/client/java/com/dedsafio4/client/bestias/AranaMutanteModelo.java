package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * La Araña Mutante: el modelo de assets/dedsafio4/cem/arana_mutante.jem (formato de OptiFine) dibujado sobre las
 * partes de la araña de Minecraft. Cada parte del archivo (head, neck, body, leg1..leg8) cuelga de la parte de la
 * araña con ese nombre, así se mueve con la animación normal (mirar, caminar).
 */
public class AranaMutanteModelo<T extends Entity> extends SpiderModel<T> {
	private static final ResourceLocation ARCHIVO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cem/arana_mutante.jem");
	/** De los nombres de OptiFine a las partes de la araña de Minecraft. */
	private static final Map<String, String> PARTES = Map.ofEntries(Map.entry("head", "head"), Map.entry("neck", "body0"),
			Map.entry("body", "body1"), Map.entry("leg1", "right_hind_leg"), Map.entry("leg2", "left_hind_leg"),
			Map.entry("leg3", "right_middle_hind_leg"), Map.entry("leg4", "left_middle_hind_leg"),
			Map.entry("leg5", "right_middle_front_leg"), Map.entry("leg6", "left_middle_front_leg"),
			Map.entry("leg7", "right_front_leg"), Map.entry("leg8", "left_front_leg"));

	/** Una pieza: dónde gira (píxeles), cuánto (radianes), sus caras (por cara: 4 × (x, y, z, u, v) y la normal) y sus hijas. */
	private record Nodo(float[] pivote, float[] rot, float[] caras, List<Nodo> hijos) {}

	private record Raiz(ModelPart parte, Nodo nodo) {}

	private final ModelPart raiz;
	private List<Raiz> raices;

	public AranaMutanteModelo(ModelPart raiz) {
		super(raiz);
		this.raiz = raiz;
	}

	private List<Raiz> raices() {
		if (raices != null) return raices;
		raices = new ArrayList<>();
		try (Reader lector = Minecraft.getInstance().getResourceManager().openAsReader(ARCHIVO)) {
			JsonObject jem = JsonParser.parseReader(lector).getAsJsonObject();
			JsonArray tam = jem.getAsJsonArray("textureSize");
			float tw = tam.get(0).getAsFloat(), th = tam.get(1).getAsFloat();
			for (JsonElement e : jem.getAsJsonArray("models")) {
				JsonObject m = e.getAsJsonObject();
				String parte = PARTES.get(m.get("part").getAsString());
				if (parte != null) raices.add(new Raiz(raiz.getChild(parte), nodo(m, tw, th)));
			}
		} catch (Exception ex) {
			Dedsafio4.LOGGER.error("No se pudo cargar la Araña Mutante", ex);
		}
		return raices;
	}

	private static float[] numeros(JsonElement e, int n) {
		float[] r = new float[n];
		if (e == null) return r;
		JsonArray a = e.getAsJsonArray();
		for (int i = 0; i < n && i < a.size(); i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	/** Con invertAxis "xy" (como los hace Blockbench): x e y van al revés que en el modelo de Minecraft. */
	private static Nodo nodo(JsonObject m, float tw, float th) {
		float[] t = numeros(m.get("translate"), 3), r = numeros(m.get("rotate"), 3);
		float[] pivote = {-t[0], -t[1], t[2]};
		float[] rot = {-r[0] * Mth.DEG_TO_RAD, -r[1] * Mth.DEG_TO_RAD, r[2] * Mth.DEG_TO_RAD};
		List<Float> caras = new ArrayList<>();
		if (m.has("boxes")) for (JsonElement b : m.getAsJsonArray("boxes")) caja(b.getAsJsonObject(), tw, th, caras);
		float[] q = new float[caras.size()];
		for (int i = 0; i < q.length; i++) q[i] = caras.get(i);
		List<Nodo> hijos = new ArrayList<>();
		if (m.has("submodels")) for (JsonElement s : m.getAsJsonArray("submodels")) hijos.add(nodo(s.getAsJsonObject(), tw, th));
		return new Nodo(pivote, rot, q, hijos);
	}

	private static void caja(JsonObject b, float tw, float th, List<Float> q) {
		float[] c = numeros(b.get("coordinates"), 6);
		float g = b.has("sizeAdd") ? b.get("sizeAdd").getAsFloat() : 0;
		float x0 = -c[0] - c[3] - g, y0 = -c[1] - c[4] - g, z0 = c[2] - g;
		float x1 = -c[0] + g, y1 = -c[1] + g, z1 = c[2] + c[5] + g;
		float[] v7 = {x0, y0, z0}, v = {x1, y0, z0}, v1 = {x1, y1, z0}, v2 = {x0, y1, z0};
		float[] v3 = {x0, y0, z1}, v4 = {x1, y0, z1}, v5 = {x1, y1, z1}, v6 = {x0, y1, z1};
		// Como las caras de un cubo de Minecraft ("up" es la de y chica: en el modelo y crece para abajo).
		Object[][] lados = {
				{"uvEast", new float[][]{v4, v, v1, v5}, 1f, 0f, 0f}, {"uvWest", new float[][]{v7, v3, v6, v2}, -1f, 0f, 0f},
				{"uvUp", new float[][]{v4, v3, v7, v}, 0f, -1f, 0f}, {"uvDown", new float[][]{v1, v2, v6, v5}, 0f, 1f, 0f},
				{"uvNorth", new float[][]{v, v7, v2, v1}, 0f, 0f, -1f}, {"uvSouth", new float[][]{v3, v4, v5, v6}, 0f, 0f, 1f}};
		for (Object[] l : lados) {
			if (!b.has((String) l[0])) continue;
			float[] uv = numeros(b.get((String) l[0]), 4);
			if (uv[0] == uv[2] || uv[1] == uv[3]) continue;   // cara sin textura
			float[][] vs = (float[][]) l[1];
			float[][] uvs = {{uv[2], uv[1]}, {uv[0], uv[1]}, {uv[0], uv[3]}, {uv[2], uv[3]}};
			for (int k = 0; k < 4; k++) {
				q.add(vs[k][0]);
				q.add(vs[k][1]);
				q.add(vs[k][2]);
				q.add(uvs[k][0] / tw);
				q.add(uvs[k][1] / th);
			}
			q.add((float) l[2]);
			q.add((float) l[3]);
			q.add((float) l[4]);
		}
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer vc, int luz, int overlay, int color) {
		for (Raiz r : raices()) {
			ModelPart p = r.parte();
			pose.pushPose();
			pose.translate(p.x / 16f, p.y / 16f, p.z / 16f);
			pose.mulPose(new Quaternionf().rotationZYX(p.zRot, p.yRot, p.xRot));
			dibujar(r.nodo(), pose, vc, luz, overlay, color);
			pose.popPose();
		}
	}

	private static void dibujar(Nodo n, PoseStack pose, VertexConsumer vc, int luz, int overlay, int color) {
		pose.pushPose();
		pose.translate(n.pivote()[0] / 16f, n.pivote()[1] / 16f, n.pivote()[2] / 16f);
		pose.mulPose(new Quaternionf().rotationZYX(n.rot()[2], n.rot()[1], n.rot()[0]));
		PoseStack.Pose ultimo = pose.last();
		float[] q = n.caras();
		for (int k = 0; k + 23 <= q.length; k += 23) {
			for (int v = 0; v < 4; v++) {
				int o = k + v * 5;
				vc.addVertex(ultimo, q[o] / 16f, q[o + 1] / 16f, q[o + 2] / 16f).setColor(color).setUv(q[o + 3], q[o + 4])
						.setOverlay(overlay).setLight(luz).setNormal(ultimo, q[k + 20], q[k + 21], q[k + 22]);
			}
		}
		for (Nodo h : n.hijos()) dibujar(h, pose, vc, luz, overlay, color);
		pose.popPose();
	}
}
