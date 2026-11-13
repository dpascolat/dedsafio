package com.dedsafio4.client.portal;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.portal.PortalEspacioEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * El remolino del Portal del Espacio: un disco que gira, con brazos en espiral violetas, azules y celestes, y el borde
 * brillante (celeste y rosado). Los colores se calculan en cada punto, así que no necesita textura.
 */
public class PortalEspacioRenderer extends EntityRenderer<PortalEspacioEntity> {
	private static final ResourceLocation NADA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/blanco.png");
	/** Anillos y gajos del disco. */
	private static final int ANILLOS = 18, GAJOS = 64;

	public PortalEspacioRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public ResourceLocation getTextureLocation(PortalEspacioEntity portal) {
		return NADA;
	}

	@Override
	public void render(PortalEspacioEntity portal, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		float t = portal.tickCount + parcial;
		pose.pushPose();
		pose.translate(0, PortalEspacioEntity.CENTRO, 0);
		pose.mulPose(Axis.YP.rotationDegrees(-portal.getYRot()));
		Matrix4f m = pose.last().pose();
		float r = PortalEspacioEntity.RADIO;

		// El remolino.
		VertexConsumer v = buffers.getBuffer(RenderType.debugQuads());
		for (int i = 0; i < ANILLOS; i++) {
			float r0 = (float) i / ANILLOS, r1 = (float) (i + 1) / ANILLOS;
			for (int j = 0; j < GAJOS; j++) {
				float a0 = Mth.TWO_PI * j / GAJOS, a1 = Mth.TWO_PI * (j + 1) / GAJOS;
				punto(v, m, r, r0, a0, t);
				punto(v, m, r, r1, a0, t);
				punto(v, m, r, r1, a1, t);
				punto(v, m, r, r0, a1, t);
			}
		}

		// El borde que brilla: celeste por fuera, un poco rosado adentro, y late despacio.
		VertexConsumer brillo = buffers.getBuffer(RenderType.lightning());
		float late = 0.75f + 0.25f * Mth.sin(t * 0.12f);
		aro(brillo, m, r * 0.97f, r * 1.06f, t, 70, 210, 235, (int) (170 * late));
		aro(brillo, m, r * 0.90f, r * 0.97f, t, 200, 110, 230, (int) (90 * late));
		aro(brillo, m, r * 1.06f, r * 1.13f, t, 70, 190, 235, (int) (50 * late));
		pose.popPose();
		super.render(portal, giro, parcial, pose, buffers, luz);
	}

	/** El borde ondula un poquito, como agua. */
	private static float onda(float a, float t) {
		return 1 + 0.025f * Mth.sin(a * 7 + t * 0.2f) + 0.015f * Mth.sin(a * 13 - t * 0.31f);
	}

	/** Un punto del disco (radio de 0 a 1, ángulo) con el color del remolino en ese lugar. */
	private static void punto(VertexConsumer v, Matrix4f m, float radio, float rr, float a, float t) {
		float d = rr * onda(a, t);
		float x = Mth.cos(a) * d * radio, y = Mth.sin(a) * d * radio;
		// Brazos en espiral que giran: el ángulo se tuerce más cuanto más afuera.
		float brazo = 0.5f + 0.5f * Mth.sin(3 * a + 7 * rr - t * 0.09f);
		float vetas = 0.5f + 0.5f * Mth.sin(5 * a - 11 * rr + t * 0.13f + Mth.sin(rr * 9 + t * 0.05f));
		// Azul muy oscuro → violeta en los brazos → celeste en las vetas finas.
		float cr = Mth.lerp(brazo, 35, 125), cg = Mth.lerp(brazo, 25, 55), cb = Mth.lerp(brazo, 95, 205);
		float celeste = Mth.clamp((vetas - 0.72f) * 3.5f, 0, 1) * (0.4f + 0.6f * rr);
		cr = Mth.lerp(celeste, cr, 80);
		cg = Mth.lerp(celeste, cg, 185);
		cb = Mth.lerp(celeste, cb, 220);
		// Más oscuro en el medio (el fondo del túnel) y un poco más claro cerca del borde.
		float luz = 0.55f + 0.45f * rr + (rr > 0.85f ? (rr - 0.85f) * 2 : 0);
		int alfa = rr > 0.95f ? (int) Mth.lerp((rr - 0.95f) / 0.05f, 230, 120) : 230;
		v.addVertex(m, x, y, 0).setColor(c(cr * luz), c(cg * luz), c(cb * luz), alfa);
	}

	private static int c(float valor) {
		return Mth.clamp((int) valor, 0, 255);
	}

	/** Un aro de luz entre dos radios, de las dos caras. */
	private static void aro(VertexConsumer v, Matrix4f m, float desde, float hasta, float t, int r, int g, int b, int alfa) {
		for (int j = 0; j < GAJOS; j++) {
			float a0 = Mth.TWO_PI * j / GAJOS, a1 = Mth.TWO_PI * (j + 1) / GAJOS;
			float o0 = onda(a0, t), o1 = onda(a1, t);
			float x00 = Mth.cos(a0) * desde * o0, y00 = Mth.sin(a0) * desde * o0, x01 = Mth.cos(a0) * hasta * o0, y01 = Mth.sin(a0) * hasta * o0;
			float x10 = Mth.cos(a1) * desde * o1, y10 = Mth.sin(a1) * desde * o1, x11 = Mth.cos(a1) * hasta * o1, y11 = Mth.sin(a1) * hasta * o1;
			v.addVertex(m, x00, y00, 0.01f).setColor(r, g, b, alfa);
			v.addVertex(m, x01, y01, 0.01f).setColor(r, g, b, alfa);
			v.addVertex(m, x11, y11, 0.01f).setColor(r, g, b, alfa);
			v.addVertex(m, x10, y10, 0.01f).setColor(r, g, b, alfa);
			v.addVertex(m, x10, y10, -0.01f).setColor(r, g, b, alfa);
			v.addVertex(m, x11, y11, -0.01f).setColor(r, g, b, alfa);
			v.addVertex(m, x01, y01, -0.01f).setColor(r, g, b, alfa);
			v.addVertex(m, x00, y00, -0.01f).setColor(r, g, b, alfa);
		}
	}

	@Override
	public boolean shouldRender(PortalEspacioEntity portal, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(portal.getBoundingBox().inflate(1));
	}
}
