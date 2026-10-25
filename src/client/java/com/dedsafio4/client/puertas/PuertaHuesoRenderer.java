package com.dedsafio4.client.puertas;

import com.dedsafio4.puertas.PuertaHuesoAnimacion;
import com.dedsafio4.puertas.PuertaHuesoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Dibuja la reja de huesos (ver PuertaHuesoAnimacion): cada bloque es un bloque de hueso de Minecraft (con sus
 * texturas de siempre: el costado y la punta), los postes girados 45° y las vigas acostadas a lo largo. Se estira al
 * grosor y al alto de la puerta. La puerta del medio gira sobre su bisagra; con los golpes se abomba y al romperse
 * cada bloque va donde lo dejó su caída.
 */
public class PuertaHuesoRenderer extends EntityRenderer<PuertaHuesoEntity> {
	private static final ResourceLocation COSTADO = ResourceLocation.withDefaultNamespace("textures/block/bone_block_side.png");
	private static final ResourceLocation PUNTA = ResourceLocation.withDefaultNamespace("textures/block/bone_block_top.png");

	public PuertaHuesoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 0;
	}

	@Override
	public void render(PuertaHuesoEntity puerta, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		// Primero todos los costados y después todas las puntas (una tanda por textura).
		dibujar(puerta, parcial, pose, buffers.getBuffer(RenderType.entityCutout(COSTADO)), luz, false);
		dibujar(puerta, parcial, pose, buffers.getBuffer(RenderType.entityCutout(PUNTA)), luz, true);
		super.render(puerta, yaw, parcial, pose, buffers, luz);
	}

	private void dibujar(PuertaHuesoEntity puerta, float parcial, PoseStack pose, VertexConsumer vc, int luz, boolean puntas) {
		PuertaHuesoAnimacion anim = puerta.animacion;
		float sy = puerta.alto() / PuertaHuesoEntity.ALTO_NORMAL, sz = puerta.grueso() / PuertaHuesoEntity.GRUESO_NORMAL;
		int lado = puerta.lado();
		pose.pushPose();
		float temblor = anim.temblor(parcial);
		pose.mulPose(Axis.YP.rotationDegrees(-puerta.getYRot()));
		pose.scale(1 / 16f, sy / 16f, sz / 16f);
		if (temblor > 0) {
			float t = puerta.tickCount + parcial;
			pose.translate(Mth.sin(t * 2.3f) * temblor, 0, Mth.cos(t * 1.9f) * temblor * 0.5f);
		}
		float abierta = anim.apertura(parcial);
		for (int i = 0; i < PuertaHuesoAnimacion.BLOQUES.size(); i++) {
			PuertaHuesoAnimacion.Bloque b = PuertaHuesoAnimacion.BLOQUES.get(i);
			pose.pushPose();
			float[] cae = anim.desarme(i, parcial);
			if (cae != null) {
				pose.translate(b.cx + cae[0], b.cy + cae[1], b.cz + cae[2]);
				pose.mulPose(Axis.XP.rotation(cae[3]));
				pose.mulPose(Axis.ZP.rotation(cae[4]));
			} else {
				if (b.puerta && abierta != 0) {
					float x0 = PuertaHuesoAnimacion.puertaX0();
					pose.translate(x0, 0, 0);
					pose.mulPose(Axis.YP.rotation(abierta));
					pose.translate(-x0, 0, 0);
				}
				pose.translate(b.cx, b.cy, b.cz + anim.abombado(b, parcial) * lado);
			}
			if (b.poste) pose.mulPose(Axis.YP.rotationDegrees(45));
			caja(pose.last(), vc, luz, b.w / 2, b.h / 2, b.d / 2, b.poste, puntas);
			pose.popPose();
		}
		pose.popPose();
	}

	/**
	 * Un bloque de hueso centrado: el poste tiene las puntas arriba y abajo; la viga, a los costados (x), y el resto de
	 * las caras con el costado girado.
	 */
	private static void caja(PoseStack.Pose p, VertexConsumer vc, int luz, float x, float y, float z, boolean poste, boolean puntas) {
		// Caras: +x, -x, +y, -y, +z, -z (cada una: normal y 4 esquinas).
		float[][] n = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		float[][][] v = {
				{{x, -y, z}, {x, -y, -z}, {x, y, -z}, {x, y, z}},
				{{-x, -y, -z}, {-x, -y, z}, {-x, y, z}, {-x, y, -z}},
				{{-x, y, z}, {x, y, z}, {x, y, -z}, {-x, y, -z}},
				{{-x, -y, -z}, {x, -y, -z}, {x, -y, z}, {-x, -y, z}},
				{{-x, -y, z}, {x, -y, z}, {x, y, z}, {-x, y, z}},
				{{x, -y, -z}, {-x, -y, -z}, {-x, y, -z}, {x, y, -z}}};
		float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
		for (int c = 0; c < 6; c++) {
			boolean esPunta = poste ? c == 2 || c == 3 : c == 0 || c == 1;
			if (esPunta != puntas) continue;
			// En la viga el costado va girado (las vetas a lo largo).
			boolean girada = !poste && !esPunta;
			for (int k = 0; k < 4; k++) {
				float[] t = uv[girada ? (k + 1) % 4 : k];
				vc.addVertex(p, v[c][k][0], v[c][k][1], v[c][k][2]).setColor(-1).setUv(t[0], t[1])
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(p, n[c][0], n[c][1], n[c][2]);
			}
		}
	}

	@Override
	public ResourceLocation getTextureLocation(PuertaHuesoEntity puerta) {
		return COSTADO;
	}
}
