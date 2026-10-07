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
 * El cartel de /aviso pegado arriba a la izquierda: fondo oscuro y transparente, el título en grande con el ícono ◐,
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
		int color = 0xFF000000 | actual.color();
		List<FormattedCharSequence> renglones = font.split(Component.literal(actual.texto()), ancho - 12);
		boolean hayTitulo = !actual.titulo().isEmpty();
		// Como en el diseño: título grande, el texto, y abajo un buen espacio vacío antes del pie.
		int alto = 8 + (hayTitulo ? 26 : 0) + renglones.size() * 10 + 26;

		g.pose().pushPose();
		g.pose().scale(escala, escala, 1);
		int x = 0, y = 0;
		// Fondo oscuro y transparente (se ve un poco lo de atrás).
		g.fillGradient(x, y, x + ancho, y + alto, 0x9A140A06, 0x8C1E0E08);
		int ty = y + 7;
		if (hayTitulo) {
			g.pose().pushPose();
			g.pose().translate(x + 5, ty, 0);
			g.pose().scale(1.5f, 1.5f, 1);
			Component titulo = Component.literal("◐ ").withColor(actual.color())
					.append(Component.literal(actual.titulo()).withStyle(Style.EMPTY.withBold(true).withColor(actual.color())));
			g.drawString(font, titulo, 0, 0, color, true);
			g.pose().popPose();
			ty += 24;
		}
		for (FormattedCharSequence r : renglones) {
			g.drawString(font, r, x + 6, ty, color, true);
			ty += 10;
		}
		// El pie, abajo del cartel, sin fondo y con la letra más grande.
		int pie = y + alto;
		g.pose().pushPose();
		g.pose().translate(x + 6, pie + 6, 0);
		g.pose().scale(1.3f, 1.3f, 1);
		g.drawString(font, "[Presiona O] para ocultar aviso", 0, 0, 0xFFC8B8AC, true);
		g.pose().popPose();
		g.pose().popPose();
	}
}
