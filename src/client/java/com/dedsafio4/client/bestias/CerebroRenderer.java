package com.dedsafio4.client.bestias;

import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * El Cerebro Amarillo y la Garrapata Cerebral (el mismo cuerpo, cambian los colores de la textura). Del diseño: la campana es un cerebro rosa (dos hemisferios con la fisura, pliegues, venas
 * amarillas, cerebelo y tronco), con el borde de medusa abajo y 18 tentáculos morados de 7 tramos con las puntas
 * amarillas. Suelto, los tentáculos se abren hasta el piso y ondulan; pegado a la cabeza de un jugador, la campana
 * se apoya en la coronilla y los tentáculos bajan pegados a la cara y se curvan debajo del mentón, apretando.
 */
public class CerebroRenderer<T extends LivingEntity> extends CajasRenderer<T> {
	/** Los colores de la textura: las crestas de a una (amarillas en el Cerebro Amarillo) y las puntas van aparte. */
	private static final int CEREBRO = 0, PLIEGUE = 1, HONDO = 2, VENA = 3, MORADO = 4, MORADO_OSCURO = 5, PUNTA = 6, CRESTA = 7;
	private final ResourceLocation textura;
	private static final float BY = 14, W = 9, H = 7, D = 10, ARRIBA = BY - H / 2 - 1.2f;
	/** Pegado: cuánto se baja el modelo para que el borde de la campana quede sobre la cabeza del jugador. */
	private static final float BAJA_PEGADO = 1.2f - (BY - H / 2 - 1.4f) + 0.2f;

	private boolean pegado;
	private float tiempo;

	public CerebroRenderer(EntityRendererProvider.Context contexto, ResourceLocation textura) {
		super(contexto, 0.5f, 8);
		this.textura = textura;
		for (int sx = -1; sx <= 1; sx += 2) {
			float hx = sx * (W / 4 + 0.25f);
			caja(raiz, CEREBRO, W / 2 - 0.5f, H, D, hx, BY, 0);
			caja(raiz, CEREBRO, W / 2 - 1.5f, 1, D - 2, hx, BY + H / 2 + 0.5f, -0.3f);
			for (int i = 0; i < 5; i++) {
				float z = -D / 2 + 1.3f + i * 1.9f, corre = (i % 2 == 1 ? 0.6f : -0.6f) * sx;
				caja(raiz, i % 2 == 1 ? CRESTA : PLIEGUE, 2.6f, 0.7f, 1, hx + corre, BY + H / 2 + 1.15f, z);
			}
			for (int r = 0; r < 3; r++) {
				for (int c = 0; c < 3; c++) {
					if ((r + c) % 2 == 1) continue;
					caja(raiz, PLIEGUE, 0.7f, 1.4f, 2.8f, sx * (W / 2 + 0.1f), BY + 2.2f - r * 2.2f, -3.2f + c * 3.2f + (r % 2) * 1.2f);
				}
			}
			caja(raiz, PLIEGUE, W / 2 - 1.6f, 2, 0.7f, hx, BY + 1.6f, D / 2 + 0.1f);
			caja(raiz, PLIEGUE, W / 2 - 1.6f, 2.4f, 0.7f, hx, BY + 0.6f, -D / 2 - 0.1f);
		}
		caja(raiz, HONDO, 1, H + 0.6f, D + 0.4f, 0, BY + 0.6f, 0);
		float[][] venas = {{-2.6f, 2}, {2, -2.4f}, {-1.4f, -3.6f}};
		for (float[] v : venas) caja(raiz, VENA, 0.4f, 0.4f, 3.4f, v[0], BY + H / 2 + 1.6f, v[1]);
		caja(raiz, VENA, 0.3f, 4, 0.4f, -W / 2 - 0.35f, BY - 0.5f, 1.8f);
		caja(raiz, VENA, 0.3f, 3.4f, 0.4f, W / 2 + 0.35f, BY - 0.2f, -1.4f);
		caja(raiz, PLIEGUE, 5, 2.4f, 3, 0, BY - H / 2 - 0.4f, -D / 2 + 1.4f);
		caja(raiz, HONDO, 2, 2.6f, 2, 0, BY - H / 2 - 1.4f, -D / 2 - 0.4f);
		caja(raiz, PLIEGUE, W + 1.2f, 1.2f, D + 1.2f, 0, BY - H / 2 - 0.2f, 0);
		caja(raiz, HONDO, W - 1, 0.8f, D - 1, 0, BY - H / 2 - 1, 0);
	}

