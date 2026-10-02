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
 * Mientras te trae no podés saltar. Para resistirte hay que hacer clicks con el espacio (apretar y soltar):
 * mantenerlo apretado no cuenta. Cada click te deja resistiendo un ratito y te trae muy lento; no se gasta nunca,
 * así que mientras sigas apretando te podés resistir todo lo que quieras. En los costados del cartel saltan
 * chispas rojas mientras te resistís. Con el espacio apretado no podés caminar (la cámara sí se mueve), así no se
 * puede resistir y escapar a la vez (ver KeyboardInputAtraerMixin).
 */
public final class AtraerCliente {
	private AtraerCliente() {}

	/** Resistiéndote te trae a esta parte de la velocidad (medio bloque por segundo: muy lento). */
	private static final double RESISTIENDO = 0.05;
	/** Cada click en el espacio te deja resistiendo este tiempo (en ticks): hay que seguir apretando. */
	private static final int POR_CLICK = 6;

	private static int resistenciaRestante;
	private static boolean resistiendo, atraido, espacioAntes;

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
			// Un click es cuando el espacio pasa de suelto a apretado: mantenerlo no suma (ni con la repetición
			// de teclas de Windows, que por eso no se usan los clicks de la tecla).
			boolean espacio = mc.options.keyJump.isDown() && mc.screen == null;
			boolean click = espacio && !espacioAntes;
			espacioAntes = espacio;
			QumaraEntity q = atrae(mc);
			if (q == null) {
				resistenciaRestante = 0;
				atraido = false;
				resistiendo = false;
				return;
			}
			atraido = true;
			if (click) resistenciaRestante = POR_CLICK;
			else if (resistenciaRestante > 0) resistenciaRestante--;
			resistiendo = resistenciaRestante > 0;
			double dx = q.getX() - mc.player.getX(), dz = q.getZ() - mc.player.getZ(), d = Math.sqrt(dx * dx + dz * dz);
			if (d < QumaraEntity.RADIO_CERCA - 1) return;   // ya llegó
			double v = QumaraEntity.VELOCIDAD_ATRAER * (resistiendo ? RESISTIENDO : 1);
			Vec3 actual = mc.player.getDeltaMovement();
			mc.player.setDeltaMovement(dx / d * v, actual.y, dz / d * v);
		});
	}

	/** ¿Te está atrayendo y tenés el espacio apretado? Entonces no podés caminar (KeyboardInputAtraerMixin). */
	public static boolean sinCaminar() {
		return atraido && espacioAntes;
	}

	/** ¿Te está atrayendo ahora? (entonces no podés saltar: LivingEntitySaltoMixin) */
	public static boolean atraido() {
		return atraido;
	}

	/** Para las pruebas: ya no hay barra que se gaste (siempre llena). */
	public static float aguante() {
		return 1;
	}

	/** El cartel "[ESPACIO] para resistir", con chispas rojas en las puntas mientras te resistís. */
	public static void dibujar(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (!atraido || mc.options.hideGui) return;
		String texto = "Clicks con [" + mc.options.keyJump.getTranslatedKeyMessage().getString().toUpperCase() + "] para resistir";
		int ancho = mc.font.width(texto) + 10, alto = 13, x0 = g.guiWidth() / 2 - ancho / 2, y0 = g.guiHeight() / 2 + 26;
		g.fill(x0, y0, x0 + ancho, y0 + alto, resistiendo ? 0xCC3A1218 : 0xAA1A1C20);
		g.drawString(mc.font, texto, x0 + 5, y0 + 3, resistiendo ? 0xFFFFD0D0 : 0xFFFFFFFF, true);
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
