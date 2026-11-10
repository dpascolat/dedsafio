package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.brazalete.TrajeEspacial;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * El Traje Espacial puesto (Modo de Vuelo del Brazalete): el modelo "outer_armor_astronauta" de Patricio, cada caja
 * pegada a la parte del cuerpo que le toca (casco a la cabeza, hombreras a los brazos...), así se mueve con el jugador.
 * Las medidas salen del .bbmodel (Blockbench: x al revés, y para arriba desde los pies), pasadas a cada parte.
 */
public class TrajeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/traje_espacial.png");
	private static final ModelPart CASCO, PECHERA, BRAZO_DER, BRAZO_IZQ, PIERNA_DER, PIERNA_IZQ;

	static {
		MeshDefinition malla = new MeshDefinition();
		var raiz = malla.getRoot();
		raiz.addOrReplaceChild("casco", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -9, -5, 10, 10, 10), PartPose.ZERO);
		raiz.addOrReplaceChild("pechera", CubeListBuilder.create().texOffs(0, 20)
				.addBox(-4, 0, -2, 8, 12, 4, new CubeDeformation(1f)), PartPose.ZERO);
		raiz.addOrReplaceChild("brazo_der", CubeListBuilder.create()
				.texOffs(24, 20).addBox(-4, -2, -2, 4, 12, 4, new CubeDeformation(1f))
				.texOffs(40, 0).addBox(-4.5f, -3, -2.5f, 5, 5, 5, new CubeDeformation(1.25f)), PartPose.ZERO);
		raiz.addOrReplaceChild("brazo_izq", CubeListBuilder.create()
				.texOffs(0, 36).addBox(0, -2, -2, 4, 12, 4, new CubeDeformation(1f))
				.texOffs(40, 10).addBox(-0.5f, -3, -2.5f, 5, 5, 5, new CubeDeformation(1.25f)), PartPose.ZERO);
		raiz.addOrReplaceChild("pierna_der", CubeListBuilder.create().texOffs(16, 36)
				.addBox(-2, 0, -2, 4, 12, 4, new CubeDeformation(1f)), PartPose.ZERO);
		raiz.addOrReplaceChild("pierna_izq", CubeListBuilder.create().texOffs(32, 36)
				.addBox(-2, 0, -2, 4, 12, 4, new CubeDeformation(1f)), PartPose.ZERO);
		ModelPart todo = LayerDefinition.create(malla, 64, 64).bakeRoot();
		CASCO = todo.getChild("casco");
		PECHERA = todo.getChild("pechera");
		BRAZO_DER = todo.getChild("brazo_der");
		BRAZO_IZQ = todo.getChild("brazo_izq");
		PIERNA_DER = todo.getChild("pierna_der");
		PIERNA_IZQ = todo.getChild("pierna_izq");
	}

	public TrajeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre) {
		super(padre);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador, float giro, float paso,
					   float parcial, float edad, float cabeza, float inclinacion) {
		if (jugador.isInvisible() || !TrajeEspacial.puesto(jugador)) return;
		VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		int sombra = LivingEntityRenderer.getOverlayCoords(jugador, 0f);
		PlayerModel<AbstractClientPlayer> m = getParentModel();
		pegado(pose, v, luz, sombra, m.head, CASCO);
		pegado(pose, v, luz, sombra, m.body, PECHERA);
		pegado(pose, v, luz, sombra, m.rightArm, BRAZO_DER);
		pegado(pose, v, luz, sombra, m.leftArm, BRAZO_IZQ);
		pegado(pose, v, luz, sombra, m.rightLeg, PIERNA_DER);
		pegado(pose, v, luz, sombra, m.leftLeg, PIERNA_IZQ);
	}

	private static void pegado(PoseStack pose, VertexConsumer v, int luz, int sombra, ModelPart parte, ModelPart caja) {
		pose.pushPose();
		parte.translateAndRotate(pose);
		caja.render(pose, v, luz, sombra);
		pose.popPose();
	}
}
