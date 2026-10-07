package com.dedsafio4.client;

import com.dedsafio4.aviso.Aviso;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * El cartel de /aviso arriba a la izquierda: fondo oscuro, el título en grande con su ícono, el texto en el color
 * del aviso y abajo "[Presiona O] para ocultar aviso". Se dibuja siempre del mismo tamaño (como con escala de
 * interfaz 2), sea cual sea la escala que tenga cada uno.
 */
public final class AvisoCliente {
	private AvisoCliente() {}

	public static final KeyMapping OCULTAR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.dedsafio4.ocultar_aviso", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.dedsafio4"));

	private static Aviso.Payload actual;
	private static boolean oculto;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(Aviso.Payload.TYPE, (payload, context) -> context.client().execute(() -> {
			actual = payload.texto().isEmpty() ? null : payload;
			oculto = false;
		}));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (OCULTAR.consumeClick()) if (actual != null) oculto = true;
		});
		HudRenderCallback.EVENT.register((g, contador) -> dibujar(g));
	}

	private static int oscurecer(int rgb, float f) {
		return ((int) ((rgb >> 16 & 255) * f) << 16) | ((int) ((rgb >> 8 & 255) * f) << 8) | (int) ((rgb & 255) * f);
	}

	private static void dibujar(GuiGraphics g) {
		if (actual == null || oculto) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) return;
		Font font = mc.font;
		// Siempre como con escala de interfaz 2.
		float escala = 2f / (float) mc.getWindow().getGuiScale();
		int anchoPantalla = (int) (g.guiWidth() / escala);
		int ancho = Math.min(370, anchoPantalla - 12);
		int color = 0xFF000000 | actual.color();
		List<FormattedCharSequence> renglones = font.split(Component.literal(actual.texto()), ancho - 16);
		boolean hayTitulo = !actual.titulo().isEmpty();
		int alto = 10 + (hayTitulo ? 24 : 0) + renglones.size() * 10 + 8;

		g.pose().pushPose();
		g.pose().scale(escala, escala, 1);
		int x = 6, y = 6;
		g.fillGradient(x, y, x + ancho, y + alto, 0xE0281410, 0xD0401C14);
		g.fill(x, y, x + ancho, y + 1, 0x80000000 | actual.color());
		int ty = y + 8;
		if (hayTitulo) {
			// El título en grande (x1,5), con un circulito del color adelante.
			g.pose().pushPose();
			g.pose().translate(x + 8, ty, 0);
			g.pose().scale(1.5f, 1.5f, 1);
			Component titulo = Component.literal("● ").withColor(oscurecer(actual.color(), 0.8f))
					.append(Component.literal(actual.titulo()).withStyle(Style.EMPTY.withBold(true).withColor(actual.color())));
			g.drawString(font, titulo, 0, 0, color, true);
			g.pose().popPose();
			ty += 22;
		}
		for (FormattedCharSequence r : renglones) {
			g.drawString(font, r, x + 8, ty, color, true);
			ty += 10;
		}
		// Abajo, afuera del cartel.
		g.fill(x, y + alto, x + ancho, y + alto + 18, 0x90201010);
		g.drawString(font, "[Presiona O] para ocultar aviso", x + 8, y + alto + 5, 0xFFD8D8D8, true);
		g.pose().popPose();
	}
}
