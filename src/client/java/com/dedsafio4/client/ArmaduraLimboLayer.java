package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ArmaduraLimbo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * La Armadura del Limbo puesta, con el modelo "outer_armor" de Patricio (textures/models/armor/limbo_armadura.png),
 * en jugadores, mobs y soportes de armadura. El casco pone la cabeza (capa de adentro +0,3 y de afuera +0,5, más finas que en Blockbench), la
 * pechera el cuerpo (las dos capas) y los brazos, los pantalones la mitad de arriba de las piernas y las botas la
 * mitad de abajo. La armadura normal de Minecraft no se dibuja (HumanoidArmorLimboMixin).
 */
public class ArmaduraLimboLayer<E extends LivingEntity, M extends HumanoidModel<E>> extends RenderLayer<E, M> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/models/armor/limbo_armadura.png");
	private static final CubeDeformation ADENTRO = new CubeDeformation(0.3f), AFUERA = new CubeDeformation(0.5f);
	/** Las botas apenas más grandes, para que donde se juntan con los pantalones no titile. */
	private static final CubeDeformation BOTA_ADENTRO = new CubeDeformation(0.35f), BOTA_AFUERA = new CubeDeformation(0.55f);
	private static final ModelPart CABEZA, CUERPO, BRAZO_DER, BRAZO_IZQ, PANTALON_DER, PANTALON_IZQ, BOTA_DER, BOTA_IZQ;

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
		// Las piernas se parten a la mitad (6 píxeles de 12): arriba los pantalones, abajo las botas. Cada mitad usa la
		// parte de la textura de la pierna que le toca (la de abajo empieza 6 filas más abajo en la textura).
		raiz.addOrReplaceChild("pantalon_der", CubeListBuilder.create()
				.texOffs(24, 32).addBox(-2, 0, -2, 4, 6, 4, ADENTRO)
				.texOffs(48, 48).addBox(-2, 0, -2, 4, 6, 4, AFUERA), PartPose.ZERO);
		raiz.addOrReplaceChild("pantalon_izq", CubeListBuilder.create().mirror()
				.texOffs(24, 32).addBox(-2, 0, -2, 4, 6, 4, ADENTRO)
				.texOffs(48, 48).addBox(-2, 0, -2, 4, 6, 4, AFUERA), PartPose.ZERO);
		raiz.addOrReplaceChild("bota_der", CubeListBuilder.create()
				.texOffs(24, 38).addBox(-2, 6, -2, 4, 6, 4, BOTA_ADENTRO)
				.texOffs(48, 54).addBox(-2, 6, -2, 4, 6, 4, BOTA_AFUERA), PartPose.ZERO);
		raiz.addOrReplaceChild("bota_izq", CubeListBuilder.create().mirror()
				.texOffs(24, 38).addBox(-2, 6, -2, 4, 6, 4, BOTA_ADENTRO)
				.texOffs(48, 54).addBox(-2, 6, -2, 4, 6, 4, BOTA_AFUERA), PartPose.ZERO);
		ModelPart todo = LayerDefinition.create(malla, 64, 64).bakeRoot();
		CABEZA = todo.getChild("cabeza");
		CUERPO = todo.getChild("cuerpo");
		BRAZO_DER = todo.getChild("brazo_der");
		BRAZO_IZQ = todo.getChild("brazo_izq");
		PANTALON_DER = todo.getChild("pantalon_der");
		PANTALON_IZQ = todo.getChild("pantalon_izq");
		BOTA_DER = todo.getChild("bota_der");
		BOTA_IZQ = todo.getChild("bota_izq");
	}

	public ArmaduraLimboLayer(RenderLayerParent<E, M> padre) {
		super(padre);
	}

	private static boolean esLimbo(ItemStack pila) {
		return pila.getItem() instanceof ArmaduraLimbo.Pieza;
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int luz, E entidad, float giro, float paso,
					   float parcial, float edad, float cabeza, float inclinacion) {
		if (entidad.isInvisible()) return;
		// Con el Traje Espacial puesto no se ve la armadura.
		if (entidad instanceof net.minecraft.world.entity.player.Player jugador && com.dedsafio4.brazalete.TrajeEspacial.puesto(jugador)) return;
		ItemStack[] piezas = {entidad.getItemBySlot(EquipmentSlot.HEAD), entidad.getItemBySlot(EquipmentSlot.CHEST),
				entidad.getItemBySlot(EquipmentSlot.LEGS), entidad.getItemBySlot(EquipmentSlot.FEET)};
		boolean alguna = false;
		for (ItemStack p : piezas) alguna |= esLimbo(p);
		if (!alguna) return;
		M m = getParentModel();
		VertexConsumer v = buffers.getBuffer(RenderType.armorCutoutNoCull(TEXTURA));
		VertexConsumer brillo = null;
		for (int i = 0; i < piezas.length; i++) {
			if (!esLimbo(piezas[i])) continue;
			dibujarPieza(pose, v, luz, m, i);
			// El brillo de los encantamientos, solo en las piezas encantadas.
			if (piezas[i].hasFoil()) {
				if (brillo == null) brillo = buffers.getBuffer(RenderType.armorEntityGlint());
				dibujarPieza(pose, brillo, luz, m, i);
			}
		}
	}

	/** 0 casco, 1 pechera, 2 pantalones, 3 botas. */
	private static void dibujarPieza(PoseStack pose, VertexConsumer v, int luz, HumanoidModel<?> m, int pieza) {
		switch (pieza) {
			case 0 -> pegado(pose, v, luz, m.head, CABEZA);
			case 1 -> {
				pegado(pose, v, luz, m.body, CUERPO);
				pegado(pose, v, luz, m.rightArm, BRAZO_DER);
				pegado(pose, v, luz, m.leftArm, BRAZO_IZQ);
			}
			case 2 -> {
				pegado(pose, v, luz, m.rightLeg, PANTALON_DER);
				pegado(pose, v, luz, m.leftLeg, PANTALON_IZQ);
			}
			default -> {
				pegado(pose, v, luz, m.rightLeg, BOTA_DER);
				pegado(pose, v, luz, m.leftLeg, BOTA_IZQ);
			}
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
