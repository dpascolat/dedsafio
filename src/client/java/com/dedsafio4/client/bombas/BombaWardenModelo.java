package com.dedsafio4.client.bombas;

import com.dedsafio4.bombas.BombaWardenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de la Bomba Warden, armado con las medidas del diseño (Bomba Warden.html):
 * cuerpo cúbico con cara de Warden, cuernos, alas de murciélago, mecha encendida atrás
 * y tentáculos de sculk. Cada grupo de piezas lleva su color; la textura es blanca.
 * En el diseño 1 unidad = 1 bloque y el frente mira a +Z; acá el frente es -Z, como en Minecraft.
 */
public class BombaWardenModelo extends EntityModel<BombaWardenEntity> {
	/** Altura de la base del cuerpo en el diseño. */
	private static final float Y = 0.55f;
	/** El suelo del modelo (en píxeles, con la Y hacia abajo). */
	private static final float SUELO = 24f;

	private static final int PIEL = 0xFF0F3A44;
	private static final int OSCURO = 0xFF071D24;
	private static final int HUESO = 0xFF2F6B72;
	private static final int ALMA = 0xFF46F0E6;
	private static final int MECHA = 0xFFC9B48A;
	private static final int CHISPA = 0xFFFFD23A;
	private static final int MEMBRANA = 0xFF14505A;

	/** Lo que brilla se dibuja a plena luz, como la capa brillante del Warden. */
	private static final int LUZ_MAXIMA = 0xF000F0;

	private record Pieza(ModelPart parte, int color, boolean brilla) {}

	private final ModelPart raiz;
	private final List<Pieza> piezas = new ArrayList<>();
	private final List<ModelPart> alaDerecha = new ArrayList<>();
	private final List<ModelPart> alaIzquierda = new ArrayList<>();
	private final float giroAlaDerecha, giroAlaIzquierda;

	public BombaWardenModelo(ModelPart raiz) {
		this.raiz = raiz;
		agregar("piel", PIEL, false);
		agregar("oscuro", OSCURO, false);
		agregar("hueso", HUESO, false);
		agregar("mecha", MECHA, false);
		agregar("alma", ALMA, true);
		agregar("chispa", CHISPA, true);
		for (int lado = -1; lado <= 1; lado += 2) {
			String s = lado > 0 ? "der" : "izq";
			agregar("cuerno_" + s + "_hueso", HUESO, false);
			agregar("cuerno_" + s + "_alma", ALMA, true);
			List<ModelPart> ala = lado > 0 ? alaDerecha : alaIzquierda;
			ala.add(agregar("ala_" + s + "_hueso", HUESO, false));
			ala.add(agregar("ala_" + s + "_membrana", MEMBRANA, false));
			ala.add(agregar("ala_" + s + "_alma", ALMA, true));
		}
		giroAlaDerecha = alaDerecha.get(0).zRot;
		giroAlaIzquierda = alaIzquierda.get(0).zRot;
	}

	private ModelPart agregar(String nombre, int color, boolean brilla) {
		ModelPart parte = raiz.getChild(nombre);
		piezas.add(new Pieza(parte, color, brilla));
		return parte;
	}

	public static LayerDefinition crearCapa() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();

		CubeListBuilder piel = CubeListBuilder.create();
		caja(piel, 0.8f, 0.8f, 0.8f, 0, Y + 0.4f, 0);
		raiz.addOrReplaceChild("piel", piel, PartPose.ZERO);

		CubeListBuilder oscuro = CubeListBuilder.create();
		caja(oscuro, 0.82f, 0.12f, 0.82f, 0, Y + 0.4f, 0);        // banda horizontal
		caja(oscuro, 0.12f, 0.82f, 0.82f, 0, Y + 0.4f, 0);        // banda vertical
		caja(oscuro, 0.7f, 0.06f, 0.7f, 0, Y - 0.03f, 0);         // chapa de abajo
		caja(oscuro, 0.3f, 0.07f, 0.02f, 0, Y + 0.52f, 0.411f);   // boca
		caja(oscuro, 0.22f, 0.22f, 0.08f, 0, Y + 0.4f, -0.44f);   // tapa de la mecha
		raiz.addOrReplaceChild("oscuro", oscuro, PartPose.ZERO);

