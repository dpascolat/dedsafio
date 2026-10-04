package com.dedsafio4.client.despegue;

import com.dedsafio4.despegue.Tunel;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix4f;

/**
 * El túnel del hiperespacio (/tptp), como en tunel-animacion.html: fondo negro, 230 rayos blancos en forma
 * de cuchilla que salen del centro y se acercan cada vez más rápido (suma de luz), con el centro oscuro.
 * Tapa toda la pantalla; entra y sale con un fundido.
 */
public final class TunelPantalla {
	private TunelPantalla() {}

	private static final int N = 230, PASOS = 6;
	private static final float ENTRA = 500, SALE = 800;

	private static final float[] ang = new float[N], prof = new float[N], ancho = new float[N], brillo = new float[N];
	private static final boolean[] neblina = new boolean[N];
	private static int semilla = 3;
	private static long desde = -1, ultimo, duracion;

	private static float azar() {
		semilla = (int) ((semilla * 16807L) % 2147483647L);
		return semilla / 2147483647f;
	}

	private static void nacer(int i, boolean inicio) {
		ang[i] = azar() * (float) Math.PI * 2;
		prof[i] = inicio ? 0.03f + azar() * 0.97f : 1;
		ancho[i] = 0.4f + (float) Math.pow(azar(), 3) * 3.2f;
		brillo[i] = 0.45f + azar() * 0.55f;
		neblina[i] = azar() < 0.08f;
	}

	public static void registrar() {
		for (int i = 0; i < N; i++) nacer(i, true);
		ClientPlayNetworking.registerGlobalReceiver(Tunel.Payload.TYPE, (payload, context) -> context.client().execute(() -> {
			desde = ultimo = Util.getMillis();
			duracion = payload.ticks() * 50L;
			context.client().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 0.6f, 1f));
		}));
		HudRenderCallback.EVENT.register((g, contador) -> dibujar(g));
	}

	private static void dibujar(GuiGraphics g) {
		if (desde < 0) return;
		long ahora = Util.getMillis(), pasaron = ahora - desde;
		if (pasaron > duracion) {
			desde = -1;
			return;
		}
		float dt = Math.min(0.05f, (ahora - ultimo) / 1000f);
		ultimo = ahora;
		float opacidad = Math.min(1, pasaron / ENTRA) * Math.min(1, (duracion - pasaron) / SALE);

		int w = g.guiWidth(), h = g.guiHeight();
		g.fill(0, 0, w, h, ((int) (opacidad * 255) << 24));
		float s = Math.max(w, h * 16f / 9f) / 1920f, cx = w / 2f, cy = h / 2f, k = 60 * s;
		float maxR = (float) Math.hypot(w, h) * 0.6f;

		RenderSystem.enableBlend();
		RenderSystem.disableCull();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);   // suma de luz
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Matrix4f m = g.pose().last().pose();
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < N; i++) {
			prof[i] -= 0.3f * dt * (0.3f + prof[i] * 0.7f);
			if (prof[i] < 0.015f) {
				nacer(i, false);
				continue;
			}
			float z = prof[i], zt = Math.min(z * 1.7f + 0.004f, z + 0.09f);
			float r1 = k / z, r0 = k / zt;
			if (r0 > maxR * 1.6f) continue;
			float a = (neblina[i] ? 0.08f : brillo[i]) * Math.min(1, (1 - z) * 3.5f) * opacidad;
			float w1 = Math.min((neblina[i] ? 140 : 60) * s, ancho[i] * s * (neblina[i] ? 6 : 1.6f) / (float) Math.pow(z, 0.9));
			rayo(b, m, cx, cy, ang[i], r0, r1, w1, a);
		}
		MeshData malla = b.build();
		if (malla != null) BufferUploader.drawWithShader(malla);
		RenderSystem.defaultBlendFunc();
		// El centro, oscuro.
		centro(m, cx, cy, 120 * s, 0.7f * opacidad);
		RenderSystem.enableCull();
	}

	/**
	 * Un rayo en forma de cuchilla: angosto en la cola, ancho cerca del 55% y en punta en la cabeza, con
	 * la luz que sube y baja a lo largo (0 → A al 30% → 0,85·A al 75% → 0).
	 */
	private static void rayo(BufferBuilder b, Matrix4f m, float cx, float cy, float ang, float r0, float r1, float w1, float a) {
		float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang), px = -sa, py = ca;
		float largo = r1 - r0, rm = r0 + largo * 0.55f, w0 = w1 * 0.08f;
		float rAntes = 0, wAntes = 0, aAntes = 0;
		for (int p = 0; p <= PASOS; p++) {
			float t = p / (float) PASOS, u = 1 - t;
			float r = u * u * r0 + 2 * u * t * rm + t * t * r1;
			float ww = u * u * w0 + 2 * u * t * w1 + t * t * w1 * 0.15f;
			float f = (r - r0) / largo;
			float al = f < 0.3f ? a * f / 0.3f : f < 0.75f ? a * (1 - 0.15f * (f - 0.3f) / 0.45f) : a * 0.85f * (1 - f) / 0.25f;
			if (p > 0) {
				float x0 = cx + ca * rAntes, y0 = cy + sa * rAntes, x1 = cx + ca * r, y1 = cy + sa * r;
				punto(b, m, x0 + px * wAntes, y0 + py * wAntes, aAntes);
				punto(b, m, x0 - px * wAntes, y0 - py * wAntes, aAntes);
				punto(b, m, x1 + px * ww, y1 + py * ww, al);
				punto(b, m, x0 - px * wAntes, y0 - py * wAntes, aAntes);
				punto(b, m, x1 - px * ww, y1 - py * ww, al);
				punto(b, m, x1 + px * ww, y1 + py * ww, al);
			}
			rAntes = r;
			wAntes = ww;
			aAntes = al;
		}
	}

	private static void punto(BufferBuilder b, Matrix4f m, float x, float y, float alfa) {
		b.addVertex(m, x, y, 0).setColor(1f, 1f, 1f, Math.max(0, Math.min(1, alfa)));
	}

	/** Gradiente redondo negro en el medio (alfa en el centro → 0 en el borde). */
	private static void centro(Matrix4f m, float cx, float cy, float radio, float alfa) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		int lados = 32;
		for (int i = 0; i < lados; i++) {
			double a0 = i * Math.PI * 2 / lados, a1 = (i + 1) * Math.PI * 2 / lados;
			b.addVertex(m, cx, cy, 0).setColor(0f, 0f, 0f, alfa);
			b.addVertex(m, cx + (float) Math.cos(a1) * radio, cy + (float) Math.sin(a1) * radio, 0).setColor(0f, 0f, 0f, 0f);
			b.addVertex(m, cx + (float) Math.cos(a0) * radio, cy + (float) Math.sin(a0) * radio, 0).setColor(0f, 0f, 0f, 0f);
		}
		MeshData malla = b.build();
		if (malla != null) BufferUploader.drawWithShader(malla);
	}
}
