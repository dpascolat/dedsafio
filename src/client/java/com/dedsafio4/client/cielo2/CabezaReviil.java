package com.dedsafio4.client.cielo2;

import com.dedsafio4.Dedsafio4;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.io.Reader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * La cabeza 3D de Reviil que espía por la grieta del cielo rojo 2. Se dibuja en una imagen aparte (1024x768) antes del
 * mundo, y el shader del cielo la pone dentro de la grieta. Se mueve sola (respira, gira un poco, los ojos giran) y
 * con /cielo cabezazo se echa atrás y golpea contra la grieta.
 * El modelo (modelos/reviil_cabeza.json) tiene huesos con su padre y su pivote, y las caras ya armadas.
 */
final class CabezaReviil {
	private CabezaReviil() {}

	private static final ResourceLocation MODELO = id("modelos/reviil_cabeza.json");
	private static final ResourceLocation TEXTURA = id("textures/cielo2/reviil_cabeza.png");
	private static final ResourceLocation BRILLO = id("textures/cielo2/reviil_cabeza_brillo.png");
	private static final ResourceLocation RULETA = id("textures/cielo2/reviil_ruleta.png");
	private static final ResourceLocation HALO = id("textures/cielo2/reviil_frente_halo.png");
	private static final String[] ANIMADOS = {"head2", "right_hat", "left_hat", "right_bone", "right_bone2", "right_eye", "left_eye", "mouth"};

	static ShaderInstance shader;

	private static TextureTarget imagen;
	private static boolean cargado, fallo, filtroPuesto;
	private static int[] padre;
	private static float[][] pivote;
	private static int[] animado;
	private static float[] caras;
	private static int nCaras;
	private static int ruletaHueso = -1;
	private static float[] ruletaCentro;
	private static float ruletaMedio;
	private static int haloHueso = -1;
	private static float[] haloCentro, haloMedio;

