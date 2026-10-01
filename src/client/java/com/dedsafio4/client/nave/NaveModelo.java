package com.dedsafio4.client.nave;

import com.dedsafio4.nave.NaveEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.FastColor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de la nave, cargado de assets/dedsafio4/modelos/nave.json. Ese archivo sale de mi-nave.bbmodel
 * (1368 cubitos de 1 píxel, el color de cada uno sale de su textura) uniendo las caras del mismo color
 * y sacando las tapadas: quedan ~1200 caras de color liso. Se dibujan con una textura blanca y el color
 * va en cada vértice.
 */
public class NaveModelo extends EntityModel<NaveEntity> {
	/** El modelo se dibuja al tamaño que tiene en Blockbench (1 píxel = 1/16 de bloque). */
	public static final float ESCALA = 1f;

	private record Cara(float[] vertices, float nx, float ny, float nz, int color) {}

	private record Pieza(float[] rotacion, float[] origen, List<Cara> caras) {}

	private final List<Pieza> piezas = new ArrayList<>();
	private final float centroX, minY, centroZ;

	public NaveModelo() {
		JsonObject json = leer();
		for (JsonElement p : json.getAsJsonArray("piezas")) {
			JsonObject pieza = p.getAsJsonObject();
			List<Cara> caras = new ArrayList<>();
			for (JsonElement c : pieza.getAsJsonArray("caras")) {
				JsonObject cara = c.getAsJsonObject();
				float[] v = new float[12];
				JsonArray esquinas = cara.getAsJsonArray("v");
				for (int i = 0; i < 4; i++) {
					JsonArray e = esquinas.get(i).getAsJsonArray();
					for (int k = 0; k < 3; k++) v[i * 3 + k] = e.get(k).getAsFloat();
				}
				JsonArray n = cara.getAsJsonArray("n"), col = cara.getAsJsonArray("c");
				caras.add(new Cara(v, n.get(0).getAsFloat(), n.get(1).getAsFloat(), n.get(2).getAsFloat(),
						FastColor.ARGB32.color(255, col.get(0).getAsInt(), col.get(1).getAsInt(), col.get(2).getAsInt())));
			}
			piezas.add(new Pieza(aFloat(pieza.getAsJsonArray("rotacion")), aFloat(pieza.getAsJsonArray("origen")), caras));
		}
		JsonObject limites = json.getAsJsonObject("limites");
		float[] min = aFloat(limites.getAsJsonArray("min")), max = aFloat(limites.getAsJsonArray("max"));
		centroX = (min[0] + max[0]) / 2f;
		minY = min[1];
		centroZ = (min[2] + max[2]) / 2f;
	}

	private static JsonObject leer() {
		try (InputStream in = NaveModelo.class.getResourceAsStream("/assets/dedsafio4/modelos/nave.json")) {
			if (in == null) throw new IllegalStateException("Falta assets/dedsafio4/modelos/nave.json");
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo leer el modelo de la nave", e);
		}
	}

	private static float[] aFloat(JsonArray a) {
		float[] r = new float[a.size()];
		for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	@Override
	public void setupAnim(NaveEntity nave, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int luz, int overlay, int color) {
		pose.pushPose();
		// Espacio de los modelos de entidad: Y hacia abajo y los pies a 1.501 bloques del origen.
		// El .bbmodel tiene Y hacia arriba, en píxeles: se centra en X/Z y se apoya en los pies.
		pose.translate(0f, 1.501f, 0f);
		pose.scale(ESCALA / 16f, -ESCALA / 16f, ESCALA / 16f);
		pose.translate(-centroX, -minY, -centroZ);
		for (Pieza pieza : piezas) {
			pose.pushPose();
			pose.translate(pieza.origen()[0], pieza.origen()[1], pieza.origen()[2]);
			pose.mulPose(Axis.ZP.rotationDegrees(pieza.rotacion()[2]));
			pose.mulPose(Axis.YP.rotationDegrees(pieza.rotacion()[1]));
			pose.mulPose(Axis.XP.rotationDegrees(pieza.rotacion()[0]));
			pose.translate(-pieza.origen()[0], -pieza.origen()[1], -pieza.origen()[2]);
			PoseStack.Pose p = pose.last();
			for (Cara cara : pieza.caras()) {
				int c = FastColor.ARGB32.multiply(cara.color(), color);
				float[] v = cara.vertices();
				for (int i = 0; i < 4; i++) {
					buffer.addVertex(p, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).setColor(c).setUv(0.5f, 0.5f)
							.setOverlay(overlay).setLight(luz).setNormal(p, cara.nx(), cara.ny(), cara.nz());
				}
			}
			pose.popPose();
		}
		pose.popPose();
	}
}
