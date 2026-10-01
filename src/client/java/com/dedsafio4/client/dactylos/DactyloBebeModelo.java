package com.dedsafio4.client.dactylos;

import com.dedsafio4.dactylos.DactyloBebeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Modelo del Dáctylo Bebé con las cajas del diseño (en píxeles, y hacia arriba, mirando a +z).
 * Aletea sin parar (las alas y las puntas), el cuerpo sube y baja, y la cabeza se mueve un poco y
 * levanta el pico al escupir.
 */
public class DactyloBebeModelo extends EntityModel<DactyloBebeEntity> {
	// Colores de la paleta (textures/entity/dactylo_bebe.png).
	static final int PIEL = 0, OSCURA = 1, PANZA = 2, MEMBRANA = 3, PICO = 4, OJO = 5, BRILLO = 6, TINTA = 7;

	/** Una caja: color, tamaño (ancho, alto, profundo) y centro. */
	record Caja(int color, float w, float h, float d, float x, float y, float z) {}

	private static final List<Caja> CUERPO = List.of(
			new Caja(PIEL, 5, 5, 7, 0, 6, 0), new Caja(PANZA, 4, 4, 6, 0, 5.4f, 0.4f), new Caja(OSCURA, 3, 0.6f, 5, 0, 8.8f, -0.5f),
			new Caja(PIEL, 2, 2, 5, 0, 6.5f, -6), new Caja(PANZA, 3.4f, 0.6f, 2.6f, 0, 6.5f, -9.5f),
			new Caja(OSCURA, 1.2f, 2.5f, 1.2f, -1.5f, 2.3f, -1), new Caja(OSCURA, 1.2f, 2.5f, 1.2f, 1.5f, 2.3f, -1),
			new Caja(PICO, 1.6f, 0.6f, 2, -1.5f, 0.8f, -0.6f), new Caja(PICO, 1.6f, 0.6f, 2, 1.5f, 0.8f, -0.6f));
	private static final float[] PIVOTE_CABEZA = {0, 9.5f, 4.2f};
	private static final List<Caja> CABEZA = List.of(
			new Caja(PIEL, 5, 4.5f, 5, 0, 0, 0),
			new Caja(PICO, 2.4f, 1.6f, 5, 0, -0.6f, 4.6f), new Caja(PICO, 2, 0.8f, 4, 0, -1.8f, 4),
			new Caja(OSCURA, 1.2f, 1.6f, 3, 0, 3, -1.2f), new Caja(OSCURA, 1.2f, 1.2f, 3, 0, 2.6f, -3.8f), new Caja(PANZA, 1.2f, 0.8f, 2, 0, 2.2f, -5.8f),
			new Caja(OJO, 0.4f, 1.6f, 1.6f, -2.7f, 0.6f, 1.2f), new Caja(OJO, 0.4f, 1.6f, 1.6f, 2.7f, 0.6f, 1.2f),
			new Caja(BRILLO, 0.2f, 0.6f, 0.6f, -3, 1, 1.6f), new Caja(BRILLO, 0.2f, 0.6f, 0.6f, 3, 1, 1.6f),
			new Caja(PANZA, 0.3f, 0.8f, 1.4f, -2.65f, -1.2f, 1.2f), new Caja(PANZA, 0.3f, 0.8f, 1.4f, 2.65f, -1.2f, 1.2f));

	private static List<Caja> ala(int s) {
		return List.of(new Caja(OSCURA, 8, 1, 1.4f, s * 4, 0, 2.2f), new Caja(MEMBRANA, 8, 0.5f, 5, s * 4, -0.1f, -1),
				new Caja(PANZA, 5, 0.55f, 1.4f, s * 3.5f, -0.05f, 0.4f));
	}

	private static List<Caja> punta(int s) {
		return List.of(new Caja(OSCURA, 6, 0.9f, 1.2f, s * 3, 0, 2.2f), new Caja(MEMBRANA, 6, 0.5f, 3.6f, s * 2.6f, -0.1f, -0.2f),
				new Caja(PICO, 1, 0.6f, 1.4f, s * 6.2f, 0, 3.2f));
	}

