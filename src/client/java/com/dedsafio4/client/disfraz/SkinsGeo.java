package com.dedsafio4.client.disfraz;

import com.dedsafio4.Dedsafio4;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.joml.Quaternionf;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Las skins de /cambiarmob hechas en Blockbench (modelo Bedrock: assets/dedsafio4/skins/<nombre>.geo.json y su
 * textura en textures/entity/skins/<nombre>.png). Los huesos de más arriba se mueven con la animación normal del
 * jugador (cabeza, cuerpo, brazos y piernas) según su nombre, y los huesos "armor_..." no se dibujan con la skin:
 * se dibujan con la textura de la armadura que tiene puesta el jugador, así la armadura queda a la medida del
 * personaje (casco en armor_helmet, pechera en armor_body y los brazos, pantalones en armor_leggings y las piernas,
 * botas en armor_..._foot).
 */
public final class SkinsGeo {
	private SkinsGeo() {}

	/** Una cara ya armada: 4 vértices (x, y, z, u, v) en el espacio del modelo del jugador y la normal. */
	private record Cara(float[] q, float nx, float ny, float nz) {}

	/** Un hueso: dónde gira (en píxeles, en el espacio del modelo del jugador), cuánto (radianes) y sus caras. */
	private record Hueso(String nombre, float[] pivote, float[] rot, List<Cara> caras, List<Integer> hijos) {}

	/** El modelo: sus huesos, cuáles van colgados de cada parte del jugador, y para cuál armadura es cada uno. */
	private record Modelo(ResourceLocation textura, List<Hueso> huesos, List<Integer> raices, Map<Integer, Integer> parte,
						  Map<Integer, EquipmentSlot> armadura) {}

	private static final Map<String, Modelo> MODELOS = new HashMap<>();

	/** Dónde está cada parte del jugador cuando está quieto (para mover el hueso lo mismo que se movió la parte). */
	private static final float[][] REPOSO = {{0, 0, 0}, {0, 0, 0}, {5, 2, 0}, {-5, 2, 0}, {1.9f, 12, 0}, {-1.9f, 12, 0}};

