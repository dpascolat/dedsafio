package com.dedsafio4.client.trex;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.trex.TRexEntity;
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

import java.util.ArrayList;
import java.util.List;

/**
 * El T-Rex gigante del diseño (40 bloques de alto): cajas con pivotes, en bloques (y arriba, +z el
 * hocico). Camina y ruge con las mismas fórmulas del diseño.
 */
public class TRexRenderer extends EntityRenderer<TRexEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/trex.png");
	// Colores (cada uno tiene 3 tonos en la paleta, para que se vea "de bloques").
	private static final int NARANJA = 0, DORADO = 1, MORADO = 2, GARRA = 3, BLANCO = 4, NEGRO = 5, BOCA = 6;

	private record Caja(float w, float h, float d, float x, float y, float z, int color, float rx) {}

	private static final class Nodo {
		final float x, y, z;
		float rx, ry, rz;
		final List<Caja> cajas = new ArrayList<>();
		final List<Nodo> hijos = new ArrayList<>();

		Nodo(float x, float y, float z, Nodo padre) {
			this.x = x;
			this.y = y;
			this.z = z;
			if (padre != null) padre.hijos.add(this);
		}
	}

	private int cajasHechas;
	private final Nodo raiz, cuerpo, cabeza, mandibula;
	private final Nodo[] hombros = new Nodo[2], manos = new Nodo[2], caderas = new Nodo[2], rodillas = new Nodo[2], tobillos = new Nodo[2];
	private final Nodo[] cola = new Nodo[6];

	private void caja(Nodo n, float w, float h, float d, int color, float x, float y, float z) {
		caja(n, w, h, d, color, x, y, z, 0);
	}

	private void caja(Nodo n, float w, float h, float d, int color, float x, float y, float z, float rx) {
		int tono = (cajasHechas++ * 7 + 3) % 3;
		n.cajas.add(new Caja(w, h, d, x, y, z, color * 3 + tono, rx));
	}

	public TRexRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 8f;
		raiz = new Nodo(0, 0, 5, null);
		cuerpo = new Nodo(0, 0, 0, raiz);
		// Torso y pecho
		caja(cuerpo, 16, 11, 15, NARANJA, 0, 23.5f, 0);
		caja(cuerpo, 12, 1, 13, DORADO, 0, 17.5f, 0.5f);
		caja(cuerpo, 14, 10, 5, NARANJA, 0, 26, 9.5f);
		caja(cuerpo, 10, 8, 0.2f, DORADO, 0, 25.5f, 12.1f);
		for (float z : new float[]{-5.5f, -2, 1.5f, 5}) caja(cuerpo, 16.04f, 4.5f, 1.4f, MORADO, 0, 26.8f, z);
		for (float z : new float[]{-6, -3, 0, 3, 6}) caja(cuerpo, 1.6f, 2.4f, 1.6f, MORADO, 0, 30.2f, z);
		// Cuello
		caja(cuerpo, 10, 6, 5, NARANJA, 0, 31.5f, 12.5f);
		caja(cuerpo, 7, 5, 0.2f, DORADO, 0, 30.8f, 15.1f);
		// Cabeza
		cabeza = new Nodo(0, 32, 13, cuerpo);
		caja(cabeza, 12, 7, 9, NARANJA, 0, 4, 2.5f);
		caja(cabeza, 10, 3, 7, NARANJA, 0, 3, 10.5f);
		caja(cabeza, 10.04f, 0.8f, 1.2f, MORADO, 0, 4.1f, 9);
		caja(cabeza, 3, 1, 5, MORADO, 0, 8, 1.5f);
		for (int s : new int[]{-1, 1}) {
			caja(cabeza, 0.2f, 2.2f, 2.4f, DORADO, s * 6.05f, 5, 4);
			caja(cabeza, 0.25f, 2, 0.7f, NEGRO, s * 6.12f, 5, 4.3f);
			caja(cabeza, 2.2f, 1.2f, 4, MORADO, s * 5.1f, 8, 3.8f);
			caja(cabeza, 1, 0.2f, 1, NEGRO, s * 2.2f, 4.55f, 13.2f);
		}
		float[] zSup = {8, 9.5f, 11, 12.5f};
		for (int i = 0; i < zSup.length; i++) {
			boolean grande = i == 1;
			for (int s : new int[]{-1, 1}) {
				caja(cabeza, grande ? 1.2f : 1, grande ? 3.2f : 2.2f, grande ? 1.2f : 1, BLANCO, s * 4.4f, 1.5f - (grande ? 1.6f : 1.1f), zSup[i]);
			}
		}
		for (float x : new float[]{-2.8f, -1, 1, 2.8f}) caja(cabeza, 1.1f, 2.6f, 1, BLANCO, x, 0.2f, 13.4f);
		// Mandíbula
		mandibula = new Nodo(0, 1.5f, 3, cabeza);
		caja(mandibula, 9.4f, 2, 11, NARANJA, 0, -1, 5.5f);
		caja(mandibula, 8, 0.2f, 10, DORADO, 0, -2.1f, 5.5f);
		caja(mandibula, 8, 0.2f, 9.6f, BOCA, 0, 0.1f, 5.6f);
		float[] zInf = {1.8f, 3.3f, 4.8f, 6.3f, 7.8f};
		for (int i = 0; i < zInf.length; i++) {
			boolean grande = i == 3;
			for (int s : new int[]{-1, 1}) {
				caja(mandibula, grande ? 1.1f : 0.9f, grande ? 2.6f : 1.8f, grande ? 1.1f : 0.9f, BLANCO, s * 4.05f, grande ? 1.3f : 0.9f, zInf[i]);
			}
		}
		for (float x : new float[]{-2.4f, -0.8f, 0.8f, 2.4f}) caja(mandibula, 0.9f, 1.6f, 0.8f, BLANCO, x, 0.8f, 10.3f);
		// Brazos, manos y garras
		for (int i = 0; i < 2; i++) {
			int s = i == 0 ? -1 : 1;
			Nodo hombro = new Nodo(s * 7.6f, 26.5f, 11, cuerpo);
			hombro.rz = s;
			caja(hombro, 3.6f, 3.6f, 3.6f, NARANJA, 0, 0, 0);
			caja(hombro, 2.8f, 6, 2.8f, NARANJA, 0, -3.5f, 0);
			caja(hombro, 2.84f, 1, 2.84f, MORADO, 0, -2.5f, 0);
			Nodo antebrazo = new Nodo(0, -6.3f, 0, hombro);
			antebrazo.rz = -s;
			caja(antebrazo, 2.8f, 2.8f, 5, NARANJA, 0, 0, 2);
			Nodo mano = new Nodo(0, 0, 4.6f, antebrazo);
			caja(mano, 4.4f, 3.4f, 3.4f, NARANJA, 0, 0, 1.4f);
			caja(mano, 4.44f, 1, 1.2f, MORADO, 0, 1.25f, 2.4f);
			for (float x : new float[]{-1.5f, 0, 1.5f}) {
				caja(mano, 1.2f, 1.2f, 2, NARANJA, x, -0.4f, 3.9f);
				caja(mano, 1, 1, 4.2f, GARRA, x, -1.2f, 6.3f, 0.45f);
			}
			hombros[i] = hombro;
			manos[i] = mano;
		}
		// Piernas
		for (int i = 0; i < 2; i++) {
			int s = i == 0 ? -1 : 1;
			Nodo cadera = new Nodo(s * 8, 19, 0, cuerpo);
			caja(cadera, 5, 9, 7, NARANJA, 0, -3.5f, 0.5f);
			caja(cadera, 5.04f, 1.4f, 7.04f, MORADO, 0, -1, 0.5f);
			Nodo rodilla = new Nodo(0, -8, -0.5f, cadera);
			caja(rodilla, 4, 8, 4, NARANJA, 0, -4, 0);
			Nodo tobillo = new Nodo(0, -8, 0, rodilla);
			caja(tobillo, 3.2f, 2, 3.2f, NARANJA, 0, -1, 0);
			caja(tobillo, 6, 2, 8, NARANJA, 0, -2, 2);
			caja(tobillo, 4.6f, 0.2f, 6, DORADO, 0, -0.95f, 2.4f);
			for (float x : new float[]{-2, 0, 2}) caja(tobillo, 1.3f, 1.5f, 3.2f, GARRA, x, -2.1f, 6.8f, 0.3f);
			caderas[i] = cadera;
			rodillas[i] = rodilla;
			tobillos[i] = tobillo;
		}
		// Cola (cadena)
		Nodo padre = cuerpo;
		float[] pos = {0, 24.5f, -7.5f};
		for (int i = 0; i < 6; i++) {
			Nodo seg = new Nodo(pos[0], pos[1], pos[2], padre);
			float w = 13 - 2.2f * i, h = 8 - 1.2f * i;
			caja(seg, w, h, 5.52f, NARANJA, 0, 0, -2.75f);
			if (i < 5) caja(seg, w + 0.04f, h * 0.45f, 1.4f, MORADO, 0, h * 0.28f, -2.75f);
			caja(seg, 1.2f, 1.6f - i * 0.15f, 1.2f, MORADO, 0, h / 2 + 0.7f, -2.75f);
			cola[i] = seg;
			padre = seg;
			pos = new float[]{0, -0.4f, -5.5f};
		}
	}

	/** Pone la pose del momento: caminar (según qué tanto camina) y, encima, el rugido. */
	private void animar(TRexEntity trex, float parcial) {
		float p = Mth.lerp(parcial, trex.faseAntes, trex.fase);
		float m = Mth.lerp(parcial, trex.mezclaAntes, trex.mezcla);
		float t = (trex.tickCount + parcial) / 20f;
		for (int i = 0; i < 2; i++) {
			float ph = p + i * Mth.PI;
			float balanceo = Mth.sin(ph) * 0.42f * m, doblez = Math.max(0, Mth.cos(ph)) * 0.7f * m;
			caderas[i].rx = balanceo - doblez * 0.5f;
			rodillas[i].rx = doblez;
			tobillos[i].rx = -(caderas[i].rx + rodillas[i].rx);
		}
		float cuerpoY = (0.25f - Math.abs(Mth.sin(p)) * 0.5f) * m;
		cuerpo.rz = Mth.sin(p) * 0.02f * m;
		cuerpo.rx = 0;
		cabeza.rx = Mth.sin(p * 2) * 0.04f * m;
		cabeza.ry = Mth.sin(p * 0.5f) * 0.08f * m;
		mandibula.rx = 0.22f + Math.max(0, Mth.sin(t * 0.9f)) * 0.2f;
		for (int i = 0; i < 2; i++) {
			hombros[i].rx = Mth.sin(p + i * Mth.PI) * 0.12f * m;
			manos[i].rx = 0.1f + Math.max(0, Mth.sin(t * 1.3f + i)) * 0.35f;
		}
		for (int i = 0; i < 6; i++) {
			cola[i].ry = Mth.sin(p - i * 0.6f) * 0.09f * m;
			cola[i].rx = 0.04f + Mth.sin(p * 2 - i * 0.5f) * 0.02f * m;
		}
		// Rugido: se planta, abre la boca, barre con la cabeza y tiembla.
		if (trex.rugido >= 0) {
			float tau = (trex.rugido + parcial) / 20f;
			float r = trex.fuerzaRugido(parcial), k = 1 - r;
			float sx = (tau > 0.4f && tau < 2.5f ? 1 : 0) * Mth.sin(tau * 60) * 0.03f;
			for (int i = 0; i < 2; i++) {
				caderas[i].rx *= k;
				rodillas[i].rx *= k;
				tobillos[i].rx = -(caderas[i].rx + rodillas[i].rx);
			}
			cuerpoY = cuerpoY * k - r * 0.6f;
			cuerpo.rx = -r * 0.12f;
			cuerpo.rz = cuerpo.rz * k + sx * 0.3f;
			cabeza.rx = cabeza.rx * k - r * 0.45f + sx;
			cabeza.ry = cabeza.ry * k + Mth.sin(tau * 2.2f) * 0.25f * r;
			mandibula.rx = mandibula.rx * k + r * 0.85f + sx;
			for (int i = 0; i < 2; i++) {
				hombros[i].rx = hombros[i].rx * k - r * 0.6f;
				manos[i].rx = manos[i].rx * k + r * 0.6f + sx * 4;
			}
			for (Nodo seg : cola) {
				seg.rx = seg.rx * k - r * 0.07f;
				seg.ry += sx * 2;
			}
		}
		cuerpoDesplazado = cuerpoY;
	}

	private float cuerpoDesplazado;

	@Override
	public void render(TRexEntity trex, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		animar(trex, parcial);
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, trex.yBodyRotO, trex.yBodyRot)));
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		int overlay = OverlayTexture.NO_OVERLAY;
		pose.translate(raiz.x, raiz.y, raiz.z);
		pose.translate(0, cuerpoDesplazado, 0);
		// El que lo maneja ve desde sus ojos: para él no se dibuja la cabeza (si no, vería el cráneo por dentro).
		var mc = net.minecraft.client.Minecraft.getInstance();
		sinCabeza = mc.player != null && mc.player.getVehicle() == trex && mc.options.getCameraType().isFirstPerson();
		dibujar(pose, vc, cuerpo, luz, overlay);
		pose.popPose();
		super.render(trex, yaw, parcial, pose, buffers, luz);
	}

	private boolean sinCabeza;

	private void dibujar(PoseStack pose, VertexConsumer vc, Nodo n, int luz, int overlay) {
		if (sinCabeza && n == cabeza) return;
		pose.pushPose();
		pose.translate(n.x, n.y, n.z);
		if (n.rx != 0) pose.mulPose(Axis.XP.rotation(n.rx));
		if (n.ry != 0) pose.mulPose(Axis.YP.rotation(n.ry));
		if (n.rz != 0) pose.mulPose(Axis.ZP.rotation(n.rz));
		for (Caja c : n.cajas) {
			pose.pushPose();
			pose.translate(c.x, c.y, c.z);
			if (c.rx != 0) pose.mulPose(Axis.XP.rotation(c.rx));
			caja(vc, pose.last(), c, luz, overlay);
			pose.popPose();
		}
		for (Nodo hijo : n.hijos) dibujar(pose, vc, hijo, luz, overlay);
		pose.popPose();
	}

	private static void caja(VertexConsumer vc, PoseStack.Pose p, Caja c, int luz, int overlay) {
		float x0 = -c.w / 2, x1 = c.w / 2, y0 = -c.h / 2, y1 = c.h / 2, z0 = -c.d / 2, z1 = c.d / 2;
		float u = ((c.color % 8) * 2 + 1) / 16f, v = ((c.color / 8) * 2 + 1) / 16f;
		cara(vc, p, u, v, luz, overlay, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
		cara(vc, p, u, v, luz, overlay, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
		cara(vc, p, u, v, luz, overlay, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		cara(vc, p, u, v, luz, overlay, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
		cara(vc, p, u, v, luz, overlay, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		cara(vc, p, u, v, luz, overlay, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, float u, float v, int luz, int overlay,
							 float nx, float ny, float nz, float... q) {
		for (int i = 0; i < 12; i += 3) {
			vc.addVertex(p, q[i], q[i + 1], q[i + 2]).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	/** Mide mucho más que su caja de choque: se dibuja mientras cualquier parte esté a la vista. */
	@Override
	public boolean shouldRender(TRexEntity trex, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(trex.getBoundingBox().inflate(20, 25, 40));
	}

	@Override
	public ResourceLocation getTextureLocation(TRexEntity trex) {
		return TEXTURA;
	}
}
