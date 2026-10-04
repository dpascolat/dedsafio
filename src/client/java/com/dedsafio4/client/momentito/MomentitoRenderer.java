package com.dedsafio4.client.momentito;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.momentito.MomentitoEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dibuja la escena del hacker (/momentito 1), como en "Animacion Hacker.html": todo es una función del tiempo
 * (0 a 30 s), así que se ve igual para todos. Medidas en metros (= bloques), y para arriba; el héroe en el
 * centro, mirando hacia el que escribió el comando.
 */
public class MomentitoRenderer extends EntityRenderer<MomentitoEntity> {
	private static final float P = 1 / 16f, SS = 2.4f / 16f, FC_Y = 1f, TB0 = 8.5f, TB1 = 12f;
	private static final ResourceLocation HEROE = tex("heroe"), NAVE = tex("nave"), NAVE_BRILLO = tex("nave_brillo"),
			CRISTAL = tex("cristal"), TNT_LADO = tex("tnt_lado"), TNT_TAPA = tex("tnt_tapa"), BLANCO = tex("blanco"),
			EXPLOSION = tex("explosion");

	private static ResourceLocation tex(String n) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/momentito/" + n + ".png");
	}

	/** Un cubo de la nave: pivote, giro (grados, se aplica Z, Y, X), centro, medidas y las 6 caras de la textura. */
	private record CuboNave(float px, float py, float pz, float rx, float ry, float rz, float mx, float my, float mz,
							float w, float h, float d, float[][] uv) {}

	private final List<CuboNave> nave = new ArrayList<>();
	/** Cada píxel del Cristal del Desierto: {x, y, u, v}. */
	private final List<float[]> cristal = new ArrayList<>();
	/** La esfera del campo de fuerza (triángulos) y sus aristas. */
	private final List<Vector3f[]> esfera = new ArrayList<>();
	private final List<Vector3f[]> aristas = new ArrayList<>();

	public MomentitoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		cargarNave(contexto);
		cargarCristal(contexto);
		armarEsfera();
	}

	// ---------------------------------------------------------------- Carga

	private void cargarNave(EntityRendererProvider.Context contexto) {
		try (Reader lector = contexto.getResourceManager().openAsReader(
				ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "modelos/momentito_nave.json"))) {
			JsonObject geo = JsonParser.parseReader(lector).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
			float tw = geo.getAsJsonObject("description").get("texture_width").getAsFloat();
			float th = geo.getAsJsonObject("description").get("texture_height").getAsFloat();
			for (JsonElement b : geo.getAsJsonArray("bones")) {
				JsonArray cubos = b.getAsJsonObject().getAsJsonArray("cubes");
				if (cubos == null) continue;
				for (JsonElement e : cubos) {
					JsonObject c = e.getAsJsonObject();
					float[] o = tres(c, "origin", 0), s = tres(c, "size", 0), pv = tres(c, "pivot", 0), rot = tres(c, "rotation", 0);
					float inf = c.has("inflate") ? c.get("inflate").getAsFloat() : 0;
					boolean espejo = c.has("mirror") && c.get("mirror").getAsBoolean();
					float u = c.getAsJsonArray("uv").get(0).getAsFloat(), v = c.getAsJsonArray("uv").get(1).getAsFloat();
					float w = s[0], h = s[1], d = s[2];
					float[][] r = {
							{u, v + d, u + d, v + d + h}, {u + d + w, v + d, u + 2 * d + w, v + d + h},
							{u + d + w, v + d, u + d, v}, {u + d + w, v, u + d + 2 * w, v + d},
							{u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h}, {u + d, v + d, u + d + w, v + d + h}};
					if (espejo) {
						float[][] q = {r[1], r[0], r[2], r[3], r[4], r[5]};
						for (int i = 0; i < 6; i++) q[i] = new float[]{q[i][2], q[i][1], q[i][0], q[i][3]};
						r = q;
					}
					for (float[] cara : r) {
						cara[0] /= tw;
						cara[2] /= tw;
						cara[1] /= th;
						cara[3] /= th;
					}
					nave.add(new CuboNave(-pv[0], pv[1], pv[2], -rot[0], -rot[1], rot[2],
							-(o[0] + w / 2) + pv[0], o[1] + h / 2 - pv[1], o[2] + d / 2 - pv[2],
							w + 2 * inf, h + 2 * inf, d + 2 * inf, r));
				}
			}
		} catch (Exception e) {
			Dedsafio4.LOGGER.error("No se pudo cargar la nave del momentito", e);
		}
	}

	private static float[] tres(JsonObject o, String clave, float porDefecto) {
		if (!o.has(clave)) return new float[]{porDefecto, porDefecto, porDefecto};
		JsonArray a = o.getAsJsonArray(clave);
		return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
	}

	private void cargarCristal(EntityRendererProvider.Context contexto) {
		try (InputStream entrada = contexto.getResourceManager().open(CRISTAL); NativeImage img = NativeImage.read(entrada)) {
			int w = img.getWidth(), h = img.getHeight();
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if (((img.getPixelRGBA(x, y) >>> 24) & 255) < 128) continue;
					cristal.add(new float[]{x - w / 2f + 0.5f, h / 2f - y - 0.5f, (x + 0.5f) / w, (y + 0.5f) / h});
				}
			}
		} catch (Exception e) {
			Dedsafio4.LOGGER.error("No se pudo cargar el Cristal del Desierto", e);
		}
	}

	/** Icosaedro con cada cara partida en 16 (como IcosahedronGeometry(1, 3)), en una esfera de radio 1. */
	private void armarEsfera() {
		float t = (1 + (float) Math.sqrt(5)) / 2;
		float[][] v = {{-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0}, {0, -1, t}, {0, 1, t}, {0, -1, -t}, {0, 1, -t},
				{t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}};
		int[][] caras = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6},
				{7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
		int n = 4;   // detalle 3: cada cara partida en 16
		Set<String> vistas = new HashSet<>();
		for (int[] c : caras) {
			Vector3f a = new Vector3f(v[c[0]][0], v[c[0]][1], v[c[0]][2]), b = new Vector3f(v[c[1]][0], v[c[1]][1], v[c[1]][2]),
					cc = new Vector3f(v[c[2]][0], v[c[2]][1], v[c[2]][2]);
			Vector3f[][] p = new Vector3f[n + 1][];
			for (int i = 0; i <= n; i++) {
				p[i] = new Vector3f[n - i + 1];
				for (int j = 0; j <= n - i; j++) {
					p[i][j] = new Vector3f(a).mul((n - i - j) / (float) n).add(new Vector3f(b).mul(i / (float) n))
							.add(new Vector3f(cc).mul(j / (float) n)).normalize();
				}
			}
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n - i; j++) {
					triangulo(vistas, p[i][j], p[i + 1][j], p[i][j + 1]);
					if (j < n - i - 1) triangulo(vistas, p[i + 1][j], p[i + 1][j + 1], p[i][j + 1]);
				}
			}
		}
	}

	private void triangulo(Set<String> vistas, Vector3f a, Vector3f b, Vector3f c) {
		esfera.add(new Vector3f[]{a, b, c});
		for (Vector3f[] e : new Vector3f[][]{{a, b}, {b, c}, {c, a}}) {
			String k1 = clave(e[0]) + "|" + clave(e[1]), k2 = clave(e[1]) + "|" + clave(e[0]);
			if (vistas.add(k1) && vistas.add(k2)) aristas.add(e);
		}
	}

	private static String clave(Vector3f v) {
		return Math.round(v.x * 1000) + "," + Math.round(v.y * 1000) + "," + Math.round(v.z * 1000);
	}

	// ---------------------------------------------------------------- Ayudas

	private static float clamp(float x) {
		return Math.max(0, Math.min(1, x));
	}

	private static float seg(float t, float a, float b) {
		return clamp((t - a) / (b - a));
	}

	private static float sm(float s) {
		return s * s * (3 - 2 * s);
	}

	private static float lerp(float a, float b, float s) {
		return a + (b - a) * s;
	}

	/** Valores clave: entre uno y otro, suave. */
	private static float kf(float t, float[][] k) {
		if (t <= k[0][0]) return k[0][1];
		for (int i = 1; i < k.length; i++) {
			if (t <= k[i][0]) return lerp(k[i - 1][1], k[i][1], sm((t - k[i - 1][0]) / (k[i][0] - k[i - 1][0])));
		}
		return k[k.length - 1][1];
	}

	private static float azar(float i, float k) {
		double x = Math.sin(i * 127.1 + k * 311.7) * 43758.5453;
		return (float) (x - Math.floor(x));
	}

	/**
	 * Una caja centrada en (cx, cy, cz) de w × h × d. uv: las 6 caras (+x, -x, +y, -y, +z, -z), cada una
	 * {u0, v0, u1, v1} con (u0, v0) en la esquina de arriba a la izquierda mirando la cara de afuera.
	 */
	private static void caja(VertexConsumer vc, PoseStack.Pose p, float cx, float cy, float cz, float w, float h, float d,
							 float[][] uv, int r, int g, int b, int a, int luz) {
		float x0 = cx - w / 2, x1 = cx + w / 2, y0 = cy - h / 2, y1 = cy + h / 2, z0 = cz - d / 2, z1 = cz + d / 2;
		float[][][] caras = {
				{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z1}, {x1, y0, z0}, {1, 0, 0}},
				{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z0}, {x0, y0, z1}, {-1, 0, 0}},
				{{x0, y1, z0}, {x1, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {0, 1, 0}},
				{{x0, y0, z1}, {x1, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {0, -1, 0}},
				{{x0, y1, z1}, {x1, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {0, 0, 1}},
				{{x1, y1, z0}, {x0, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {0, 0, -1}}};
		for (int i = 0; i < 6; i++) {
			float[][] c = caras[i];
			float[] q = uv[i];
			if (q == null) continue;   // esa cara no se dibuja
			float[] n = c[4];
			vertice(vc, p, c[0], q[0], q[1], n, r, g, b, a, luz);
			vertice(vc, p, c[2], q[0], q[3], n, r, g, b, a, luz);
			vertice(vc, p, c[3], q[2], q[3], n, r, g, b, a, luz);
			vertice(vc, p, c[1], q[2], q[1], n, r, g, b, a, luz);
		}
	}

	private static void vertice(VertexConsumer vc, PoseStack.Pose p, float[] xyz, float u, float v, float[] n,
								int r, int g, int b, int a, int luz) {
		vc.addVertex(p, xyz[0], xyz[1], xyz[2]).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(luz).setNormal(p, n[0], n[1], n[2]);
	}

	private static final float[][] LISO = {{0.5f, 0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, 0.5f, 0.5f},
			{0.5f, 0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, 0.5f, 0.5f}};

	/** Caja de un color liso. */
	private static void color(VertexConsumer vc, PoseStack.Pose p, float cx, float cy, float cz, float w, float h, float d,
							  int rgb, int a, int luz) {
		caja(vc, p, cx, cy, cz, w, h, d, LISO, rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, a, luz);
	}

	/** Las caras de una caja con el mapa de las skins de Minecraft (64 × 64). */
	private static float[][] uvSkin(float u, float v, float w, float h, float d) {
		float[][] f = {{u + d + w, v + d, d, h}, {u, v + d, d, h}, {u + d, v, w, d}, {u + d + w, v, w, d},
				{u + d, v + d, w, h}, {u + 2 * d + w, v + d, w, h}};
		float[][] r = new float[6][];
		for (int i = 0; i < 6; i++) r[i] = new float[]{f[i][0] / 64, f[i][1] / 64, (f[i][0] + f[i][2]) / 64, (f[i][1] + f[i][3]) / 64};
		return r;
	}

	// ---------------------------------------------------------------- La escena (también la usa MomentitoCamara)

	// Las cuentas de la escena (dónde está la nave, la bomba y la cámara) están en Escena, que usa también el servidor.

	public static float radio(float[] c) {
		return com.dedsafio4.momentito.Escena.radio(c);
	}

	public static float alturaNave(float[] c) {
		return com.dedsafio4.momentito.Escena.alturaNave(c);
	}

	public static float alturaImpacto(float[] c) {
		return com.dedsafio4.momentito.Escena.alturaImpacto(c);
	}

	public static final float SALE_GRIETA = com.dedsafio4.momentito.Escena.SALE_GRIETA;

	public static Vector3f posNave(float t, float[] c) {
		return com.dedsafio4.momentito.Escena.posNave(t, c);
	}

	public static Vector3f posBomba(float t, float[] c) {
		return com.dedsafio4.momentito.Escena.posBomba(t, c);
	}

	public static float[] toma(float t, float[] c) {
		return com.dedsafio4.momentito.Escena.toma(t, c);
	}

	@Override
	public void render(MomentitoEntity m, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		float t = Mth.clamp(m.tiempoEscena(parcial), 0, 30);
		float[] fr = m.campo();
		ultimoGiro = m.getYRot();
		pose.pushPose();
		// El héroe mira hacia el que escribió el comando.
		pose.mulPose(Axis.YP.rotationDegrees(180 - m.getYRot()));
		nave(t, fr, pose, buffers, luz);
		bomba(t, fr, pose, buffers, luz);
		heroe(t, pose, buffers, luz);
		campo(t, fr, pose, buffers);
		explosion(t, fr, pose, buffers);
		pose.popPose();
	}

	private void nave(float t, float[] fr, PoseStack pose, MultiBufferSource buffers, int luz) {
		if (t < SALE_GRIETA || t >= 21) return;
		float llega = 1 - (float) Math.pow(1 - seg(t, SALE_GRIETA, 6), 3);
		Vector3f n = posNave(t, fr);
		pose.pushPose();
		pose.translate(n.x, n.y, n.z);
		float golpe = seg(t, TB1, TB1 + 1.6f);
		float rx = (t > TB1 && t < TB1 + 1.6f) ? Mth.sin(t * 40) * 0.07f * (1 - golpe) : 0;
		float rz = 0.5f * sm(seg(t, 17, 17.8f)) - 0.1f * (1 - llega);
		pose.mulPose(Axis.XP.rotation(rx));
		pose.mulPose(Axis.ZP.rotation(rz));

		pose.pushPose();
		pose.mulPose(Axis.YP.rotation(-Mth.HALF_PI));
		pose.scale(SS, SS, SS);
		VertexConsumer base = buffers.getBuffer(RenderType.entityCutoutNoCull(NAVE));
		for (CuboNave c : nave) {
			pose.pushPose();
			pose.translate(c.px, c.py, c.pz);
			pose.mulPose(Axis.ZP.rotationDegrees(c.rz));
			pose.mulPose(Axis.YP.rotationDegrees(c.ry));
			pose.mulPose(Axis.XP.rotationDegrees(c.rx));
			caja(base, pose.last(), c.mx, c.my, c.mz, c.w, c.h, c.d, c.uv, 255, 255, 255, 255, luz);
			pose.popPose();
		}
		VertexConsumer brillo = buffers.getBuffer(RenderType.eyes(NAVE_BRILLO));
		for (CuboNave c : nave) {
			pose.pushPose();
			pose.translate(c.px, c.py, c.pz);
			pose.mulPose(Axis.ZP.rotationDegrees(c.rz));
			pose.mulPose(Axis.YP.rotationDegrees(c.ry));
			pose.mulPose(Axis.XP.rotationDegrees(c.rx));
			caja(brillo, pose.last(), c.mx, c.my, c.mz, c.w * 1.002f, c.h * 1.002f, c.d * 1.002f, c.uv, 255, 255, 255, 255, LightTexture.FULL_BRIGHT);
			pose.popPose();
		}
		pose.popPose();

		// El hacker, parado en la nave.
		pose.pushPose();
		pose.translate(-4.5f * SS, 5 * SS, 0);
		pose.mulPose(Axis.YP.rotation(0.8f));
		pose.scale(0.85f, 0.85f, 0.85f);
		hacker(t, pose, buffers, luz);
		pose.popPose();
		pose.popPose();
	}

	private void hacker(float t, PoseStack pose, MultiBufferSource buffers, int luz) {
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO));
		int capucha = 0x141418, pantalon = 0x2B2D34, cara = 0x3A3530, neon = 0x39FF6A, fb = LightTexture.FULL_BRIGHT;
		boolean enojado = t > 14.5f && t < 17;
		float risa = t > 7.2f && t < 9 ? 1 : 0;
		// Cabeza
		pose.pushPose();
		pose.translate(0, 24 * P, 0);
		pose.mulPose(Axis.XP.rotation(risa * Mth.sin(t * 18) * 0.12f));
		pose.mulPose(Axis.YP.rotation(enojado ? Mth.sin(t * 14) * 0.45f : 0));
		color(vc, pose.last(), 0, 4 * P, 0, 8 * P, 8 * P, 8 * P, cara, 255, luz);
		color(vc, pose.last(), 0, 4.3f * P, -0.8f * P, 8.6f * P, 8.6f * P, 8.6f * P, capucha, 255, luz);
		color(vc, pose.last(), -2 * P, 4 * P, 4.35f * P, 1.6f * P, P, 0.4f * P, neon, 255, fb);
		color(vc, pose.last(), 2 * P, 4 * P, 4.35f * P, 1.6f * P, P, 0.4f * P, neon, 255, fb);
		pose.popPose();
		// Cuerpo
		pose.pushPose();
		pose.translate(0, 18 * P, 0);
		color(vc, pose.last(), 0, 0, 0, 8 * P, 12 * P, 4 * P, capucha, 255, luz);
		color(vc, pose.last(), 1.5f * P, 0, 2.1f * P, P, 8 * P, 0.3f * P, neon, 255, fb);
		pose.popPose();
		// Brazos: con el derecho aprieta el botón; con el izquierdo levanta el puño cuando se enoja.
		float brazoDer = -1.3f * sm(seg(t, 6.6f, 7)) * (1 - sm(seg(t, 7.8f, 8.2f)));
		float brazoIzq = enojado ? -2.6f + Mth.sin(t * 16) * 0.35f : (t > 17 ? -2.6f * (1 - sm(seg(t, 17, 17.5f))) : 0);
		for (int i = 0; i < 2; i++) {
			pose.pushPose();
			pose.translate((i == 0 ? -6 : 6) * P, 22 * P, 0);
			pose.mulPose(Axis.XP.rotation(i == 0 ? brazoDer : brazoIzq));
			color(vc, pose.last(), 0, -4 * P, 0, 4 * P, 12 * P, 4 * P, capucha, 255, luz);
			pose.popPose();
			color(vc, pose.last(), (i == 0 ? -2 : 2) * P, 6 * P, 0, 4 * P, 12 * P, 4 * P, pantalon, 255, luz);
		}
	}

	private void bomba(float t, float[] fr, PoseStack pose, MultiBufferSource buffers, int luz) {
		if (t < 7.4f || t >= TB1) return;
		float cae = seg(t, TB0, TB1);
		Vector3f b = posBomba(t, fr);
		pose.pushPose();
		pose.translate(b.x, b.y, b.z);
		pose.mulPose(Axis.XP.rotation(cae * 4));
		pose.mulPose(Axis.YP.rotation(cae * 6));
		pose.mulPose(Axis.ZP.rotation(cae * 1.4f));
		// Al final se infla, como la TNT de Minecraft a punto de explotar.
		float infla = 1 + 0.2f * seg(t, TB1 - 0.3f, TB1);
		pose.scale(infla, infla, infla);
		float[][] todo = {{0, 0, 1, 1}, {0, 0, 1, 1}, {0, 0, 1, 1}, {0, 0, 1, 1}, {0, 0, 1, 1}, {0, 0, 1, 1}};
		// Los costados con "H4CK" y, arriba y abajo, la tapa.
		float[][] lados = {todo[0], todo[1], null, null, todo[4], todo[5]};
		caja(buffers.getBuffer(RenderType.entityCutoutNoCull(TNT_LADO)), pose.last(), 0, 0, 0, 0.8f, 0.8f, 0.8f, lados, 255, 255, 255, 255, luz);
		float[][] tapas = {null, null, todo[2], todo[3], null, null};
		caja(buffers.getBuffer(RenderType.entityCutoutNoCull(TNT_TAPA)), pose.last(), 0, 0, 0, 0.8f, 0.8f, 0.8f, tapas, 255, 255, 255, 255, luz);
		// El aura verde que titila.
		float alfa = 0.3f + 0.7f * (Mth.sin(t * 22) > 0 ? 1 : 0);
		// La mecha: titila en blanco (más rápido al final).
		boolean mecha = t > TB1 - 1.8f && Math.floorMod((int) Math.floor((t - TB1) * (t > TB1 - 0.6f ? 10 : 4)), 2) == 0;
		if (mecha) {
			color(buffers.getBuffer(RenderType.entityTranslucentEmissive(BLANCO)), pose.last(), 0, 0, 0, 0.81f, 0.81f, 0.81f,
					0xFFFFFF, 210, LightTexture.FULL_BRIGHT);
		}
		cajaLineas(buffers.getBuffer(RenderType.lines()), pose.last(), 0.46f, 0x39FF6A, alfa);
		pose.popPose();
	}

	private static void cajaLineas(VertexConsumer vc, PoseStack.Pose p, float m, int rgb, float alfa) {
		float[][] v = {{-m, -m, -m}, {m, -m, -m}, {m, m, -m}, {-m, m, -m}, {-m, -m, m}, {m, -m, m}, {m, m, m}, {-m, m, m}};
		int[][] e = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
		for (int[] a : e) linea(vc, p, v[a[0]], v[a[1]], rgb, alfa);
	}

	private static void linea(VertexConsumer vc, PoseStack.Pose p, float[] a, float[] b, int rgb, float alfa) {
		float nx = b[0] - a[0], ny = b[1] - a[1], nz = b[2] - a[2];
		float l = Math.max(1e-5f, Mth.sqrt(nx * nx + ny * ny + nz * nz));
		int r = rgb >> 16 & 255, g = rgb >> 8 & 255, bl = rgb & 255, al = (int) (clamp(alfa) * 255);
		vc.addVertex(p, a[0], a[1], a[2]).setColor(r, g, bl, al).setNormal(p, nx / l, ny / l, nz / l);
		vc.addVertex(p, b[0], b[1], b[2]).setColor(r, g, bl, al).setNormal(p, nx / l, ny / l, nz / l);
	}

	private void heroe(float t, PoseStack pose, MultiBufferSource buffers, int luz) {
		float nota = sm(seg(t, 2.5f, 3.6f)) * (1 - sm(seg(t, 26, 28)));
		float salto = t > 2.5f && t < 2.9f ? Mth.sin(seg(t, 2.5f, 2.9f) * Mth.PI) * 0.18f : 0;
		float mira = sm(seg(t, 3, 4.5f)) * (1 - sm(seg(t, 25, 27)));
		float cielo = sm(seg(t, 17, 18)) * (1 - sm(seg(t, 24, 25.5f)));
		boolean busca = t > 8.4f && t < 11.4f;
		float sPitch = kf(t, new float[][]{{8.4f, 0}, {9.0f, 0.55f}, {9.4f, 0.5f}, {9.7f, 0.15f}, {10.3f, 0.15f}, {10.6f, 0.6f}, {10.9f, 0.55f}, {11.3f, 0}});
		float sYaw = kf(t, new float[][]{{9.4f, 0}, {9.8f, 0.55f}, {10.0f, 0.5f}, {10.35f, -0.45f}, {10.55f, 0}});
		float upW = kf(t, new float[][]{{8.4f, 1}, {8.9f, 0}, {11.0f, 0}, {11.4f, 1}});
		float cabezaX = (-0.55f * mira * upW + sPitch) * (1 - cielo) - 0.85f * cielo * mira;
		float cabezaY = -0.2f * mira * upW + sYaw;
		float brazosArriba = sm(seg(t, 10.9f, 11.5f)) * (1 - sm(seg(t, 24, 25)));
		float quieto = Mth.sin(t * 2.2f) * 0.05f * (busca ? 0 : 1);
		float rX = kf(t, new float[][]{{8.5f, 0}, {9.0f, -0.95f}, {9.25f, -0.85f}, {9.5f, -0.95f}, {9.75f, -0.2f}, {10.4f, -0.25f}, {10.7f, -1.15f}, {10.9f, -1.1f}});
		float rZ = kf(t, new float[][]{{8.5f, 0}, {9.0f, 0.45f}, {9.75f, 0.1f}, {10.4f, 0.1f}, {10.7f, 0.5f}, {10.9f, 0.45f}});
		float lX = kf(t, new float[][]{{9.3f, 0}, {9.6f, -0.35f}, {9.9f, -0.2f}, {10.2f, -0.35f}, {10.5f, 0}});
		float lZ = kf(t, new float[][]{{9.3f, 0}, {9.6f, 0.25f}, {10.2f, 0.25f}, {10.5f, 0}});
		float sw = 1 - brazosArriba;
		float agarra = kf(t, new float[][]{{10.6f, 0}, {10.8f, 1}, {11.1f, 1}, {11.3f, 0}});
		float firme = sm(seg(t, TB1, TB1 + 0.2f)) * (1 - sm(seg(t, TB1 + 0.2f, TB1 + 1)));

		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(HEROE));
		pose.pushPose();
		pose.translate(0, salto - 0.04f * firme, 0);
		pose.mulPose(Axis.YP.rotation(lerp(0.35f, -0.25f, nota)));
		parte(vc, pose, 0, 24, 0, cabezaX, cabezaY, 0, 4, 8, 8, 8, 0, 0, 32, 0, 1, luz);
		parte(vc, pose, 0, 18, 0, 0, sYaw * 0.25f, 0, 0, 8, 12, 4, 16, 16, 16, 32, 0.5f, luz);
		parte(vc, pose, -6, 22, 0, -2.7f * brazosArriba + quieto + rX * sw, 0, -0.25f * brazosArriba + rZ * sw, -4, 4, 12, 4, 40, 16, 40, 32, 0.5f, luz);
		parte(vc, pose, 6, 22, 0, -2.7f * brazosArriba - quieto + lX * sw, 0, 0.25f * brazosArriba - lZ * sw, -4, 4, 12, 4, 32, 48, 48, 48, 0.5f, luz);
		parte(vc, pose, -2, 12, 0, -0.25f * firme, 0, 0, -6, 4, 12, 4, 0, 16, 0, 32, 0.5f, luz);
		parte(vc, pose, 2, 12, 0, 0.25f * firme, 0, 0, -6, 4, 12, 4, 16, 48, 0, 48, 0.5f, luz);

		// La cadena dorada (media vuelta colgando del cuello).
		VertexConsumer oro = buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO));
		for (int i = 0; i <= 16; i++) {
			float a = i / 16f * Mth.PI;
			color(oro, pose.last(), 0.17f * Mth.cos(a), 23 * P - 0.17f * 1.2f * Mth.sin(a), 2.3f * P, 0.014f, 0.014f, 0.014f, 0xF2B632, 255, luz);
		}
		// El Cristal del Desierto: en el pecho; al levantar los brazos sube, se agranda y gira.
		pose.pushPose();
		pose.translate(0, lerp(18.5f * P, 2.45f, brazosArriba), lerp(2.6f * P, 0.55f, brazosArriba));
		float esc = 1 + 2.4f * brazosArriba;
		pose.scale(esc, esc, esc);
		pose.mulPose(Axis.YP.rotation(brazosArriba * t * 3));
		VertexConsumer cr = buffers.getBuffer(RenderType.entityCutoutNoCull(CRISTAL));
		float px = 0.0125f;
		int brilloCristal = brazosArriba > 0.05f || agarra > 0.05f ? LightTexture.FULL_BRIGHT : Math.max(luz, LightTexture.pack(10, 10));
		for (float[] c : cristal) {
			float[][] uv = {{c[2], c[3], c[2], c[3]}, {c[2], c[3], c[2], c[3]}, {c[2], c[3], c[2], c[3]},
					{c[2], c[3], c[2], c[3]}, {c[2], c[3], c[2], c[3]}, {c[2], c[3], c[2], c[3]}};
			caja(cr, pose.last(), c[0] * px, c[1] * px, 0, px, px, px * 1.6f, uv, 255, 255, 255, 255, brilloCristal);
		}
		// Hay que volver a pedir el buffer: al pedir el del cristal, el dorado anterior ya se cerró.
		color(buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO)), pose.last(), 0, 8 * px + 0.012f, 0, 0.036f, 0.012f, 0.012f,
				0xF2B632, 255, luz);
		pose.popPose();
		pose.popPose();
	}

	/**
	 * Una parte del héroe con su skin: pivote (px, py, pz en píxeles), giro (X, Y, Z), caja de w × h × d corrida
	 * oy hacia arriba, con la capa de afuera un poco más grande.
	 */
	private static void parte(VertexConsumer vc, PoseStack pose, float px, float py, float pz, float rx, float ry, float rz,
							  float oy, float w, float h, float d, float u, float v, float uo, float vo, float infla, int luz) {
		pose.pushPose();
		pose.translate(px * P, py * P, pz * P);
		if (rx != 0) pose.mulPose(Axis.XP.rotation(rx));
		if (ry != 0) pose.mulPose(Axis.YP.rotation(ry));
		if (rz != 0) pose.mulPose(Axis.ZP.rotation(rz));
		caja(vc, pose.last(), 0, oy * P, 0, w * P, h * P, d * P, uvSkin(u, v, w, h, d), 255, 255, 255, 255, luz);
		caja(vc, pose.last(), 0, oy * P, 0, (w + infla) * P, (h + infla) * P, (d + infla) * P, uvSkin(uo, vo, w, h, d), 255, 255, 255, 255, luz);
		pose.popPose();
	}

	/**
	 * El campo de fuerza: un óvalo centrado en los pies del héroe, de ancho (x) × largo (z), que sube "arriba"
	 * bloques y baja "abajo" (las dos mitades son medios elipsoides que comparten el borde a la altura del piso).
	 * Crece de 0 a su tamaño (11,2 a 11,9 s), tiembla con el golpe y se achica al final.
	 */
	private void campo(float t, float[] c, PoseStack pose, MultiBufferSource buffers) {
		float crece = sm(seg(t, 11.2f, 11.9f)) * (1 - sm(seg(t, 24.5f, 25.5f)));
		if (crece <= 0.001f) return;
		float tau = t - TB1;
		float onda = tau > 0 ? 0.08f * Mth.sin(tau * 24) * (float) Math.exp(-3 * tau) : 0;
		float choque = tau > 0 ? (float) Math.exp(-2.5 * tau) : 0;
		float pulso = 0.5f + 0.5f * Mth.sin(t * 6);
		float e = crece * (1 + onda);
		float ax = c[1] / 2 * e, az = c[0] / 2 * e, arriba = c[2] * e, abajo = c[3] * e;
		PoseStack.Pose p = pose.last();
		// La cáscara violeta transparente (72 × 48 como en el diseño).
		VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(BLANCO));
		int alfa = (int) (clamp(0.2f + 0.35f * choque + 0.05f * pulso) * 255);
		int r = (int) Math.min(255, 185 + 70 * choque), g = (int) Math.min(255, 120 + 100 * choque);
		int lon = 72, lat = 48;
		for (int i = 0; i < lat; i++) {
			for (int j = 0; j < lon; j++) {
				float[][] q = {punto(i, j, lat, lon), punto(i + 1, j, lat, lon), punto(i + 1, j + 1, lat, lon), punto(i, j + 1, lat, lon)};
				for (float[] v : q) {
					float y = v[1] * (v[1] > 0 ? arriba : abajo);
					vc.addVertex(p, v[0] * ax, y, v[2] * az).setColor(r, g, 255, alfa).setUv(0.5f, 0.5f)
							.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, v[0], v[1], v[2]);
				}
			}
		}
		// Las líneas (32 × 20) y el borde en el piso.
		VertexConsumer lineas = buffers.getBuffer(RenderType.lines());
		float alfaGrilla = (0.35f + 0.4f * choque) * crece, alfaBorde = (0.55f + 0.4f * choque) * crece;
		int lonL = 32, latL = 20;
		for (int i = 0; i <= latL; i++) {
			for (int j = 0; j < lonL; j++) {
				float[] a = escalar(punto(i, j, latL, lonL), ax, arriba, abajo, az), b = escalar(punto(i, j + 1, latL, lonL), ax, arriba, abajo, az);
				if (i > 0 && i < latL) linea(lineas, p, a, b, 0xD0A5FF, alfaGrilla);
				if (i < latL) linea(lineas, p, a, escalar(punto(i + 1, j, latL, lonL), ax, arriba, abajo, az), 0xD0A5FF, alfaGrilla);
			}
		}
		for (int i = 0; i < 128; i++) {
			float a0 = i / 128f * Mth.TWO_PI, a1 = (i + 1) / 128f * Mth.TWO_PI;
			linea(lineas, p, new float[]{Mth.cos(a0) * c[1] / 2 * crece, 0.03f, Mth.sin(a0) * c[0] / 2 * crece},
					new float[]{Mth.cos(a1) * c[1] / 2 * crece, 0.03f, Mth.sin(a1) * c[0] / 2 * crece}, 0xD0A5FF, alfaBorde);
		}
	}

	/** Un punto de la esfera de radio 1 (fila i de arriba a abajo, columna j). */
	private static float[] punto(int i, int j, int lat, int lon) {
		float th = i / (float) lat * Mth.PI, ph = j / (float) lon * Mth.TWO_PI;
		return new float[]{Mth.sin(th) * Mth.cos(ph), Mth.cos(th), Mth.sin(th) * Mth.sin(ph)};
	}

	private static float[] escalar(float[] v, float ax, float arriba, float abajo, float az) {
		return new float[]{v[0] * ax, v[1] * (v[1] > 0 ? arriba : abajo), v[2] * az};
	}

	/** Una dirección al azar (fija) un poco hacia arriba, como rndDir del diseño. */
	private static Vector3f direccion(int i, int k, float arriba) {
		float u = azar(i, k) * 2 - 1, th = azar(i, k + 1) * Mth.TWO_PI, sq = Mth.sqrt(1 - u * u);
		return new Vector3f(sq * Mth.cos(th), Math.abs(u) * arriba + u * (1 - arriba), sq * Mth.sin(th));
	}

	/**
	 * La explosión al estilo Minecraft: 40 nubes pixeladas que crecen y se deshacen (0,8 s) y 24 de humo oscuro
	 * que suben (2,4 s), del tamaño del campo de fuerza y por afuera de él, siempre mirando a la cámara.
	 */
	private void explosion(float t, float[] fr, PoseStack pose, MultiBufferSource buffers) {
		float tau = t - TB1;
		if (tau < 0 || tau >= 3.5f) return;
		float k = Math.max(1, Math.min(radio(fr), fr[2] * 2) / 12), imp = alturaImpacto(fr);
		float crece = sm(seg(t, 11.2f, 11.9f)) * (1 - sm(seg(t, 24.5f, 25.5f)));
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(EXPLOSION));
		for (int i = 0; i < 40; i++) {
			float lt = tau - azar(i, 14) * 0.5f;
			if (lt < 0 || lt >= 0.8f) continue;
			Vector3f pos = direccion(i, 11, 0.6f).mul((0.5f + azar(i, 13) * 3.5f) * k).add(0, imp, 0);
			nube(vc, pose, afuera(pos, fr, crece), (1.6f + azar(i, 15) * 1.8f) * k, i % 2, (int) (lt / 0.8f * 8));
		}
		for (int i = 0; i < 24; i++) {
			float lt = tau - (0.25f + azar(i, 24) * 0.7f);
			if (lt < 0 || lt >= 2.4f) continue;
			Vector3f pos = afuera(direccion(i, 21, 0.8f).mul((1 + azar(i, 23) * 3) * k).add(0, imp, 0), fr, crece);
			pos.y += (0.8f + azar(i, 26) * 1.2f) * lt * k;
			nube(vc, pose, pos, (1 + azar(i, 25) * 1.2f) * k, 2, (int) (lt / 2.4f * 8));
		}
	}

	/** Si está dentro del campo de fuerza (el óvalo), la empuja hasta su borde. */
	private static Vector3f afuera(Vector3f v, float[] c, float crece) {
		if (crece < 0.5f) return v;
		float ax = c[1] / 2 * crece, az = c[0] / 2 * crece, ay = (v.y > 0 ? c[2] : Math.max(c[3], 0.01f)) * crece;
		float nx = v.x / ax, ny = v.y / ay, nz = v.z / az, n = Mth.sqrt(nx * nx + ny * ny + nz * nz);
		if (n < 1.02f) {
			float f = 1.02f / Math.max(n, 1e-4f);
			return new Vector3f(nx * f * ax, (n < 1e-4f ? 1.02f : ny * f) * ay, nz * f * az);
		}
		return v;
	}

	/** Un cuadrado de la textura de la explosión (juego, cuadro) mirando a la cámara. */
	private void nube(VertexConsumer vc, PoseStack pose, Vector3f pos, float tam, int juego, int cuadro) {
		cuadro = Math.min(7, Math.max(0, cuadro));
		float u0 = cuadro * 16 / 128f, u1 = u0 + 16 / 128f, v0 = juego * 16 / 48f, v1 = v0 + 16 / 48f;
		pose.pushPose();
		pose.translate(pos.x, pos.y, pos.z);
		pose.mulPose(Axis.YP.rotationDegrees(-(180 - ultimoGiro)));   // deshace el giro de la escena
		pose.mulPose(entityRenderDispatcher.cameraOrientation());
		pose.scale(tam, tam, tam);
		PoseStack.Pose p = pose.last();
		float[] n = {0, 0, 1};
		vertice(vc, p, new float[]{-0.5f, -0.5f, 0}, u0, v1, n, 255, 255, 255, 255, LightTexture.FULL_BRIGHT);
		vertice(vc, p, new float[]{0.5f, -0.5f, 0}, u1, v1, n, 255, 255, 255, 255, LightTexture.FULL_BRIGHT);
		vertice(vc, p, new float[]{0.5f, 0.5f, 0}, u1, v0, n, 255, 255, 255, 255, LightTexture.FULL_BRIGHT);
		vertice(vc, p, new float[]{-0.5f, 0.5f, 0}, u0, v0, n, 255, 255, 255, 255, LightTexture.FULL_BRIGHT);
		pose.popPose();
	}

	private float ultimoGiro;

	@Override
	public boolean shouldRender(MomentitoEntity m, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public ResourceLocation getTextureLocation(MomentitoEntity m) {
		return BLANCO;
	}
}
