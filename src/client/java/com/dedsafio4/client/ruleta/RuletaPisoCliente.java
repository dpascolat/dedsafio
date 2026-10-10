package com.dedsafio4.client.ruleta;

import com.dedsafio4.ruleta.RuletaPiso;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * La ruleta de 8 colores en el piso (/mision 1 y el bloque de Entrega de Misiones), dibujada lisa con triángulos, y
 * la esfera aurora. Ciclo de 236 ticks (11,8 s):
 *   0–80     salen los 8 colores a la vez, del centro al borde (rápido al principio, frena al final; el borde tiembla)
 *   80–140   quieta, completa
 *   140–156  se encogen al centro y desaparecen
 *   156–236  sale un anillo (solo el borde) con los 8 colores en sus lugares, que se agranda y se desvanece
 * Los colores tienen vetas claras y oscuras que fluyen como un líquido, y rayitas negras en los bordes de cada
 * porción. La esfera aurora (transparente y luminosa, con bandas verdes que fluyen y el borde más brillante) gira y
 * sube y baja en su lugar todo el ciclo.
 * Las posiciones de los colores van desde abajo a la izquierda, de izquierda a derecha, mirando desde arriba con el
 * jugador abajo.
 */
public final class RuletaPisoCliente {
	private RuletaPisoCliente() {}

	/** La del diseño mide 8 de radio; acá un bloque más hacia cada lado. */
	private static final float RADIO = 9f, ESCALA = RADIO / 8f;
	private static final int CRECER = 80, QUIETA = 60, VUELVE = 16, ANILLO = 80;
	/** En orden: celeste, azul, morado, rosa, naranja, rojo, amarillo, verde. */
	private static final int[] COLORES = {0x7FD3F5, 0x2A5BD7, 0x7B3FC4, 0xF27BB5, 0xF28A1E, 0xD8261C, 0xF7D531, 0x3FAE3A};
	private static final int OSCURO = 0x201E1D;

