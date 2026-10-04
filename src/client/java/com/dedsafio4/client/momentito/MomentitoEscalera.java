package com.dedsafio4.client.momentito;

import com.dedsafio4.momentito.Escena2;
import com.dedsafio4.momentito.MomentitoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.Map;

import static com.dedsafio4.client.momentito.MomentitoRenderer.caja;
import static com.dedsafio4.client.momentito.MomentitoRenderer.color;
import static com.dedsafio4.client.momentito.MomentitoRenderer.parte;
import static com.dedsafio4.client.momentito.MomentitoRenderer.tex;
import static com.dedsafio4.client.momentito.MomentitoRenderer.uvSkin;

/**
 * Dibuja /momentito 2 ("La escalera", como en Escalera.html): el héroe con la skin del momentito 1 y la cara
 * que cambia (decidido, triste, llorando, sorpresa...), los 5 compañeros caídos en los escalones (con casco y
 * espadas), el esqueleto acostado arriba y el amuleto con el Cristal del Desierto. La escalera NO se dibuja:
 * la construye el jugador. Las cuentas del movimiento están en Escena2.
 */
final class MomentitoEscalera {
	private static final float P = 1 / 16f, PX = 0.0175f, CADENA = 0.26f, MEDIO_CRISTAL = 8 * PX;
	private static final ResourceLocation HEROE = tex("heroe"), BLANCO = tex("blanco"), CRISTAL = tex("cristal"),
			ESQUELETO = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
	private static final ResourceLocation[] COMPANEROS = {tex("companero1"), tex("companero2"), tex("companero3"), tex("companero4"), tex("companero5")};

	private static final Map<Character, Integer> PALETA = Map.of('H', 0x381B00, 'S', 0xEBAF7A, 'W', 0xFFFFFF, 'B', 0x00008A,
			'M', 0xE99E5E, 'D', 0x6A2E14, 'L', 0xC98D5C, 'T', 0x7FD0FF);
	/** Las caras del héroe (8 × 8; las 3 filas de arriba son siempre pelo). */
	private static final Map<String, String[]> CARAS = Map.of(
			"normal", new String[]{"HHHSHSHH", "HWBSSBWH", "HWBSSBWH", "SSSSSSSS", "SSSMMSSS"},
			"parpadeo", new String[]{"HHHSHSHH", "HSSSSSSH", "HLLSSLLH", "SSSSSSSS", "SSSMMSSS"},
			"decidido", new String[]{"HSHHHHSH", "HLBSSBLH", "HWBSSBWH", "SSSSSSSS", "SSMMMMSS"},
			"triste", new String[]{"HHSHHSHH", "HLLSSLLH", "HWBSSBWH", "SSSMMSSS", "SSMSSMSS"},
			"llorando", new String[]{"HHSHHSHH", "HSSSSSSH", "HLLSSLLH", "STSMMSTS", "STMSSMTS"},
			"sorpresa", new String[]{"HWWSSWWH", "HWBSSBWH", "HWBSSBWH", "SSSDDSSS", "SSSDDSSS"},
			"asombro", new String[]{"HSHSSHSH", "HWBSSBWH", "HWBSSBWH", "SSSSSSSS", "SSMDDMSS"});
	/** Solo la cara de adelante (+z) de una caja. */
	private static final float[][] SOLO_FRENTE = {null, null, null, null, {0.5f, 0.5f, 0.5f, 0.5f}, null};

	private final MomentitoRenderer renderer;
	private MomentitoEntity m;

	MomentitoEscalera(MomentitoRenderer renderer) {
		this.renderer = renderer;
	}

	void render(MomentitoEntity m, float parcial, PoseStack pose, MultiBufferSource buffers) {
		this.m = m;
		float t = Mth.clamp(m.tiempoEscena(parcial), 0, Escena2.T);
		Escena2.Pose p = Escena2.pose(t);
		pose.pushPose();
		// La escalera sube hacia donde miraba el que escribió el comando (ver MomentitoEntity.empezarEscalera).
		pose.mulPose(Axis.YP.rotationDegrees(180 - m.getYRot()));
		heroe(p, pose, buffers);
		for (int i = 0; i < Escena2.COMP.length; i++) companero(i, pose, buffers);
		esqueleto(p, pose, buffers);
		amuleto(t, p, pose, buffers);
		pose.popPose();
	}

