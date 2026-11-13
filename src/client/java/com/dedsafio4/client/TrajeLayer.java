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
 * Al apretar J las piezas llegan volando y se encajan solas (ver TrajeAnimacion); al sacarlo se van volando.
 */
public class TrajeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/traje_espacial.png");
	/** Más finito que en Blockbench (ahí las capas eran +1 y +1,25, y el casco de 10). */
	private static final CubeDeformation FINO = new CubeDeformation(0.4f), HOMBRERA = new CubeDeformation(0.6f),
			CASCO = new CubeDeformation(-0.6f);
	private static final ModelPart CASCO_PARTE, PECHERA, BRAZO_DER, BRAZO_IZQ, PIERNA_DER, PIERNA_IZQ;

	static {
		MeshDefinition malla = new MeshDefinition();
		var raiz = malla.getRoot();
		raiz.addOrReplaceChild("casco", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -9, -5, 10, 10, 10, CASCO), PartPose.ZERO);
		raiz.addOrReplaceChild("pechera", CubeListBuilder.create().texOffs(0, 20)
				.addBox(-4, 0, -2, 8, 12, 4, FINO), PartPose.ZERO);
		raiz.addOrReplaceChild("brazo_der", CubeListBuilder.create()
				.texOffs(24, 20).addBox(-4, -2, -2, 4, 12, 4, FINO)
				.texOffs(40, 0).addBox(-4.5f, -3, -2.5f, 5, 5, 5, HOMBRERA), PartPose.ZERO);
		raiz.addOrReplaceChild("brazo_izq", CubeListBuilder.create()
				.texOffs(0, 36).addBox(0, -2, -2, 4, 12, 4, FINO)
				.texOffs(40, 10).addBox(-0.5f, -3, -2.5f, 5, 5, 5, HOMBRERA), PartPose.ZERO);
		raiz.addOrReplaceChild("pierna_der", CubeListBuilder.create().texOffs(16, 36)
				.addBox(-2, 0, -2, 4, 12, 4, FINO), PartPose.ZERO);
		raiz.addOrReplaceChild("pierna_izq", CubeListBuilder.create().texOffs(32, 36)
				.addBox(-2, 0, -2, 4, 12, 4, FINO), PartPose.ZERO);
		ModelPart todo = LayerDefinition.create(malla, 64, 64).bakeRoot();
		CASCO_PARTE = todo.getChild("casco");
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
		if (jugador.isInvisible()) return;
		float a = TrajeAnimacion.avance(jugador.getUUID());
		if (TrajeEspacial.puesto(jugador)) {
			if (a < 0) a = 1;
		} else if (a < 0 || !TrajeAnimacion.sacando(jugador.getUUID())) {
			return;
		}
		VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		int sombra = LivingEntityRenderer.getOverlayCoords(jugador, 0f);
		PlayerModel<AbstractClientPlayer> m = getParentModel();
		// Cuánto llegó cada pieza: primero las piernas, después la pechera, los brazos y al final el casco.
		float piernas = TrajeAnimacion.pieza(a, 0f, 0.35f), pecho = TrajeAnimacion.pieza(a, 0.2f, 0.55f),
				brazos = TrajeAnimacion.pieza(a, 0.35f, 0.75f), casco = TrajeAnimacion.pieza(a, 0.6f, 1f);
		// Cada una viene de su lado (en píxeles del modelo; y para abajo, -z adelante, -x a la derecha del jugador).
		pegado(pose, v, luz, sombra, m.head, CASCO_PARTE, casco, 0, -18, 0, true);
		pegado(pose, v, luz, sombra, m.body, PECHERA, pecho, 0, 0, -14, false);
		pegado(pose, v, luz, sombra, m.rightArm, BRAZO_DER, brazos, -14, -2, 0, false);
		pegado(pose, v, luz, sombra, m.leftArm, BRAZO_IZQ, brazos, 14, -2, 0, false);
		pegado(pose, v, luz, sombra, m.rightLeg, PIERNA_DER, piernas, -3, 14, 0, false);
		pegado(pose, v, luz, sombra, m.leftLeg, PIERNA_IZQ, piernas, 3, 14, 0, false);
	}

	/** Una pieza pegada a su parte del cuerpo; si todavía no llegó (llego &lt; 1), viene desde (dx, dy, dz), chiquita. */
	private static void pegado(PoseStack pose, VertexConsumer v, int luz, int sombra, ModelPart parte, ModelPart caja,
							   float llego, float dx, float dy, float dz, boolean gira) {
		if (llego <= 0.001f) return;
		pose.pushPose();
		parte.translateAndRotate(pose);
		if (llego < 1) {
			float falta = 1 - llego;
			pose.translate(dx * falta / 16f, dy * falta / 16f, dz * falta / 16f);
			if (gira) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(falta * 360f));
			float s = 0.4f + 0.6f * llego;
			pose.scale(s, s, s);
		}
		caja.render(pose, v, luz, sombra);
		pose.popPose();
	}
}