	private static Vec3 centro, adelante, derecha, esfera;
	private static float radioEsfera;
	private static long inicio = -1;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(RuletaPiso.Payload.TYPE, (payload, context) -> context.client().execute(() -> {
			Minecraft mc = context.client();
			if (mc.level == null) return;
			centro = new Vec3(payload.x(), payload.y(), payload.z());
			float yaw = payload.yaw() * Mth.DEG_TO_RAD;
			adelante = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
			derecha = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
			esfera = new Vec3(payload.esferaX(), payload.esferaY(), payload.esferaZ());
			radioEsfera = payload.radioEsfera();
			inicio = mc.level.getGameTime();
		}));
		WorldRenderEvents.AFTER_TRANSLUCENT.register(RuletaPisoCliente::dibujar);
	}

	/** Un punto de la ruleta (relativo al centro): ángulo como en la referencia (0 = hacia el jugador, crece hacia su derecha). */
	private static Vec3 punto(double angulo, double radio) {
		return derecha.scale(radio * Math.sin(angulo)).add(adelante.scale(-radio * Math.cos(angulo)));
	}

	private static float clamp01(float x) {
		return Mth.clamp(x, 0, 1);
	}

	private static void dibujar(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		if (inicio < 0 || mc.level == null) return;
		float t = mc.level.getGameTime() - inicio + contexto.tickCounter().getGameTimeDeltaPartialTick(false);
		if (t >= RuletaPiso.DURACION || t < 0) {
			inicio = -1;
			return;
		}
		float segundos = (Util.getMillis() % 3_600_000L) / 1000f;
		Vec3 camara = contexto.camera().getPosition();
		Matrix4f m = new Matrix4f().translate((float) (centro.x - camara.x), (float) (centro.y - camara.y), (float) (centro.z - camara.z));
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());

		// --- Las 8 porciones ---
		float p = clamp01(t / CRECER);
		float vuelta = clamp01((t - (CRECER + QUIETA)) / VUELVE);
		float encoge = 1 - (vuelta < 0.5f ? 2 * vuelta * vuelta : 1 - (float) Math.pow(-2 * vuelta + 2, 2) / 2);
		float radio = RADIO * (1 - (1 - p) * (1 - p)) * encoge;
		if (p > 0 && vuelta < 1 && radio > 0.01f) {
			for (int k = 0; k < 8; k++) {
				int posicion = (k + 1) % 8;   // celeste en el lugar 2, ..., verde en el 1
				double desde = Math.toRadians(315 + 45 * posicion);
				float tiembla = p < 1 ? 0.025f * Mth.sin(segundos * 1000 / 140f + k) * (1 - p) : 0;
				porcion(vc, m, desde, radio, tiembla, brillante(COLORES[k]), segundos * 0.7f + k * 1.37f);
				// Las rayitas negras en los dos bordes.
				rayita(vc, m, desde, radio);
				rayita(vc, m, desde + Math.PI / 4, radio);
			}
		}
		// El centro oscuro.
		disco(vc, m, 0.7f, 0.05f, OSCURO);

		// --- El anillo de colores (solo el borde) ---
		if (t >= CRECER + QUIETA + VUELVE) {
			float q = clamp01((t - (CRECER + QUIETA + VUELVE)) / ANILLO);
			float r = (0.8f + 11 * q) * ESCALA, alfa = Math.min(1, q * 6) * (1 - q);
			for (int k = 0; k < 8; k++) {
				double desde = Math.toRadians(315 + 45 * ((k + 1) % 8));
				arco(vc, m, desde, r * 0.94f, r, mezclar(brillante(COLORES[k]), 0xFFFFFF, 0.25f), alfa);
			}
		}
		buffers.endBatch(RenderType.debugQuads());

		// --- La esfera aurora (luminosa: se suma a lo de atrás) ---
		if (radioEsfera > 0) {
			float sube = 0.03f * Mth.sin(segundos * 1000 / 600f);
			Vec3 c = esfera.add(0, sube, 0);
			Matrix4f me = new Matrix4f().translate((float) (c.x - camara.x), (float) (c.y - camara.y), (float) (c.z - camara.z));
			// Primero el cuerpo (colores transparentes normales, así se ve también de día) y encima el brillo.
			esferaAurora(buffers.getBuffer(RenderType.debugQuads()), me, c.subtract(camara), radioEsfera, segundos, false);
			buffers.endBatch(RenderType.debugQuads());
			esferaAurora(buffers.getBuffer(Aurora.TIPO), me, c.subtract(camara), radioEsfera, segundos, true);
			buffers.endBatch(Aurora.TIPO);
		}
	}

	// --- La ruleta ---

	/**
	 * Una porción, en una grilla (para que las vetas del líquido se vean): el brillo va de 78 % a 116 % del color,
	 * con un destello suave en las crestas, y las vetas fluyen todo el tiempo.
	 */
	private static void porcion(VertexConsumer vc, Matrix4f m, double desde, float radio, float tiembla, int color, float tiempo) {
		int anillos = 14, partes = 10;
		for (int i = 0; i < anillos; i++) {
			for (int j = 0; j < partes; j++) {
				double a0 = desde + Math.PI / 4 * j / partes, a1 = desde + Math.PI / 4 * (j + 1) / partes;
				float r0 = radio * i / anillos, r1 = radio * (i + 1) / anillos;
				Vec3[] v = {punto(a0, r0 * deforma(a0, tiembla)), punto(a0, r1 * deforma(a0, tiembla)),
						punto(a1, r1 * deforma(a1, tiembla)), punto(a1, r0 * deforma(a1, tiembla))};
				for (int n : new int[]{0, 1, 2, 3, 3, 2, 1, 0}) {
					Vec3 w = v[n];
					vertice(vc, m, (float) w.x, 0.03f, (float) w.z, liquido(color, w, tiempo), 1f);
				}
			}
		}
	}

	/** El temblor del borde mientras crece (la porción se estira un poco para un lado y se achica para el otro). */
	private static float deforma(double angulo, float tiembla) {
		return 1 + tiembla * (float) Math.cos(2 * angulo);
	}

	/** El color con las vetas del líquido en ese punto. */
	private static int liquido(int color, Vec3 w, float t) {
		double qx = w.x * 0.55 / ESCALA, qy = w.z * 0.55 / ESCALA;
		double nx = qx + 0.6 * Math.sin(qy * 1.3 + t), ny = qy + 0.6 * Math.cos(qx * 1.1 - t * 0.8);
		nx += 0.4 * Math.sin(ny * 2.1 - t * 1.2);
		ny += 0.4 * Math.cos(nx * 1.9 + t);
		double v = 0.5 + 0.5 * Math.sin(nx * 1.7 + ny * 1.3 + t * 1.5);
		double brillo = 0.78 + 0.38 * v, destello = 0.12 * Math.pow(v, 6) * 255;
		int r = (int) Math.min(255, (color >> 16 & 255) * brillo + destello), g = (int) Math.min(255, (color >> 8 & 255) * brillo + destello),
				b = (int) Math.min(255, (color & 255) * brillo + destello);
		return r << 16 | g << 8 | b;
	}

	/** Una rayita negra del centro al borde, en ese ángulo. */
	private static void rayita(VertexConsumer vc, Matrix4f m, double angulo, float radio) {
		Vec3 dir = punto(angulo, 1), costado = new Vec3(-dir.z, 0, dir.x).scale(0.045);
		Vec3 fin = dir.scale(radio);
		Vec3[] v = {costado, fin.add(costado), fin.subtract(costado), costado.reverse()};
		for (int n : new int[]{0, 1, 2, 3, 3, 2, 1, 0}) vertice(vc, m, (float) v[n].x, 0.04f, (float) v[n].z, OSCURO, 1f);
	}

	/** Un círculo lleno acostado. */
	private static void disco(VertexConsumer vc, Matrix4f m, float radio, float altura, int color) {
		int partes = 32;
		for (int i = 0; i < partes; i++) {
			Vec3 a = punto(2 * Math.PI * i / partes, radio), c = punto(2 * Math.PI * (i + 1) / partes, radio);
			for (Vec3 w : new Vec3[]{Vec3.ZERO, Vec3.ZERO, c, a, a, c, Vec3.ZERO, Vec3.ZERO}) {
				vertice(vc, m, (float) w.x, altura, (float) w.z, color, 1f);
			}
		}
	}

	/** Un pedazo de anillo (de un color) acostado. */
	private static void arco(VertexConsumer vc, Matrix4f m, double desde, float adentro, float afuera, int color, float alfa) {
		int partes = 12;
		for (int i = 0; i < partes; i++) {
			double a0 = desde + Math.PI / 4 * i / partes, a1 = desde + Math.PI / 4 * (i + 1) / partes;
			Vec3[] v = {punto(a0, adentro), punto(a0, afuera), punto(a1, afuera), punto(a1, adentro)};
			for (int n : new int[]{0, 1, 2, 3, 3, 2, 1, 0}) vertice(vc, m, (float) v[n].x, 0.035f, (float) v[n].z, color, alfa);
		}
	}

	// --- La esfera aurora ---

	/**
	 * Una esfera con las bandas de la aurora (verde, verde azulado y verde lima) que fluyen y se deforman; el borde
	 * de la silueta brilla más. Gira despacio sobre su eje (1 vuelta cada 19 s).
	 * relativa: el centro de la esfera visto desde la cámara.
	 */
	/** brillo = false: el cuerpo (colores transparentes normales, para que se vea de día); true: la luz que se suma encima. */
	private static void esferaAurora(VertexConsumer vc, Matrix4f m, Vec3 relativa, float radio, float segundos, boolean brillo) {
		int lat = 20, lon = 40;
		double giro = segundos / 3.0;
		float t = segundos * 0.6f;
		for (int i = 0; i < lat; i++) {
			for (int j = 0; j < lon; j++) {
				double[][] esquinas = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
				float[][] v = new float[4][];
				for (int n = 0; n < 4; n++) {
					double th = Math.PI * esquinas[n][0] / lat, fi = 2 * Math.PI * esquinas[n][1] / lon;
					// La dirección en la esfera (sin girar: así el dibujo gira con ella).
					double dx = Math.sin(th) * Math.cos(fi), dy = Math.cos(th), dz = Math.sin(th) * Math.sin(fi);
					double gx = dx * Math.cos(giro) - dz * Math.sin(giro), gz = dx * Math.sin(giro) + dz * Math.cos(giro);
					float x = (float) (gx * radio), y = (float) (dy * radio), z = (float) (gz * radio);
					// Qué tan de costado se ve este punto (el borde brilla más).
					Vec3 aOjo = relativa.add(x, y, z).reverse().normalize();
					double fres = Math.pow(1 - Math.abs(aOjo.x * gx + aOjo.y * dy + aOjo.z * gz), 1.5);
					v[n] = aurora(x, y, z, dx * 1.1, dy * 1.1, dz * 1.1, t, fres);
				}
				for (int n : new int[]{0, 1, 2, 3, 3, 2, 1, 0}) {
					float[] w = v[n];
					if (brillo) vc.addVertex(m, w[0], w[1], w[2]).setColor(w[3], w[4], w[5], w[6]);
					else vc.addVertex(m, w[0], w[1], w[2]).setColor(Math.min(1, 0.15f + w[3] * 1.3f), Math.min(1, 0.35f + w[4] * 1.1f),
							Math.min(1, 0.3f + w[5] * 1.2f), Math.min(0.85f, 0.3f + w[6] * 0.9f));
				}
			}
		}
	}

	/** El color (r, g, b, alfa) de la aurora en un punto de la esfera (px, py, pz: el punto en una esfera de radio 1,1). */
	private static float[] aurora(float x, float y, float z, double px, double py, double pz, float t, double fres) {
		double r = py / 1.1;
		double qx = Math.atan2(pz, px) * 1.4, qy = py * 2.2;
		double ax = qx + 0.5 * Math.sin(qy * 1.7 + t), ay = qy + 0.5 * Math.cos(qx * 1.5 - t * 0.9);
		double bx = ax + 0.3 * Math.sin(ay * 2.9 - t * 1.3), by = ay + 0.3 * Math.cos(ax * 2.6 + t * 1.1);
		double banda = Math.pow(0.5 + 0.5 * Math.sin(bx * 2.0 + by * 1.2 + t * 1.6), 3);
		double banda2 = Math.pow(0.5 + 0.5 * Math.sin(by * 2.4 - bx * 0.8 - t * 1.2), 4);
		double mezcla = 0.5 + 0.5 * Math.sin(t + r * 3.0);
		// verde (0.25, 0.95, 0.62), verde azulado (0.10, 0.75, 0.80), lima (0.55, 1.0, 0.55)
		double cr = (0.25 + (0.10 - 0.25) * mezcla) * banda + 0.55 * banda2 * 0.6 + 0.10 * fres * 0.4;
		double cg = (0.95 + (0.75 - 0.95) * mezcla) * banda + 1.00 * banda2 * 0.6 + 0.75 * fres * 0.4;
		double cb = (0.62 + (0.80 - 0.62) * mezcla) * banda + 0.55 * banda2 * 0.6 + 0.80 * fres * 0.4;
		double alfa = (0.2 + 0.7 * Math.max(banda, banda2)) * (0.45 + 0.55 * fres);
		return new float[]{x, y, z, (float) Math.min(1, cr), (float) Math.min(1, cg), (float) Math.min(1, cb), (float) Math.min(1, alfa)};
	}

	// --- Colores ---

	private static void vertice(VertexConsumer vc, Matrix4f m, float x, float y, float z, int color, float alfa) {
		vc.addVertex(m, x, y, z).setColor(color >> 16 & 255, color >> 8 & 255, color & 255, (int) (clamp01(alfa) * 255));
	}

	/** El mismo color pero lo más brillante posible (el canal más fuerte llega a 255). */
	private static int brillante(int c) {
		int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
		float k = 255f / Math.max(1, Math.max(r, Math.max(g, b)));
		return Math.min(255, (int) (r * k)) << 16 | Math.min(255, (int) (g * k)) << 8 | Math.min(255, (int) (b * k));
	}

	private static int mezclar(int a, int b, float k) {
		int r = (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255), g = (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255),
				bl = (int) Mth.lerp(k, a & 255, b & 255);
		return r << 16 | g << 8 | bl;
	}

	/** Para la esfera: transparente y luminosa (se suma a lo que hay atrás), sin escribir profundidad, de las dos caras. */
	private static final class Aurora extends RenderType {
		static final RenderType TIPO = create("dedsafio4_esfera_aurora", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
				65536, false, false, CompositeState.builder()
						.setShaderState(RENDERTYPE_LIGHTNING_SHADER)
						.setTransparencyState(LIGHTNING_TRANSPARENCY)
						.setWriteMaskState(COLOR_WRITE)
						.setCullState(NO_CULL)
						.createCompositeState(false));

		private Aurora(String nombre, VertexFormat formato, VertexFormat.Mode modo, int tamanio, boolean afectaCrumbling,
					   boolean ordenar, Runnable antes, Runnable despues) {
			super(nombre, formato, modo, tamanio, afectaCrumbling, ordenar, antes, despues);
		}
	}
}
