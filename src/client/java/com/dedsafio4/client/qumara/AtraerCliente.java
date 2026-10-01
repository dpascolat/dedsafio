package com.dedsafio4.client.qumara;

import com.dedsafio4.qumara.QumaraEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * El Atraer de Qumara (botón 5) lo hace el cliente de cada jugador: cada tick, antes de moverse, su
 * velocidad horizontal apunta a la planta (así va igual caminando que saltando).
 *
 * Mientras te trae no podés saltar y aparece la barra de aguante: manteniendo el espacio apretado te
 * resistís y te trae mucho más lento, pero la barra se gasta (4 segundos) y en los costados saltan chispas rojas; al soltar
 * se vuelve a llenar. Vacía, no te podés resistir.
 */
public final class AtraerCliente {
	private AtraerCliente() {}

	/** Con el espacio apretado te trae a esta parte de la velocidad (1,5 bloques por segundo). */
	private static final double RESISTIENDO = 0.15;
	/** Cuánto dura la barra resistiendo, y cuánto tarda en llenarse de nuevo (segundos). */
	private static final float DURA = 4, LLENA = 6;

	private static float aguante = 1;
	private static boolean resistiendo, atraido;

	/** La Qumara que está atrayendo a este jugador (o null). */
	private static QumaraEntity atrae(Minecraft mc) {
		if (mc.player == null || mc.level == null) return null;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof QumaraEntity q && q.atrayendo() && q.esBlanco(mc.player)) return q;
		}
		return null;
	}

	public static void registrar() {
		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			if (mc.player == null || mc.level == null || mc.isPaused()) return;
			QumaraEntity q = atrae(mc);
			if (q == null) {
				// Entre Atraer y Atraer la barra se llena del todo.
				atraido = false;
				resistiendo = false;
				aguante = 1;
				return;
			}
			atraido = true;
			resistiendo = mc.options.keyJump.isDown() && mc.screen == null && aguante > 0;
			aguante = resistiendo ? Math.max(0, aguante - 1 / (DURA * 20)) : Math.min(1, aguante + 1 / (LLENA * 20));
			double dx = q.getX() - mc.player.getX(), dz = q.getZ() - mc.player.getZ(), d = Math.sqrt(dx * dx + dz * dz);
			if (d < QumaraEntity.RADIO_CERCA - 1) return;   // ya llegó
			double v = QumaraEntity.VELOCIDAD_ATRAER * (resistiendo ? RESISTIENDO : 1);
			Vec3 actual = mc.player.getDeltaMovement();
			mc.player.setDeltaMovement(dx / d * v, actual.y, dz / d * v);
		});
	}

	/** ¿Te está atrayendo ahora? (entonces no podés saltar: LivingEntitySaltoMixin) */
	public static boolean atraido() {
		return atraido;
	}

	/** Para las pruebas. */
	public static float aguante() {
		return aguante;
	}

	/** La barra: blanca y brillante como un tubo, con chispas rojas en las puntas mientras te resistís. */
	public static void dibujar(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (!atraido || mc.options.hideGui) return;
		int ancho = 80, alto = 9, x0 = g.guiWidth() / 2 - ancho / 2, y0 = g.guiHeight() / 2 + 28;
		// Borde gris con las esquinas redondeadas y el fondo oscuro de lo gastado.
		g.fill(x0 + 1, y0, x0 + ancho - 1, y0 + alto, 0xFF5E6268);
		g.fill(x0, y0 + 1, x0 + ancho, y0 + alto - 1, 0xFF5E6268);
		g.fill(x0 + 1, y0 + 1, x0 + ancho - 1, y0 + alto - 1, 0xAA1A1C20);
		// El relleno: de arriba a abajo, blanco → celeste grisáceo (como un tubo).
		int lleno = Math.round((ancho - 2) * aguante);
		int[] filas = {0xFFFFFFFF, 0xFFFFFFFF, 0xFFF4F7FA, 0xFFE6ECF1, 0xFFD6DEE5, 0xFFC4CDD6, 0xFFAEB8C2};
		for (int i = 0; i < filas.length; i++) {
			int x1 = x0 + 1 + lleno - (i == 0 || i == filas.length - 1 ? 1 : 0);
			if (x1 > x0 + 1) g.fill(x0 + 1 + (i == 0 || i == filas.length - 1 ? 1 : 0), y0 + 1 + i, x1, y0 + 2 + i, filas[i]);
		}
		if (aguante < 0.25f && (System.currentTimeMillis() / 200) % 2 == 0) g.fill(x0 + 1, y0 + 1, x0 + ancho - 1, y0 + alto - 1, 0x40FF3030);
		// Chispas rojas en las dos puntas mientras te resistís.
		if (resistiendo) {
			long t = System.currentTimeMillis() / 60;
			for (int lado = 0; lado < 2; lado++) {
				int bx = lado == 0 ? x0 - 3 : x0 + ancho + 1;
				for (int k = 0; k < 4; k++) {
					long h = (t + k * 7 + lado * 13) * 2654435761L;
					int dx = (int) ((h >>> 8) % 5) - 2 + (lado == 0 ? -1 : 1) * (int) ((h >>> 16) % 3);
					int dy = (int) ((h >>> 4) % (alto + 4)) - 2;
					int color = (h & 1) == 0 ? 0xFFE02020 : 0xFFFF6A50;
					g.fill(bx + dx, y0 + dy, bx + dx + 2, y0 + dy + 2, color);
				}
			}
		}
	}
}
