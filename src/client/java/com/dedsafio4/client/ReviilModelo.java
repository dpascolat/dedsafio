package com.dedsafio4.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Modelo de Reviil, reconstruido del ModelReviil de MCreator (mod "REvill 1.18.2").
 * Generado automáticamente a partir de su bytecode: no editar a mano.
 * Partes raíz: "Cuerno" y "bone". Textura de 512x512.
 */
final class ReviilModelo {
	private ReviilModelo() {}

	/** Límites del modelo en píxeles: X -53.44727F..51.0849F, Y -103.51656F..25.43448F, Z -22.94047F..19.0F. */
	static final float CENTRO_X = -1.18119F, CENTRO_Y = -39.04104F, CENTRO_Z = -1.97024F;
	static final float ANCHO = 104.53217F, ALTO = 128.95104F;

	static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition raiz = malla.getRoot();
		PartDefinition p0 = raiz.addOrReplaceChild("Cuerno", CubeListBuilder.create(),
				PartPose.offsetAndRotation(2.0928F, -38.099F, 4.5F, 0.004F, -0.0054F, -0.1307F));
		p0.addOrReplaceChild("cube_r1", CubeListBuilder.create()
				.texOffs(125, 66).addBox(-1.0F, 9.0F, -2.0F, 2.0F, 6.0F, 4.0F, CubeDeformation.NONE)
				.texOffs(133, 124).addBox(7.0F, 9.0F, -2.0F, 2.0F, 6.0F, 4.0F, CubeDeformation.NONE)
				.texOffs(24, 7).addBox(3.0F, 0.0F, 0.0F, 2.0F, 7.0F, 0.0F, CubeDeformation.NONE)
				.texOffs(112, 163).addBox(1.0F, 7.0F, -2.0F, 6.0F, 10.0F, 4.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-57.7278F, 27.0947F, -1.0F, -0.1304F, 0.0114F, -0.088F));
		p0.addOrReplaceChild("cube_r2", CubeListBuilder.create()
				.texOffs(24, 0).addBox(-5.7957F, -7.3674F, -0.0704F, 2.0F, 7.0F, 0.0F, CubeDeformation.NONE)
				.texOffs(6, 124).addBox(-9.7957F, 1.6326F, -2.0704F, 2.0F, 6.0F, 4.0F, CubeDeformation.NONE)
				.texOffs(112, 163).addBox(-7.7957F, -0.3674F, -2.0704F, 6.0F, 10.0F, 4.0F, CubeDeformation.NONE)
				.texOffs(57, 115).addBox(-1.7957F, 1.6326F, -2.0704F, 2.0F, 6.0F, 4.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(41.119F, 46.6177F, -5.7199F, -0.1106F, 0.0702F, 0.3888F));
		p0.addOrReplaceChild("cube_r3", CubeListBuilder.create()
				.texOffs(133, 124).addBox(1.0F, -4.0F, -5.0F, 7.0F, 7.0F, 12.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-57.7278F, 27.0947F, -1.0F, 0.0F, 0.0F, -0.5672F));
		p0.addOrReplaceChild("cube_r4", CubeListBuilder.create()
				.texOffs(50, 236).addBox(-15.5F, -7.0F, -6.5F, 12.0F, 14.0F, 13.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-41.2578F, 13.2748F, 0.0F, 0.0F, 0.0F, -0.6981F));
		p0.addOrReplaceChild("cube_r5", CubeListBuilder.create()
				.texOffs(108, 222).addBox(-5.5F, -9.0F, -4.0F, 17.0F, 16.0F, 14.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-41.2578F, 13.2748F, -3.0F, 0.0F, 0.0F, -0.9599F));
		p0.addOrReplaceChild("cube_r6", CubeListBuilder.create()
				.texOffs(168, 66).addBox(0.1768F, -13.8493F, -8.0F, 16.0F, 27.0F, 16.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-32.0928F, -4.901F, 0.0F, 0.0F, 0.0F, -0.6109F));
		p0.addOrReplaceChild("cube_r7", CubeListBuilder.create()
				.texOffs(190, 109).addBox(-11.8232F, -10.8493F, -7.5F, 17.0F, 24.0F, 15.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-32.0928F, -4.901F, 0.0F, 0.0F, 0.0F, -1.0472F));
		p0.addOrReplaceChild("cube_r8", CubeListBuilder.create()
				.texOffs(192, 201).addBox(1.1768F, -21.8493F, -8.5F, 13.0F, 18.0F, 17.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-32.0928F, -4.901F, 0.0F, 0.0F, 0.0F, 0.1745F));
		p0.addOrReplaceChild("cube_r9", CubeListBuilder.create()
				.texOffs(133, 144).addBox(-25.2544F, -7.3265F, -9.5046F, 19.0F, 20.0F, 19.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-5.0441F, -7.4302F, 0.0046F, -3.1214F, -0.0387F, 2.923F));
		p0.addOrReplaceChild("cube_r10", CubeListBuilder.create()
				.texOffs(192, 201).addBox(-30.9306F, -13.6197F, -8.5046F, 13.0F, 18.0F, 17.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-5.0441F, -7.4302F, 0.0046F, -3.134F, -0.043F, -3.0545F));
		p0.addOrReplaceChild("cube_r11", CubeListBuilder.create()
				.texOffs(190, 109).addBox(-8.5F, -12.0F, -7.5F, 17.0F, 24.0F, 15.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(25.9416F, 7.066F, -1.4614F, -0.0378F, -0.0218F, -1.8322F));
		p0.addOrReplaceChild("cube_r12", CubeListBuilder.create()
				.texOffs(168, 66).addBox(-8.0F, -13.5F, -8.0F, 16.0F, 27.0F, 16.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(21.2112F, -2.9283F, -1.149F, -0.025F, -0.0357F, -2.2685F));
		p0.addOrReplaceChild("cube_r13", CubeListBuilder.create()
				.texOffs(108, 222).addBox(-8.5F, -8.0F, -7.0F, 17.0F, 16.0F, 14.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(30.2482F, 20.3238F, -1.7929F, -0.0357F, -0.025F, -1.9194F));
		p0.addOrReplaceChild("cube_r14", CubeListBuilder.create()
				.texOffs(50, 236).addBox(-6.0F, -7.0F, -6.5F, 12.0F, 14.0F, 13.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(35.776F, 31.2648F, -2.1496F, -0.0281F, -0.0334F, -2.1812F));
		p0.addOrReplaceChild("cube_r15", CubeListBuilder.create()
				.texOffs(133, 124).addBox(-67.0435F, -6.4127F, -6.0046F, 7.0F, 7.0F, 12.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-5.0441F, -7.4302F, 0.0046F, 3.1181F, -0.0368F, -2.3121F));
		p0.addOrReplaceChild("cube_r16", CubeListBuilder.create()
				.texOffs(133, 144).addBox(3.5F, -15.0F, -9.5F, 19.0F, 20.0F, 19.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-28.0928F, -13.901F, 0.0F, 0.0F, 0.0F, 0.48F));
		PartDefinition p1 = raiz.addOrReplaceChild("bone", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-42.5F, -67.5F, 4.0F, 38.0F, 28.0F, 29.0F, CubeDeformation.NONE)
				.texOffs(0, 57).addBox(-42.5F, -39.5F, 6.0F, 38.0F, 5.0F, 25.0F, CubeDeformation.NONE)
				.texOffs(200, 236).addBox(-23.5F, -39.5F, 4.0F, 7.0F, 16.0F, 8.0F, CubeDeformation.NONE)
				.texOffs(0, 87).addBox(-41.5F, -74.5F, 8.0F, 36.0F, 7.0F, 21.0F, CubeDeformation.NONE)
				.texOffs(170, 228).addBox(-30.5F, -39.5F, 4.0F, 7.0F, 16.0F, 8.0F, CubeDeformation.NONE)
				.texOffs(180, 491).addBox(-23.2F, -23.5F, 4.0F, 7.0F, 11.0F, 8.0F, CubeDeformation.NONE)
				.texOffs(150, 483).addBox(-30.8F, -23.5F, 4.0F, 7.0F, 11.0F, 8.0F, CubeDeformation.NONE)
				.texOffs(89, 90).addBox(-37.0F, -76.5F, 6.0F, 27.0F, 9.0F, 25.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(22.5F, 20.5F, -14.0F, 0.0F, 0.0F, 0.0F));
		p1.addOrReplaceChild("cube_r17", CubeListBuilder.create()
				.texOffs(433, 505).addBox(0.9658F, -2.7844F, 1.7F, 14.0F, 4.0F, 3.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-44.0F, -49.0F, 1.0F, 0.0F, 0.0F, 1.4835F));
		p1.addOrReplaceChild("cube_r18", CubeListBuilder.create()
				.texOffs(474, 505).addBox(-20.4405F, 1.8906F, 1.7F, 16.0F, 4.0F, 3.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-24.0F, -51.0F, 1.0F, 0.0F, 0.0F, 0.1309F));
		p1.addOrReplaceChild("cube_r19", CubeListBuilder.create()
				.texOffs(474, 477).addBox(0.4239F, -1.2982F, 1.7F, 14.0F, 4.0F, 3.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-4.0F, -49.0F, 1.0F, 0.0F, 0.0F, 1.6144F));
		p1.addOrReplaceChild("cube_r20", CubeListBuilder.create()
				.texOffs(474, 497).addBox(-20.5F, -4.0413F, 1.7F, 16.0F, 4.0F, 3.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-24.0F, -49.0F, 1.0F, -0.0435F, -0.0038F, 2.9671F));
		p1.addOrReplaceChild("cube_r21", CubeListBuilder.create()
				.texOffs(0, 115).addBox(-12.5F, -4.5F, 0.0F, 5.0F, 13.0F, 0.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-14.9718F, -19.1265F, 0.8893F, -2.9109F, -0.144F, -2.8843F));
		p1.addOrReplaceChild("cube_r22", CubeListBuilder.create()
				.texOffs(0, 115).addBox(-2.5F, -7.5F, 0.0F, 5.0F, 13.0F, 0.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-40.1376F, -12.5182F, 1.0F, -0.0869F, 0.0076F, -0.2185F));
		p1.addOrReplaceChild("cube_r23", CubeListBuilder.create()
				.texOffs(10, 73).addBox(-16.5F, -7.5F, 0.0F, 5.0F, 9.0F, 0.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-12.106F, 0.4584F, -4.4831F, 3.1249F, -0.0403F, -2.9667F));
		p1.addOrReplaceChild("cube_r24", CubeListBuilder.create()
				.texOffs(10, 73).addBox(-2.5F, -4.5F, 0.0F, 5.0F, 9.0F, 0.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-47.0F, 0.0F, -3.0F, 0.0F, 0.0F, -0.2182F));
		p1.addOrReplaceChild("cube_r25", CubeListBuilder.create()
				.texOffs(164, 66).addBox(-3.1832F, 2.8953F, -2.9838F, 2.0F, 6.0F, 8.0F, CubeDeformation.NONE)
				.texOffs(101, 66).addBox(-1.1832F, 0.8953F, -2.9838F, 4.0F, 8.0F, 8.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-47.7703F, -14.1197F, -1.4162F, -0.237F, -0.1945F, -0.2386F));
		p1.addOrReplaceChild("cube_r26", CubeListBuilder.create()
				.texOffs(0, 57).addBox(-3.1832F, 0.8953F, -2.9838F, 4.0F, 8.0F, 8.0F, CubeDeformation.NONE)
				.texOffs(0, 87).addBox(0.8168F, 0.8953F, -2.9838F, 2.0F, 8.0F, 8.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-47.7703F, -14.1197F, -1.4162F, -0.2099F, 0.2236F, 1.2417F));
		p1.addOrReplaceChild("cube_r27", CubeListBuilder.create()
				.texOffs(209, 148).addBox(-3.0F, 9.5F, -3.5F, 6.0F, 11.0F, 7.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-40.5F, -30.0F, 5.5F, -0.3054F, 0.0F, 0.4363F));
		p1.addOrReplaceChild("cube_r28", CubeListBuilder.create()
				.texOffs(182, 39).addBox(-2.0F, 2.5F, -4.2117F, 6.0F, 8.0F, 8.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(1.0598F, -15.0769F, -1.2883F, -0.2978F, 0.1848F, 0.1031F));
		p1.addOrReplaceChild("cube_r29", CubeListBuilder.create()
				.texOffs(230, 236).addBox(-3.0F, 8.5F, -3.5F, 6.0F, 12.0F, 7.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-5.5F, -30.0F, 4.5F, -0.3491F, 0.0F, -0.4363F));
		p1.addOrReplaceChild("cube_r30", CubeListBuilder.create()
				.texOffs(105, 0).addBox(-6.0F, -7.5F, -6.5F, 12.0F, 16.0F, 13.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-40.5F, -29.0F, 5.5F, -0.3054F, 0.0F, 0.4363F));
		p1.addOrReplaceChild("cube_r31", CubeListBuilder.create()
				.texOffs(0, 477).addBox(10.0F, 17.5F, -11.0F, 4.0F, 8.0F, 6.0F, CubeDeformation.NONE)
				.texOffs(0, 0).addBox(8.0F, 0.5F, -11.0F, 6.0F, 17.0F, 6.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-24.5F, -41.0F, 16.0F, 0.0F, 0.0F, 0.0436F));
		p1.addOrReplaceChild("cube_r32", CubeListBuilder.create()
				.texOffs(0, 232).addBox(-6.0F, -8.5F, -6.5F, 12.0F, 16.0F, 13.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-5.5F, -29.0F, 4.5F, -0.3491F, 0.0F, -0.4363F));
		p1.addOrReplaceChild("cube_r33", CubeListBuilder.create()
				.texOffs(155, 0).addBox(-12.0F, 0.5F, -11.0F, 6.0F, 17.0F, 6.0F, CubeDeformation.NONE)
				.texOffs(155, 477).addBox(-12.0F, 17.5F, -11.0F, 4.0F, 8.0F, 6.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-24.5F, -41.0F, 16.0F, 0.0F, 0.0F, -0.0436F));
		p1.addOrReplaceChild("cube_r34", CubeListBuilder.create()
				.texOffs(110, 33).addBox(-12.0F, -5.5F, -12.5F, 24.0F, 9.0F, 24.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-23.5F, -32.0F, 18.5F, 0.5236F, 0.0F, 0.0F));
		p1.addOrReplaceChild("cube_r35", CubeListBuilder.create()
				.texOffs(33, 498).addBox(-20.0F, -7.0F, -2.05F, 16.0F, 12.0F, 2.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-23.5F, -43.5F, 5.0F, 0.0F, 0.0F, -3.1416F));
		p1.addOrReplaceChild("cube_r36", CubeListBuilder.create()
				.texOffs(0, 497).addBox(-20.0F, -5.5F, -2.05F, 16.0F, 12.0F, 2.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-23.5F, -43.5F, 5.0F, 0.0F, 0.0F, -0.0436F));
		p1.addOrReplaceChild("cube_r37", CubeListBuilder.create()
				.texOffs(48, 154).addBox(-1.4879F, 1.8468F, -1.6466F, 6.0F, 8.0F, 8.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(0.0383F, -13.6895F, -3.8534F, -0.2622F, -0.2332F, -1.1472F));
		return LayerDefinition.create(malla, 512, 512);
	}
}
