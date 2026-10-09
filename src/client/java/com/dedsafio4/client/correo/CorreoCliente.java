package com.dedsafio4.client.correo;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.correo.Correo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * El correo del lado del cliente: guarda los mensajes que manda el servidor, abre las pantallas y dibuja el sobre
 * con los mensajes sin leer (arriba a la derecha, debajo de las deditas).
 */
public final class CorreoCliente {
	private CorreoCliente() {}

	private static final ResourceLocation SOBRE = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/sobre.png");
	private static List<Correo.CartaVista> cartas = List.of();
	/** Cuándo llegó la lista (para ir sumando el tiempo de cada mensaje). */
	private static long recibido;

	public static List<Correo.CartaVista> cartas() {
		return cartas;
	}

	public static Correo.CartaVista carta(int id) {
		for (Correo.CartaVista c : cartas) if (c.id() == id) return c;
		return null;
	}

	/** Hace cuántos segundos llegó (ahora mismo). */
	public static long segundos(Correo.CartaVista c) {
		return c.segundos() + (Util.getMillis() - recibido) / 1000;
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(Correo.BuzonPayload.TYPE, (payload, context) -> {
			cartas = List.copyOf(payload.cartas());
			recibido = Util.getMillis();
		});
		ClientPlayNetworking.registerGlobalReceiver(Correo.AbiertaPayload.TYPE, (payload, context) -> Correo.ABIERTA_CLIENTE = payload.id());
		ClientPlayNetworking.registerGlobalReceiver(Correo.ResultadoPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			Minecraft mc = context.client();
			if (payload.error().isEmpty()) mc.setScreen(new BuzonScreen());
			else if (mc.screen instanceof CorreoScreen s) s.error(payload.error());
		}));
	}

	/** El sobre con los mensajes sin leer, debajo del recuadro de las deditas (con su mismo estilo). */
	public static void dibujarSobre(GuiGraphics g, Minecraft mc, int yArriba) {
		long sinLeer = cartas.stream().filter(c -> !c.leida()).count();
		if (sinLeer == 0) return;
		String n = Long.toString(sinLeer);
		int relleno = 3, icono = 10;
		int ancho = relleno * 2 + mc.font.width(n) + 4 + icono;
		int alto = relleno + Math.max(icono, mc.font.lineHeight) + relleno;
		g.pose().pushPose();
		g.pose().translate(g.guiWidth() - 3, yArriba, 0);
		g.pose().scale(0.75f, 0.75f, 1);
		int x = -ancho;
		g.fill(x, 0, 0, alto, 0xFF55555A);             // borde
		g.fill(x + 1, 1, -1, alto - 1, 0xFF1E1E22);    // fondo
		g.drawString(mc.font, n, x + relleno, relleno + 2, 0xFFFFFFFF, true);
		g.blit(SOBRE, x + relleno + mc.font.width(n) + 4, relleno, 0, 0, icono, icono, icono, icono);
		g.pose().popPose();
	}
}
