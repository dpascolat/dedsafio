package com.dedsafio4.client.subastas;

import com.dedsafio4.subastas.VitrinaBlockEntity;
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

/** Dibuja la Vitrina: el ítem a la venta girando (una vuelta entera cada 4 segundos), subiendo y bajando, con el precio arriba. */
public class VitrinaRenderer implements BlockEntityRenderer<VitrinaBlockEntity> {
	public VitrinaRenderer(BlockEntityRendererProvider.Context contexto) {}

	@Override
	public void render(VitrinaBlockEntity vitrina, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		ItemStack item = vitrina.item();
		if (item.isEmpty() || vitrina.getLevel() == null) return;
		Minecraft mc = Minecraft.getInstance();
		float tiempo = (vitrina.getLevel().getGameTime() % 7200L) + parcial;
		int brillo = LightTexture.FULL_BRIGHT;

		pose.pushPose();
		pose.translate(0.5, 0.45 + Math.sin(tiempo * 0.05) * 0.06, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(tiempo * 4.5f % 360f));
		pose.scale(0.75f, 0.75f, 0.75f);
		mc.getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, brillo, overlay, pose, buffers, vitrina.getLevel(), 0);
		pose.popPose();

		// El nombre y el precio arriba, siempre mirando a la cámara (como el nombre de un jugador).
		pose.pushPose();
		pose.translate(0.5, 1.15, 0.5);
		pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
		pose.scale(0.02f, -0.02f, 0.02f);
		Font letra = mc.font;
		Matrix4f matriz = pose.last().pose();
		Component nombre = item.getCount() > 1 ? item.getHoverName().copy().append(" x" + item.getCount()) : item.getHoverName();
		Component precio = Component.literal(vitrina.precio() + " deditas").withStyle(ChatFormatting.GOLD);
		int fondo = (int) (mc.options.getBackgroundOpacity(0.25f) * 255f) << 24;
		letra.drawInBatch(nombre, -letra.width(nombre) / 2f, 0, 0xFFFFFFFF, false, matriz, buffers, Font.DisplayMode.NORMAL, fondo, brillo);
		letra.drawInBatch(precio, -letra.width(precio) / 2f, 11, 0xFFFFFFFF, false, matriz, buffers, Font.DisplayMode.NORMAL, fondo, brillo);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(VitrinaBlockEntity vitrina) {
		return true;
	}
}
