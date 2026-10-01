package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperPastoEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo del Creeper de Pasto hecho con los vóxeles del usuario (modelos/creeper_pasto.json).
 * Cada vóxel mide medio píxel. Se mueven la cabeza (mira) y las cuatro patas (al caminar), como un creeper.
 */
public class CreeperPastoModelo extends EntityModel<CreeperPastoEntity> {
	private static final ResourceLocation DATOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "modelos/creeper_pasto.json");
	private static final String[] PATAS = {"pata_di", "pata_dr", "pata_ti", "pata_tr"};

	/** Una caja ya pasada a píxeles del modelo (y hacia abajo, el frente hacia -z), con su color y caras visibles. */
	private record Caja(float x0, float y0, float z0, float x1, float y1, float z1, float u, float v, int mascara) {}

	private final Map<String, List<Caja>> partes = new HashMap<>();
	/** Pivote de cada parte (en píxeles del modelo). */
	private final Map<String, float[]> pivotes = new HashMap<>();

	private float cabezaYaw, cabezaPitch;
	private final float[] patas = new float[4];

	public CreeperPastoModelo(ResourceManager recursos) {
		try (Reader lector = recursos.openAsReader(DATOS)) {
			JsonObject json = JsonParser.parseReader(lector).getAsJsonObject();
			for (Map.Entry<String, JsonElement> parte : json.getAsJsonObject("partes").entrySet()) {
				List<Caja> cajas = new ArrayList<>();
				float minX = 99, maxX = -99, minZ = 99, maxZ = -99, maxY = -99;
				for (JsonElement e : parte.getValue().getAsJsonArray()) {
					JsonArray a = e.getAsJsonArray();
					float x0 = a.get(0).getAsFloat(), y0 = a.get(1).getAsFloat(), z0 = a.get(2).getAsFloat();
					float x1 = a.get(3).getAsFloat(), y1 = a.get(4).getAsFloat(), z1 = a.get(5).getAsFloat();
					int color = a.get(6).getAsInt();
					// Vóxel (1/32 de bloque, y arriba, cara a +z) -> píxel del modelo (y abajo desde 24, cara a -z).
					cajas.add(new Caja(x0 * 0.5f, 24 - y1 * 0.5f, -z1 * 0.5f, x1 * 0.5f, 24 - y0 * 0.5f, -z0 * 0.5f,
							((color % 8) * 2 + 1) / 16f, ((color / 8) * 2 + 1) / 16f, a.get(7).getAsInt()));
					minX = Math.min(minX, x0); maxX = Math.max(maxX, x1);
					minZ = Math.min(minZ, z0); maxZ = Math.max(maxZ, z1);
					maxY = Math.max(maxY, y1);
				}
				partes.put(parte.getKey(), cajas);
				if (parte.getKey().startsWith("pata")) {
					// Las patas giran desde arriba, en su centro.
					pivotes.put(parte.getKey(), new float[]{(minX + maxX) / 4f, 24 - maxY * 0.5f, -(minZ + maxZ) / 4f});
				}
			}
			// La cabeza gira desde el cuello (arriba del cuerpo), en el medio.
			pivotes.put("cabeza", new float[]{0, 24 - 14 * 0.5f, 0});
		} catch (Exception e) {
			Dedsafio4.LOGGER.error("No se pudo cargar el modelo del Creeper de Pasto", e);
		}
	}

	@Override
	public void setupAnim(CreeperPastoEntity creeper, float paso, float cuanto, float edad, float yaw, float pitch) {
		cabezaYaw = yaw * Mth.DEG_TO_RAD;
		cabezaPitch = pitch * Mth.DEG_TO_RAD;
		float a = Mth.cos(paso * 0.6662f) * 1.4f * cuanto, b = Mth.cos(paso * 0.6662f + Mth.PI) * 1.4f * cuanto;
		patas[0] = a;   // delantera izquierda
		patas[1] = b;   // delantera derecha
		patas[2] = b;   // trasera izquierda
		patas[3] = a;   // trasera derecha
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer vc, int luz, int overlay, int color) {
		dibujar(pose, vc, partes.get("cuerpo"), luz, overlay, color);

		pose.pushPose();
		girar(pose, pivotes.get("cabeza"), cabezaYaw, cabezaPitch);
		dibujar(pose, vc, partes.get("cabeza"), luz, overlay, color);
		pose.popPose();

		for (int i = 0; i < 4; i++) {
			pose.pushPose();
			girar(pose, pivotes.get(PATAS[i]), 0, patas[i]);
			dibujar(pose, vc, partes.get(PATAS[i]), luz, overlay, color);
			pose.popPose();
		}
	}

	private static void girar(PoseStack pose, float[] pivote, float yaw, float pitch) {
		if (pivote == null) return;
		pose.translate(pivote[0] / 16f, pivote[1] / 16f, pivote[2] / 16f);
		if (yaw != 0) pose.mulPose(Axis.YP.rotation(yaw));
		if (pitch != 0) pose.mulPose(Axis.XP.rotation(pitch));
		pose.translate(-pivote[0] / 16f, -pivote[1] / 16f, -pivote[2] / 16f);
	}

	private static void dibujar(PoseStack pose, VertexConsumer vc, List<Caja> cajas, int luz, int overlay, int color) {
		if (cajas == null) return;
		PoseStack.Pose p = pose.last();
		for (Caja c : cajas) {
			float x0 = c.x0 / 16, y0 = c.y0 / 16, z0 = c.z0 / 16, x1 = c.x1 / 16, y1 = c.y1 / 16, z1 = c.z1 / 16;
			int m = c.mascara;
			// Bits en coordenadas de vóxel: 0 +x, 1 -x, 2 +y (arriba = -y del modelo), 3 -y, 4 +z (frente = -z del modelo), 5 -z.
			if ((m & 1) != 0) cara(vc, p, c, luz, overlay, color, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
			if ((m & 2) != 0) cara(vc, p, c, luz, overlay, color, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
			if ((m & 4) != 0) cara(vc, p, c, luz, overlay, color, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
			if ((m & 8) != 0) cara(vc, p, c, luz, overlay, color, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
			if ((m & 16) != 0) cara(vc, p, c, luz, overlay, color, 0, 0, -1, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0);
			if ((m & 32) != 0) cara(vc, p, c, luz, overlay, color, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		}
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, Caja c, int luz, int overlay, int color,
							 float nx, float ny, float nz, float... v) {
		for (int i = 0; i < 12; i += 3) {
			vc.addVertex(p, v[i], v[i + 1], v[i + 2]).setColor(color).setUv(c.u, c.v).setOverlay(overlay).setLight(luz)
					.setNormal(p, nx, ny, nz);
		}
	}
}