		CubeListBuilder hueso = CubeListBuilder.create();
		caja(hueso, 0.05f, 0.3f, 0.025f, 0, Y + 0.25f, 0.412f);   // esternón
		for (int i = 0; i < 4; i++) {
			caja(hueso, 0.04f, 0.035f, 0.022f, -0.105f + i * 0.07f, Y + 0.54f, 0.413f);   // dientes
		}
		CubeListBuilder alma = CubeListBuilder.create();
		for (int i = 0; i < 3; i++) {
			caja(alma, 0.34f, 0.05f, 0.02f, 0, Y + 0.15f + i * 0.1f, 0.411f);             // costillas
		}
		caja(alma, 0.12f, 0.1f, 0.02f, -0.24f, Y + 0.62f, 0.411f);   // mejillas (no tiene ojos)
		caja(alma, 0.12f, 0.1f, 0.02f, 0.24f, Y + 0.62f, 0.411f);
		float[][] manchas = {{0.411f, 0.2f, -0.2f}, {0.411f, 0.65f, 0.22f}, {-0.411f, 0.3f, 0.15f}, {-0.411f, 0.62f, -0.18f}};
		for (float[] m : manchas) caja(alma, 0.02f, 0.1f, 0.1f, m[0], Y + m[1], m[2]);
		caja(alma, 0.18f, 0.14f, 0.02f, 0.12f, Y + 0.28f, -0.411f);   // mancha de la espalda

		// Tentáculos de sculk que van atrás, con la punta brillante.
		float[][] tentaculos = {{-0.22f, 0.1f}, {0.2f, 0.05f}, {0.02f, 0f}, {-0.1f, 0.15f}, {0.24f, 0.2f}};
		for (int i = 0; i < tentaculos.length; i++) {
			float x = tentaculos[i][0], y = tentaculos[i][1];
			float largo = 0.2f + (i % 3) * 0.1f;
			caja(hueso, 0.05f, 0.05f, largo, x, Y + y, -0.4f - largo / 2f);
			caja(alma, 0.06f, 0.06f, 0.06f, x, Y + y, -0.4f - largo - 0.03f);
		}
		raiz.addOrReplaceChild("hueso", hueso, PartPose.ZERO);
		raiz.addOrReplaceChild("alma", alma, PartPose.ZERO);

		CubeListBuilder mecha = CubeListBuilder.create();
		caja(mecha, 0.05f, 0.05f, 0.14f, 0, Y + 0.4f, -0.55f);
		caja(mecha, 0.05f, 0.12f, 0.05f, 0, Y + 0.44f, -0.6f);
		caja(mecha, 0.05f, 0.05f, 0.1f, 0, Y + 0.48f, -0.66f);
		raiz.addOrReplaceChild("mecha", mecha, PartPose.ZERO);

		CubeListBuilder chispa = CubeListBuilder.create();
		caja(chispa, 0.1f, 0.1f, 0.1f, 0, Y + 0.48f, -0.74f);
		raiz.addOrReplaceChild("chispa", chispa, PartPose.rotation(0.6f, 0.6f, 0f));

