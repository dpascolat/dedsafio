package com.dedsafio4.client.qumara;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.qumara.CapulloEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Capullo como en el diseño: el bulbo de capas, 4 filas de pétalos, la corona y 4 tallos con hojas.
 * Respira siempre; abierto, los pétalos se despliegan hacia afuera (de afuera hacia adentro) y el bulbo
 * y la corona se esconden (queda vacío); cerrado, vuelve a ser el capullo.
 */
public class CapulloRenderer extends EntityRenderer<CapulloEntity> {
	private static final int MORADO = 0, PETALO = 1, OSCURO = 2, TALLO = 3, HOJA = 4;
	private static final String[] NOMBRES = {"morado", "petalo", "morado_oscuro", "tallo", "hoja"};
	private static final RenderType[] TIPOS = new RenderType[NOMBRES.length];

	static {
		for (int i = 0; i < NOMBRES.length; i++) {
			TIPOS[i] = RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/capullo/" + NOMBRES[i] + ".png"));
		}
	}

	/** Capas del bulbo: y, ancho, alto. */
	private static final float[][] NUCLEO = {{0, 7, 1}, {1, 9, 1.2f}, {2.2f, 10, 2}, {4.2f, 9.4f, 1.6f}, {5.8f, 8.2f, 1.4f},
			{7.2f, 6.6f, 1.3f}, {8.5f, 5, 1.2f}, {9.7f, 3.6f, 1.1f}, {10.8f, 2.4f, 1}, {11.8f, 1.4f, 1}};
	/** Filas de pétalos: cantidad, radio, y0, ancho, largo, inclinación, desfase. */
	private static final float[][] FILAS = {{8, 4.9f, 1.2f, 6.2f, 5.6f, 0.22f, 0}, {8, 3.7f, 4.6f, 4.6f, 4.8f, 0.3f, Mth.PI / 8},
			{6, 2.3f, 7.8f, 3, 3.8f, 0.35f, 0}, {4, 1.2f, 10.2f, 1.6f, 2.6f, 0.3f, Mth.PI / 4}};
	/** Tallos: ángulo y hacia qué lado va la hoja. */
	private static final float[][] TALLOS = {{0.4f, 1}, {2.3f, -1}, {4.1f, 1}, {5.4f, -1}};

