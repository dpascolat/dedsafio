package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Cielo rojo (/cielo rojo): una esfera alrededor de la cámara.
 * - Costados: fondo casi negro con auroras rojas, dando la vuelta completa.
 * - Arriba: más auroras, proyectadas desde el cenit.
 * - Arriba del todo: la grieta con Reviil espiando (ver ReviilCielo).
 *
 * Animación al activarlo con el comando (en ticks desde que llegó el aviso):
 *   0..100   el rojo se esparce desde el cenit hasta cubrir todo el cielo
 *   100..160 se abre la grieta
 *   160..240 Reviil sube hasta asomarse y abre los ojos
 * Al desactivarlo con el comando hay un destello blanco (ver Destello) y el cielo vuelve a la normalidad.
 */
public final class CieloCliente {
	private CieloCliente() {}

	private static final ResourceLocation COSTADO =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/environment/cielo_rojo_costado.png");
	private static final ResourceLocation ARRIBA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/environment/cielo_rojo_arriba.png");

	/** Color de la niebla y del fondo, para que el horizonte no quede celeste. */
	public static final float NIEBLA_R = 0.10f, NIEBLA_G = 0.01f, NIEBLA_B = 0.01f;

	private static final float RADIO = 100f;
	private static final int PASOS_LONGITUD = 64;
	/** Veces que se repite la textura de los costados alrededor del horizonte. */
	private static final float REPETICIONES = 3f;
	/** Alto / ancho de la textura de los costados (512x256). */
	private static final float PROPORCION = 0.5f;
	/** La cúpula de arriba va desde esta latitud hasta el cenit, y se funde entre INICIO y FIN. */
	private static final float CUPULA_INICIO = 50f, CUPULA_FIN = 64f;

	// Tiempos de la animación, en ticks (20 = 1 segundo).
	private static final float DURACION_CIELO = 100f, INICIO_GRIETA = 100f, DURACION_GRIETA = 60f,
			INICIO_REVIIL = 160f, DURACION_REVIIL = 80f;
	/** Ancho (en grados) del borde difuso del rojo mientras avanza. */
	private static final float BORDE_AVANCE = 25f;

	private static boolean activo;
	/** Tick del mundo en que empezó la animación; Long.MIN_VALUE = sin animación (todo ya visible). */
	private static long inicio = Long.MIN_VALUE;

	/** El cielo rojo lo puso /momentito 1 (sin Reviil: en su lugar está la nave del hacker). */
	private static boolean porMomentito;

	/** Lo llama la escena del hacker al empezar y al terminar (si el cielo ya estaba rojo, no lo toca). */
	public static void momentito(boolean empieza) {
		Minecraft mc = Minecraft.getInstance();
		if (empieza && !activo) {
			activo = true;
			porMomentito = true;
			inicio = mc.level != null ? mc.level.getGameTime() : Long.MIN_VALUE;
		} else if (!empieza && porMomentito) {
			activo = false;
			porMomentito = false;
		}
	}

	public static void setActivo(boolean valor, boolean animar) {
		porMomentito = false;
		Minecraft mc = Minecraft.getInstance();
		if (valor && !activo) inicio = animar && mc.level != null ? mc.level.getGameTime() : Long.MIN_VALUE;
		if (!valor && activo && animar) Destello.iniciar();
		activo = valor;
	}

	/** Solo en el Overworld: el Nether y el End tienen su propio cielo. */
	public static boolean activoAqui() {
		Minecraft mc = Minecraft.getInstance();
		return activo && mc.level != null && mc.level.dimension() == Level.OVERWORLD;
	}

	private static float transcurrido(float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (inicio == Long.MIN_VALUE || mc.level == null) return Float.MAX_VALUE;
		return mc.level.getGameTime() - inicio + partialTick;
	}

	private static float progreso(float partialTick, float desde, float duracion) {
		return Mth.clamp((transcurrido(partialTick) - desde) / duracion, 0f, 1f);
	}

	/** 0 → 1 mientras el rojo cubre el cielo. Con 1, el cielo de Minecraft ya no se dibuja. */
	public static float progresoCielo(float partialTick) {
		// Con /momentito 1 el rojo cubre el cielo en 1 segundo (la escena dura poco).
		return progreso(partialTick, 0f, porMomentito ? 20f : DURACION_CIELO);
	}

	/** Para la niebla y las nubes, que no reciben partialTick. */
	public static float progresoCielo() {
		return progresoCielo(0f);
	}