	private static ResourceLocation id(String ruta) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, ruta);
	}

	private static boolean cargar() {
		if (cargado) return true;
		if (fallo) return false;
		try {
			var r = Minecraft.getInstance().getResourceManager().getResource(MODELO);
			if (r.isEmpty()) throw new IllegalStateException("no está " + MODELO);
			JsonObject raiz;
			try (Reader lector = r.get().openAsReader()) {
				raiz = JsonParser.parseReader(lector).getAsJsonObject();
			}
			JsonArray huesos = raiz.getAsJsonArray("huesos");
			int n = huesos.size();
			padre = new int[n];
			pivote = new float[n][3];
			animado = new int[n];
			for (int i = 0; i < n; i++) {
				JsonObject h = huesos.get(i).getAsJsonObject();
				padre[i] = h.get("padre").getAsInt();
				JsonArray p = h.getAsJsonArray("pivote");
				for (int k = 0; k < 3; k++) pivote[i][k] = p.get(k).getAsFloat();
				animado[i] = -1;
				for (int a = 0; a < ANIMADOS.length; a++) {
					if (ANIMADOS[a].equals(h.get("nombre").getAsString())) animado[i] = a;
				}
			}
			JsonArray qs = raiz.getAsJsonArray("quads");
			nCaras = qs.size();
			caras = new float[nCaras * 24];
			int k = 0;
			for (JsonElement e : qs) {
				for (JsonElement v : e.getAsJsonArray()) caras[k++] = v.getAsFloat();
			}
			JsonObject ru = raiz.getAsJsonObject("ruleta");
			if (ru != null) {
				ruletaHueso = ru.get("hueso").getAsInt();
				JsonArray c = ru.getAsJsonArray("centro");
				ruletaCentro = new float[]{c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()};
				ruletaMedio = ru.get("medio").getAsFloat();
			}
			JsonObject ha = raiz.getAsJsonObject("halo");
			if (ha != null) {
				haloHueso = ha.get("hueso").getAsInt();
				JsonArray c = ha.getAsJsonArray("centro");
				JsonArray m = ha.getAsJsonArray("medio");
				haloCentro = new float[]{c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat()};
				haloMedio = new float[]{m.get(0).getAsFloat(), m.get(1).getAsFloat()};
			}
			cargado = true;
			return true;
		} catch (Exception e) {
			fallo = true;
			Dedsafio4.LOGGER.warn("[cielo rojo 2] no se pudo cargar la cabeza de Reviil", e);
			return false;
		}
	}

	private static float suave(float x) {
		x = Math.max(0f, Math.min(1f, x));
		return x * x * (3f - 2f * x);
	}

	/** La pose de cada hueso animado: {rotX, rotY, rotZ, x, y, z}. {@code le} = segundos desde el cabezazo (< 0: no hay). */
	private static float[][] poses(float t, float le) {
		float yaw = (float) (7.0 * Math.sin(0.55 * t) + 2.5 * Math.sin(1.7 * t + 1.0));
		float cab = (float) (3.5 * Math.sin(0.8 * t + 2.0));
		float roll = (float) (2.5 * Math.sin(0.45 * t + 0.5));
		float y = (float) (0.8 * Math.sin(1.1 * t));
		float z = 0f;
		float alas = (float) (4.0 * Math.sin(1.1 * t));
		float huesoD = (float) (5.0 * Math.sin(1.3 * t));
		float huesoI = (float) (5.0 * Math.sin(1.3 * t + 0.7));
		float huesoX = 0f, boca = 0f;
		float ojos = 50f * t;
		if (le >= 0f) {
			if (le < 0.14f) {
				// Se echa atrás.
				float k = suave(le / 0.14f);
				z += 6f * k;
				cab -= 10f * k;
				alas += 6f * k;
			} else if (le < 0.2f) {
				// Golpe.
				float k = (le - 0.14f) / 0.06f;
				k *= k;
				z += 6f - 39f * k;
				cab += -10f + 28f * k;
				alas += 6f - 10f * k;
				boca = 12f * k;
			} else {
				// Rebota y se calma.
				float g = le - 0.2f;
				float r = (float) (Math.exp(-g / 0.13) * Math.cos(g * 13f));
				z += -33f * r;
				cab += 18f * r;
				alas += (float) (28.0 * Math.exp(-g / 0.25) * Math.cos(g * 9f));
				huesoX = (float) (25.0 * Math.exp(-g / 0.3) * Math.cos(g * 8f));
				boca = (float) (12.0 * Math.exp(-g / 0.3));
				ojos += (float) (400.0 * (1.0 - Math.exp(-g / 0.3)));
			}
		}
		return new float[][]{
				{-cab, yaw, roll, 0f, y, z},
				{0f, 0f, -alas, 0f, 0f, 0f},
				{0f, 0f, alas, 0f, 0f, 0f},
				{huesoX, 0f, huesoD, 0f, 0f, 0f},
				{huesoX, 0f, -huesoI, 0f, 0f, 0f},
				{0f, 0f, ojos, 0f, 0f, 0f},
				{0f, 0f, -ojos, 0f, 0f, 0f},
				{boca, 0f, 0f, 0f, 0f, 0f}
		};
	}

	/** Dibuja la cabeza en su imagen y devuelve la textura de OpenGL (-1 si no se pudo). */
	static int dibujar(float t, float le) {
		if (!cargar() || shader == null) return -1;
		Minecraft mc = Minecraft.getInstance();
		if (imagen == null) {
			imagen = new TextureTarget(1024, 768, true, Minecraft.ON_OSX);
			imagen.setFilterMode(9729);   // GL_LINEAR
		}
		AbstractTexture tex = mc.getTextureManager().getTexture(TEXTURA);
		AbstractTexture bri = mc.getTextureManager().getTexture(BRILLO);
		AbstractTexture rul = mc.getTextureManager().getTexture(RULETA);
		AbstractTexture hal = haloHueso >= 0 ? mc.getTextureManager().getTexture(HALO) : null;
		if (!filtroPuesto) {
			tex.setFilter(false, false);
			bri.setFilter(false, false);
			rul.setFilter(false, false);
			if (hal != null) hal.setFilter(true, false);
			filtroPuesto = true;
		}

		float[][] pose = poses(t, le);
		Matrix4f[] mundo = new Matrix4f[padre.length];
		for (int i = 0; i < padre.length; i++) {
			Matrix4f m = padre[i] >= 0 ? new Matrix4f(mundo[padre[i]]) : new Matrix4f();
			int a = animado[i];
			if (a >= 0) {
				float[] p = pose[a];
				float[] v = pivote[i];
				m.translate(p[3], p[4], p[5])
						.translate(v[0], v[1], v[2])
						.rotateZ((float) Math.toRadians(p[2]))
						.rotateY((float) Math.toRadians(p[1]))
						.rotateX((float) Math.toRadians(p[0]))
						.translate(-v[0], -v[1], -v[2]);
			}
			mundo[i] = m;
		}

		imagen.setClearColor(0f, 0f, 0f, 0f);
		imagen.clear(Minecraft.ON_OSX);
		imagen.bindWrite(true);
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float) Math.toRadians(20.0), 4f / 3f, 10f, 400f),
				VertexSorting.DISTANCE_TO_ORIGIN);
		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mv.pushMatrix();
		mv.identity();
		mv.lookAt(0f, 77f, -157.6f, 0f, 77f, -21.6f, 0f, 1f, 0f);
		RenderSystem.applyModelViewMatrix();
		try {
			ShaderInstance sh = shader;
			RenderSystem.setShader(() -> sh);
			RenderSystem.setShaderTexture(0, tex.getId());
			RenderSystem.setShaderTexture(1, bri.getId());
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			RenderSystem.disableCull();
			BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
			Vector4f pos = new Vector4f();
			Vector3f nor = new Vector3f();
			for (int c = 0; c < nCaras; c++) {
				int o = c * 24;
				Matrix4f m = mundo[(int) caras[o]];
				nor.set(caras[o + 21], caras[o + 22], caras[o + 23]);
				m.transformDirection(nor);
				for (int k = 0; k < 4; k++) {
					int v = o + 1 + k * 5;
					pos.set(caras[v], caras[v + 1], caras[v + 2], 1f);
					m.transform(pos);
					b.addVertex(pos.x, pos.y, pos.z).setUv(caras[v + 3], caras[v + 4]).setColor(-1).setNormal(nor.x, nor.y, nor.z);
				}
			}
			BufferUploader.drawWithShader(b.buildOrThrow());
			if (ruletaHueso >= 0 && ruletaHueso < mundo.length) {
				// La ruleta de colores de la frente.
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderTexture(0, rul.getId());
				RenderSystem.enableBlend();
				RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
						GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				RenderSystem.depthMask(false);
				cuadro(mundo[ruletaHueso], ruletaCentro, ruletaMedio, ruletaMedio);
				RenderSystem.depthMask(true);
				RenderSystem.defaultBlendFunc();
				RenderSystem.disableBlend();
			}
			if (hal != null && haloHueso < mundo.length) {
				// El brillo alrededor de la ruleta.
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderTexture(0, hal.getId());
				RenderSystem.enableBlend();
				RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
						GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
				RenderSystem.depthMask(false);
				RenderSystem.disableDepthTest();
				cuadro(mundo[haloHueso], haloCentro, haloMedio[0], haloMedio[1]);
				RenderSystem.enableDepthTest();
				RenderSystem.depthMask(true);
				RenderSystem.defaultBlendFunc();
				RenderSystem.disableBlend();
			}
			RenderSystem.enableCull();
		} finally {
			mv.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.restoreProjectionMatrix();
			mc.getMainRenderTarget().bindWrite(true);
		}
		return imagen.getColorTextureId();
	}

	private static void cuadro(Matrix4f m, float[] c, float hx, float hy) {
		float cx = c[0], cy = c[1], cz = c[2];
		float[][] esquinas = {{cx + hx, cy + hy, 0f, 0f}, {cx - hx, cy + hy, 1f, 0f}, {cx - hx, cy - hy, 1f, 1f}, {cx + hx, cy - hy, 0f, 1f}};
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		Vector4f pos = new Vector4f();
		for (float[] e : esquinas) {
			pos.set(e[0], e[1], cz, 1f);
			m.transform(pos);
			b.addVertex(pos.x, pos.y, pos.z).setUv(e[2], e[3]);
		}
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	static void limpiar() {
		if (imagen != null) {
			imagen.destroyBuffers();
			imagen = null;
		}
	}
}
