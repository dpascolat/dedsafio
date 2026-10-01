package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * La grieta grande en lo más alto del cielo, con Reviil espiando desde atrás.
 * Se dibuja junto con el cielo rojo, en el espacio del cielo: la cámara está en el origen.
 *
 * Orden de dibujo:
 * 1. Fondo de la grieta (galaxia).
 * 2. Máscara invisible: todo el plano de la grieta menos la abertura, escrita solo en profundidad.
 * 3. Reviil, detrás del plano: la máscara tapa lo que queda fuera de la abertura.
 * 4. Borde brillante de la grieta, encima de todo.
 * Reviil usa el modelo y la textura originales del mod "REvill" (ver ReviilModelo).
 */
final class ReviilCielo {
	private ReviilCielo() {}

	private static final ResourceLocation FONDO =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/environment/grieta_fondo.png");
	private static final ResourceLocation BORDE =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/environment/grieta_borde.png");
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/environment/reviil.png");

	/** La grieta está justo arriba (cenit), a esta distancia de la cámara. */
	private static final float DISTANCIA = 60f;
	private static final float ANCHO_GRIETA = 210f, ALTO_GRIETA = 53f;
	/** Media altura de la abertura respecto de la media altura del quad (igual que en la textura). */
	private static final float ABERTURA = 0.36f;
	/** Hasta dónde llega la máscara alrededor de la grieta (tiene que tapar todo Reviil). */
	private static final float MASCARA = 130f;

	/** Tamaño de Reviil: con 11,2, la cabeza (38 píxeles de modelo) mide unos 27 bloques. */
	private static final float ESCALA = 11.2f;
	/**
	 * Qué tan detrás del plano de la grieta está el centro de Reviil. Tiene que alcanzar para que
	 * ninguna parte (las manos sobresalen) quede delante del plano, o se vería fuera de la abertura.
	 */
	private static final float PROFUNDIDAD = 33.6f;
	/** Cuánto más abajo (en bloques) arranca Reviil antes de subir hasta asomarse. */
	private static final float SUBIDA = 45f;
	/** Punto del modelo (en píxeles) que queda en el medio de la grieta: los ojos. */
	private static final float CABEZA_X = -1f, CABEZA_Y = -22f, CABEZA_Z = 4.5f;
	/** Rotación Z original de la parte "Cuerno" (las alas); se anima alrededor de este valor. */
	private static final float CUERNO_ZROT = -0.1307f;
	/** La textura original es grisácea; se tiñe de un beige rojizo. */
	private static final int TONO_BEIGE = 0xFFFFC9AE;

	private static ModelPart modelo, cuerno, ojoIzquierdo, ojoDerecho;

	private static ModelPart modelo() {
		if (modelo == null) {
			modelo = ReviilModelo.crear().bakeRoot();
			cuerno = modelo.getChild("Cuerno");
			ModelPart cuerpo = modelo.getChild("bone");
			ojoIzquierdo = cuerpo.getChild("cube_r35");
			ojoDerecho = cuerpo.getChild("cube_r36");
		}
		return modelo;
	}

	/**
	 * @param apertura 0 → 1 mientras se abre la grieta: primero se estira de lado a lado como un tajo
	 *                 finito y después se abre hacia arriba y abajo.
	 * @param aparicion 0 → 1 mientras Reviil sube desde abajo hasta asomarse; abre los ojos al final.
	 */
	static void dibujar(Matrix4f vista, float tiempo, float apertura, float aparicion) {
		// Fija en el cielo. Local: +Z mira hacia abajo (a la cámara) y +Y apunta al sur, así mirando
		// hacia el norte y levantando la vista Reviil se ve derecho.
		Matrix4f base = new Matrix4f(vista).translate(0, DISTANCIA, 0)
				.rotateTowards(new Vector3f(0, -1, 0), new Vector3f(0, 0, 1));
		float ancho = suave(Mth.clamp(apertura / 0.45f, 0f, 1f));
		float alto = Math.max(0.05f, suave(Mth.clamp((apertura - 0.35f) / 0.65f, 0f, 1f)));
		Matrix4f grieta = new Matrix4f(base).scale(ancho, alto, 1f);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		quadGrieta(grieta, FONDO, -PROFUNDIDAD * 2.5f);

		if (aparicion > 0f) {
			mascara(grieta);
			dibujarReviil(base, tiempo, aparicion);
		}

		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.disableCull();
		quadGrieta(grieta, BORDE, 0f);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();

		// Se borra la profundidad para que nada de esto tape el terreno (el cielo se dibuja primero).
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		RenderSystem.depthMask(false);
	}

	private static void quadGrieta(Matrix4f base, ResourceLocation textura, float z) {
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, textura);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		// El fondo va más atrás: se agranda para que ocupe lo mismo visto desde la cámara.
		float s = (DISTANCIA - z) / DISTANCIA;
		float w = ANCHO_GRIETA / 2 * s, h = ALTO_GRIETA / 2 * s;
		b.addVertex(base, -w, -h, z).setUv(0, 1).setColor(1f, 1f, 1f, 1f);
		b.addVertex(base, w, -h, z).setUv(1, 1).setColor(1f, 1f, 1f, 1f);
		b.addVertex(base, w, h, z).setUv(1, 0).setColor(1f, 1f, 1f, 1f);
		b.addVertex(base, -w, h, z).setUv(0, 0).setColor(1f, 1f, 1f, 1f);
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	/** Media altura de la abertura (en bloques) en la posición x del plano. */
	private static float abertura(float x) {
		float u = x / (ANCHO_GRIETA / 2);
		return ALTO_GRIETA / 2 * ABERTURA * (float) Math.pow(Math.max(0, 1 - u * u), 0.75);
	}

