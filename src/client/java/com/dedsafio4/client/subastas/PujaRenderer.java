package com.dedsafio4.client.subastas;

import com.dedsafio4.subastas.PujaBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import com.dedsafio4.subastas.Puja;

/**
 * Dibuja el Bloque de Puja: lo que se puja girando y subiendo y bajando, con la puja actual arriba y lo menos que se
 * puede pujar abajo (con los íconos de las deditas).
 */
public class PujaRenderer implements BlockEntityRenderer<PujaBlockEntity> {
	public PujaRenderer(BlockEntityRendererProvider.Context contexto) {}

	@Override
	public void render(PujaBlockEntity bloque, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		ItemStack item = bloque.item();
		if (item.isEmpty() || bloque.getLevel() == null) return;
		Minecraft mc = Minecraft.getInstance();
		float tiempo = (bloque.getLevel().getGameTime() % 7200L) + parcial;
		int brillo = LightTexture.FULL_BRIGHT;

		pose.pushPose();
		pose.translate(0.5, 0.45 + Math.sin(tiempo * 0.05) * 0.06, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(tiempo * 4.5f % 360f));
		pose.scale(0.75f, 0.75f, 0.75f);
		mc.getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, brillo, overlay, pose, buffers, bloque.getLevel(), 0);
		pose.popPose();

		Component nombre = item.getCount() > 1 ? item.getHoverName().copy().append(" x" + item.getCount()) : item.getHoverName();
		Component actual = bloque.actual() > 0
				? Component.literal("Puja: ").withStyle(ChatFormatting.GOLD).append(Puja.cantidad(bloque.actual()))
				: Component.literal("Sin pujas todavía").withStyle(ChatFormatting.GOLD);
		Component minimo = Component.literal("Mínimo: ").withStyle(ChatFormatting.GRAY).append(Puja.cantidad(bloque.siguiente()));
		// Arriba el nombre y la puja actual; abajo lo menos que se puede pujar. Siempre mirando a la cámara.
		texto(mc, pose, buffers, 1.27, brillo, nombre, actual);
		texto(mc, pose, buffers, 0.05, brillo, minimo);
	}

	private static void texto(Minecraft mc, PoseStack pose, MultiBufferSource buffers, double alto, int brillo, Component... lineas) {
		pose.pushPose();
		pose.translate(0.5, alto, 0.5);
		pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
		pose.scale(0.02f, -0.02f, 0.02f);
		Font letra = mc.font;
		Matrix4f matriz = pose.last().pose();
		int fondo = (int) (mc.options.getBackgroundOpacity(0.25f) * 255f) << 24;
		for (int i = 0; i < lineas.length; i++) {
			letra.drawInBatch(lineas[i], -letra.width(lineas[i]) / 2f, i * 11, 0xFFFFFFFF, false, matriz, buffers, Font.DisplayMode.NORMAL, fondo, brillo);
		}
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(PujaBlockEntity bloque) {
		return true;
	}
}
