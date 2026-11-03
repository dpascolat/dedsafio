package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Los cerebros de Gleba (modelos de Patricio en Blockbench: gleba 67, gleba_amarilla y gleba_roja): un cerebro con
 * patas planas alrededor. Es el mismo cuerpo para la Garrapata Cerebral, el Cerebro Amarillo y el Cerebro Rojo; cambia
 * la textura. Al atacar se tira para adelante (la animación "attack" del modelo).
 */
public class CerebroGlebaModelo<T extends Mob> extends HierarchicalModel<T> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cerebro_gleba"), "main");

	private static final AnimationDefinition ATAQUE = AnimationDefinition.Builder.withLength(0.75F)
			.addAnimation("gleba", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.12F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.5F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.28F, KeyframeAnimations.posVec(0.0F, 1.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.45F, KeyframeAnimations.posVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("cerebro", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.12F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.28F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.45F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.addAnimation("patas", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.12F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.28F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.45F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
					new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)))
			.build();

	/** El ataque de cada cerebro (sólo en el cliente; los cerebros no guardan animaciones). */
	private static final Map<Mob, AnimationState> ATAQUES = new WeakHashMap<>();

	private final ModelPart raiz, patas;

	public CerebroGlebaModelo(ModelPart raiz) {
		this.raiz = raiz;
		this.patas = raiz.getChild("gleba").getChild("patas");
	}

	@Override
	public ModelPart root() {
		return raiz;
	}

	public static LayerDefinition crearCapa() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition partes = malla.getRoot();
		PartDefinition gleba = partes.addOrReplaceChild("gleba", CubeListBuilder.create(),
				PartPose.offset(0.0F, 24.0F, 0.0F));
		gleba.addOrReplaceChild("cerebro", CubeListBuilder.create()
						.texOffs(0, 0).addBox(-5.5F, -9.0F, -7.0F, 11.0F, 9.0F, 14.0F),
				PartPose.offset(-0.5F, -10.0F, 1.0F));
		PartDefinition patas = gleba.addOrReplaceChild("patas", CubeListBuilder.create()
						.texOffs(0, 31).addBox(-6.0F, -4.0F, -7.0F, 11.0F, 8.0F, 0.0F)
						.texOffs(22, 31).addBox(-6.0F, -4.0F, 7.0F, 11.0F, 8.0F, 0.0F)
						.texOffs(50, 12).addBox(5.0F, -4.0F, 0.0F, 3.0F, 2.0F, 2.0F)
						.texOffs(18, 39).addBox(6.0F, -4.0F, 1.0F, 9.0F, 7.0F, 0.0F)
						.texOffs(50, 16).addBox(-9.0F, -4.0F, 0.0F, 3.0F, 2.0F, 2.0F)
						.texOffs(44, 31).addBox(-16.0F, -4.0F, 1.0F, 9.0F, 7.0F, 0.0F),
				PartPose.offset(0.0F, -6.0F, 1.0F));
		patas.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 23).addBox(-8.0F, -5.0F, -1.0F, 14.0F, 8.0F, 0.0F),
				PartPose.offsetAndRotation(4.0F, 1.0F, 1.0F, 0.0F, -1.5708F, 0.0F));
		patas.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(28, 23).addBox(-8.0F, -5.0F, -1.0F, 14.0F, 8.0F, 0.0F),
				PartPose.offsetAndRotation(-7.0F, 1.0F, 1.0F, 0.0F, -1.5708F, 0.0F));
		patas.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(36, 46).addBox(-3.0F, -2.0F, -1.0F, 5.0F, 2.0F, 2.0F),
				PartPose.offsetAndRotation(6.0F, -2.0F, -5.0F, 0.0F, 0.1745F, 0.3054F));
		patas.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 39).addBox(6.0F, -2.0F, -1.0F, 9.0F, 7.0F, 0.0F),
				PartPose.offsetAndRotation(0.0F, -4.0F, -3.0F, 0.0F, 0.1745F, 0.3054F));
		patas.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(36, 39).addBox(6.0F, -2.0F, -1.0F, 9.0F, 7.0F, 0.0F),
				PartPose.offsetAndRotation(-21.0F, 2.0F, -7.0F, 0.0F, -0.2618F, -0.3491F));
		patas.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(50, 8).addBox(-1.0F, -2.0F, -1.0F, 4.0F, 2.0F, 2.0F),
				PartPose.offsetAndRotation(-8.0F, -2.0F, -5.0F, 0.0F, -0.2618F, -0.3491F));
		patas.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(50, 0).addBox(-1.0F, -2.0F, -1.0F, 5.0F, 2.0F, 2.0F),
				PartPose.offsetAndRotation(5.0F, -2.0F, 6.0F, 0.0F, -0.8727F, -0.2182F));
		patas.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 46).addBox(6.0F, -2.0F, -1.0F, 9.0F, 7.0F, 0.0F),
				PartPose.offsetAndRotation(0.0F, -2.0F, 3.0F, 0.0F, -0.8727F, -0.2182F));
		patas.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(50, 4).addBox(-1.0F, -2.0F, -1.0F, 5.0F, 2.0F, 2.0F),
				PartPose.offsetAndRotation(-7.0F, -2.0F, 7.0F, 0.0F, 0.8727F, 0.2182F));
		patas.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(18, 46).addBox(6.0F, -2.0F, -1.0F, 9.0F, 7.0F, 0.0F),
				PartPose.offsetAndRotation(-14.0F, -4.0F, 17.0F, 0.0F, 0.8727F, 0.2182F));
		return LayerDefinition.create(malla, 64, 64);
	}

	@Override
	public void setupAnim(T e, float limbSwing, float limbSwingAmount, float edad, float giroCabeza, float inclinacionCabeza) {
		root().getAllParts().forEach(ModelPart::resetPose);
		// Al caminar las patas se mueven un poco de costado.
		patas.zRot = net.minecraft.util.Mth.sin(limbSwing * 0.9F) * 0.12F * limbSwingAmount;
		// Atacar (y cada vez que drena estando en la cabeza): se tira para adelante.
		AnimationState ataque = ATAQUES.computeIfAbsent(e, x -> new AnimationState());
		if (e.swinging && e.swingTime == 0) ataque.start((int) edad);
		if (ataque.isStarted() && ataque.getAccumulatedTime() > 750) ataque.stop();
		animate(ataque, ATAQUE, edad, 1.0F);
	}

	public static class Dibujante<T extends Mob> extends MobRenderer<T, CerebroGlebaModelo<T>> {
		/** En primera persona, cuánto se baja el cerebro que tienes en la cabeza para que lo veas arriba de la pantalla. */
		private static final float BAJA_PRIMERA_PERSONA = 0.4F;
		/** Visto desde afuera: cuánto se baja para que el cerebro quede apoyado en la cabeza, con las patas agarrándola. */
		private static final float BAJA_EN_CABEZA = 0.55F;
		private final ResourceLocation textura;

		public Dibujante(EntityRendererProvider.Context contexto, ResourceLocation textura) {
			super(contexto, new CerebroGlebaModelo<>(contexto.bakeLayer(CAPA)), 0.5F);
			this.textura = textura;
		}

		@Override
		public void render(T bicho, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
			Minecraft mc = Minecraft.getInstance();
			boolean enMiCabeza = bicho.getVehicle() == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
			pose.pushPose();
			if (enMiCabeza) pose.translate(0, -BAJA_PRIMERA_PERSONA, 0);
			else if (bicho.getVehicle() instanceof net.minecraft.world.entity.player.Player) pose.translate(0, -BAJA_EN_CABEZA, 0);
			super.render(bicho, yaw, parcial, pose, buffers, luz);
			pose.popPose();
		}

		@Override
		public ResourceLocation getTextureLocation(T bicho) {
			return textura;
		}
	}
}
