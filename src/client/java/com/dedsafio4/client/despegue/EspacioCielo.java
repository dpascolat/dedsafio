package com.dedsafio4.client.despegue;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.despegue.ModDespegue;
import com.dedsafio4.despegue.NaveViajeEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * El cielo del Espacio (la dimensión vacía por donde viaja la nave): negro, lleno de estrellas, con los dos planetas.
 * Los planetas son gigantes, como en la vida real (la nave es un puntito al lado): esferas en 3D con su mapa envuelto,
 * lado de día y de noche, atmósfera y girando despacio. El viaje (de 0 a 1, ver NaveCinematica.progresoEspacio):
 * <ul>
 *   <li>0 a 1/3: salís del planeta. Lo tenés abajo, enorme, con el horizonte curvo, y te vas alejando.</li>
 *   <li>1/3: destello y SALTO A LA VELOCIDAD DE LA LUZ: las estrellas se estiran en rayas y el polvo pasa volando.</li>
 *   <li>2/3: otro destello, salís del salto y el planeta de destino está arriba; te acercás hasta que llena el cielo.</li>
 * </ul>
 * Se dibujan dentro del cielo (achicados, con la misma forma y tamaño que se verían de verdad), así nunca quedan cortados
 * por la distancia de renderizado.
 */
public final class EspacioCielo {
	private EspacioCielo() {}

	private static final ResourceLocation TIERRA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/planeta_overworld.png");
	private static final ResourceLocation ROJO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/planeta_rojo.png");
	/** A qué distancia se dibuja el cielo. */
	private static final float CIELO = 90;
	/** El color del brillo de la atmósfera de cada planeta. */
	private static final int[] AIRE_TIERRA = {110, 170, 255}, AIRE_ROJO = {255, 120, 160};
	/** Cuándo empieza y termina el salto a la velocidad de la luz (en el progreso del viaje, de 0 a 1). */
	public static final float SALTO = 0.33f, FIN_SALTO = 0.67f;
	/**
	 * Distancia al centro del planeta, en radios del planeta: 1.035 es estar pegado (el planeta llena medio cielo y se ve
	 * el horizonte curvo); 1.5 ya es lejos (igual ocupa casi todo el cielo).
	 */
	private static final double CERCA = 1.012, LEJOS_SALIDA = 1.45, LEJOS_LLEGADA = 1.7;

	private static final int ESTRELLAS = 1500;
	private static final float[][] ESTRELLA = new float[ESTRELLAS][5];   // dirección x, y, z, tamaño, fase

	static {
		RandomSource azar = RandomSource.create(4567L);
		for (int i = 0; i < ESTRELLAS; ) {
			float x = azar.nextFloat() * 2 - 1, y = azar.nextFloat() * 2 - 1, z = azar.nextFloat() * 2 - 1;
			float largo = Mth.sqrt(x * x + y * y + z * z);
			if (largo < 0.01f || largo > 1) continue;
			ESTRELLA[i] = new float[]{x / largo, y / largo, z / largo, 0.12f + azar.nextFloat() * 0.22f, azar.nextFloat() * 6.28f};
			i++;
		}
	}

	public static void registrar() {
		DimensionRenderingRegistry.registerSkyRenderer(ModDespegue.ESPACIO, EspacioCielo::dibujar);
		DimensionRenderingRegistry.registerCloudRenderer(ModDespegue.ESPACIO, contexto -> {});
		DimensionRenderingRegistry.registerWeatherRenderer(ModDespegue.ESPACIO, contexto -> {});
	}

	/** Cuánto se está yendo a la velocidad de la luz: 0 normal, 1 a full (con un ratito para arrancar y frenar). */
	public static float velocidadLuz(float p) {
		return suave((p - SALTO) / 0.03f) * (1 - suave((p - (FIN_SALTO - 0.03f)) / 0.03f));
	}

	/** El destello blanco al entrar y al salir del salto: de 0 a 1. */
	public static float destello(float p) {
		return Math.max(0, 1 - Math.min(Math.abs(p - SALTO), Math.abs(p - FIN_SALTO)) / 0.025f);
	}

	private static float suave(float t) {
		t = Mth.clamp(t, 0f, 1f);
		return t * t * (3 - 2 * t);
	}