	@Override
	protected void animar(T bicho, float parcial) {
		pegado = bicho.isPassenger();
		tiempo = (bicho.tickCount + parcial) / 20f;
		raiz.y = pegado ? BAJA_PEGADO : 0;
		// Late despacio (más fuerte cuando está drenando).
		raiz.x = 0;
	}

	@Override
	protected float giroDiseno() {
		return 0;
	}

	@Override
	public void render(T bicho, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		super.render(bicho, yaw, parcial, pose, buffers, luz);
		// Los tentáculos: tramos finos de un punto al otro (no entran en las cajas derechas de CajasRenderer).
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, bicho.yBodyRotO, bicho.yBodyRot)));
		if (bicho.deathTime > 0) {
			float caida = Math.min(1f, Mth.sqrt((bicho.deathTime + parcial - 1) / 20f * 1.6f));
			pose.mulPose(Axis.ZP.rotationDegrees(caida * 90f));
		}
		pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
		pose.translate(0, raiz.y, 0);
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(textura));
		int overlay = LivingEntityRenderer.getOverlayCoords(bicho, 0);
		float caminar = bicho.walkAnimation.position(parcial) * 0.6f;
		float aprieta = pegado ? 0.03f * Mth.sin(tiempo * 2.2f) : 0;
		for (int i = 0; i < 18; i++) {
			float ang = i / 18f * Mth.TWO_PI + 0.3f;
			float rx = Mth.cos(ang) * (W / 2 - 0.4f), rz = Mth.sin(ang) * (D / 2 - 0.4f);
			float[][] pts = new float[9][];
			for (int k = 0; k <= 7; k++) {
				float t = k / 7f;
				float abre = pegado ? 1.06f + t * 0.06f - aprieta : 1 + t * 0.55f;
				float ondula = pegado ? Mth.sin(t * 6 + i * 1.7f + tiempo * 1.5f) * 0.6f
						: Mth.sin(t * 7 + i * 1.7f + tiempo * 2 + caminar) * 1.1f;
				pts[k] = new float[]{rx * abre + Mth.cos(ang + 1.57f) * ondula, ARRIBA - t * (ARRIBA - 0.6f),
						rz * abre + Mth.sin(ang + 1.57f) * ondula};
			}
			float[] fin = pts[7];
			// Suelto: la punta se enrula para afuera en el piso. Pegado: se curva para adentro, debajo del mentón.
			float lado = pegado ? -1.8f : 1.8f;
			pts[8] = new float[]{fin[0] + Mth.cos(ang) * lado, pegado ? -0.6f : 0.5f, fin[2] + Mth.sin(ang) * lado};
			for (int k = 0; k < 8; k++) {
				int color = k >= 6 ? PUNTA : (k % 2 == 1 ? MORADO_OSCURO : MORADO);
				barra(pose, vc, pts[k], pts[k + 1], 0.7f - k * 0.03f, color, luz, overlay);
			}
		}
		pose.popPose();
	}

	/** Un tramo de sección cuadrada de grosor g que va de a hasta b. */
	private static void barra(PoseStack pose, VertexConsumer vc, float[] a, float[] b, float g, int color, int luz, int overlay) {
		Vector3f dir = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
		float largo = dir.length();
		if (largo < 1e-4f) return;
		pose.pushPose();
		pose.translate((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2);
		pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), dir.normalize()));
		float h = (largo + g * 0.5f) / 2, s = g / 2;
		float u = (color * 4 + 2) / 32f, v = 0.5f;
		PoseStack.Pose p = pose.last();
		cara(vc, p, u, v, luz, overlay, 1, 0, 0, s, -h, -s, s, h, -s, s, h, s, s, -h, s);
		cara(vc, p, u, v, luz, overlay, -1, 0, 0, -s, -h, s, -s, h, s, -s, h, -s, -s, -h, -s);
		cara(vc, p, u, v, luz, overlay, 0, 1, 0, -s, h, -s, -s, h, s, s, h, s, s, h, -s);
		cara(vc, p, u, v, luz, overlay, 0, -1, 0, -s, -h, s, -s, -h, -s, s, -h, -s, s, -h, s);
		cara(vc, p, u, v, luz, overlay, 0, 0, 1, -s, -h, s, s, -h, s, s, h, s, -s, h, s);
		cara(vc, p, u, v, luz, overlay, 0, 0, -1, s, -h, -s, -s, -h, -s, -s, h, -s, s, h, -s);
		pose.popPose();
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, float u, float v, int luz, int overlay,
							 float nx, float ny, float nz, float... q) {
		for (int i = 0; i < 12; i += 3) {
			vc.addVertex(p, q[i], q[i + 1], q[i + 2]).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay)
					.setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(T bicho) {
		return textura;
	}
}