	private static Modelo modelo(String nombre) {
		if (MODELOS.containsKey(nombre)) return MODELOS.get(nombre);
		Modelo m = null;
		ResourceLocation ruta = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "skins/" + nombre + ".geo.json");
		var recurso = Minecraft.getInstance().getResourceManager().getResource(ruta);
		if (recurso.isPresent()) {
			try (Reader lector = recurso.get().openAsReader()) {
				m = leer(nombre, JsonParser.parseReader(lector).getAsJsonObject());
			} catch (Exception e) {
				Dedsafio4.LOGGER.error("No se pudo cargar la skin " + nombre, e);
			}
		}
		MODELOS.put(nombre, m);
		return m;
	}

	/** Si hay un modelo de Blockbench con ese nombre. */
	public static boolean existe(String nombre) {
		return modelo(nombre) != null;
	}

	// ---------------------------------------------------------------- Lectura

	private static float[] numeros(JsonElement e, float... siNoHay) {
		if (e == null) return siNoHay;
		JsonArray a = e.getAsJsonArray();
		float[] r = new float[a.size()];
		for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	/** De Bedrock (Y para arriba, desde los pies) al modelo del jugador (Y para abajo, la cabeza en 0). */
	private static float[] punto(float x, float y, float z) {
		return new float[]{x, 24 - y, z};
	}

	private static Modelo leer(String nombre, JsonObject raiz) {
		JsonObject geo = raiz.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
		JsonObject desc = geo.getAsJsonObject("description");
		float texAncho = desc.has("texture_width") ? desc.get("texture_width").getAsFloat() : 64;
		float texAlto = desc.has("texture_height") ? desc.get("texture_height").getAsFloat() : 64;

		List<Hueso> huesos = new ArrayList<>();
		Map<String, Integer> indices = new HashMap<>();
		List<String> padres = new ArrayList<>();
		for (JsonElement e : geo.getAsJsonArray("bones")) {
			JsonObject b = e.getAsJsonObject();
			String n = b.get("name").getAsString();
			float[] pv = numeros(b.get("pivot"), 0, 0, 0);
			float[] r = numeros(b.get("rotation"), 0, 0, 0);
			boolean esArmadura = n.startsWith("armor_");
			// La armadura usa la textura de la armadura (64x32), el resto la de la skin.
			float tw = esArmadura ? 64 : texAncho, th = esArmadura ? 32 : texAlto;
			List<Cara> caras = new ArrayList<>();
			if (b.has("cubes")) for (JsonElement c : b.getAsJsonArray("cubes")) cubo(c.getAsJsonObject(), tw, th, caras);
			indices.put(n, huesos.size());
			padres.add(b.has("parent") ? b.get("parent").getAsString() : null);
			huesos.add(new Hueso(n, punto(pv[0], pv[1], pv[2]),
					new float[]{r[0] * Mth.DEG_TO_RAD, r[1] * Mth.DEG_TO_RAD, r[2] * Mth.DEG_TO_RAD}, caras, new ArrayList<>()));
		}
		List<Integer> raices = new ArrayList<>();
		Map<Integer, Integer> parte = new HashMap<>();
		Map<Integer, EquipmentSlot> armadura = new HashMap<>();
		for (int i = 0; i < huesos.size(); i++) {
			Integer padre = padres.get(i) == null ? null : indices.get(padres.get(i));
			if (padre != null) {
				huesos.get(padre).hijos().add(i);
				continue;
			}
			String n = huesos.get(i).nombre().toLowerCase();
			if (n.startsWith("elytra")) continue;   // las alas no se dibujan
			raices.add(i);
			parte.put(i, n.contains("head") || n.contains("helmet") ? 0 : n.contains("left_arm") ? 2 : n.contains("right_arm") ? 3
					: n.contains("left_leg") || n.contains("left_foot") ? 4 : n.contains("right_leg") || n.contains("right_foot") ? 5 : 1);
			if (n.startsWith("armor_")) armadura.put(i, n.contains("helmet") ? EquipmentSlot.HEAD
					: n.contains("leggings") || n.contains("_leg") ? EquipmentSlot.LEGS
					: n.contains("foot") || n.contains("boots") ? EquipmentSlot.FEET : EquipmentSlot.CHEST);
		}
		return new Modelo(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/skins/" + nombre + ".png"),
				huesos, raices, parte, armadura);
	}

	/** Arma las 6 caras de un cubo como lo hace Minecraft (el mismo orden de vértices y la misma textura por caja). */
	private static void cubo(JsonObject c, float tw, float th, List<Cara> caras) {
		float[] o = numeros(c.get("origin"), 0, 0, 0), s = numeros(c.get("size"), 0, 0, 0);
		float inf = c.has("inflate") ? c.get("inflate").getAsFloat() : 0;
		boolean espejo = c.has("mirror") && c.get("mirror").getAsBoolean();
		float x0 = o[0] - inf, x1 = o[0] + s[0] + inf;
		float y0 = 24 - (o[1] + s[1]) - inf, y1 = 24 - o[1] + inf;
		float z0 = o[2] - inf, z1 = o[2] + s[2] + inf;
		if (espejo) {
			float t = x0;
			x0 = x1;
			x1 = t;
		}
		float[] v0 = {x0, y0, z0}, v1 = {x1, y0, z0}, v2 = {x1, y1, z0}, v3 = {x0, y1, z0},
				v4 = {x0, y0, z1}, v5 = {x1, y0, z1}, v6 = {x1, y1, z1}, v7 = {x0, y1, z1};
		// Por cada cara de Minecraft: sus vértices, la cara de Bedrock que le corresponde y la normal.
		Object[][] lados = {
				{new float[][]{v5, v4, v0, v1}, "up", 0f, -1f, 0f},
				{new float[][]{v2, v3, v7, v6}, "down", 0f, 1f, 0f},
				{new float[][]{v0, v4, v7, v3}, "east", -1f, 0f, 0f},
				{new float[][]{v1, v0, v3, v2}, "north", 0f, 0f, -1f},
				{new float[][]{v5, v1, v2, v6}, "west", 1f, 0f, 0f},
				{new float[][]{v4, v5, v6, v7}, "south", 0f, 0f, 1f}};
		JsonElement uv = c.get("uv");
		float[][] cajas = null;
		if (uv != null && uv.isJsonArray()) {
			float u = uv.getAsJsonArray().get(0).getAsFloat(), v = uv.getAsJsonArray().get(1).getAsFloat();
			float w = s[0], h = s[1], d = s[2];
			float f4 = u, f5 = u + d, f6 = u + d + w, f7 = u + d + w + w, f8 = u + d + w + d, f9 = u + d + w + d + w;
			float f10 = v, f11 = v + d, f12 = v + d + h;
			cajas = new float[][]{{f5, f10, f6, f11}, {f6, f11, f7, f10}, {f4, f11, f5, f12}, {f5, f11, f6, f12},
					{f6, f11, f8, f12}, {f8, f11, f9, f12}};
		}
		for (int i = 0; i < 6; i++) {
			float[] r;
			if (cajas != null) r = cajas[i];
			else {
				if (uv == null || !uv.getAsJsonObject().has((String) lados[i][1])) continue;
				JsonObject f = uv.getAsJsonObject().getAsJsonObject((String) lados[i][1]);
				float[] a = numeros(f.get("uv"), 0, 0), b = numeros(f.get("uv_size"), 0, 0);
				r = new float[]{a[0], a[1], a[0] + b[0], a[1] + b[1]};
			}
			if (r[0] == r[2] || r[1] == r[3]) continue;   // cara sin textura (los planos y los cubos vacíos)
			float[][] vs = (float[][]) lados[i][0];
			// Como en Minecraft: vértice 0 = (u2, v1), 1 = (u1, v1), 2 = (u1, v2), 3 = (u2, v2).
			float[][] uvs = {{r[2], r[1]}, {r[0], r[1]}, {r[0], r[3]}, {r[2], r[3]}};
			float[] q = new float[20];
			for (int k = 0; k < 4; k++) {
				q[k * 5] = vs[k][0];
				q[k * 5 + 1] = vs[k][1];
				q[k * 5 + 2] = vs[k][2];
				q[k * 5 + 3] = uvs[k][0] / tw;
				q[k * 5 + 4] = uvs[k][1] / th;
			}
			float nx = (float) lados[i][2];
			caras.add(new Cara(q, espejo ? -nx : nx, (float) lados[i][3], (float) lados[i][4]));
		}
	}

	// ---------------------------------------------------------------- Dibujo

	/**
	 * Dibuja el modelo y la armadura (el PoseStack ya está como el del jugador y el modelo de jugador ya tiene su
	 * animación). Devuelve false si no hay modelo de Blockbench con ese nombre.
	 */
	public static boolean dibujar(String nombre, HumanoidModel<?> jugadorModelo, AbstractClientPlayer jugador, PoseStack pose,
								  MultiBufferSource buffers, int luz, int overlay) {
		Modelo m = modelo(nombre);
		if (m == null) return false;
		ModelPart[] partes = {jugadorModelo.head, jugadorModelo.body, jugadorModelo.leftArm, jugadorModelo.rightArm,
				jugadorModelo.leftLeg, jugadorModelo.rightLeg};
		for (int i : m.raices()) {
			EquipmentSlot lugar = m.armadura().get(i);
			if (lugar == null) {
				VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(m.textura()));
				raiz(m, i, partes[m.parte().get(i)], m.parte().get(i), pose, vc, -1, luz, overlay);
				continue;
			}
			ItemStack cosa = jugador.getItemBySlot(lugar);
			if (!(cosa.getItem() instanceof ArmorItem armadura) || armadura.getEquipmentSlot() != lugar) continue;
			Holder<ArmorMaterial> material = armadura.getMaterial();
			int color = cosa.is(net.minecraft.tags.ItemTags.DYEABLE) ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(cosa, -6265536)) : -1;
			for (ArmorMaterial.Layer capa : material.value().layers()) {
				int c = capa.dyeable() ? color : -1;
				VertexConsumer vc = buffers.getBuffer(RenderType.armorCutoutNoCull(capa.texture(lugar == EquipmentSlot.LEGS)));
				raiz(m, i, partes[m.parte().get(i)], m.parte().get(i), pose, vc, c, luz, overlay);
			}
			if (cosa.hasFoil()) raiz(m, i, partes[m.parte().get(i)], m.parte().get(i), pose,
					buffers.getBuffer(RenderType.armorEntityGlint()), -1, luz, overlay);
		}
		return true;
	}

	/** Un hueso de arriba: se corre y gira igual que la parte del jugador de la que cuelga. */
	private static void raiz(Modelo m, int i, ModelPart parte, int cual, PoseStack pose, VertexConsumer vc, int color, int luz, int overlay) {
		Hueso h = m.huesos().get(i);
		float[] p = h.pivote(), reposo = REPOSO[cual];
		pose.pushPose();
		pose.translate((p[0] + parte.x - reposo[0]) / 16f, (p[1] + parte.y - reposo[1]) / 16f, (p[2] + parte.z - reposo[2]) / 16f);
		pose.mulPose(new Quaternionf().rotationZYX(parte.zRot, parte.yRot, parte.xRot));
		pose.mulPose(new Quaternionf().rotationZYX(h.rot()[2], h.rot()[1], h.rot()[0]));
		pose.translate(-p[0] / 16f, -p[1] / 16f, -p[2] / 16f);
		dibujarHueso(m, h, pose, vc, color, luz, overlay);
		pose.popPose();
	}

	private static void hijo(Modelo m, int i, PoseStack pose, VertexConsumer vc, int color, int luz, int overlay) {
		Hueso h = m.huesos().get(i);
		float[] p = h.pivote();
		pose.pushPose();
		pose.translate(p[0] / 16f, p[1] / 16f, p[2] / 16f);
		pose.mulPose(new Quaternionf().rotationZYX(h.rot()[2], h.rot()[1], h.rot()[0]));
		pose.translate(-p[0] / 16f, -p[1] / 16f, -p[2] / 16f);
		dibujarHueso(m, h, pose, vc, color, luz, overlay);
		pose.popPose();
	}

	private static void dibujarHueso(Modelo m, Hueso h, PoseStack pose, VertexConsumer vc, int color, int luz, int overlay) {
		PoseStack.Pose ultimo = pose.last();
		for (Cara c : h.caras()) {
			float[] q = c.q();
			for (int v = 0; v < 4; v++) {
				int o = v * 5;
				vc.addVertex(ultimo, q[o] / 16f, q[o + 1] / 16f, q[o + 2] / 16f).setColor(color).setUv(q[o + 3], q[o + 4])
						.setOverlay(overlay).setLight(luz).setNormal(ultimo, c.nx(), c.ny(), c.nz());
			}
		}
		for (int k : h.hijos()) hijo(m, k, pose, vc, color, luz, overlay);
	}
}
