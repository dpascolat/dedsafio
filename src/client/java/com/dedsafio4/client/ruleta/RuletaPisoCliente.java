package com.dedsafio4.client.ruleta;

import com.dedsafio4.ruleta.RuletaPiso;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * La ruleta de 8 colores en el piso (/mision 1), dibujada como un círculo liso con triángulos.
 * Ciclo de 18 s (360 ticks): cada porción crece del centro al borde en 64 ticks (rápido al principio, frena al final)
 * y la siguiente arranca 32 ticks después; todas completas en el tick 288, quieta hasta el 348 y se encoge hasta el
 * 360. Mientras crece, cada porción brilla un poco.
 * Las posiciones van desde abajo a la izquierda, de izquierda a derecha, mirando desde arriba con el jugador abajo.
 */
public final class RuletaPisoCliente {
	private RuletaPisoCliente() {}

	private static final float RADIO = 8f;
	private static final int DURACION = 360, PASO = 32, CRECER = 64, LLENA = 7 * PASO + CRECER, QUIETA = 60, SALIDA = 12;
	/** En orden de aparición: celeste, azul, morado, rosa, naranja, rojo, amarillo, verde. */
	private static final int[] COLORES = {0x7FD3F5, 0x2A5BD7, 0x7B3FC4, 0xF27BB5, 0xF28A1E, 0xD8261C, 0xF7D531, 0x3FAE3A};
	private static final int OSCURO = 0x201E1D;

	private static Vec3 centro;
	/** Hacia adelante y hacia la derecha del jugador que la puso. */
	private static Vec3 adelante, derecha;
	private static long inicio = -1;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(RuletaPiso.Payload.TYPE, (payload, context) -> context.client().execute(() -> {
			Minecraft mc = context.client();
			if (mc.level == null) return;
			centro = new Vec3(payload.x(), payload.y(), payload.z());
			float yaw = payload.yaw() * Mth.DEG_TO_RAD;
			adelante = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
			derecha = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
			inicio = mc.level.getGameTime();
		}));
		WorldRenderEvents.AFTER_TRANSLUCENT.register(RuletaPisoCliente::dibujar);
	}

	/** Un punto de la ruleta: ángulo como en la referencia (0 = hacia el jugador, crece hacia su derecha). */
	private static Vec3 punto(double angulo, double radio) {
		double x = radio * Math.sin(angulo), z = radio * Math.cos(angulo);
		return derecha.scale(x).add(adelante.scale(-z));
	}

	private static void dibujar(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		if (inicio < 0 || mc.level == null) return;
		float t = mc.level.getGameTime() - inicio + contexto.tickCounter().getGameTimeDeltaPartialTick(false);
		if (t >= DURACION || t < 0) {
			inicio = -1;
			return;
		}
		Vec3 camara = contexto.camera().getPosition();
		Matrix4f m = new Matrix4f().translate((float) (centro.x - camara.x), (float) (centro.y - camara.y), (float) (centro.z - camara.z));
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());

		float salida = Mth.clamp((t - (LLENA + QUIETA)) / SALIDA, 0, 1);
		// Sin borde debajo de los colores: solo el centro oscuro (arriba).
		for (int k = 0; k < 8; k++) {
			float p = Mth.clamp((t - k * PASO) / CRECER, 0, 1);
			if (p <= 0) continue;
			float radio = RADIO * (1 - (1 - p) * (1 - p)) * (1 - salida);
			if (radio < 0.01f) continue;
			int posicion = (k + 1) % 8;   // celeste en el lugar 2, ..., verde en el 1
			double desde = Math.toRadians(315 + 45 * posicion);
			int color = COLORES[k];
			if (p < 1) color = mezclar(color, 0xFFFFFF, 0.3f);   // brilla mientras crece
			disco(vc, m, desde, Math.PI / 4, radio, 0.03f, color, 16);
		}
		disco(vc, m, 0, Math.PI * 2, 0.7 * (1 - salida), 0.045f, OSCURO, 32);
		buffers.endBatch(RenderType.debugQuads());
	}

	private static int mezclar(int a, int b, float k) {
		int r = (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255), g = (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255),
				bl = (int) Mth.lerp(k, a & 255, b & 255);
		return r << 16 | g << 8 | bl;
	}

	/** Una porción de círculo (o el círculo entero) acostada a esa altura sobre el piso, hecha de triángulos. */
	private static void disco(VertexConsumer vc, Matrix4f m, double desde, double abertura, double radio, float altura, int color, int partes) {
		int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
		for (int i = 0; i < partes; i++) {
			Vec3 a = punto(desde + abertura * i / partes, radio), c = punto(desde + abertura * (i + 1) / partes, radio);
			vc.addVertex(m, 0, altura, 0).setColor(r, g, b, 255);
			vc.addVertex(m, 0, altura, 0).setColor(r, g, b, 255);
			vc.addVertex(m, (float) c.x, altura, (float) c.z).setColor(r, g, b, 255);
			vc.addVertex(m, (float) a.x, altura, (float) a.z).setColor(r, g, b, 255);
			// Y al revés, por si se mira desde abajo o la cara de arriba no se dibuja.
			vc.addVertex(m, 0, altura, 0).setColor(r, g, b, 255);
			vc.addVertex(m, (float) a.x, altura, (float) a.z).setColor(r, g, b, 255);
			vc.addVertex(m, (float) c.x, altura, (float) c.z).setColor(r, g, b, 255);
			vc.addVertex(m, 0, altura, 0).setColor(r, g, b, 255);
		}
	}
}
