package com.dedsafio4.client.robots;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * La forma del Aldeano Phora: el modelo "pa_aldeano_robot_onixestudio" de Patricio (Blockbench, Bedrock), pasado a
 * partes de Minecraft por un script (las medidas salen del .bbmodel; los cubos girados van en partes propias).
 * Huesos: root (waist, body, leftLeg, rightLeg), head (head2, hat), leftArm y rightArm.
 */
public final class AldeanoPhoraMalla {
	private AldeanoPhoraMalla() {}

	public static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();
		PartDefinition p_root = raiz.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, 24f, 0f, 0f, 0f, 0f));
		PartDefinition p_waist = p_root.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, -12f, 0f, 0f, 0f, 0f));
		PartDefinition p_body = p_waist.addOrReplaceChild("body", CubeListBuilder.create().texOffs(17, 42).addBox(-5f, 9f, -2f, 10f, 10f, 10f, new CubeDeformation(0f)).texOffs(60, 44).addBox(-4f, 19f, -1f, 8f, 1f, 8f, new CubeDeformation(0f)), PartPose.offsetAndRotation(0f, -15f, 0f, 0f, 0f, 0f));
		PartDefinition p_cape = p_body.addOrReplaceChild("cape", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, 0f, 3f, 0f, 0f, 0f));
		PartDefinition p_jacket = p_body.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
		PartDefinition p_leftLeg = p_root.addOrReplaceChild("leftLeg", CubeListBuilder.create(), PartPose.offsetAndRotation(1.9f, -12f, 0f, 0f, 0f, 0f));
		PartDefinition p_leftPants = p_leftLeg.addOrReplaceChild("leftPants", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
		PartDefinition p_rightLeg = p_root.addOrReplaceChild("rightLeg", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.9f, -12f, 0f, 0f, 0f, 0f));
		PartDefinition p_rightPants = p_rightLeg.addOrReplaceChild("rightPants", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
		PartDefinition p_head = raiz.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, -3f, 0f, 0f, 0f, 0f));
		PartDefinition p_head2 = p_head.addOrReplaceChild("head2", CubeListBuilder.create().texOffs(0, 0).addBox(-6f, -25f, -4f, 12f, 9f, 4f, new CubeDeformation(0f)).texOffs(29, 28).addBox(-6f, -25f, 0f, 12f, 6f, 8f, new CubeDeformation(0f)).texOffs(32, 8).addBox(-6f, -25f, -5f, 12f, 1f, 1f, new CubeDeformation(0f)).texOffs(0, 13).addBox(-6f, -17f, -5f, 12f, 1f, 1f, new CubeDeformation(0f)), PartPose.offsetAndRotation(0f, 24f, 0f, 0f, 0f, 0f));
		p_head2.addOrReplaceChild("head2_girado1", CubeListBuilder.create().texOffs(32, 0).addBox(-2f, -33f, 2f, 4f, 6f, 2f, new CubeDeformation(0f)), PartPose.offsetAndRotation(0f, 11f, 14f, 0.5236f, 0f, 0f));
		p_head2.addOrReplaceChild("head2_girado2", CubeListBuilder.create().texOffs(16, 31).addBox(-4f, -33f, -1f, 7f, 1f, 1f, new CubeDeformation(0f)), PartPose.offsetAndRotation(-38f, -20f, -4f, 0f, 0f, 1.5708f));
		p_head2.addOrReplaceChild("head2_girado3", CubeListBuilder.create().texOffs(16, 31).addBox(-4f, -33f, -1f, 7f, 1f, 1f, new CubeDeformation(0f)), PartPose.offsetAndRotation(-27f, -20f, -4f, 0f, 0f, 1.5708f));
		PartDefinition p_hat = p_head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
		PartDefinition p_leftArm = raiz.addOrReplaceChild("leftArm", CubeListBuilder.create(), PartPose.offsetAndRotation(5f, -1f, 0f, 0f, 0f, 0f));
		p_leftArm.addOrReplaceChild("leftArm_girado4", CubeListBuilder.create().texOffs(0, 15).addBox(4f, -24f, 0f, 2f, 7f, 3f, new CubeDeformation(0f)), PartPose.offsetAndRotation(5f, 32f, 0f, 0f, 0f, -0.3927f));
		p_leftArm.addOrReplaceChild("leftArm_girado5", CubeListBuilder.create().texOffs(0, 15).addBox(4f, -24f, 0f, 2f, 7f, 3f, new CubeDeformation(0f)), PartPose.offsetAndRotation(5f, 29f, -15f, -0.7418f, 0f, -0.3927f));
		p_leftArm.addOrReplaceChild("leftArm_girado6", CubeListBuilder.create().texOffs(1, 16).addBox(4f, -21f, 0f, 1f, 4f, 2f, new CubeDeformation(0f)), PartPose.offsetAndRotation(-1f, 12f, -22f, -1.7453f, 0f, -0.3927f));
		p_leftArm.addOrReplaceChild("leftArm_girado7", CubeListBuilder.create().texOffs(1, 16).addBox(4f, -21f, 0f, 1f, 3f, 2f, new CubeDeformation(0f)), PartPose.offsetAndRotation(6f, 29f, -19f, -0.9163f, 0f, -0.3927f));
		PartDefinition p_leftSleeve = p_leftArm.addOrReplaceChild("leftSleeve", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
		PartDefinition p_leftItem = p_leftArm.addOrReplaceChild("leftItem", CubeListBuilder.create(), PartPose.offsetAndRotation(1f, 7f, 1f, 0f, 0f, 0f));
		PartDefinition p_rightArm = raiz.addOrReplaceChild("rightArm", CubeListBuilder.create(), PartPose.offsetAndRotation(-5f, -1f, 0f, 0f, 0f, 0f));
		p_rightArm.addOrReplaceChild("rightArm_girado8", CubeListBuilder.create().texOffs(10, 15).addBox(-8f, -24f, -2f, 2f, 8f, 3f, new CubeDeformation(0f)), PartPose.offsetAndRotation(-5f, 32f, 2f, 0f, 0f, 0.48f));
		p_rightArm.addOrReplaceChild("rightArm_girado9", CubeListBuilder.create().texOffs(10, 15).addBox(-8f, -24f, -2f, 2f, 8f, 3f, new CubeDeformation(0f)), PartPose.offsetAndRotation(-6f, 31f, -13f, -0.6981f, 0f, 0.48f));
		p_rightArm.addOrReplaceChild("rightArm_girado10", CubeListBuilder.create().texOffs(11, 16).addBox(-8f, -24f, -1f, 1f, 3f, 2f, new CubeDeformation(0f)), PartPose.offsetAndRotation(1f, 19f, -27f, -1.5272f, 0f, 0.48f));
		p_rightArm.addOrReplaceChild("rightArm_girado11", CubeListBuilder.create().texOffs(11, 16).addBox(-8f, -24f, -1f, 1f, 3f, 2f, new CubeDeformation(0f)), PartPose.offsetAndRotation(-7f, 35f, -20f, -0.829f, 0f, 0.48f));
		PartDefinition p_rightSleeve = p_rightArm.addOrReplaceChild("rightSleeve", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
		PartDefinition p_rightItem = p_rightArm.addOrReplaceChild("rightItem", CubeListBuilder.create(), PartPose.offsetAndRotation(-1f, 7f, 1f, 0f, 0f, 0f));
		return LayerDefinition.create(malla, 128, 128);
	}
}