	private static void dibujar(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		Matrix4f m = contexto.positionMatrix();
		float parcial = contexto.tickCounter().getGameTimeDeltaPartialTick(false);
		float tiempo = (mc.level == null ? 0 : mc.level.getGameTime() % 100000L) + parcial;
		NaveViajeEntity nave = mc.player != null && mc.player.getVehicle() instanceof NaveViajeEntity n ? n : null;
		boolean haciaRojo = nave == null || nave.haciaRojo();
		float p = NaveCinematica.progresoEspacio(parcial);
		float luz = velocidadLuz(p);

		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1, 1, 1, 1);

		// Las estrellas: cuadraditos que titilan. A la velocidad de la luz se estiran en rayas para atrás (para abajo).
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder estrellas = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (float[] e : ESTRELLA) {
			int brillo = (int) Math.min(255, 170 + 85 * Mth.sin(tiempo * 0.08f + e[4]) + 80 * luz);
			estrella(estrellas, m, e, luz, brillo);
		}
		BufferUploader.drawWithShader(estrellas.buildOrThrow());

		// Los planetas: el de salida abajo (alejándose) antes del salto y el de destino arriba (acercándose) después.
		if (p < SALTO) {
			double distancia = Mth.lerp(suave(p / SALTO), CERCA, LEJOS_SALIDA);
			planeta(m, new Vec3(0.12, -1, 0.08).normalize(), distancia, haciaRojo ? TIERRA : ROJO, tiempo,
					haciaRojo ? AIRE_TIERRA : AIRE_ROJO);
		} else if (p > FIN_SALTO) {
			double distancia = Mth.lerp(suave((p - FIN_SALTO) / (1 - FIN_SALTO)), LEJOS_LLEGADA, CERCA);
			planeta(m, new Vec3(-0.1, 1, 0.12).normalize(), distancia, haciaRojo ? ROJO : TIERRA, tiempo,
					haciaRojo ? AIRE_ROJO : AIRE_TIERRA);
		}

		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
	}

	/** De dónde viene la luz del sol (la mitad de cada planeta queda de día y la otra de noche). */
	private static final Vec3 SOL = new Vec3(1, 0.35, -0.6).normalize();
	/** Cuántas partes tiene la esfera de cada planeta, alrededor y de polo a polo. */
	private static final int LONGITUDES = 192, LATITUDES = 96;

	/**
	 * Un planeta: una esfera con su mapa envuelto, iluminada de un lado y girando despacio sobre sí misma. Se dibuja
	 * achicada dentro del cielo: en esa dirección y a {@code distancia} radios de su centro.
	 */
	private static void planeta(Matrix4f m, Vec3 dir, double distancia, ResourceLocation textura, float tiempo, int[] aire) {
		double radio = CIELO / distancia;
		Vec3 centro = dir.scale(CIELO);
		float giro = tiempo * 0.0015f;

		// Cada punto de la esfera: su dirección desde el centro (girada e inclinada) y su lugar en el mapa.
		Vec3[][] normal = new Vec3[LONGITUDES + 1][LATITUDES + 1];
		for (int i = 0; i <= LONGITUDES; i++) {
			double lon = 2 * Math.PI * i / LONGITUDES + giro;
			for (int j = 0; j <= LATITUDES; j++) {
				double lat = Math.PI * (0.5 - (double) j / LATITUDES);
				normal[i][j] = new Vec3(Math.cos(lat) * Math.cos(lon), Math.sin(lat), Math.cos(lat) * Math.sin(lon)).zRot(0.4f);
			}
		}

		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, textura);
		// Suavizado: de tan cerca, si no, se verían los pixeles del mapa.
		Minecraft.getInstance().getTextureManager().getTexture(textura).setFilter(true, false);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		int caras = 0;
		for (int i = 0; i < LONGITUDES; i++) {
			for (int j = 0; j < LATITUDES; j++) {
				if (!seVe(centro, radio, normal, i, j)) continue;
				int[][] esquinas = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
				for (int[] q : esquinas) {
					Vec3 n = normal[q[0]][q[1]];
					Vec3 punto = centro.add(n.scale(radio));
					int c = (int) (255 * luz(n));
					b.addVertex(m, (float) punto.x, (float) punto.y, (float) punto.z)
							.setUv((float) q[0] / LONGITUDES, (float) q[1] / LATITUDES).setColor(c, c, c, 255);
				}
				caras++;
			}
		}
		if (caras > 0) BufferUploader.drawWithShader(b.buildOrThrow());
		else b.build();

		// La atmósfera: una capa finita alrededor que brilla de color en el borde (el horizonte), del lado de día.
		double radioAire = Math.min(radio * 1.02, CIELO - 0.25);   // la cámara siempre queda afuera
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
				com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
		BufferBuilder a = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		caras = 0;
		for (int i = 0; i < LONGITUDES; i++) {
			for (int j = 0; j < LATITUDES; j++) {
				if (!seVe(centro, radioAire, normal, i, j)) continue;
				int[][] esquinas = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
				for (int[] q : esquinas) {
					Vec3 n = normal[q[0]][q[1]];
					Vec3 punto = centro.add(n.scale(radioAire));
					double deFrente = Math.max(0, n.dot(punto.scale(-1).normalize()));
					double borde = Math.pow(1 - deFrente, 3);
					int alfa = (int) (210 * borde * (0.2 + 0.8 * luz(n)));
					a.addVertex(m, (float) punto.x, (float) punto.y, (float) punto.z).setColor(aire[0], aire[1], aire[2], alfa);
				}
				caras++;
			}
		}
		if (caras > 0) BufferUploader.drawWithShader(a.buildOrThrow());
		else a.build();
		RenderSystem.defaultBlendFunc();
	}

	/** Si esa parte de la esfera mira hacia la cámara (la de atrás no se dibuja). */
	private static boolean seVe(Vec3 centro, double radio, Vec3[][] normal, int i, int j) {
		Vec3 n = normal[i][j].add(normal[i + 1][j]).add(normal[i][j + 1]).add(normal[i + 1][j + 1]).normalize();
		Vec3 punto = centro.add(n.scale(radio));
		return n.dot(punto) < 0;   // la cámara está en el (0, 0, 0)
	}

	/** Cuánta luz le llega a ese punto: de día bien claro, de noche oscuro (pero no negro del todo). */
	private static float luz(Vec3 n) {
		double sol = n.dot(SOL);
		return (float) (0.13 + 0.87 * Mth.clamp(sol * 1.6 + 0.15, 0, 1));
	}

	/**
	 * Una estrella: un cuadradito mirando a la cámara. Con {@code luz} > 0 (velocidad de la luz) se estira en una raya
	 * hacia atrás, más larga cuanto más rápido.
	 */
	private static void estrella(BufferBuilder b, Matrix4f m, float[] e, float luz, int brillo) {
		Vec3 d = new Vec3(e[0], e[1], e[2]);
		Vec3 desde = d.scale(100);
		Vec3 hasta = d.subtract(0, luz * 1.4, 0).normalize().scale(100);
		Vec3 largo = hasta.subtract(desde);
		int azul = Math.min(255, brillo + 15 + (int) (40 * luz));
		if (largo.lengthSqr() < 0.25) {
			Vec3 arriba = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
			Vec3 a = d.cross(arriba).normalize().scale(e[3]), c = d.cross(a).normalize().scale(e[3]);
			vertice(b, m, desde.subtract(a).subtract(c), brillo, azul);
			vertice(b, m, desde.add(a).subtract(c), brillo, azul);
			vertice(b, m, desde.add(a).add(c), brillo, azul);
			vertice(b, m, desde.subtract(a).add(c), brillo, azul);
		} else {
			Vec3 costado = largo.cross(desde).normalize().scale(e[3] * 0.7);
			vertice(b, m, desde.add(costado), brillo, azul);
			vertice(b, m, desde.subtract(costado), brillo, azul);
			vertice(b, m, hasta.subtract(costado), brillo, azul);
			vertice(b, m, hasta.add(costado), brillo, azul);
		}
	}

	private static void vertice(BufferBuilder b, Matrix4f m, Vec3 v, int brillo, int azul) {
		b.addVertex(m, (float) v.x, (float) v.y, (float) v.z).setColor(brillo, brillo, azul, 255);
	}
}
