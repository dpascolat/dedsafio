package com.dedsafio4.client.lagartos;

import com.dedsafio4.lagartos.LagartoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de los lagartos, armado con las medidas del diseño (Mira.html): en píxeles, con la Y
 * hacia arriba y mirando al frente (+Z), igual que en el archivo. Las piezas llevan su color en
 * los vértices, así que la textura es blanca.
 */
public class LagartoModelo extends EntityModel<LagartoEntity> {
	/** 1 píxel del diseño = 1/16 de bloque. */
	private static final float ESCALA = 1f;
	private static final int NEGRO = 0xFF161616;

	private record Caja(float ancho, float alto, float fondo, float x, float y, float z, int color) {}

	/** Un hueso: su pivote, su rotación y lo que cuelga de él. */
	private static final class Hueso {
		final float px, py, pz;
		float rx, ry, rz;
		final List<Caja> cajas = new ArrayList<>();
		final List<Hueso> hijos = new ArrayList<>();

		Hueso(float px, float py, float pz) {
			this.px = px;
			this.py = py;
			this.pz = pz;
		}

		Hueso caja(float ancho, float alto, float fondo, float x, float y, float z, int color) {
			cajas.add(new Caja(ancho, alto, fondo, x, y, z, 0xFF000000 | color));
			return this;
		}

		Hueso hijo(Hueso hueso) {
			hijos.add(hueso);
			return hueso;
		}
	}

	private final Hueso raiz = new Hueso(0, 0, 0);
	private final Hueso torso, cuello, caderaDer, caderaIzq, rodillaDer, rodillaIzq, hombroDer, hombroIzq;
	/** Rebote del cuerpo, en píxeles. */
	private float alturaRaiz;

	public LagartoModelo(LagartoEntity.Variante variante) {
		int escamas = variante.escamas, oscuro = variante.oscuro, panza = variante.panza;
		int cresta = variante.cresta, garras = variante.garras;

		raiz.caja(8, 4, 6, 0, 13, 0, escamas);   // pelvis

		// --- Piernas ---
		caderaDer = pierna(-1, escamas, oscuro, garras);
		caderaIzq = pierna(1, escamas, oscuro, garras);
		rodillaDer = caderaDer.hijos.get(0);
		rodillaIzq = caderaIzq.hijos.get(0);

		// --- Torso ---
		torso = raiz.hijo(new Hueso(0, 14.5f, 0));
		torso.rx = 0.3f;
		torso.caja(8, 11, 6, 0, 5.5f, 0, escamas)
				.caja(6, 9, 0.2f, 0, 5, 3.1f, panza)
				.caja(8.02f, 1, 6.02f, 0, 3, 0, oscuro)
				.caja(8.02f, 1, 6.02f, 0, 7, 0, oscuro);
		for (int i = 0; i < 4; i++) {
			if (variante == LagartoEntity.Variante.REPTISAURIO_ARQUERO) {
				torso.caja(1.4f, 1.4f, 3 + 0.6f * i, 0, 2 + 2.6f * i, -4.5f - 0.3f * i, cresta);
			} else {
				torso.caja(1, 1.6f, 1.4f, 0, 2 + 2.6f * i, -3.7f, cresta);
			}
		}

		// --- Cuello y cabeza ---
		cuello = torso.hijo(new Hueso(0, 11, 1));
		cuello.caja(7, 6, 7, 0, 3, 1, escamas)
				.caja(5, 3, 5, 0, 1.5f, 7, escamas)
				.caja(5.02f, 1, 5.02f, 0, 0.3f, 7, oscuro)
				.caja(6, 1, 6, 0, 0.3f, 1, panza)
				.caja(0.2f, 2, 2, -3.6f, 4.2f, 2.5f, garras)
				.caja(0.2f, 2, 2, 3.6f, 4.2f, 2.5f, garras)
				.caja(0.3f, 1, 1, -3.65f, 4.2f, 3, NEGRO)
				.caja(0.3f, 1, 1, 3.65f, 4.2f, 3, NEGRO)
				.caja(1, 0.6f, 0.6f, -1.3f, 2.6f, 9.55f, NEGRO)
				.caja(1, 0.6f, 0.6f, 1.3f, 2.6f, 9.55f, NEGRO);
		if (variante == LagartoEntity.Variante.REPTISAURIO_ARQUERO) {
			// Cresta alta en la cabeza.
			float[][] crestaAlta = {{5, 8.5f, 3}, {4, 8, 1}, {3, 7.5f, -1}, {2, 7, -3}};
			for (float[] c : crestaAlta) cuello.caja(1.4f, c[0], 2, 0, c[1], c[2], cresta);
		} else {
			cuello.caja(1, 2.5f, 2, 0, 7.2f, 2.5f, cresta)
					.caja(1, 2, 2, 0, 6.8f, 0, cresta)
					.caja(1, 1.5f, 2, 0, 6.5f, -2, cresta);
		}
		if (variante == LagartoEntity.Variante.REPTISAURIO_GUERRERO) {
			for (int lado = -1; lado <= 1; lado += 2) {
				Hueso cuerno = cuello.hijo(new Hueso(lado * 3, 6, -1));
				cuerno.rz = lado * -0.5f;
				cuerno.rx = -0.5f;
				cuerno.caja(1.6f, 3, 1.6f, 0, 1.5f, 0, cresta).caja(1, 2, 1, 0, 4, 0, garras);
			}
		}

		// --- Brazos ---
		hombroDer = brazo(-1, variante, escamas, oscuro, cresta, garras);
		hombroIzq = brazo(1, variante, escamas, oscuro, cresta, garras);
	}

