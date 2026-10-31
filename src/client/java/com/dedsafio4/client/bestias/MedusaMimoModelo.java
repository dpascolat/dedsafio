package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.MedusaMimoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** La Medusa Mimo como medusa: una campana que late y 8 tentáculos que ondulan. */
public class MedusaMimoModelo extends EntityModel<MedusaMimoEntity> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("dedsafio4", "medusa_mimo"), "main");
	private static final int TENTACULOS = 8, TRAMOS = 4;

	private final ModelPart raiz, campana;
	private final ModelPart[][] tentaculos = new ModelPart[TENTACULOS][TRAMOS];

	public MedusaMimoModelo(ModelPart raiz) {
		this.raiz = raiz.getChild("raiz");
		campana = this.raiz.getChild("campana");
		for (int i = 0; i < TENTACULOS; i++) {
			ModelPart parte = this.raiz.getChild("tentaculo" + i);
			tentaculos[i][0] = parte;
			for (int t = 1; t < TRAMOS; t++) {
				parte = parte.getChild("tramo");
				tentaculos[i][t] = parte;
			}
		}
	}

	public static LayerDefinition crearCapa() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot().addOrReplaceChild("raiz", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
		raiz.addOrReplaceChild("campana", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-19, -6, -19, 38, 8, 38)
				.texOffs(0, 0).addBox(-15, -14, -15, 30, 8, 30)
				.texOffs(0, 0).addBox(-10, -20, -10, 20, 6, 20)
				.texOffs(0, 60).addBox(-9, -8, -9, 18, 12, 18), PartPose.offset(0, -26, 0));
		for (int i = 0; i < TENTACULOS; i++) {
			double angulo = Math.PI * 2 * i / TENTACULOS;
			PartDefinition parte = raiz.addOrReplaceChild("tentaculo" + i, tramo(), PartPose.offset(Mth.sin((float) angulo) * 14, -24, Mth.cos((float) angulo) * 14));
			for (int t = 1; t < TRAMOS; t++) parte = parte.addOrReplaceChild("tramo", tramo(), PartPose.offset(0, 6, 0));
		}
		return LayerDefinition.create(malla, 256, 128);
	}

	private static CubeListBuilder tramo() {
		return CubeListBuilder.create().texOffs(0, 100).addBox(-1.5f, 0, -1.5f, 3, 6, 3);
	}

	@Override
	public void setupAnim(MedusaMimoEntity medusa, float paso, float cuanto, float edad, float cabeza, float inclina) {
		float late = Mth.sin(edad * 0.14f);
		campana.xScale = campana.zScale = 1 + 0.07f * late;
		campana.yScale = 1 - 0.09f * late;
		for (int i = 0; i < TENTACULOS; i++) {
			for (int t = 0; t < TRAMOS; t++) {
				float onda = Mth.sin(edad * 0.1f - t * 0.8f + i * 1.1f) * (0.12f + 0.08f * t);
				tentaculos[i][t].xRot = onda;
				tentaculos[i][t].zRot = Mth.cos(edad * 0.08f - t * 0.7f + i * 0.7f) * (0.08f + 0.06f * t);
			}
		}
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int luz, int overlay, int color) {
		raiz.render(pose, buffer, luz, overlay, color);
	}
}
