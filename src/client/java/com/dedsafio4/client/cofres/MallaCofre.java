package com.dedsafio4.client.cofres;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Un cofre de vóxeles de los diseños (assets/dedsafio4/modelos/...json): base y tapa por separado,
 * centrado en X/Z. En los diseños el candado (o el emblema) está del lado z=0 y la bisagra del lado
 * z=15, así que se da vuelta para que ese lado quede como frente (+Z) y la tapa se abra hacia atrás,
 * como un cofre normal. Los colores que brillan se dibujan con luz máxima y el interior se enciende
 * a medida que se abre.
 */
public final class MallaCofre {
	private static final ResourceLocation BLANCO =
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/entity/blanco.png");
	private static final int LUZ_MAXIMA = 0xF000F0;

	private record Cara(float[] v, float nx, float ny, float nz, int color, int tipo) {}

	private final String archivo;
	private final Set<Integer> brillan;
	private final int interior;
	private List<Cara> base, tapa;

	/**
	 * @param brillan  los tipos ("k") que brillan siempre
	 * @param interior el tipo que brilla cuando está abierto
	 */
	public MallaCofre(String archivo, Set<Integer> brillan, int interior) {
		this.archivo = archivo;
		this.brillan = brillan;
		this.interior = interior;
	}

	private void cargar() {
		if (base != null) return;
		try (InputStream in = MallaCofre.class.getResourceAsStream("/assets/dedsafio4/modelos/" + archivo)) {
			if (in == null) throw new IllegalStateException("Falta assets/dedsafio4/modelos/" + archivo);
			JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			base = leer(json.getAsJsonArray("base"));
			tapa = leer(json.getAsJsonArray("tapa"));
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo leer el modelo " + archivo, e);
		}
	}

	private static List<Cara> leer(JsonArray caras) {
		List<Cara> lista = new ArrayList<>(caras.size());
		for (JsonElement e : caras) {
			JsonObject c = e.getAsJsonObject();
			float[] v = new float[12];
			JsonArray esquinas = c.getAsJsonArray("v");
			for (int i = 0; i < 4; i++) {
				JsonArray p = esquinas.get(i).getAsJsonArray();
				for (int k = 0; k < 3; k++) v[i * 3 + k] = p.get(k).getAsFloat() / 16f;
			}
			JsonArray n = c.getAsJsonArray("n"), col = c.getAsJsonArray("c");
			int color = 0xFF000000 | (col.get(0).getAsInt() << 16) | (col.get(1).getAsInt() << 8) | col.get(2).getAsInt();
			lista.add(new Cara(v, n.get(0).getAsFloat(), n.get(1).getAsFloat(), n.get(2).getAsFloat(), color,
					c.get("k").getAsInt()));
		}
		return lista;
	}

	/**
	 * Dibuja el cofre donde está parado el PoseStack (abajo en el centro del modelo), con el frente
	 * hacia +Z; "abierto" va de 0 a 1.
	 */
	public void dibujar(PoseStack pose, MultiBufferSource buffers, int luz, int overlay, float abierto) {
		cargar();
		VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(BLANCO));
		float suave = 1f - (1f - abierto) * (1f - abierto) * (1f - abierto);   // easeOutCubic
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180f));   // el lado del candado adelante
		for (Cara cara : base) poner(vc, pose.last(), cara, luz, overlay, abierto);
		// Tapa: bisagra atrás (z=15 del diseño), a 11 píxeles de altura.
		pose.pushPose();
		pose.translate(0, 11 / 16f, 8 / 16f);
		pose.mulPose(Axis.XP.rotationDegrees(100f * suave));
		pose.translate(0, -11 / 16f, -8 / 16f);
		for (Cara cara : tapa) poner(vc, pose.last(), cara, luz, overlay, abierto);
		pose.popPose();
		pose.popPose();
	}

	private void poner(VertexConsumer vc, PoseStack.Pose p, Cara cara, int luz, int overlay, float abierto) {
		int luzCara = luz;
		if (brillan.contains(cara.tipo())) luzCara = LUZ_MAXIMA;
		else if (cara.tipo() == interior && abierto > 0.3f) luzCara = LUZ_MAXIMA;
		float[] v = cara.v();
		for (int i = 0; i < 4; i++) {
			vc.addVertex(p, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).setColor(cara.color()).setUv(0.5f, 0.5f)
					.setOverlay(overlay).setLight(luzCara).setNormal(p, cara.nx(), cara.ny(), cara.nz());
		}
	}
}
