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
 * El cartel de /aviso pegado arriba a la izquierda: fondo como el del chat, el título en grande con el ícono ◐,
 * el texto en el color del aviso y abajo, sin fondo, "[Presiona O] para ocultar aviso". Se dibuja siempre del mismo tamaño (como con escala de
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

	private static void dibujar(GuiGraphics g) {
		if (actual == null || oculto) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) return;
		Font font = mc.font;
		// Siempre como con escala de interfaz 2.
		float escala = 2f / (float) mc.getWindow().getGuiScale();
		int anchoPantalla = (int) (g.guiWidth() / escala);
		int ancho = Math.min(370, anchoPantalla - 4);
		int color = 0xFF000000 | actual.colorTexto();
		List<FormattedCharSequence> renglones = font.split(Component.literal(actual.texto()), ancho - 12);
		boolean hayTitulo = !actual.titulo().isEmpty();
		// Como en el diseño: título grande, el texto, y abajo un buen espacio vacío antes del pie.
		int alto = 8 + (hayTitulo ? 17 : 0) + renglones.size() * 10 + 26;

		g.pose().pushPose();
		g.pose().scale(escala, escala, 1);
		int x = 0, y = 0;
		// El mismo fondo que el chat (negro, con la transparencia que tenga cada uno en Opciones).
		int alfa = (int) (mc.options.textBackgroundOpacity().get() * 255);
		g.fill(x, y, x + ancho, y + alto, alfa << 24);
		int ty = y + 7;
		if (hayTitulo) {
			g.pose().pushPose();
			g.pose().translate(x + 5, ty, 0);
			g.pose().scale(1.2f, 1.2f, 1);   // un poquito más grande que el texto
			Component icono = "cambio".equals(actual.icono())
					? Component.literal(String.valueOf((char) 0xE001)).withStyle(Style.EMPTY.withFont(
							net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.dedsafio4.Dedsafio4.MOD_ID, "iconos")))
							.append(Component.literal(" "))
					: Component.literal("◐ ").withColor(actual.color());
			Component titulo = Component.empty().append(icono)
					.append(Component.literal(actual.titulo()).withStyle(Style.EMPTY.withBold(true).withColor(actual.color())));
			g.drawString(font, titulo, 0, 0, color, true);
			g.pose().popPose();
			ty += 15;
		}
		for (FormattedCharSequence r : renglones) {
			g.drawString(font, r, x + 6, ty, color, true);
			ty += 10;
		}
		// El pie, abajo del cartel: sin fondo, chiquito y medio transparente.
		int pie = y + alto;
		g.pose().pushPose();
		g.pose().translate(x + 6, pie + 4, 0);
		g.pose().scale(1.0f, 1.0f, 1);
		g.drawString(font, "[Presiona O] para ocultar aviso", 0, 0, 0x88C8B8AC, false);
		g.pose().popPose();
		g.pose().popPose();
	}
}