	/** Una pierna entera; devuelve la cadera (la rodilla queda como su primer hijo). */
	private Hueso pierna(int lado, int escamas, int oscuro, int garras) {
		Hueso cadera = raiz.hijo(new Hueso(lado * 3.2f, 12.5f, 0));
		cadera.caja(3, 7, 4, 0, -3, 0.3f, escamas).caja(3.02f, 1, 4.02f, 0, -2, 0.3f, oscuro);
		Hueso rodilla = cadera.hijo(new Hueso(0, -6.5f, 0));
		rodilla.caja(2.6f, 5, 2.6f, 0, -2.5f, -0.4f, escamas).caja(3.6f, 1.5f, 6, 0, -5.25f, 1.2f, oscuro);
		for (float x : new float[]{-1.1f, 0, 1.1f}) rodilla.caja(0.8f, 0.8f, 1.2f, x, -5.6f, 4.8f, garras);
		return cadera;
	}

	private Hueso brazo(int lado, LagartoEntity.Variante variante, int escamas, int oscuro, int cresta, int garras) {
		Hueso hombro = torso.hijo(new Hueso(lado * 5, 9, 1));
		hombro.caja(2, 8, 2, 0, -4, 0, escamas)
				.caja(2.02f, 1, 2.02f, 0, -2, 0, oscuro)
				.caja(1.8f, 7, 1.8f, 0, -11.5f, 0, oscuro)
				.caja(3, 2, 3, 0, -16, 0, escamas);
		boolean conLanza = variante == LagartoEntity.Variante.REPTISAURIO_LANZA;
		boolean conArco = variante == LagartoEntity.Variante.REPTISAURIO_ARQUERO;
		for (float x : new float[]{-1, 0, 1}) {
			if (conLanza) hombro.caja(0.9f, 3, 0.9f, x, -18.5f, 0.8f, garras);
			else hombro.caja(0.7f, 2, 0.7f, x, -18, 0.8f, garras);
		}
		if (conLanza) {
			hombro.caja(3, 3, 3.2f, lado * 0.3f, 0.3f, 0, cresta);
			for (int i = -1; i <= 1; i += 2) hombro.caja(0.8f, 2, 0.8f, i * 0.6f, 2.6f, i * 0.8f, garras);
			// La lanza va en una sola mano: asta larga, agarre oscuro y punta de hueso.
			if (lado < 0) {
				Hueso lanza = hombro.hijo(new Hueso(0, -16, 0));
				lanza.rx = -0.35f;
				lanza.caja(1.1f, 34, 1.1f, 0, 0, 0, oscuro);
				lanza.caja(1.5f, 3, 1.5f, 0, -2, 0, cresta);
				lanza.caja(1.6f, 4, 1.6f, 0, 15, 0, cresta);
				lanza.caja(1.2f, 5, 1.2f, 0, 19.5f, 0, garras);
				lanza.caja(0.6f, 3, 0.6f, 0, 23, 0, garras);
			}
		}
		if (conArco && lado < 0) {
			// El arco: la madera curvada y la cuerda.
			Hueso arco = hombro.hijo(new Hueso(0, -16, 1));
			arco.rx = -0.25f;
			arco.caja(1.2f, 10, 1.2f, 0, 0, 0, oscuro);
			arco.caja(1.2f, 3, 1.2f, 0, 5.6f, -1, oscuro);
			arco.caja(1.2f, 3, 1.2f, 0, -5.6f, -1, oscuro);
			arco.caja(0.4f, 13, 0.4f, 0, 0, 1.2f, garras);
		}
		return hombro;
	}