	public CapulloRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 5f;
	}

	private static float suave(float x) {
		return x < 0 ? 0 : x > 1 ? 1 : x * x * (3 - 2 * x);
	}

	private int luz, overlay;
	private MultiBufferSource buffers;

	@Override
	public void render(CapulloEntity c, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		this.luz = luz;
		this.overlay = OverlayTexture.pack(0f, c.hurtTime > 0);
		this.buffers = buffers;
		float t = (c.tickCount + parcial) / 20f;
		float op = c.apertura < 0 ? (c.abierto() ? 1 : 0) : Mth.lerp(parcial, c.aperturaAntes, c.apertura);
		float b = Mth.sin(t * 1.3f);
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, c.yBodyRotO, c.yBodyRot)));

		// El capullo (respira).
		pose.pushPose();
		pose.scale(1 + b * 0.02f, 1 - b * 0.012f, 1 + b * 0.02f);
		float nucleo = suave(op * 1.4f - 0.1f);
		for (int i = 0; i < NUCLEO.length; i++) {
			float y = NUCLEO[i][0], w = NUCLEO[i][1], h = NUCLEO[i][2];
			float f = y > 4 ? 1 - nucleo : 1 - nucleo * 0.4f;
			if (f <= 0.01f) continue;
			pose.pushPose();
			pose.translate(0, y, 0);
			pose.scale(1, f, 1);
			caja(pose, i % 2 == 1 ? OSCURO : MORADO, w, h, w, 0, h / 2, 0);
			pose.popPose();
		}
		for (int fila = 0; fila < FILAS.length; fila++) {
			float[] f = FILAS[fila];
			int n = (int) f[0];
			float r = f[1], y0 = f[2], w = f[3], largo = f[4], inc = f[5], des = f[6];
			float o = suave(op * 1.6f - fila * 0.2f);
			for (int i = 0; i < n; i++) {
				float a = des + i * 2 * Mth.PI / n;
				pose.pushPose();
				pose.translate(Mth.sin(a) * r, y0, Mth.cos(a) * r);
				pose.mulPose(Axis.YP.rotation(a));
				pose.mulPose(Axis.XP.rotation(-inc * (1 - o) + o * (1.25f - fila * 0.12f) + Mth.sin(t * 1.3f - fila * 0.5f + i) * 0.03f * (1 + o)));
				caja(pose, PETALO, w, largo, 0.9f, 0, largo / 2, 0.45f);
				caja(pose, PETALO, w * 0.5f, 0.9f, 0.9f, 0, largo + 0.45f, 0.45f);
				caja(pose, OSCURO, w + 0.1f, 0.5f, 0.95f, 0, 0.25f, 0.45f);
				pose.popPose();
			}
		}
		// Corona (baja y se achica al abrirse).
		if (nucleo < 0.98f) {
			pose.pushPose();
			pose.translate(0, -nucleo * 8, 0);
			float e = Math.max(0.001f, 1 - nucleo);
			pose.scale(e, e, e);
			caja(pose, MORADO, 0.9f, 1.6f, 0.9f, 0, 13.6f, 0);
			for (int i = 0; i < 4; i++) {
				float a = i * Mth.PI / 2 + Mth.PI / 4;
				pose.pushPose();
				pose.translate(Mth.sin(a) * 0.6f, 12.9f, Mth.cos(a) * 0.6f);
				pose.mulPose(Axis.YP.rotation(a));
				pose.mulPose(Axis.XP.rotation(0.7f));
				caja(pose, PETALO, 0.6f, 1.4f, 0.6f, 0, 0.7f, 0);
				pose.popPose();
			}
			pose.popPose();
		}
		pose.popPose();

		// Tallos y hojas.
		for (int k = 0; k < TALLOS.length; k++) {
			float a = TALLOS[k][0], s = TALLOS[k][1];
			pose.pushPose();
			pose.translate(Mth.sin(a) * 4.2f, 0, Mth.cos(a) * 4.2f);
			pose.mulPose(Axis.YP.rotation(a));
			for (int i = 0; i < 10; i++) {
				if (i > 0) pose.translate(0, 2.2f, 0);
				pose.mulPose(Axis.XP.rotation((i < 4 ? 0.12f : 0.2f) + Mth.sin(t * 1.1f + k - i * 0.5f) * 0.04f));
				pose.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.9f + k * 2 - i * 0.4f) * 0.06f));
				caja(pose, TALLO, 0.9f, 2.3f, 0.9f, 0, 1.1f, 0);
			}
			pose.popPose();
			pose.pushPose();
			pose.translate(Mth.sin(a + s * 0.5f) * 5.2f, 0.2f, Mth.cos(a + s * 0.5f) * 5.2f);
			pose.mulPose(Axis.YP.rotation(a + s * 0.5f));
			caja(pose, HOJA, 2.2f, 0.3f, 4.5f, 0, 0.15f, 2.2f);
			caja(pose, HOJA, 1.2f, 0.3f, 1.4f, 0, 0.15f, 5.1f);
			pose.popPose();
		}
		pose.popPose();
		super.render(c, yaw, parcial, pose, buffers, luz);
	}

	/** Una caja w×h×d centrada en (x, y, z), con la textura una vez por bloque y la sombra falsa del diseño. */
	private void caja(PoseStack pose, int mat, float w, float h, float d, float x, float y, float z) {
		VertexConsumer vc = buffers.getBuffer(TIPOS[mat]);
		PoseStack.Pose p = pose.last();
		float x0 = x - w / 2, x1 = x + w / 2, y0 = y - h / 2, y1 = y + h / 2, z0 = z - d / 2, z1 = z + d / 2;
		cara(vc, p, 1, 0, 0, d, h, 0, y0, y1, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
		cara(vc, p, -1, 0, 0, d, h, 0, y0, y1, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		cara(vc, p, 0, 1, 0, w, d, 2, y0, y1, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
		cara(vc, p, 0, -1, 0, w, d, 1, y0, y1, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		cara(vc, p, 0, 0, 1, w, h, 0, y0, y1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		cara(vc, p, 0, 0, -1, w, h, 0, y0, y1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
	}

	private void cara(VertexConsumer vc, PoseStack.Pose p, float nx, float ny, float nz, float ancho, float alto, int tipo,
					  float y0, float y1, float... q) {
		float[] us = {0, ancho, ancho, 0}, vs = {alto, alto, 0, 0};
		for (int i = 0; i < 4; i++) {
			float y = q[i * 3 + 1];
			float fy = tipo == 1 ? 0 : tipo == 2 ? 1 : (y - y0) / (y1 - y0);
			float v = 0.62f + 0.38f * fy;
			if (tipo == 1) v *= 0.7f;
			v = (float) Math.pow(v, 1 / 2.2);
			int r = (int) (255 * Math.min(1, v * 0.97f)), g = (int) (255 * Math.min(1, v * 0.96f)), bl = (int) (255 * Math.min(1, v * 1.05f));
			vc.addVertex(p, q[i * 3], y, q[i * 3 + 2]).setColor(r, g, bl, 255).setUv(us[i], vs[i]).setOverlay(overlay)
					.setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	/** Los tallos llegan a ~22 bloques: se dibuja mientras cualquier parte esté a la vista. */
	@Override
	public boolean shouldRender(CapulloEntity c, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(c.getBoundingBox().inflate(16, 4, 16));
	}

	@Override
	public ResourceLocation getTextureLocation(CapulloEntity c) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/capullo/morado.png");
	}
}