		for (int lado = -1; lado <= 1; lado += 2) {
			String s = lado > 0 ? "der" : "izq";

			// Cuernos ("orejas" del Warden).
			CubeListBuilder cuernoHueso = CubeListBuilder.create();
			cajaLocal(cuernoHueso, 0.14f, 0.14f, 0.14f, lado * 0.07f, 0, 0);
			cajaLocal(cuernoHueso, 0.12f, 0.28f, 0.1f, lado * 0.13f, 0.18f, 0);
			CubeListBuilder cuernoAlma = CubeListBuilder.create();
			cajaLocal(cuernoAlma, 0.08f, 0.18f, 0.07f, lado * 0.17f, 0.39f, 0);
			PartPose poseCuerno = pose(lado * 0.4f, Y + 0.72f, 0, 0, 0, lado * 0.35f);
			raiz.addOrReplaceChild("cuerno_" + s + "_hueso", cuernoHueso, poseCuerno);
			raiz.addOrReplaceChild("cuerno_" + s + "_alma", cuernoAlma, poseCuerno);

			// Alas de murciélago, echadas hacia atrás.
			CubeListBuilder alaHueso = CubeListBuilder.create();
			cajaLocal(alaHueso, 0.7f, 0.06f, 0.06f, lado * 0.35f, 0.18f, 0);
			cajaLocal(alaHueso, 0.035f, 0.36f, 0.03f, lado * 0.26f, 0, 0.005f);
			cajaLocal(alaHueso, 0.035f, 0.36f, 0.03f, lado * 0.5f, 0, 0.005f);
			CubeListBuilder alaMembrana = CubeListBuilder.create();
			cajaLocal(alaMembrana, 0.25f, 0.4f, 0.015f, lado * 0.14f, -0.02f, 0);
			cajaLocal(alaMembrana, 0.22f, 0.32f, 0.015f, lado * 0.38f, 0.02f, 0);
			cajaLocal(alaMembrana, 0.18f, 0.22f, 0.015f, lado * 0.58f, 0.06f, 0);
			CubeListBuilder alaAlma = CubeListBuilder.create();
			cajaLocal(alaAlma, 0.08f, 0.1f, 0.07f, lado * 0.72f, 0.2f, 0);
			PartPose poseAla = pose(lado * 0.4f, Y + 0.45f, -0.25f, 0, -lado * 0.6f, -lado * 0.1f);
			raiz.addOrReplaceChild("ala_" + s + "_hueso", alaHueso, poseAla);
			raiz.addOrReplaceChild("ala_" + s + "_membrana", alaMembrana, poseAla);
			raiz.addOrReplaceChild("ala_" + s + "_alma", alaAlma, poseAla);
		}
		return LayerDefinition.create(malla, 16, 16);
	}

	/** Caja del diseño (metros, Y hacia arriba, frente a +Z) puesta en el modelo (píxeles, Y hacia abajo, frente a -Z). */
	private static void caja(CubeListBuilder builder, float ancho, float alto, float fondo, float cx, float cy, float cz) {
		agregarCaja(builder, ancho, alto, fondo, cx, cy, cz, SUELO);
	}

	/** Igual, pero dentro de un grupo (cuerno o ala): las medidas son relativas a su pivote. */
	private static void cajaLocal(CubeListBuilder builder, float ancho, float alto, float fondo, float cx, float cy, float cz) {
		agregarCaja(builder, ancho, alto, fondo, cx, cy, cz, 0f);
	}

	private static void agregarCaja(CubeListBuilder builder, float ancho, float alto, float fondo,
									float cx, float cy, float cz, float baseY) {
		float w = ancho * 16f, h = alto * 16f, d = fondo * 16f;
		builder.addBox(cx * 16f - w / 2f, baseY - cy * 16f - h / 2f, -cz * 16f - d / 2f, w, h, d);
	}

	private static PartPose pose(float cx, float cy, float cz, float rx, float ry, float rz) {
		return PartPose.offsetAndRotation(cx * 16f, SUELO - cy * 16f, -cz * 16f, rx, ry, rz);
	}

	@Override
	public void setupAnim(BombaWardenEntity bomba, float limbSwing, float limbSwingAmount, float ageInTicks,
						  float netHeadYaw, float headPitch) {
		// Aletea unas 4 veces por segundo.
		float aleteo = Mth.cos(ageInTicks * 1.25f) * 0.4f;
		for (ModelPart parte : alaDerecha) parte.zRot = giroAlaDerecha + aleteo;
		for (ModelPart parte : alaIzquierda) parte.zRot = giroAlaIzquierda - aleteo;
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int luz, int overlay, int color) {
		for (Pieza pieza : piezas) {
			pieza.parte().render(pose, buffer, pieza.brilla() ? LUZ_MAXIMA : luz, overlay,
					FastColor.ARGB32.multiply(pieza.color(), color));
		}
	}
}
