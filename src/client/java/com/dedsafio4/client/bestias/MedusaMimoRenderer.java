package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.MedusaMimoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Dibuja la Medusa Mimo: la medusa o, si ya copió a alguien, ese jugador con su skin (brazos finos o anchos). */
public class MedusaMimoRenderer extends EntityRenderer<MedusaMimoEntity> {
	/** La skin del que copió: se recuerda por si ese jugador se va del servidor. */
	private static final Map<UUID, PlayerSkin> SKINS = new HashMap<>();

	private final FormaMedusa medusa;
	private final FormaJugador ancho, fino;

	public MedusaMimoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		medusa = new FormaMedusa(contexto);
		ancho = new FormaJugador(contexto, false);
		fino = new FormaJugador(contexto, true);
		shadowRadius = 0.6f;
	}

	private static PlayerSkin skin(UUID uuid) {
		var conexion = Minecraft.getInstance().getConnection();
		PlayerInfo info = conexion == null ? null : conexion.getPlayerInfo(uuid);
		if (info != null) SKINS.put(uuid, info.getSkin());
		PlayerSkin guardada = SKINS.get(uuid);
		return guardada != null ? guardada : DefaultPlayerSkin.get(uuid);
	}

	private EntityRenderer<MedusaMimoEntity> forma(MedusaMimoEntity bicho) {
		UUID copia = bicho.copiado();
		if (copia == null) return medusa;
		return skin(copia).model() == PlayerSkin.Model.SLIM ? fino : ancho;
	}

	@Override
	public void render(MedusaMimoEntity bicho, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		forma(bicho).render(bicho, giro, parcial, pose, buffers, luz);
	}

	@Override
	public ResourceLocation getTextureLocation(MedusaMimoEntity bicho) {
		return forma(bicho).getTextureLocation(bicho);
	}

	private static final class FormaMedusa extends MobRenderer<MedusaMimoEntity, MedusaMimoModelo> {
		private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/entity/medusa_mimo.png");

		FormaMedusa(EntityRendererProvider.Context contexto) {
			super(contexto, new MedusaMimoModelo(contexto.bakeLayer(MedusaMimoModelo.CAPA)), 1.2f);
		}

		@Override
		public ResourceLocation getTextureLocation(MedusaMimoEntity bicho) {
			return TEXTURA;
		}

		@Override
		protected RenderType getRenderType(MedusaMimoEntity bicho, boolean visible, boolean translucido, boolean brillo) {
			return RenderType.entityTranslucent(TEXTURA);
		}

		/** Brilla un poco en la oscuridad. */
		@Override
		protected int getBlockLightLevel(MedusaMimoEntity bicho, BlockPos pos) {
			return 11;
		}
	}

	private static final class FormaJugador extends LivingEntityRenderer<MedusaMimoEntity, PlayerModel<MedusaMimoEntity>> {
		FormaJugador(EntityRendererProvider.Context contexto, boolean finos) {
			super(contexto, new PlayerModel<>(contexto.bakeLayer(finos ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), finos), 0.5f);
			addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(contexto.bakeLayer(finos ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
					new HumanoidArmorModel<>(contexto.bakeLayer(finos ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)), contexto.getModelManager()));
			addLayer(new ItemInHandLayer<>(this, contexto.getItemInHandRenderer()));
		}

		@Override
		public ResourceLocation getTextureLocation(MedusaMimoEntity bicho) {
			return skin(bicho.copiado()).texture();
		}

		private static HumanoidModel.ArmPose brazo(MedusaMimoEntity bicho, InteractionHand mano) {
			ItemStack pila = bicho.getItemInHand(mano);
			if (pila.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
			if (!bicho.isUsingItem() || bicho.getUsedItemHand() != mano) return HumanoidModel.ArmPose.ITEM;
			UseAnim uso = pila.getUseAnimation();
			return uso == UseAnim.BLOCK ? HumanoidModel.ArmPose.BLOCK : uso == UseAnim.BOW ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.ITEM;
		}

		@Override
		public void render(MedusaMimoEntity bicho, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
			model.rightArmPose = brazo(bicho, InteractionHand.MAIN_HAND);
			model.leftArmPose = brazo(bicho, InteractionHand.OFF_HAND);
			super.render(bicho, giro, parcial, pose, buffers, luz);
		}

		/** Del tamaño de un jugador. */
		@Override
		protected void scale(MedusaMimoEntity bicho, PoseStack pose, float parcial) {
			pose.scale(0.9375f, 0.9375f, 0.9375f);
		}
	}
}
