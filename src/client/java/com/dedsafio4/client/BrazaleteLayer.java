package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * El Brazalete Interdimensional puesto en la muñeca: si el jugador lo tiene en una mano (o en las dos), se dibuja
 * alrededor de ese brazo con el modelo "Pulsera" de Patricio (la banda blanca y la gema de colores, mirando para
 * afuera). En tercera persona el ítem no se ve en la mano (su modelo tiene escala 0 ahí): solo la pulsera.
 */
public class BrazaleteLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/item/brazalete_interdimensional.png");
	private static final float P = 1 / 16f;
	/** Qué tan gruesa: el modelo mide 5,5 píxeles de ancho y el brazo 4; así queda apenas más ancha que el brazo. */
	private static final float GROSOR = 0.82f;
	/** Con el Traje Espacial puesto el brazo es más grueso: la pulsera también, para que se vea encima. */
	private static final float GROSOR_TRAJE = 1.2f;
	/** Qué parte del largo del modelo (10 píxeles) se usa: así queda de unos 8. */
	private static final float LARGO = 0.7f;
	/** El modelo de Blockbench (la y da vuelta: en los modelos de Minecraft la y va para abajo). */
	private static final ModelPart PULSERA;

	static {
		MeshDefinition malla = new MeshDefinition();
		malla.getRoot().addOrReplaceChild("pulsera", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-8, -4, -1, 10, 4, 4, new CubeDeformation(0.75f))
				.texOffs(0, 8).addBox(-4, -5, 0, 2, 2, 2), PartPose.ZERO);
		PULSERA = LayerDefinition.create(malla, 32, 32).bakeRoot().getChild("pulsera");
	}

	public BrazaleteLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre) {
		super(padre);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador, float giro, float paso,
					   float parcial, float edad, float cabeza, float inclinacion) {
		if (jugador.isInvisible()) return;
		boolean derecha = false, izquierda = false;
		for (var mano : net.minecraft.world.InteractionHand.values()) {
			if (!jugador.getItemInHand(mano).is(ModItems.BRAZALETE_INTERDIMENSIONAL)) continue;
			// La mano principal es la derecha, salvo que el jugador sea zurdo.
			boolean principalDerecha = jugador.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
			boolean esDerecha = (mano == net.minecraft.world.InteractionHand.MAIN_HAND) == principalDerecha;
			if (esDerecha) derecha = true;
			else izquierda = true;
		}
		boolean traje = com.dedsafio4.brazalete.TrajeEspacial.puesto(jugador);
		if (traje) derecha = izquierda = true;
		if (!derecha && !izquierda) return;
		boolean fino = jugador.getSkin().model() == PlayerSkin.Model.SLIM;
		var vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		float grosor = traje ? GROSOR_TRAJE : GROSOR;
		// Con el traje, la pulsera va sobre la manga del traje (que está 1 píxel más afuera).
		float centro = traje ? 2f : (fino ? 0.5f : 1f);
		if (derecha) dibujar(pose, vertices, luz, getParentModel().rightArm, -centro, -90f, grosor);
		if (izquierda) dibujar(pose, vertices, luz, getParentModel().leftArm, centro, 90f, grosor);
	}

	/**
	 * La banda va a lo largo del brazo (su lado largo, la x del modelo, pasa a ser la y del brazo), cerca de la mano,
	 * y la gema mira hacia afuera del cuerpo.
	 */
	private static void dibujar(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vertices, int luz, ModelPart brazo,
								float centroX, float giroZ, float grosor) {
		pose.pushPose();
		brazo.translateAndRotate(pose);
		pose.translate(centroX * P, 6.2f * P, 0);   // pegada a la mano (el brazo va de -2 a 10)
		pose.mulPose(Axis.ZP.rotationDegrees(giroZ));
		pose.scale(LARGO, grosor, grosor);          // un poco más corta y más pegada al brazo
		pose.translate(3 * P, 2 * P, -1 * P);   // el centro de la banda al origen
		PULSERA.render(pose, vertices, luz, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}
}
