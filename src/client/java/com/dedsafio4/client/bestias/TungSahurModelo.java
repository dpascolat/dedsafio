package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.TungSahurEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * El Tung Tung Tung Sahur (del mod italian_brainrots_ft, de luisd): el tronco con cara, brazos flaquitos, piernas con
 * zapatos y el bate en la mano derecha. Las animaciones (quieto, caminar y pegar) están en TungSahurAnimacion.
 */
public class TungSahurModelo extends HierarchicalModel<TungSahurEntity> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "tung_tung_tung_sahur"), "main");
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID,
			"textures/entity/tung_tung_tung_sahur.png");

	private final ModelPart raiz;

	public TungSahurModelo(ModelPart raiz) {
		this.raiz = raiz;
	}

	@Override
	public ModelPart root() {
		return raiz;
	}

	public static LayerDefinition crearCapa() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition partes = malla.getRoot();
		PartDefinition root = partes.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition chest = root.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.offset(0.0F, -11.0F, 1.0F));
		PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
						.texOffs(22, 28).addBox(0.5F, -2.8224F, -3.6426F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
						.texOffs(8, 33).mirror().addBox(-4.0F, -4.8724F, -3.3926F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(22, 28).mirror().addBox(-4.5F, -2.8224F, -3.6426F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(8, 33).addBox(1.0F, -4.8724F, -3.3926F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
						.texOffs(0, 0).addBox(-3.5F, -9.1724F, -2.8926F, 7.0F, 11.0F, 6.0F, new CubeDeformation(0.15F)),
				PartPose.offset(0.0F, -12.1276F, -0.1074F));
		head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(34, 0)
						.addBox(-1.25F, -0.875F, -1.1F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.25F, -1.0284F, -3.2573F, 0.2182F, 0.0F, 0.0F));
		head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(30, 31)
						.addBox(-1.5F, -1.5F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.5F, -3.3724F, -3.0426F, -0.1309F, 0.0F, 0.0F));
		chest.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 17)
						.addBox(-3.0F, -5.5F, -2.5F, 6.0F, 11.0F, 5.0F, new CubeDeformation(0.3F)),
				PartPose.offset(0.0F, -5.5F, 0.0F));
		PartDefinition leftArm = chest.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(3.75F, -10.35F, 0.0F));
		leftArm.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(26, 0).mirror()
						.addBox(-0.8294F, -5.4069F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(1.5F, 4.75F, 0.0F, 0.0F, 0.0F, -0.3054F));
		leftArm.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 33).mirror()
						.addBox(-0.75F, 0.4F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.1F)).mirror(false),
				PartPose.offsetAndRotation(1.5F, 4.75F, 0.0F, 0.0F, 0.0F, -0.1309F));
		PartDefinition rightArm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-3.75F, -10.35F, 0.0F));
		rightArm.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 33)
						.addBox(-1.25F, 0.4F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.1F)),
				PartPose.offsetAndRotation(-1.5F, 4.75F, 0.0F, 0.0F, 0.0F, 0.1309F));
		rightArm.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(26, 0)
						.addBox(-1.1706F, -5.4069F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.5F, 4.75F, 0.0F, 0.0F, 0.0F, 0.3054F));
		rightArm.addOrReplaceChild("bate", CubeListBuilder.create()
						.texOffs(26, 15).addBox(-1.7572F, -0.5565F, -0.555F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
						.texOffs(34, 7).addBox(2.2228F, -0.529F, -0.5825F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
						.texOffs(17, 30).addBox(-3.0772F, -0.529F, -0.5825F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.4F))
						.texOffs(22, 17).addBox(3.7678F, -1.0516F, -1.11F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.0F, 7.85F, 0.1F, 0.0F, 1.5708F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
						.texOffs(22, 21).addBox(-1.5F, 9.0F, -4.0F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
						.texOffs(26, 8).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
						.texOffs(34, 3).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.3F))
						.texOffs(22, 31).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.15F)).mirror(false),
				PartPose.offset(2.0F, -11.0F, 1.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
						.texOffs(22, 21).addBox(-1.5F, 9.0F, -4.0F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
						.texOffs(26, 8).mirror().addBox(-1.0F, 4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(34, 3).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.3F))
						.texOffs(22, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.15F)),
				PartPose.offset(-2.0F, -11.0F, 1.0F));
		return LayerDefinition.create(malla, 64, 64);
	}

	@Override
	public void setupAnim(TungSahurEntity e, float limbSwing, float limbSwingAmount, float edad, float giroCabeza, float inclinacionCabeza) {
		root().getAllParts().forEach(ModelPart::resetPose);
		// Quieto: siempre (también cuando es el disfraz de un jugador, que no "tickea" como un mob).
		e.quieto.startIfStopped((int) edad);
		// Pegar: cuando mueve el brazo (el mob al atacar, o el jugador disfrazado al pegar).
		if (e.swinging && !e.ataque.isStarted()) e.ataque.start((int) edad);
		if (e.ataque.isStarted() && e.ataque.getAccumulatedTime() > 500) e.ataque.stop();
		animate(e.quieto, TungSahurAnimacion.idle, edad, 1.0F);
		animateWalk(TungSahurAnimacion.walk, limbSwing, limbSwingAmount, 1.0F, 1.0F);
		animate(e.ataque, TungSahurAnimacion.attack, edad, 1.0F);
	}

	public static class Dibujante extends MobRenderer<TungSahurEntity, TungSahurModelo> {
		public Dibujante(EntityRendererProvider.Context contexto) {
			super(contexto, new TungSahurModelo(contexto.bakeLayer(CAPA)), 0.5F);
		}

		@Override
		public ResourceLocation getTextureLocation(TungSahurEntity e) {
			return TEXTURA;
		}
	}
}
