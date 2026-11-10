package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.dimension.EspacioInterdimensional;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * El cielo del Espacio Interdimensional: una nebulosa violeta, azul y verdosa llena de estrellas, en las 6 caras de
 * un cubo gigante alrededor de la cámara (textures/environment/espacio_interdimensional). Sin nubes ni lluvia.
 */
public final class EspacioInterdimensionalCielo {
	private EspacioInterdimensionalCielo() {}

	private static final float LEJOS = 100f;
	private static final String[] CARAS = {"norte", "sur", "este", "oeste", "arriba", "abajo"};
	private static final ResourceLocation[] TEXTURAS = new ResourceLocation[CARAS.length];

	static {
		for (int i = 0; i < CARAS.length; i++) {
			TEXTURAS[i] = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/environment/espacio_interdimensional/" + CARAS[i] + ".png");
		}
	}

	public static void registrar() {
		DimensionRenderingRegistry.registerSkyRenderer(EspacioInterdimensional.DIMENSION, EspacioInterdimensionalCielo::dibujar);
		DimensionRenderingRegistry.registerCloudRenderer(EspacioInterdimensional.DIMENSION, contexto -> {});
		DimensionRenderingRegistry.registerWeatherRenderer(EspacioInterdimensional.DIMENSION, contexto -> {});
	}

	/** La dirección de cada punto de una cara, igual que como se armaron las texturas (u, w de -1 a 1). */
	private static float[] punto(int cara, float u, float w) {
		return switch (cara) {
			case 0 -> new float[]{-u, w, -1};   // norte
			case 1 -> new float[]{u, w, 1};     // sur
			case 2 -> new float[]{1, w, -u};    // este
			case 3 -> new float[]{-1, w, u};    // oeste
			case 4 -> new float[]{u, 1, -w};    // arriba
			default -> new float[]{u, -1, w};   // abajo
		};
	}

	private static void dibujar(WorldRenderContext contexto) {
		Matrix4f m = contexto.positionMatrix();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		float[][] esquinas = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
		for (int cara = 0; cara < CARAS.length; cara++) {
			RenderSystem.setShaderTexture(0, TEXTURAS[cara]);
			BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			for (float[] e : esquinas) {
				float[] p = punto(cara, e[0] * 2 - 1, 1 - e[1] * 2);
				b.addVertex(m, p[0] * LEJOS, p[1] * LEJOS, p[2] * LEJOS).setUv(e[0], e[1]);
			}
			BufferUploader.drawWithShader(b.buildOrThrow());
		}
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
	}
}
