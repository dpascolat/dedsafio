package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * El Eclipse (Momento Reviil) visto desde el cliente, en el Overworld:
 * - el cielo es negro, con el sol eclipsado (disco negro con la corona brillando); sin la grieta de Reviil;
 * - ninguna luz ilumina: ni el sol ni las antorchas; solo la Linterna (ver LinternaLuz).
 *   (Se cambia la tabla de luces con la que Minecraft pinta todo.)
 *
 * Animación al empezar con el comando (en ticks desde que llegó el aviso):
 *   0..100   la luna tapa el sol y todo se va oscureciendo
 */
public final class EclipseCliente {
	private EclipseCliente() {}

	private static final ResourceLocation ECLIPSE = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/environment/eclipse.png");
	private static final ResourceLocation LUNA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/environment/eclipse_luna.png");
	private static final ResourceLocation SOL = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
	/** Color de la niebla y del fondo del cielo: casi negro. */
	public static final float NIEBLA_R = 0.008f, NIEBLA_G = 0.008f, NIEBLA_B = 0.014f;

	private static final float DURACION_OSCURO = 100f;
	/** El sol eclipsado: a 45° del cenit hacia el sur. */
	private static final float INCLINACION = 45f;
	/** Tamaño (medio lado) del cuadro del eclipse y radio de la luna, a 100 bloques. */
	private static final float LADO_ECLIPSE = 34f, RADIO_LUNA = 34f * 2 * 70 / 256f;

	private static boolean activo;
	/** Tick del mundo en que empezó la animación; Long.MIN_VALUE = sin animación (todo ya visible). */
	private static long inicio = Long.MIN_VALUE;

	public static void setActivo(boolean valor, boolean animar) {
		Minecraft mc = Minecraft.getInstance();
		if (valor && !activo) inicio = animar && mc.level != null ? mc.level.getGameTime() : Long.MIN_VALUE;
		activo = valor;
	}

	public static boolean activoAqui() {
		Minecraft mc = Minecraft.getInstance();
		return activo && mc.level != null && mc.level.dimension() == Level.OVERWORLD;
	}

	private static float transcurrido(float parcial) {
		Minecraft mc = Minecraft.getInstance();
		if (inicio == Long.MIN_VALUE || mc.level == null) return Float.MAX_VALUE;
		return mc.level.getGameTime() - inicio + parcial;
	}

	private static float progreso(float parcial, float desde, float duracion) {
		return Mth.clamp((transcurrido(parcial) - desde) / duracion, 0f, 1f);
	}

	/** 0 → 1 mientras la luna tapa el sol y todo se oscurece. */
	public static float oscuro(float parcial) {
		float p = progreso(parcial, 0f, DURACION_OSCURO);
		return p * p * (3 - 2 * p);
	}

	/**
	 * La tabla de luces (x = luz de bloques, y = luz del cielo): ninguna luz ilumina, ni el cielo ni los
	 * bloques; todo queda casi negro (de a poco mientras empieza el eclipse).
	 */
	public static void oscurecer(NativeImage luces) {
		float p = oscuro(0f);
		for (int bloque = 0; bloque < 16; bloque++) {
			for (int cielo = 0; cielo < 16; cielo++) {
				int abgr = luces.getPixelRGBA(bloque, cielo);
				int r = mezclar(abgr & 0xFF, 5, p), g = mezclar((abgr >> 8) & 0xFF, 5, p), b = mezclar((abgr >> 16) & 0xFF, 4, p);
				luces.setPixelRGBA(bloque, cielo, 0xFF000000 | (b << 16) | (g << 8) | r);
			}
		}
	}

	private static int mezclar(int desde, int hasta, float p) {
		return Math.round(Mth.lerp(p, desde, hasta));
	}

	/** El cielo del eclipse: fondo negro (lo pone la niebla) y el sol que se va tapando. */
	public static void dibujarCielo(Matrix4f matrizVista, float parcial) {
		PoseStack pose = new PoseStack();
		pose.mulPose(matrizVista);
		pose.mulPose(Axis.XP.rotationDegrees(INCLINACION));
		Matrix4f m = pose.last().pose();
		float p = oscuro(parcial);
		// El fondo: el celeste del día que se va apagando hasta quedar negro.
		Minecraft mc0 = Minecraft.getInstance();
		if (p < 1f && mc0.level != null) {
			net.minecraft.world.phys.Vec3 cielo = mc0.level.getSkyColor(mc0.gameRenderer.getMainCamera().getPosition(), parcial);
			RenderSystem.clearColor(Mth.lerp(p, (float) cielo.x, NIEBLA_R), Mth.lerp(p, (float) cielo.y, NIEBLA_G),
					Mth.lerp(p, (float) cielo.z, NIEBLA_B), 1f);
			RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
		}
		RenderSystem.enableBlend();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);

		// La corona aparece al final, cuando la luna ya casi tapa el sol.
		float corona = Mth.clamp((p - 0.75f) / 0.25f, 0f, 1f);
		if (corona > 0f) {
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShaderColor(1f, 1f, 1f, corona);
			cuadro(m, ECLIPSE, LADO_ECLIPSE, 0);
		}
		// El sol se apaga mientras lo tapa la luna.
		if (corona < 1f) {
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f - corona);
			cuadro(m, SOL, RADIO_LUNA * 3.6f, 0);   // el sol (cuadrado, como en Minecraft) del tamaño de la luna
			// La luna entra desde el costado hasta quedar justo encima.
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			cuadro(m, LUNA, RADIO_LUNA, (1f - p) * RADIO_LUNA * 2.6f);
		}
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.defaultBlendFunc();

		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	/** Un cuadro a 100 bloques, con ese medio lado, corrido "dx" hacia un costado. */
	private static void cuadro(Matrix4f m, ResourceLocation textura, float t, float dx) {
		RenderSystem.setShaderTexture(0, textura);
		float y = 100f;
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		b.addVertex(m, dx - t, y, -t).setUv(0, 0);
		b.addVertex(m, dx + t, y, -t).setUv(1, 0);
		b.addVertex(m, dx + t, y, t).setUv(1, 1);
		b.addVertex(m, dx - t, y, t).setUv(0, 1);
		BufferUploader.drawWithShader(b.buildOrThrow());
	}
}
