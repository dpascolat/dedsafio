package com.dedsafio4.client.bombas;

import com.dedsafio4.bombas.BombaWardenEntity;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Modelo de la Bomba Warden (el de Patricio en Blockbench, "bomba 67"): el cuerpo cúbico con la cara del Warden, dos
 * alas, dos antenas y la mecha encendida atrás. En el diseño la cara mira al oeste; acá se gira para que mire adelante.
 * Siempre está volando (la animación "volar" del modelo: aletea, las antenas se mecen y la mecha chisporrotea).
 */
public class BombaWardenModelo extends HierarchicalModel<BombaWardenEntity> {
	private static final AnimationDefinition VOLAR = AnimationDefinition.Builder.withLength(1.0F).looping()
			.addAnimation("ala_norte", new AnimationChannel(AnimationChannel.Targets.ROTATION, aleteo(-35.0F)))
			.addAnimation("ala_sur", new AnimationChannel(AnimationChannel.Targets.ROTATION, aleteo(35.0F)))
			.addAnimation("antena_norte", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("antena_sur", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("cuerpo", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("mecha", new AnimationChannel(AnimationChannel.Targets.SCALE,
					new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.25F, KeyframeAnimations.scaleVec(1.0, 1.15, 1.0), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.0, 0.95, 1.0), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.75F, KeyframeAnimations.scaleVec(1.0, 1.1, 1.0), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(1.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.CATMULLROM)))
			.build();

	/** Cada ala sube y baja 4 veces por segundo (empieza en "desde" grados y va a -desde). */
	private static Keyframe[] aleteo(float desde) {
		Keyframe[] k = new Keyframe[9];
		for (int i = 0; i <= 8; i++) {
			float x = i % 2 == 0 ? desde : -desde;
			k[i] = new Keyframe(i * 0.125F, KeyframeAnimations.degreeVec(x, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM);
		}
		return k;
	}

	/** La animación de cada bomba (sólo en el cliente). */
	private static final Map<Entity, AnimationState> VUELOS = new WeakHashMap<>();

	private final ModelPart raiz;

	public BombaWardenModelo(ModelPart raiz) {
		this.raiz = raiz;
	}

	@Override
	public ModelPart root() {
		return raiz;
	}

	public static LayerDefinition crearCapa() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition partes = malla.getRoot();
		// Girada un cuarto de vuelta: la cara (al oeste en el diseño) queda mirando adelante.
		PartDefinition bomba = partes.addOrReplaceChild("bomba", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 14.0F, 0.0F, 0.0F, (float) Math.PI / 2, 0.0F));
		PartDefinition cuerpo = bomba.addOrReplaceChild("cuerpo", CubeListBuilder.create()
						.texOffs(0, 0).addBox(-8.0F, -7.0F, -7.0F, 16.0F, 14.0F, 15.0F),
				PartPose.offset(0.0F, -7.0F, 0.0F));
		cuerpo.addOrReplaceChild("ala_norte", CubeListBuilder.create()
						.texOffs(0, 29).addBox(-8.0F, 0.0F, -13.0F, 16.0F, 0.0F, 13.0F),
				PartPose.offset(0.0F, 0.0F, -7.0F));
		cuerpo.addOrReplaceChild("ala_sur", CubeListBuilder.create()
						.texOffs(0, 42).addBox(-8.0F, 0.0F, 0.0F, 16.0F, 0.0F, 13.0F),
				PartPose.offset(0.0F, 0.0F, 8.0F));
		PartDefinition antenaNorte = cuerpo.addOrReplaceChild("antena_norte", CubeListBuilder.create(),
				PartPose.offset(0.0F, -7.0F, -6.0F));
		antenaNorte.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(58, 29).addBox(-8.0F, 0.0F, -1.0F, 16.0F, 0.0F, 13.0F),
				PartPose.offsetAndRotation(0.0F, -4.0F, -12.0F, -0.3491F, 0.0F, 0.0F));
		PartDefinition antenaSur = cuerpo.addOrReplaceChild("antena_sur", CubeListBuilder.create(),
				PartPose.offset(0.0F, -7.0F, 7.0F));
		antenaSur.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 55).addBox(-8.0F, 0.0F, -1.0F, 16.0F, 0.0F, 13.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.3491F, 0.0F, 0.0F));
		cuerpo.addOrReplaceChild("mecha", CubeListBuilder.create()
						.texOffs(58, 42).addBox(-5.0F, -2.0F, -1.0F, 5.0F, 3.0F, 3.0F)
						.texOffs(58, 48).addBox(-11.0F, -2.0F, 0.0F, 7.0F, 6.0F, 0.0F),
				PartPose.offset(-8.0F, 2.0F, 1.0F));
		return LayerDefinition.create(malla, 128, 128);
	}

	@Override
	public void setupAnim(BombaWardenEntity bomba, float limbSwing, float limbSwingAmount, float ageInTicks,
						  float netHeadYaw, float headPitch) {
		root().getAllParts().forEach(ModelPart::resetPose);
		AnimationState vuelo = VUELOS.computeIfAbsent(bomba, b -> new AnimationState());
		vuelo.startIfStopped((int) ageInTicks);
		animate(vuelo, VOLAR, ageInTicks, 1.0F);
	}
}
