package com.dedsafio4.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * La capa de los miembros de una Hermandad con estandarte activo: el estandarte, del tamaño de una
 * capa y moviéndose igual que la capa de Minecraft.
 */
public class CapaEstandarteLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private final ModelPart bandera;

	public CapaEstandarteLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, EntityModelSet modelos) {
		super(padre);
		this.bandera = modelos.bakeLayer(ModelLayers.BANNER).getChild(BannerRenderer.FLAG);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador, float limbSwing,
					   float limbSwingAmount, float parcial, float edad, float cabezaGiro, float cabezaArriba) {
		if (jugador.isInvisible() || !jugador.isModelPartShown(PlayerModelPart.CAPE)) return;
		if (jugador.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
		ItemStack estandarte = HermandadesCliente.estandarteDe(jugador.getUUID());
		if (!(estandarte.getItem() instanceof BannerItem item)) return;

		pose.pushPose();
		pose.translate(0f, 0f, 0.125f);
		// El mismo movimiento que la capa de Minecraft (CapeLayer).
		double dx = Mth.lerp(parcial, jugador.xCloakO, jugador.xCloak) - Mth.lerp(parcial, jugador.xo, jugador.getX());
		double dy = Mth.lerp(parcial, jugador.yCloakO, jugador.yCloak) - Mth.lerp(parcial, jugador.yo, jugador.getY());
		double dz = Mth.lerp(parcial, jugador.zCloakO, jugador.zCloak) - Mth.lerp(parcial, jugador.zo, jugador.getZ());
		float cuerpo = Mth.rotLerp(parcial, jugador.yBodyRotO, jugador.yBodyRot);
		double seno = Mth.sin(cuerpo * Mth.DEG_TO_RAD), coseno = -Mth.cos(cuerpo * Mth.DEG_TO_RAD);
		float sube = Mth.clamp((float) dy * 10f, -6f, 32f);
		float atras = Mth.clamp((float) (dx * seno + dz * coseno) * 100f, 0f, 150f);
		float costado = Mth.clamp((float) (dx * coseno - dz * seno) * 100f, -20f, 20f);
		float balanceo = Mth.lerp(parcial, jugador.oBob, jugador.bob);
		sube += Mth.sin(Mth.lerp(parcial, jugador.walkDistO, jugador.walkDist) * 6f) * 32f * balanceo;
		if (jugador.isCrouching()) sube += 25f;
		pose.mulPose(Axis.XP.rotationDegrees(6f + atras / 2f + sube));
		pose.mulPose(Axis.ZP.rotationDegrees(costado / 2f));
		pose.mulPose(Axis.YP.rotationDegrees(180f - costado / 2f));

		// La bandera del estandarte mide 20x40 píxeles; la capa, 10x16.
		pose.scale(0.5f, 0.4f, 1f);
		bandera.x = 0f;
		bandera.y = 0f;
		bandera.z = 1f;
		bandera.xRot = 0f;
		BannerPatternLayers dibujos = estandarte.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
		BannerRenderer.renderPatterns(pose, buffers, luz, OverlayTexture.NO_OVERLAY, bandera, ModelBakery.BANNER_BASE,
				true, item.getColor(), dibujos);
		pose.popPose();
	}
}
