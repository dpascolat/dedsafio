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
 * La nave queda quieta y lo que se mueve son los planetas: el de salida está abajo y se va alejando, el de destino
 * está arriba, chiquito, y se acerca hasta ocupar casi todo el cielo. Son esferas en 3D (con su mapa envuelto, lado de
 * día y de noche, atmósfera y girando) dibujadas en el cielo, así nunca quedan cortadas por la distancia de renderizado.
 */
public final class EspacioCielo {
	private EspacioCielo() {}

	private static final ResourceLocation TIERRA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/planeta_overworld.png");
	private static final ResourceLocation ROJO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/planeta_rojo.png");
	/** Radio de los planetas (en bloques) y a qué distancia se dibuja el cielo. */
	private static final float RADIO_PLANETA = 50, CIELO = 90;
	/** El color del brillo de la atmósfera de cada planeta. */
	private static final int[] AIRE_TIERRA = {110, 170, 255}, AIRE_ROJO = {255, 120, 160};

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

	private static void dibujar(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		Matrix4f m = contexto.positionMatrix();
		Vec3 camara = contexto.camera().getPosition();
		float parcial = contexto.tickCounter().getGameTimeDeltaPartialTick(false);
		float tiempo = (mc.level == null ? 0 : mc.level.getGameTime() % 100000L) + parcial;

		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		// Las estrellas: cuadraditos blancos que titilan.
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder estrellas = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (float[] e : ESTRELLA) {
			int brillo = (int) (170 + 85 * Mth.sin(tiempo * 0.08f + e[4]));
			cuadro(estrellas, m, e[0] * 100, e[1] * 100, e[2] * 100, e[3], brillo);
		}
		BufferUploader.drawWithShader(estrellas.buildOrThrow());

		// Los planetas, según por dónde va el viaje.
		NaveViajeEntity nave = mc.player != null && mc.player.getVehicle() instanceof NaveViajeEntity n ? n : null;
		boolean haciaRojo = nave == null || nave.haciaRojo();
		float p = NaveCinematica.progresoEspacio(parcial);
		float e = p * p * (3 - 2 * p);
		Vec3 base = nave != null ? nave.getPosition(parcial) : camara;
		// Al salir el planeta está justo abajo, enorme: se ve su borde curvo.
		Vec3 salida = base.add(-15, -(56 + 900 * e), 8);
		Vec3 destino = base.add(25 * (1 - e), 950 - 870 * e, 15 * (1 - e));
		ResourceLocation texSalida = haciaRojo ? TIERRA : ROJO, texDestino = haciaRojo ? ROJO : TIERRA;
		int[] aireSalida = haciaRojo ? AIRE_TIERRA : AIRE_ROJO, aireDestino = haciaRojo ? AIRE_ROJO : AIRE_TIERRA;
		RenderSystem.setShaderColor(1, 1, 1, 1);
		// Primero el más lejano.
		if (salida.distanceTo(camara) > destino.distanceTo(camara)) {
			planeta(m, camara, salida, texSalida, tiempo, aireSalida);
			planeta(m, camara, destino, texDestino, tiempo, aireDestino);
		} else {
			planeta(m, camara, destino, texDestino, tiempo, aireDestino);
			planeta(m, camara, salida, texSalida, tiempo, aireSalida);
		}

		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
	}

	/** De dónde viene la luz del sol (la mitad de cada planeta queda de día y la otra de noche). */
	private static final Vec3 SOL = new Vec3(1, 0.35, -0.6).normalize();
	/** Cuántas partes tiene la esfera de cada planeta, alrededor y de polo a polo. */
	private static final int LONGITUDES = 48, LATITUDES = 24;

	/**
	 * Un planeta de verdad: una esfera con su mapa envuelto, iluminada de un lado y girando despacio sobre sí misma.
	 * Se dibuja achicada dentro del cielo (misma forma y tamaño aparente que a su distancia real), así se ve redondo y
	 * curvo cuando lo tenés cerca y nunca queda cortado.
	 */
	private static void planeta(Matrix4f m, Vec3 camara, Vec3 centroReal, ResourceLocation textura, float tiempo, int[] aire) {
		Vec3 hacia = centroReal.subtract(camara);
		double distancia = hacia.length();
		if (distancia < 1) return;
		Vec3 dir = hacia.scale(1 / distancia);
		double radio = CIELO * RADIO_PLANETA / Math.max(distancia, RADIO_PLANETA * 1.03);
		Vec3 centro = dir.scale(CIELO);
		float giro = tiempo * 0.006f;

		// Cada punto de la esfera: su dirección desde el centro (girada e inclinada) y su lugar en el mapa.
		Vec3[][] normal = new Vec3[LONGITUDES + 1][LATITUDES + 1];
		for (int i = 0; i <= LONGITUDES; i++) {
			double lon = 2 * Math.PI * i / LONGITUDES + giro;
			for (int j = 0; j <= LATITUDES; j++) {
				double lat = Math.PI * (0.5 - (double) j / LATITUDES);
				Vec3 n = new Vec3(Math.cos(lat) * Math.cos(lon), Math.sin(lat), Math.cos(lat) * Math.sin(lon));
				normal[i][j] = n.zRot(0.4f);
			}
		}

		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, textura);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		int caras = 0;
		for (int i = 0; i < LONGITUDES; i++) {
			for (int j = 0; j < LATITUDES; j++) {
				if (!seVe(centro, radio, normal, i, j)) continue;
				int[][] esquinas = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
				for (int[] q : esquinas) {
					Vec3 n = normal[q[0]][q[1]];
					Vec3 punto = centro.add(n.scale(radio));
					float luz = luz(n);
					int c = (int) (255 * luz);
					b.addVertex(m, (float) punto.x, (float) punto.y, (float) punto.z)
							.setUv((float) q[0] / LONGITUDES, (float) q[1] / LATITUDES).setColor(c, c, c, 255);
				}
				caras++;
			}
		}
		if (caras > 0) BufferUploader.drawWithShader(b.buildOrThrow());
		else b.build();

		// La atmósfera: un brillo de color en el borde del planeta, del lado de día.
		double radioAire = radio * 1.05;
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
					int alfa = (int) (200 * borde * (0.25 + 0.75 * luz(n)));
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

	/** Un cuadrito blanco de lado 2×{@code tam} en ese punto, mirando a la cámara (una estrella). */
	private static void cuadro(BufferBuilder b, Matrix4f m, float x, float y, float z, float tam, int brillo) {
		Vec3 d = new Vec3(x, y, z).normalize();
		Vec3 arriba = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 a = d.cross(arriba).normalize().scale(tam), c = d.cross(a).normalize().scale(tam);
		float[][] esquinas = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
		for (float[] q : esquinas) {
			float vx = (float) (x + a.x * q[0] + c.x * q[1]), vy = (float) (y + a.y * q[0] + c.y * q[1]), vz = (float) (z + a.z * q[0] + c.z * q[1]);
			b.addVertex(m, vx, vy, vz).setColor(brillo, brillo, Math.min(255, brillo + 15), 255);
		}
	}
}