	/** Plano de la grieta con un agujero con forma de ojo, escrito solo en la profundidad (invisible). */
	private static void mascara(Matrix4f base) {
		RenderSystem.colorMask(false, false, false, false);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.setShader(GameRenderer::getPositionShader);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		float mitad = ANCHO_GRIETA / 2;
		int pasos = 64;
		for (int i = 0; i < pasos; i++) {
			float x1 = -mitad + ANCHO_GRIETA * i / pasos, x2 = -mitad + ANCHO_GRIETA * (i + 1) / pasos;
			float a1 = abertura(x1), a2 = abertura(x2);
			// arriba de la abertura
			b.addVertex(base, x1, a1, 0f); b.addVertex(base, x2, a2, 0f);
			b.addVertex(base, x2, MASCARA, 0f); b.addVertex(base, x1, MASCARA, 0f);
			// abajo de la abertura
			b.addVertex(base, x1, -MASCARA, 0f); b.addVertex(base, x2, -MASCARA, 0f);
			b.addVertex(base, x2, -a2, 0f); b.addVertex(base, x1, -a1, 0f);
		}
		// a los costados de la grieta
		b.addVertex(base, -MASCARA, -MASCARA, 0f); b.addVertex(base, -mitad, -MASCARA, 0f);
		b.addVertex(base, -mitad, MASCARA, 0f); b.addVertex(base, -MASCARA, MASCARA, 0f);
		b.addVertex(base, mitad, -MASCARA, 0f); b.addVertex(base, MASCARA, -MASCARA, 0f);
		b.addVertex(base, MASCARA, MASCARA, 0f); b.addVertex(base, mitad, MASCARA, 0f);
		BufferUploader.drawWithShader(b.buildOrThrow());
		RenderSystem.colorMask(true, true, true, true);
	}

	/**
	 * Reviil con animaciones: flota, gira y cabecea la cabeza, mueve las alas y parpadea
	 * (se esconden un instante las placas de los ojos en espiral).
	 */
	private static void dibujarReviil(Matrix4f base, float tiempo, float aparicion) {
		ModelPart m = modelo();

		float flotar = Mth.sin(tiempo * 0.04f) * 0.8f;
		float giro = Mth.sin(tiempo * 0.023f) * 0.30f + Mth.sin(tiempo * 0.061f) * 0.07f;
		float cabeceo = Mth.sin(tiempo * 0.017f) * 0.09f;
		cuerno.zRot = CUERNO_ZROT + Mth.sin(tiempo * 0.05f) * 0.04f;
		// Sube desde abajo de la abertura (tapado por la máscara) y abre los ojos cuando ya llegó.
		float subida = (1f - suave(Mth.clamp(aparicion / 0.8f, 0f, 1f))) * SUBIDA;
		boolean abiertos = aparicion >= 0.9f && cierreParpadeo(tiempo) < 0.5f;
		ojoIzquierdo.visible = abiertos;
		ojoDerecho.visible = abiertos;

		PoseStack pose = new PoseStack();
		pose.mulPose(base);
		pose.translate(0f, flotar - subida, -PROFUNDIDAD);
		pose.mulPose(Axis.YP.rotation(giro));
		pose.mulPose(Axis.XP.rotation(cabeceo));
		// Los modelos de Minecraft tienen Y hacia abajo y la cara hacia -Z: se da vuelta para que mire a la cámara.
		pose.scale(ESCALA, -ESCALA, -ESCALA);
		pose.translate(-CABEZA_X / 16f, -CABEZA_Y / 16f, -CABEZA_Z / 16f);

		FogRenderer.setupNoFog();
		Lighting.setupLevel();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		// En el Eclipse ninguna luz ilumina: Reviil brilla por su cuenta (si no, se vería negro).
		RenderType tipo = EclipseCliente.activoAqui() ? RenderType.entityTranslucentEmissive(TEXTURA) : RenderType.entityCutoutNoCull(TEXTURA);
		m.render(pose, buffers.getBuffer(tipo), LightTexture.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, TONO_BEIGE);
		buffers.endBatch();
	}

	private static float suave(float t) {
		return t * t * (3f - 2f * t);
	}

	/** Parpadea cada ~4,5 segundos; una de cada tres veces parpadea dos veces seguidas. */
	private static float cierreParpadeo(float tiempo) {
		float ciclo = tiempo % 90f;
		int numero = (int) (tiempo / 90f);
		float cierre = parpadeo(ciclo);
		if (numero % 3 == 2) cierre = Math.max(cierre, parpadeo(ciclo - 9f));
		return cierre;
	}

	/** 0 → 1 → 0 en 7 ticks. */
	private static float parpadeo(float t) {
		if (t < 0 || t > 7) return 0;
		return t < 3 ? t / 3f : t < 4 ? 1f : 1f - (t - 4) / 3f;
	}
}