	/** La luz del mundo en un punto de la escena (la escalera es alta: abajo y arriba no hay la misma luz). */
	private int luz(float x, float y, float z) {
		Vector3f v = new Vector3f(x, y, z).rotateY((180 - m.getYRot()) * Mth.DEG_TO_RAD);
		return LevelRenderer.getLightColor(m.level(), BlockPos.containing(m.getX() + v.x, m.getY() + v.y, m.getZ() + v.z));
	}

	private static void rotar(PoseStack pose, float[] r) {
		if (r[0] != 0) pose.mulPose(Axis.XP.rotation(r[0]));
		if (r[1] != 0) pose.mulPose(Axis.YP.rotation(r[1]));
		if (r[2] != 0) pose.mulPose(Axis.ZP.rotation(r[2]));
	}

	// ---------------------------------------------------------------- El héroe

	private void heroe(Escena2.Pose p, PoseStack pose, MultiBufferSource buffers) {
		int luz = luz(p.heroe.x, p.heroe.y + 1, p.heroe.z);
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(HEROE));
		pose.pushPose();
		pose.translate(p.heroe.x, p.heroe.y, p.heroe.z);
		pose.mulPose(Axis.YP.rotation(p.heroeGiro));
		parte(vc, pose, -2, 12, 0, p.piernaD[0], p.piernaD[1], p.piernaD[2], -6, 4, 12, 4, 0, 16, 0, 32, 0.5f, luz);
		parte(vc, pose, 2, 12, 0, p.piernaI[0], p.piernaI[1], p.piernaI[2], -6, 4, 12, 4, 16, 48, 0, 48, 0.5f, luz);
		pose.pushPose();
		pose.translate(0, 12 * P, 0);
		rotar(pose, p.torso);
		parte(vc, pose, 0, 12, 0, p.cabeza[0], p.cabeza[1], p.cabeza[2], 4, 8, 8, 8, 0, 0, 32, 0, 1, luz);
		parte(vc, pose, 0, 6, 0, 0, 0, 0, 0, 8, 12, 4, 16, 16, 16, 32, 0.5f, luz);
		parte(vc, pose, -6, 10, 0, p.brazoD[0], p.brazoD[1], p.brazoD[2], -4, 4, 12, 4, 40, 16, 40, 32, 0.5f, luz);
		parte(vc, pose, 6, 10, 0, p.brazoI[0], p.brazoI[1], p.brazoI[2], -4, 4, 12, 4, 32, 48, 48, 48, 0.5f, luz);
		// La cara (pegada adelante de la cabeza), según la expresión de este momento.
		pose.pushPose();
		pose.translate(0, 12 * P, 0);
		rotar(pose, p.cabeza);
		VertexConsumer cara = buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO));
		String[] filas = CARAS.getOrDefault(p.expresion, CARAS.get("normal"));
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				int rgb = PALETA.get(y < 3 ? 'H' : filas[y - 3].charAt(x));
				caja(cara, pose.last(), (x - 3.5f) * P, (7.5f - y) * P, 4.02f * P, P, P, 0, SOLO_FRENTE,
						rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255, luz);
			}
		}
		pose.popPose();
		pose.popPose();
		pose.popPose();
	}

	// ---------------------------------------------------------------- Los compañeros caídos

	/** Una parte sin capa de afuera (pivote en píxeles, giro solo en Z). */
	private static void pieza(VertexConsumer vc, PoseStack pose, float px, float py, float rz, float oy, float w, float h, float d,
							  float[][] uv, int luz) {
		pose.pushPose();
		pose.translate(px * P, py * P, 0);
		if (rz != 0) pose.mulPose(Axis.ZP.rotation(rz));
		caja(vc, pose.last(), 0, oy * P, 0, w * P, h * P, d * P, uv, 255, 255, 255, 255, luz);
		pose.popPose();
	}

	private void companero(int i, PoseStack pose, MultiBufferSource buffers) {
		float[] c = Escena2.COMP[i];
		float x = c[0], z = c[1], lado = c[2], abre = c[3], arriba = Escena2.treadTop(z);
		int luz = luz(x, arriba + 0.5f, z);
		pose.pushPose();
		pose.translate(x, arriba + 0.25f, z);
		pose.mulPose(Axis.ZP.rotation(lado * Mth.HALF_PI));
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(COMPANEROS[i]));
		pieza(vc, pose, 0, 24, abre * 0.15f, 4, 8, 8, 8, uvSkin(0, 0, 8, 8, 8), luz);
		pieza(vc, pose, 0, 18, 0, 0, 8, 12, 4, uvSkin(16, 16, 8, 12, 4), luz);
		pieza(vc, pose, -6, 22, abre * 0.5f, -4, 4, 12, 4, uvSkin(40, 16, 4, 12, 4), luz);
		pieza(vc, pose, 6, 22, -abre * 0.3f, -4, 4, 12, 4, uvSkin(32, 48, 4, 12, 4), luz);
		pieza(vc, pose, -2, 12, -abre * 0.15f, -6, 4, 12, 4, uvSkin(0, 16, 4, 12, 4), luz);
		pieza(vc, pose, 2, 12, abre * 0.2f, -6, 4, 12, 4, uvSkin(16, 48, 4, 12, 4), luz);
		if (Escena2.CASCO[i]) {
			pose.pushPose();
			pose.translate(0, 24 * P, 0);
			pose.mulPose(Axis.ZP.rotation(abre * 0.15f));
			color(buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO)), pose.last(), 0, 6.6f * P, 0, 9 * P, 4.5f * P, 9 * P, 0xD8D8D8, 255, luz);
			pose.popPose();
		}
		pose.popPose();
		if (Escena2.ESPADA[i]) {
			pose.pushPose();
			pose.translate(x - lado * 2.4f, arriba + 0.02f, z);
			pose.mulPose(Axis.XP.rotation(-Mth.HALF_PI));
			pose.mulPose(Axis.ZP.rotation(0.6f * lado));
			VertexConsumer liso = buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO));
			color(liso, pose.last(), 0, 0.38f, 0, 0.08f, 0.62f, 0.025f, 0xD8D8D8, 255, luz);
			color(liso, pose.last(), 0, 0.06f, 0, 0.24f, 0.05f, 0.04f, 0x3A2A1A, 255, luz);
			color(liso, pose.last(), 0, -0.06f, 0, 0.05f, 0.18f, 0.04f, 0x6B4A2C, 255, luz);
			pose.popPose();
		}
	}

	// ---------------------------------------------------------------- El esqueleto

	/** Las caras de una caja en una textura de 64 × 32 (como la del esqueleto de Minecraft). */
	private static float[][] uv32(float u, float v, float w, float h, float d) {
		float[][] r = uvSkin(u, v, w, h, d);
		for (float[] q : r) {
			q[1] *= 2;
			q[3] *= 2;
		}
		return r;
	}

	private void esqueleto(Escena2.Pose p, PoseStack pose, MultiBufferSource buffers) {
		int luz = luz(1, Escena2.STAIR_H + 0.5f, Escena2.SKZ);
		pose.pushPose();
		pose.translate(1.05f, Escena2.STAIR_H + 2.2f * P, Escena2.SKZ);
		pose.mulPose(Axis.YP.rotation(Mth.HALF_PI));
		pose.mulPose(Axis.XP.rotation(-Mth.HALF_PI));
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(ESQUELETO));
		pieza(vc, pose, 0, 24, 0.35f, 4, 8, 8, 8, uv32(0, 0, 8, 8, 8), luz);
		pieza(vc, pose, 0, 18, 0, 0, 8, 12, 4, uv32(16, 16, 8, 12, 4), luz);
		pieza(vc, pose, -5, 22, -0.25f, -4, 2, 12, 2, uv32(40, 16, 2, 12, 2), luz);
		pieza(vc, pose, 5, 22, p.esqueletoBrazo, -4, 2, 12, 2, uv32(40, 16, 2, 12, 2), luz);
		pieza(vc, pose, -2, 12, -0.1f, -6, 2, 12, 2, uv32(0, 16, 2, 12, 2), luz);
		pieza(vc, pose, 2, 12, 0.18f, -6, 2, 12, 2, uv32(0, 16, 2, 12, 2), luz);
		pose.popPose();
	}

	// ---------------------------------------------------------------- El amuleto

	private void amuleto(float t, Escena2.Pose p, PoseStack pose, MultiBufferSource buffers) {
		int luz = luz(p.amuleto.x, p.amuleto.y + 0.3f, p.amuleto.z);
		pose.pushPose();
		pose.translate(p.amuleto.x, p.amuleto.y, p.amuleto.z);
		pose.mulPose(Axis.XP.rotation(p.amuletoGiro[0]));
		pose.mulPose(Axis.YP.rotation(p.amuletoGiro[1]));
		pose.mulPose(Axis.ZP.rotation(p.amuletoGiro[2]));
		color(buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO)), pose.last(), 0, -CADENA / 2, 0, 0.012f, CADENA, 0.012f, 0xF2B632, 255, luz);
		pose.pushPose();
		pose.translate(0, -CADENA - MEDIO_CRISTAL, 0);
		// El cristal brilla más cuando el héroe se acerca y muchísimo cuando lo levanta.
		float brillo = 0.3f + 0.4f * p.pulso + 0.6f * p.cerca + 1.5f * p.levanta;
		int luzCristal = brillo > 0.9f ? LightTexture.FULL_BRIGHT : Math.max(luz, LightTexture.pack(Math.min(15, (int) (brillo * 16)), 15));
		VertexConsumer cr = buffers.getBuffer(RenderType.entityCutoutNoCull(CRISTAL));
		for (float[] c : renderer.cristal) {
			float[][] uv = {{c[2], c[3], c[2], c[3]}, {c[2], c[3], c[2], c[3]}, {c[2], c[3], c[2], c[3]},
					{c[2], c[3], c[2], c[3]}, {c[2], c[3], c[2], c[3]}, {c[2], c[3], c[2], c[3]}};
			caja(cr, pose.last(), c[0] * PX, c[1] * PX, 0, PX, PX, PX * 1.6f, uv, 255, 255, 255, 255, luzCristal);
		}
		// El aura violeta alrededor del cristal.
		float alfa = 0.1f * p.cerca + 0.3f * p.levanta;
		if (alfa > 0.005f) aura(buffers.getBuffer(RenderType.entityTranslucentEmissive(BLANCO)), pose.last(),
				0.2f + 0.35f * p.levanta + 0.05f * p.pulso, (int) (alfa * 255));
		pose.popPose();
		pose.popPose();
	}

	private static void aura(VertexConsumer vc, PoseStack.Pose p, float radio, int alfa) {
		int lat = 12, lon = 16;
		for (int i = 0; i < lat; i++) {
			for (int j = 0; j < lon; j++) {
				float[][] q = {punto(i, j, lat, lon), punto(i + 1, j, lat, lon), punto(i + 1, j + 1, lat, lon), punto(i, j + 1, lat, lon)};
				for (float[] v : q) {
					vc.addVertex(p, v[0] * radio, v[1] * radio, v[2] * radio).setColor(0xA2, 0x4B, 0xFF, alfa).setUv(0.5f, 0.5f)
							.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, v[0], v[1], v[2]);
				}
			}
		}
	}

	private static float[] punto(int i, int j, int lat, int lon) {
		float a = Mth.PI * i / lat, b = Mth.TWO_PI * j / lon;
		return new float[]{Mth.sin(a) * Mth.cos(b), Mth.cos(a), Mth.sin(a) * Mth.sin(b)};
	}
}
