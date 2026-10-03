package com.dedsafio4.client.bulag;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * El Cráneo Explosivo (Bulag 2) arriba de la cabeza del jugador, como en el diseño "Cráneo en humano":
 * el cráneo al 60% sobre la cabeza y 8 tentáculos rojos que la rodean (los de los costados pasan por los
 * hombros y bajan por los brazos; los de adelante y atrás cuelgan), moviéndose todo el tiempo.
 * Medidas en píxeles (1/16 de bloque), con y para arriba y la cara mirando a +z.
 */
public class CraneoLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/craneo.png");
	private static final float P = 1 / 16f, ESCALA = 0.6f, LARGO = 2.2f;
	private static final int SEGMENTOS = 13;
	// Colores de la textura (cuadraditos de 4 píxeles).
	private static final int HUESO = 0, HUESO_SOMBRA = 1, HUECO = 2, ROJO = 3, ROJO_OSCURO = 4, VENTOSA = 5;

	/** La cara del cráneo, fila de arriba primero: # hueso, . hueco oscuro. */
	private static final String[] CARA = {
			"  ############  ",
			" ############## ",
			"################",
			"################",
			"################",
			"#....######....#",
			"#....######....#",
			"#....######....#",
			"##...######...##",
			"#######..#######",
			"######....######",
			"################",
			" ############## ",
			"  ############  ",
			"  #.#.#.#.#.#.  ",
			"  ############  ",
			"   ##########   ",
	};

	/** Las cajas del cráneo: {x0, y0, z0, x1, y1, z1, color} en píxeles. */
	private static final List<float[]> CRANEO = new ArrayList<>();

	static {
		int alto = CARA.length;
		for (int fila = 0; fila < alto; fila++) {
			String renglon = CARA[fila];
			float y0 = alto - fila - 1, y1 = alto - fila;
			for (int c = 0; c < 16; ) {
				char letra = renglon.charAt(c);
				int e = c;
				while (e < 16 && renglon.charAt(e) == letra) e++;
				if (letra == '#') CRANEO.add(new float[]{c - 8, y0, 6, e - 8, y1, 8, HUESO});
				if (letra == '.') CRANEO.add(new float[]{c - 8, y0, 6, e - 8, y1, 7, HUECO});
				c = e;
			}
		}
		CRANEO.add(new float[]{-8, 4, -8, 8, 15, 6, HUESO});            // cráneo
		CRANEO.add(new float[]{-7, 15, -7, 7, 16, 6, HUESO});           // arriba
		CRANEO.add(new float[]{-6, 16, -5.5f, 6, 17, 5.5f, HUESO});     // coronilla
		CRANEO.add(new float[]{-7, 5, -9, 7, 14, -8, HUESO_SOMBRA});    // nuca
		CRANEO.add(new float[]{-6, 0, -4, 6, 4, 6, HUESO_SOMBRA});      // mandíbula
		CRANEO.add(new float[]{-8, 6, -1, -7, 10, 5, HUESO});           // pómulos
		CRANEO.add(new float[]{7, 6, -1, 8, 10, 5, HUESO});
	}

	public CraneoLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre) {
		super(padre);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador, float limbSwing,
					   float limbSwingAmount, float parcial, float edad, float cabezaGiro, float cabezaArriba) {
		if (jugador.isInvisible() || !BulagCliente.tieneCraneo(jugador.getUUID())) return;
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		float t = (jugador.tickCount + parcial) / 20f + (jugador.getId() % 7);

		pose.pushPose();
		getParentModel().getHead().translateAndRotate(pose);
		// Del espacio del modelo (y para abajo) al del diseño: y para arriba, la cara hacia +z. Arriba de la cabeza.
		pose.scale(1, -1, -1);
		pose.translate(0, 0.5, 0);
		pose.scale(ESCALA * P, ESCALA * P, ESCALA * P);

		for (float[] c : CRANEO) caja(vc, pose.last(), c[0], c[1], c[2], c[3], c[4], c[5], (int) c[6], luz);

		for (int n = 0; n < 8; n++) {
			double angulo = Math.toRadians(n * 45 + 22.5);
			float cx = (float) Math.cos(angulo), cz = (float) Math.sin(angulo);
			float r = 4.6f / Math.max(Math.abs(cx), Math.abs(cz)) / ESCALA;   // justo por fuera de la cabeza
			boolean costado = Math.abs(cx) > 0.7f;
			float fase = n * 0.8f;
			pose.pushPose();
			pose.translate(cx * r, 0.5f, cz * r);
			pose.mulPose(Axis.YP.rotation((float) -angulo));
			for (int i = 0; i < SEGMENTOS; i++) {
				if (i > 0) pose.translate(LARGO, 0, 0);
				float base;
				if (i == 0) base = -1.5f;
				else if (costado) base = i == 6 ? 1.25f : i == 8 ? -1.2f : i > SEGMENTOS - 3 ? 0.2f : 0;
				else base = i > SEGMENTOS - 4 ? 0.28f : -0.03f;
				float libre = i / (float) (SEGMENTOS - 1);
				float giroZ = base + (0.02f + 0.12f * libre * libre) * Mth.sin(t * 2.2f - i * 0.55f + fase);
				float giroY = 0.12f * libre * Mth.sin(t * 1.6f - i * 0.4f + fase * 1.3f);
				pose.mulPose(Axis.YP.rotation(giroY));
				pose.mulPose(Axis.ZP.rotation(giroZ));
				float grosor = 3.4f - 2.2f * libre, m = grosor / 2;
				caja(vc, pose.last(), LARGO / 2 - LARGO * 0.54f, -m, -m, LARGO / 2 + LARGO * 0.54f, m, m,
						libre > 0.75f ? ROJO_OSCURO : ROJO, luz);
				if (i % 2 == 1 && libre < 0.85f) {
					float v = grosor * 0.3f;
					caja(vc, pose.last(), LARGO / 4, -m - 0.5f, -v, LARGO * 0.75f, -m + 0.1f, v, VENTOSA, luz);
				}
			}
			pose.popPose();
		}
		pose.popPose();
	}

	/** Una caja de un color liso. */
	private static void caja(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1,
							 int color, int luz) {
		float u = (color * 4 + 2) / 32f, v = 4 / 8f;
		cara(vc, p, u, v, luz, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		cara(vc, p, u, v, luz, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
		cara(vc, p, u, v, luz, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
		cara(vc, p, u, v, luz, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		cara(vc, p, u, v, luz, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
		cara(vc, p, u, v, luz, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, float u, float v, int luz, float nx, float ny, float nz,
							 float... xyz) {
		for (int i = 0; i < 12; i += 3) {
			vc.addVertex(p, xyz[i], xyz[i + 1], xyz[i + 2]).setColor(255, 255, 255, 255).setUv(u, v)
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}
}
