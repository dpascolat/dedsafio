package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ArmaduraLimbo;
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
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * La Armadura del Limbo puesta en el jugador, con el modelo "outer_armor" de Patricio (textures/models/armor/
 * limbo_armadura.png): cada parte tiene una capa de adentro (+0,75) y una de afuera (+1). El casco pone la cabeza,
 * la pechera el cuerpo y los brazos, y los pantalones o las botas las piernas. La armadura normal de Minecraft no se
 * dibuja en los jugadores (HumanoidArmorLimboMixin).
 */
public class ArmaduraLimboLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/models/armor/limbo_armadura.png");
	private static final CubeDeformation ADENTRO = new CubeDeformation(0.75f), AFUERA = new CubeDeformation(1f);
	private static final ModelPart CABEZA, CUERPO, BRAZO_DER, BRAZO_IZQ, PIERNA_DER, PIERNA_IZQ;

	static {
		MeshDefinition malla = new MeshDefinition();
		var raiz = malla.getRoot();
		raiz.addOrReplaceChild("cabeza", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, ADENTRO)
				.texOffs(0, 16).addBox(-4, -8, -4, 8, 8, 8, AFUERA), PartPose.ZERO);
		raiz.addOrReplaceChild("cuerpo", CubeListBuilder.create()
				.texOffs(0, 32).addBox(-4, 0, -2, 8, 12, 4, ADENTRO)
				.texOffs(32, 0).addBox(-4, 0, -2, 8, 12, 4, AFUERA), PartPose.ZERO);
		raiz.addOrReplaceChild("brazo_der", CubeListBuilder.create()
				.texOffs(0, 48).addBox(-4, -2, -2, 4, 12, 4, ADENTRO)
				.texOffs(48, 16).addBox(-4, -2, -2, 4, 12, 4, AFUERA), PartPose.ZERO);
		raiz.addOrReplaceChild("brazo_izq", CubeListBuilder.create().mirror()
				.texOffs(0, 48).addBox(0, -2, -2, 4, 12, 4, ADENTRO)
				.texOffs(48, 16).addBox(0, -2, -2, 4, 12, 4, AFUERA), PartPose.ZERO);
		raiz.addOrReplaceChild("pierna_der", CubeListBuilder.create()
				.texOffs(24, 32).addBox(-2, 0, -2, 4, 12, 4, ADENTRO)
				.texOffs(48, 48).addBox(-2, 0, -2, 4, 12, 4, AFUERA), PartPose.ZERO);
		raiz.addOrReplaceChild("pierna_izq", CubeListBuilder.create().mirror()
				.texOffs(24, 32).addBox(-2, 0, -2, 4, 12, 4, ADENTRO)
				.texOffs(48, 48).addBox(-2, 0, -2, 4, 12, 4, AFUERA), PartPose.ZERO);
		ModelPart todo = LayerDefinition.create(malla, 64, 64).bakeRoot();
		CABEZA = todo.getChild("cabeza");
		CUERPO = todo.getChild("cuerpo");
		BRAZO_DER = todo.getChild("brazo_der");
		BRAZO_IZQ = todo.getChild("brazo_izq");
		PIERNA_DER = todo.getChild("pierna_der");
		PIERNA_IZQ = todo.getChild("pierna_izq");
	}

	public ArmaduraLimboLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre) {
		super(padre);
	}

	private static boolean esLimbo(ItemStack pila) {
		return pila.getItem() instanceof ArmaduraLimbo.Pieza;
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador, float giro, float paso,
					   float parcial, float edad, float cabeza, float inclinacion) {
		if (jugador.isInvisible()) return;
		ItemStack casco = jugador.getItemBySlot(EquipmentSlot.HEAD), pechera = jugador.getItemBySlot(EquipmentSlot.CHEST);
		ItemStack pantalones = jugador.getItemBySlot(EquipmentSlot.LEGS), botas = jugador.getItemBySlot(EquipmentSlot.FEET);
		boolean conCasco = esLimbo(casco), conPechera = esLimbo(pechera), conPiernas = esLimbo(pantalones) || esLimbo(botas);
		if (!conCasco && !conPechera && !conPiernas) return;
		PlayerModel<AbstractClientPlayer> m = getParentModel();
		VertexConsumer v = buffers.getBuffer(RenderType.armorCutoutNoCull(TEXTURA));
		dibujarPartes(pose, v, luz, m, conCasco, conPechera, conPiernas);
		// El brillo de los encantamientos, en las piezas encantadas.
		boolean brillo = (conCasco && casco.hasFoil()) || (conPechera && pechera.hasFoil())
				|| (conPiernas && (pantalones.hasFoil() || botas.hasFoil()));
		if (brillo) {
			VertexConsumer g = buffers.getBuffer(RenderType.armorEntityGlint());
			dibujarPartes(pose, g, luz, m, conCasco && casco.hasFoil(), conPechera && pechera.hasFoil(),
					conPiernas && (pantalones.hasFoil() || botas.hasFoil()));
		}
	}

	private static void dibujarPartes(PoseStack pose, VertexConsumer v, int luz, PlayerModel<?> m, boolean casco, boolean pechera, boolean piernas) {
		if (casco) pegado(pose, v, luz, m.head, CABEZA);
		if (pechera) {
			pegado(pose, v, luz, m.body, CUERPO);
			pegado(pose, v, luz, m.rightArm, BRAZO_DER);
			pegado(pose, v, luz, m.leftArm, BRAZO_IZQ);
		}
		if (piernas) {
			pegado(pose, v, luz, m.rightLeg, PIERNA_DER);
			pegado(pose, v, luz, m.leftLeg, PIERNA_IZQ);
		}
	}

	private static void pegado(PoseStack pose, VertexConsumer v, int luz, ModelPart parte, ModelPart caja) {
		if (!parte.visible) return;
		pose.pushPose();
		parte.translateAndRotate(pose);
		caja.render(pose, v, luz, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}
}
