package com.dedsafio4.client;

import com.dedsafio4.cielo.CieloNoche;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * El cielo de noche de las animaciones (CieloNoche): si estás cerca de donde pasa, el cielo se oscurece de a poco,
 * aparecen estrellas y la niebla del horizonte se pone oscura; al terminar vuelve a la normalidad.
 */
public final class CieloNocheCliente {
	private CieloNocheCliente() {}

	private record Zona(Vec3 centro, float radio, long desde, long hasta) {}
	private static final List<Zona> ZONAS = new ArrayList<>();
	/** Cuánto tarda en oscurecerse y en aclararse (ms), y el borde (bloques) donde se va oscureciendo. */
	private static final long ENTRA = 1500, SALE = 1500;
	private static final float BORDE = 4f;
	/** Las estrellas: dirección y tamaño de cada una (siempre las mismas). */
	private static final float[][] ESTRELLAS = new float[1500][];

	static {
		RandomSource azar = RandomSource.create(10842L);
		for (int i = 0; i < ESTRELLAS.length; i++) {
			Vector3f d;
			do {
				d = new Vector3f(azar.nextFloat() * 2 - 1, azar.nextFloat() * 2 - 1, azar.nextFloat() * 2 - 1);
			} while (d.lengthSquared() > 1 || d.lengthSquared() < 0.01f);
			d.normalize();
			ESTRELLAS[i] = new float[]{d.x, d.y, d.z, 0.15f + azar.nextFloat() * 0.12f, azar.nextFloat() * Mth.TWO_PI, 0.5f + azar.nextFloat() * 0.5f};
		}
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(CieloNoche.Payload.TYPE, (payload, context) -> {
			long ahora = Util.getMillis();
			ZONAS.add(new Zona(new Vec3(payload.x(), payload.y(), payload.z()), payload.radio(), ahora, ahora + payload.ticks() * 50L));
		});
	}

	/** Qué tan de noche está para la cámara (0 = normal, 1 = noche del todo). */
	public static float noche() {
		if (ZONAS.isEmpty()) return 0f;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.gameRenderer == null) return 0f;
		long ahora = Util.getMillis();
		ZONAS.removeIf(z -> ahora > z.hasta() + SALE);
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		float k = 0f;
		for (Zona z : ZONAS) {
			float tiempo = Math.min(1f, (ahora - z.desde()) / (float) ENTRA);
			if (ahora > z.hasta()) tiempo = Math.min(tiempo, 1f - (ahora - z.hasta()) / (float) SALE);
			double d = cam.distanceTo(z.centro());
			float cerca = Mth.clamp((float) ((z.radio() + BORDE - d) / BORDE), 0f, 1f);
			k = Math.max(k, Mth.clamp(tiempo, 0f, 1f) * cerca);
		}
		return k;
	}

	/** Después del cielo de Minecraft: el negro encima (con su transparencia) y las estrellas. */
	public static void dibujar(Matrix4f matrizVista) {
		float k = noche();
		if (k <= 0.003f) return;
		PoseStack pose = new PoseStack();
		pose.mulPose(matrizVista);
		Matrix4f m = pose.last().pose();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		// El fondo oscuro: un cubo alrededor de la cámara.
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		float s = 80f;
		int a = Math.round(k * 255f);
		float[][] caras = {
				{-s, s, -s}, {s, s, -s}, {s, s, s}, {-s, s, s},
				{-s, -s, -s}, {s, -s, -s}, {s, s, -s}, {-s, s, -s},
				{s, -s, s}, {-s, -s, s}, {-s, s, s}, {s, s, s},
				{-s, -s, s}, {-s, -s, -s}, {-s, s, -s}, {-s, s, s},
				{s, -s, -s}, {s, -s, s}, {s, s, s}, {s, s, -s},
				{-s, -s, -s}, {-s, -s, s}, {s, -s, s}, {s, -s, -s}};
		for (float[] v : caras) b.addVertex(m, v[0], v[1], v[2]).setColor(6, 6, 14, a);
		BufferUploader.drawWithShader(b.buildOrThrow());
		// Las estrellas (titilan un poco).
		BufferBuilder e = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		float t = (Util.getMillis() % 100000L) / 1000f;
		Vector3f arriba = new Vector3f(), lado = new Vector3f(), centro = new Vector3f();
		for (float[] st : ESTRELLAS) {
			centro.set(st[0], st[1], st[2]).mul(70f);
			Vector3f dir = new Vector3f(st[0], st[1], st[2]);
			Vector3f ref = Math.abs(dir.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
			dir.cross(ref, lado).normalize().mul(st[3]);
			dir.cross(lado, arriba).normalize().mul(st[3]);
			float brillo = st[5] * (0.75f + 0.25f * Mth.sin(t * 2f + st[4]));
			int ae = Math.round(k * brillo * 255f);
			e.addVertex(m, centro.x - lado.x - arriba.x, centro.y - lado.y - arriba.y, centro.z - lado.z - arriba.z).setColor(255, 255, 255, ae);
			e.addVertex(m, centro.x + lado.x - arriba.x, centro.y + lado.y - arriba.y, centro.z + lado.z - arriba.z).setColor(255, 255, 255, ae);
			e.addVertex(m, centro.x + lado.x + arriba.x, centro.y + lado.y + arriba.y, centro.z + lado.z + arriba.z).setColor(255, 255, 255, ae);
			e.addVertex(m, centro.x - lado.x + arriba.x, centro.y - lado.y + arriba.y, centro.z - lado.z + arriba.z).setColor(255, 255, 255, ae);
		}
		BufferUploader.drawWithShader(e.buildOrThrow());
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}
}
