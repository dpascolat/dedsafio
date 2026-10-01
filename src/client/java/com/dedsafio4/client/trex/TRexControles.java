package com.dedsafio4.client.trex;

import com.dedsafio4.qumara.QumaraEntity;
import com.dedsafio4.trex.ModTRex;
import com.dedsafio4.trex.TRexEntity;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

/**
 * Los botones de los jefes que se manejan (se pueden cambiar en Controles):
 * - T-Rex: R gritar, Z abrir puerta.
 * - Qumara: R nacer, Z giro, V círculos, B atraer, N gas (el Enfriamiento no es un botón: pasa solo en los pinchitos de la barra).
 */
public final class TRexControles {
	private TRexControles() {}

	public static final KeyMapping GRITAR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.dedsafio4.trex_gritar", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.dedsafio4"));
	public static final KeyMapping ABRIR_PUERTA = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.dedsafio4.jefe_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.dedsafio4"));   // (la G es del Catálogo)

	public static final KeyMapping BOTON_4 = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.dedsafio4.jefe_4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.dedsafio4"));
	public static final KeyMapping BOTON_5 = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.dedsafio4.jefe_5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.dedsafio4"));
	public static final KeyMapping BOTON_6 = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.dedsafio4.jefe_6", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.dedsafio4"));

	private static boolean montaJefe(Minecraft mc) {
		// Solo el que la maneja (no el que tiene agarrado Qumara).
		return mc.player != null && (mc.player.getVehicle() instanceof TRexEntity || mc.player.getVehicle() instanceof QumaraEntity)
				&& mc.player.getVehicle().getControllingPassenger() == mc.player;
	}

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			boolean monta = montaJefe(mc);
			while (GRITAR.consumeClick()) if (monta) ClientPlayNetworking.send(new ModTRex.AccionPayload(ModTRex.GRITAR));
			while (ABRIR_PUERTA.consumeClick()) if (monta) ClientPlayNetworking.send(new ModTRex.AccionPayload(ModTRex.ABRIR_PUERTA));
			while (BOTON_4.consumeClick()) if (monta && mc.player.getVehicle() instanceof QumaraEntity) {
				ClientPlayNetworking.send(new ModTRex.AccionPayload(ModTRex.BOTON_4));
			}
			while (BOTON_5.consumeClick()) if (monta && mc.player.getVehicle() instanceof QumaraEntity) {
				ClientPlayNetworking.send(new ModTRex.AccionPayload(ModTRex.BOTON_5));
			}
			while (BOTON_6.consumeClick()) if (monta && mc.player.getVehicle() instanceof QumaraEntity) {
				ClientPlayNetworking.send(new ModTRex.AccionPayload(ModTRex.BOTON_6));
			}
		});
	}

	private static String tecla(KeyMapping k) {
		return "[" + k.getTranslatedKeyMessage().getString() + "] ";
	}

	/** Mientras lo manejás, arriba se ven los botones. */
	public static void dibujar(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (!montaJefe(mc) || mc.options.hideGui) return;
		Entity jefe = mc.player.getVehicle();
		String texto;
		if (jefe instanceof QumaraEntity q) {
			String giro = q.recargaBarrido() > 0 ? "Giro (" + (int) Math.ceil(q.recargaBarrido() / 20f) + " s)" : "Giro";
			String circulos = "Círculos nivel " + q.nivelCirculos() + (q.recargaCirculos() > 0 ? " (" + (int) Math.ceil(q.recargaCirculos() / 20f) + " s)" : "");
			String atraer = q.atrayendo() ? "Atrayendo..." : q.recargaAtraer() > 0 ? "Atraer (" + (int) Math.ceil(q.recargaAtraer() / 20f) + " s)" : "Atraer";
			texto = (q.nacida() ? "" : tecla(GRITAR) + "Nacer     ") + tecla(ABRIR_PUERTA) + giro + "     " + tecla(BOTON_4) + circulos
					+ "     " + tecla(BOTON_5) + atraer
					+ "     " + tecla(BOTON_6) + (q.recargaGas() > 0 ? "Gas (" + (int) Math.ceil(q.recargaGas() / 20f) + " s)" : "Gas")
					+ (q.debil() ? "     (Enfriamiento)" : "");
		} else {
			texto = tecla(GRITAR) + "Gritar     " + tecla(ABRIR_PUERTA) + "Abrir puerta";
		}
		int ancho = mc.font.width(texto), x = (g.guiWidth() - ancho) / 2, y = 8;
		if (jefe instanceof QumaraEntity) y = 52;   // debajo de la barra de vida de Qumara
		g.fill(x - 6, y - 4, x + ancho + 6, y + 12, 0xAA1D1426);
		g.drawString(mc.font, texto, x, y, 0xFFF2B82E, true);
	}
}