	public static void dibujar(Matrix4f matrizVista, float partialTick) {
		PoseStack pose = new PoseStack();
		pose.mulPose(matrizVista);
		Matrix4f m = pose.last().pose();
		float avance = progresoCielo(partialTick);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();   // la esfera se ve desde adentro
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		dibujarCostados(m, avance);
		dibujarCupula(m, avance);

		// Con /momentito 1 la grieta se abre enseguida (de 0,5 a 2,5 s) y no sale Reviil: sale la nave del hacker.
		float grieta = porMomentito ? progreso(partialTick, 10f, 40f) : progreso(partialTick, INICIO_GRIETA, DURACION_GRIETA);
		if (grieta > 0f) {
			Minecraft mc = Minecraft.getInstance();
			float tiempo = (mc.level != null ? mc.level.getGameTime() % 100000L : 0) + partialTick;
			ReviilCielo.dibujar(matrizVista, tiempo, grieta,
					porMomentito ? 0f : progreso(partialTick, INICIO_REVIIL, DURACION_REVIIL));
		}

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	/** Visibilidad (0..1) del rojo según la latitud mientras se esparce desde el cenit (90°) hasta abajo (-90°). */
	private static float alfaAvance(float lat, float avance) {
		if (avance >= 1f) return 1f;
		float limite = 90f - avance * (180f + BORDE_AVANCE);
		return Mth.clamp((lat - limite) / BORDE_AVANCE, 0f, 1f);
	}

	/** Banda de la esfera desde abajo del todo hasta un poco más arriba de donde arranca la cúpula. */
	private static void dibujarCostados(Matrix4f m, float avance) {
		RenderSystem.setShaderTexture(0, COSTADO);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		// Alto (en radianes) de cada repetición de la textura, para que no se deforme.
		float altoTextura = Mth.TWO_PI / REPETICIONES * PROPORCION;
		for (float lat = -90f; lat < CUPULA_FIN + 4f; lat += 4f) {
			float lat2 = Math.min(lat + 4f, CUPULA_FIN + 4f);
			for (int j = 0; j < PASOS_LONGITUD; j++) {
				float lon1 = Mth.TWO_PI * j / PASOS_LONGITUD, lon2 = Mth.TWO_PI * (j + 1) / PASOS_LONGITUD;
				float u1 = REPETICIONES * j / PASOS_LONGITUD, u2 = REPETICIONES * (j + 1) / PASOS_LONGITUD;
				float v1 = -lat * Mth.DEG_TO_RAD / altoTextura, v2 = -lat2 * Mth.DEG_TO_RAD / altoTextura;
				verticeCostado(b, m, lat, lon1, u1, v1, avance);
				verticeCostado(b, m, lat2, lon1, u1, v2, avance);
				verticeCostado(b, m, lat2, lon2, u2, v2, avance);
				verticeCostado(b, m, lat, lon2, u2, v1, avance);
			}
		}
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	private static void verticeCostado(BufferBuilder b, Matrix4f m, float lat, float lon, float u, float v, float avance) {
		// Debajo del horizonte se oscurece un poco, como el cielo de Minecraft.
		float brillo = lat >= 0 ? 1f : 1f - 0.45f * Math.min(1f, -lat / 35f);
		float alfa = alfaAvance(lat, avance);
		// El frente que avanza brilla en rojo.
		float frente = avance < 1f ? (1f - Math.abs(2f * alfa - 1f)) : 0f;
		punto(b, m, lat, lon).setUv(u, v).setColor(Math.min(1f, brillo + frente * 1.5f), brillo, brillo, alfa);
	}

	/** Casquete de arriba: la textura se proyecta mirando desde el cenit. */
	private static void dibujarCupula(Matrix4f m, float avance) {
		RenderSystem.setShaderTexture(0, ARRIBA);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		float radioBorde = Mth.cos(CUPULA_INICIO * Mth.DEG_TO_RAD);
		for (float lat = CUPULA_INICIO; lat < 90f; lat += 2f) {
			float lat2 = Math.min(lat + 2f, 90f);
			for (int j = 0; j < PASOS_LONGITUD; j++) {
				float lon1 = Mth.TWO_PI * j / PASOS_LONGITUD, lon2 = Mth.TWO_PI * (j + 1) / PASOS_LONGITUD;
				verticeCupula(b, m, lat, lon1, radioBorde, avance);
				verticeCupula(b, m, lat2, lon1, radioBorde, avance);
				verticeCupula(b, m, lat2, lon2, radioBorde, avance);
				verticeCupula(b, m, lat, lon2, radioBorde, avance);
			}
		}
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	private static void verticeCupula(BufferBuilder b, Matrix4f m, float lat, float lon, float radioBorde, float avance) {
		float horizontal = Mth.cos(lat * Mth.DEG_TO_RAD);
		float u = 0.5f + horizontal * Mth.cos(lon) / (2f * radioBorde);
		float v = 0.5f + horizontal * Mth.sin(lon) / (2f * radioBorde);
		float alfa = Mth.clamp((lat - CUPULA_INICIO) / (CUPULA_FIN - CUPULA_INICIO), 0f, 1f) * alfaAvance(lat, avance);
		punto(b, m, lat, lon).setUv(u, v).setColor(1f, 1f, 1f, alfa);
	}

	private static VertexConsumer punto(BufferBuilder b, Matrix4f m, float lat, float lon) {
		float la = lat * Mth.DEG_TO_RAD;
		float x = RADIO * Mth.cos(la) * Mth.cos(lon);
		float y = RADIO * Mth.sin(la);
		float z = RADIO * Mth.cos(la) * Mth.sin(lon);
		return b.addVertex(m, x, y, z);
	}
}