	@Override
	public void setupAnim(LagartoEntity lagarto, float limbSwing, float limbSwingAmount, float ageInTicks,
						  float netHeadYaw, float headPitch) {
		// 0 = quieto (encorvado, brazos colgando), 1 = corriendo (brazos al frente).
		float corre = Math.min(1f, limbSwingAmount * 2.5f);
		float fase = limbSwing * 0.7f;
		float b = Mth.sin(ageInTicks * 0.1f);           // respiración
		float rebote = Mth.abs(Mth.cos(fase)) * 0.8f;

		caderaDer.rx = mezcla(-0.35f, -Mth.sin(fase) * 0.85f, corre);
		caderaIzq.rx = mezcla(-0.35f, -Mth.sin(fase + Mth.PI) * 0.85f, corre);
		rodillaDer.rx = mezcla(0.55f, Math.max(0f, Mth.cos(fase)) * 1.1f, corre);
		rodillaIzq.rx = mezcla(0.55f, Math.max(0f, Mth.cos(fase + Mth.PI)) * 1.1f, corre);

		hombroDer.rx = mezcla(-0.75f + b * 0.04f, -1.65f + Mth.sin(fase) * 0.12f, corre);
		hombroIzq.rx = mezcla(-0.75f + b * 0.04f, -1.65f + Mth.sin(fase + Mth.PI) * 0.12f, corre);
		hombroDer.rz = mezcla(-0.12f, 0f, corre);
		hombroIzq.rz = mezcla(0.12f, 0f, corre);

		torso.rx = mezcla(0.55f + b * 0.02f, 0.3f + Mth.abs(Mth.sin(fase)) * 0.05f, corre);
		torso.ry = mezcla(0f, Mth.sin(fase) * 0.06f, corre);
		// La cabeza sigue a lo que mira, además de la postura.
		cuello.rx = mezcla(-0.5f - b * 0.03f, -0.25f - Mth.abs(Mth.sin(fase)) * 0.05f, corre)
				+ headPitch * Mth.DEG_TO_RAD * 0.5f;
		cuello.ry = netHeadYaw * Mth.DEG_TO_RAD * 0.5f;
		alturaRaiz = mezcla(-1f + b * 0.2f, rebote, corre);
	}

	private static float mezcla(float quieto, float corriendo, float cuanto) {
		return quieto + (corriendo - quieto) * cuanto;
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int luz, int overlay, int color) {
		pose.pushPose();
		// Del espacio de Minecraft (Y abajo, frente -Z) al del diseño (Y arriba, frente +Z, pies en 0).
		pose.translate(0f, 1.501f, 0f);
		pose.scale(ESCALA / 16f, -ESCALA / 16f, -ESCALA / 16f);
		pose.translate(0f, alturaRaiz, 0f);
		dibujar(raiz, pose, buffer, luz, overlay, color);
		pose.popPose();
	}

	private void dibujar(Hueso hueso, PoseStack pose, VertexConsumer buffer, int luz, int overlay, int tinte) {
		pose.pushPose();
		pose.translate(hueso.px, hueso.py, hueso.pz);
		if (hueso.rz != 0) pose.mulPose(Axis.ZP.rotation(hueso.rz));
		if (hueso.ry != 0) pose.mulPose(Axis.YP.rotation(hueso.ry));
		if (hueso.rx != 0) pose.mulPose(Axis.XP.rotation(hueso.rx));
		PoseStack.Pose p = pose.last();
		for (Caja caja : hueso.cajas) dibujarCaja(p, buffer, caja, luz, overlay, tinte);
		for (Hueso hijo : hueso.hijos) dibujar(hijo, pose, buffer, luz, overlay, tinte);
		pose.popPose();
	}

	private static final float[][] CARAS = {
			{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}
	};

	private static void dibujarCaja(PoseStack.Pose p, VertexConsumer buffer, Caja caja, int luz, int overlay, int tinte) {
		float x0 = caja.x() - caja.ancho() / 2, x1 = caja.x() + caja.ancho() / 2;
		float y0 = caja.y() - caja.alto() / 2, y1 = caja.y() + caja.alto() / 2;
		float z0 = caja.z() - caja.fondo() / 2, z1 = caja.z() + caja.fondo() / 2;
		int color = FastColor.ARGB32.multiply(caja.color(), tinte);
		float[][][] esquinas = {
				{{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}},   // frente (+Z)
				{{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}},   // atrás (-Z)
				{{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}},   // derecha (+X)
				{{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}},   // izquierda (-X)
				{{x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}},   // arriba (+Y)
				{{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}},   // abajo (-Y)
		};
		for (int cara = 0; cara < 6; cara++) {
			float[] n = CARAS[cara];
			for (float[] v : esquinas[cara]) {
				buffer.addVertex(p, v[0], v[1], v[2]).setColor(color).setUv(0.5f, 0.5f)
						.setOverlay(overlay).setLight(luz).setNormal(p, n[0], n[1], n[2]);
			}
		}
	}
}