	private static final List<Caja> ALA_IZQ = ala(-1), ALA_DER = ala(1), PUNTA_IZQ = punta(-1), PUNTA_DER = punta(1);

	private float t, cabezaYaw, cabezaPitch;

	@Override
	public void setupAnim(DactyloBebeEntity dactylo, float paso, float cuanto, float edad, float yaw, float pitch) {
		t = edad / 20f;
		// En el diseño la y va hacia arriba: el giro de la cabeza va al revés que en los modelos comunes.
		cabezaYaw = Mth.sin(t * 0.9f) * 0.15f - yaw * Mth.DEG_TO_RAD;
		cabezaPitch = pitch * Mth.DEG_TO_RAD + (edad - dactylo.ultimoDisparo < 5 ? -0.25f : 0);
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer vc, int luz, int overlay, int color) {
		float aleteo = Mth.sin(t * 7);
		pose.pushPose();
		// De píxeles del diseño (y arriba, frente +z) al espacio del modelo de Minecraft.
		pose.translate(0, 1.5, 0);
		pose.scale(1 / 16f, -1 / 16f, -1 / 16f);
		pose.translate(0, -aleteo * 0.5f, 0);
		pose.mulPose(Axis.XP.rotation(Mth.sin(t * 1.3f) * 0.05f));
		dibujar(pose, vc, CUERPO, luz, overlay, color);

		pose.pushPose();
		pose.translate(PIVOTE_CABEZA[0], PIVOTE_CABEZA[1], PIVOTE_CABEZA[2]);
		pose.mulPose(Axis.XP.rotation(cabezaPitch));
		pose.mulPose(Axis.YP.rotation(cabezaYaw));
		dibujar(pose, vc, CABEZA, luz, overlay, color);
		pose.popPose();

		for (int s : new int[]{-1, 1}) {
			pose.pushPose();
			pose.translate(s * 2.5f, 7.8f, 0.5f);
			pose.mulPose(Axis.ZP.rotation(s * aleteo * 0.55f));
			dibujar(pose, vc, s < 0 ? ALA_IZQ : ALA_DER, luz, overlay, color);
			pose.translate(s * 8, 0, 0);
			pose.mulPose(Axis.ZP.rotation(s * Mth.sin(t * 7 - 0.6f) * 0.35f));
			dibujar(pose, vc, s < 0 ? PUNTA_IZQ : PUNTA_DER, luz, overlay, color);
			pose.popPose();
		}
		pose.popPose();
	}

	static void dibujar(PoseStack pose, VertexConsumer vc, List<Caja> cajas, int luz, int overlay, int color) {
		PoseStack.Pose p = pose.last();
		for (Caja c : cajas) caja(vc, p, c, luz, overlay, color);
	}

	static void caja(VertexConsumer vc, PoseStack.Pose p, Caja c, int luz, int overlay, int color) {
		float x0 = c.x - c.w / 2, x1 = c.x + c.w / 2, y0 = c.y - c.h / 2, y1 = c.y + c.h / 2, z0 = c.z - c.d / 2, z1 = c.z + c.d / 2;
		float u = ((c.color % 8) * 2 + 1) / 16f, v = ((c.color / 8) * 2 + 1) / 16f;
		cara(vc, p, u, v, luz, overlay, color, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
		cara(vc, p, u, v, luz, overlay, color, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
		cara(vc, p, u, v, luz, overlay, color, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		cara(vc, p, u, v, luz, overlay, color, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
		cara(vc, p, u, v, luz, overlay, color, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		cara(vc, p, u, v, luz, overlay, color, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, float u, float v, int luz, int overlay, int color,
							 float nx, float ny, float nz, float... q) {
		for (int i = 0; i < 12; i += 3) {
			vc.addVertex(p, q[i], q[i + 1], q[i + 2]).setColor(color).setUv(u, v).setOverlay(overlay).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}
}
