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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import com.dedsafio4.subastas.Puja;

/**
 * Dibuja el Bloque de Puja (el diseño de Patricio): lo que se puja girando y subiendo y bajando, y debajo, en blanco, el
 * nombre, el Pujador (el que va ganando), la Puja actual, el Incremento mínimo y la Puja mínima (con las deditas).
 * Si hay 4 Bloques de Puja haciendo un cuadrado de 2×2, se dibuja una sola vez, en el medio del cuadrado.
 */
public class PujaRenderer implements BlockEntityRenderer<PujaBlockEntity> {
	public PujaRenderer(BlockEntityRendererProvider.Context contexto) {}

	@Override
	public void render(PujaBlockEntity bloque, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		ItemStack item = bloque.item();
		if (item.isEmpty() || bloque.getLevel() == null) return;
		// En un cuadrado de 4, sólo lo dibuja el de la esquina de abajo (x y z más chicas), corrido al medio.
		double medio = 0.5;
		BlockPos pos = bloque.getBlockPos();
		if (cuadrado(bloque.getLevel(), pos)) medio = 1.0;
		else if (cuadrado(bloque.getLevel(), pos.west()) || cuadrado(bloque.getLevel(), pos.north()) || cuadrado(bloque.getLevel(), pos.west().north())) return;
		Minecraft mc = Minecraft.getInstance();
		float tiempo = (bloque.getLevel().getGameTime() % 7200L) + parcial;
		int brillo = LightTexture.FULL_BRIGHT;

		Component nombre = item.getCount() > 1 ? item.getHoverName().copy().append(" x" + item.getCount()) : item.getHoverName();
		texto(mc, pose, buffers, medio, 1.35, brillo,
				Component.empty().append(nombre).withStyle(ChatFormatting.WHITE),
				Component.literal("Pujador: " + (bloque.pujador().isEmpty() ? "Ninguno" : bloque.pujador())).withStyle(ChatFormatting.WHITE),
				Component.literal("Puja actual: ").withStyle(ChatFormatting.WHITE).append(Puja.cantidad(bloque.actual())),
				Component.literal("Incremento mínimo: ").withStyle(ChatFormatting.WHITE).append(Puja.cantidad(bloque.incremento())),
				Component.literal("Puja mínima: ").withStyle(ChatFormatting.WHITE).append(Puja.cantidad(bloque.siguiente())));

		// El objeto va después del texto: si no, sus partes transparentes (que igual tapan) cortaban letras al girar.
		pose.pushPose();
		pose.translate(medio, 1.75 + Math.sin(tiempo * 0.05) * 0.06, medio);
		pose.mulPose(Axis.YP.rotationDegrees(tiempo * 4.5f % 360f));
		pose.scale(0.75f, 0.75f, 0.75f);
		mc.getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, brillo, overlay, pose, buffers, bloque.getLevel(), 0);
		pose.popPose();
	}

	/** ¿Hay Bloques de Puja en pos y en los tres de al lado hacia +x y +z (un cuadrado de 2×2)? */
	private static boolean cuadrado(Level mundo, BlockPos pos) {
		return mundo.getBlockEntity(pos) instanceof PujaBlockEntity && mundo.getBlockEntity(pos.east()) instanceof PujaBlockEntity
				&& mundo.getBlockEntity(pos.south()) instanceof PujaBlockEntity && mundo.getBlockEntity(pos.east().south()) instanceof PujaBlockEntity;
	}

	private static void texto(Minecraft mc, PoseStack pose, MultiBufferSource buffers, double medio, double alto, int brillo, Component... lineas) {
		pose.pushPose();
		pose.translate(medio, alto, medio);
		pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
		pose.scale(0.02f, -0.02f, 0.02f);
		Font letra = mc.font;
		Matrix4f matriz = pose.last().pose();
		// Fondo oscuro y letras con sombra: así se lee también contra el cielo o las nubes. El fondo va aparte y un poco
		// detrás de las letras: el que pone Minecraft queda apenas adelante y, de lejos, tapaba pedazos de letras.
		int fondo = (int) (Math.max(0.45f, mc.options.getBackgroundOpacity(0.45f)) * 255f) << 24;
		com.mojang.blaze3d.vertex.VertexConsumer vc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.textBackground());
		for (int i = 0; i < lineas.length; i++) {
			float x0 = -letra.width(lineas[i]) / 2f - 1, x1 = -x0, y0 = i * 11 - 1, y1 = i * 11 + 9, z = -2f;   // (en este espacio, -z queda detrás de las letras)
			vc.addVertex(matriz, x0, y0, z).setColor(fondo).setLight(brillo);
			vc.addVertex(matriz, x0, y1, z).setColor(fondo).setLight(brillo);
			vc.addVertex(matriz, x1, y1, z).setColor(fondo).setLight(brillo);
			vc.addVertex(matriz, x1, y0, z).setColor(fondo).setLight(brillo);
		}
		for (int i = 0; i < lineas.length; i++) {
			letra.drawInBatch(lineas[i], -letra.width(lineas[i]) / 2f, i * 11, 0xFFFFFFFF, true, matriz, buffers, Font.DisplayMode.NORMAL, 0, brillo);
		}
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(PujaBlockEntity bloque) {
		return true;
	}
}
