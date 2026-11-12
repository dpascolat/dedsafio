package com.dedsafio4.client;

import com.dedsafio4.brazalete.Brazalete;
import com.dedsafio4.brazalete.TrajeEspacial;
import com.dedsafio4.items.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * El visor del Traje Espacial (con la J del Brazalete puesto): reemplaza los corazones, la comida, la armadura, el aire,
 * la experiencia y la barra de ítems de Minecraft (los esconde GuiTrajeMixin) por un visor de nave:
 *   arriba la brújula; a la izquierda el círculo de la batería del Brazalete (con su ícono), la hora, el estado de los
 *   propulsores y el monitor cardíaco; abajo INTEGRITY (la vida) y BIO-SUSTAIN (la comida y el nivel), la barra de
 *   ítems con otro estilo y, a la derecha, el panel de MODE y VEL.
 */
public final class TrajeHud {
	private TrajeHud() {}

	private static final int CELESTE = 0xFF49C8E5, CELESTE_SUAVE = 0x8049C8E5, NARANJA = 0xFFF08A2A, BLANCO = 0xFFE8F4F8,
			GRIS = 0xFF8A98A0, OSCURO = 0xB0101418, ROJO = 0xFFFF4F4F;

	/** ¿Se muestra el visor? Con el traje puesto, en el mundo y con la interfaz visible. */
	public static boolean activo() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && !mc.options.hideGui && !mc.player.isSpectator() && TrajeEspacial.puesto(mc.player);
	}

	public static void dibujar(GuiGraphics g, DeltaTracker tiempo) {
		if (!activo()) return;
		Minecraft mc = Minecraft.getInstance();
		Player p = mc.player;
		Font f = mc.font;
		int w = g.guiWidth(), h = g.guiHeight();
		float parcial = tiempo.getGameTimeDeltaPartialTick(false);
		brujula(g, f, w, Mth.lerp(parcial, p.yRotO, p.getYRot()));
		bateria(g, f, p, h);
		inferior(g, f, p, w, h);
		panel(g, f, p, w, h);
		barra(g, f, p, w, h, tiempo);
		String pie = "mitriOS | [J] CUT THRUSTERS";
		g.drawString(f, pie, (w - f.width(pie)) / 2, h - 9, CELESTE_SUAVE, false);
	}

	// ---------------------------------------------------------------- Brújula

	private static void brujula(GuiGraphics g, Font f, int w, float yaw) {
		int x0 = Math.round(w * 0.28f), x1 = Math.round(w * 0.72f), y = 17, mitad = (x0 + x1) / 2;
		float porGrado = (x1 - x0) / 180f;   // se ven 180 grados
		g.fill(x0, y, x1, y + 1, CELESTE_SUAVE);
		String[] letras = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
		for (int grado = 0; grado < 360; grado += 15) {
			float d = Mth.wrapDegrees(grado - yaw);
			if (Math.abs(d) > 90) continue;
			int x = Math.round(mitad + d * porGrado);
			boolean grande = grado % 45 == 0;
			g.fill(x, y - (grande ? 4 : 2), x + 1, y, CELESTE_SUAVE);
			if (grado % 90 == 0) {
				String l = letras[grado / 45];
				g.drawString(f, l, x - f.width(l) / 2, y - 13, GRIS, false);
			}
		}
		// La marca del medio y hacia dónde mira.
		g.fill(mitad - 2, y + 2, mitad + 3, y + 3, NARANJA);
		g.fill(mitad - 1, y + 3, mitad + 2, y + 4, NARANJA);
		g.fill(mitad, y + 4, mitad + 1, y + 5, NARANJA);
		String rumbo = letras[Math.floorMod(Math.round(Mth.wrapDegrees(yaw) / 45f), 8)];
		g.drawString(f, rumbo, mitad - f.width(rumbo) / 2, y - 13, NARANJA, false);
	}

	// ---------------------------------------------------------------- Batería del Brazalete

	private static ItemStack brazalete(Player p) {
		if (p.getOffhandItem().is(ModItems.BRAZALETE_INTERDIMENSIONAL)) return p.getOffhandItem();
		if (p.getMainHandItem().is(ModItems.BRAZALETE_INTERDIMENSIONAL)) return p.getMainHandItem();
		return ItemStack.EMPTY;
	}

	private static void bateria(GuiGraphics g, Font f, Player p, int h) {
		ItemStack brazalete = brazalete(p);
		int porcentaje = brazalete.isEmpty() ? 0 : Math.round(100f * Brazalete.combustible(brazalete) / Brazalete.COMBUSTIBLE);
		int cx = 40, cy = h / 2 - 30, r = 24;
		// El aro: gris de fondo y celeste lo que queda de batería (desde arriba, en el sentido del reloj).
		for (int grado = 0; grado < 360; grado += 2) {
			double a = Math.toRadians(grado - 90);
			boolean lleno = grado < porcentaje * 3.6f;
			int color = lleno ? (porcentaje <= 15 ? ROJO : CELESTE) : 0x60404850;
			for (int k = 0; k < 3; k++) {
				int x = cx + (int) Math.round(Math.cos(a) * (r + k)), y = cy + (int) Math.round(Math.sin(a) * (r + k));
				g.fill(x, y, x + 1, y + 1, color);
			}
		}
		// Un arco naranja de adorno por fuera, abajo a la izquierda.
		for (int grado = 100; grado < 220; grado += 2) {
			double a = Math.toRadians(grado);
			int x = cx + (int) Math.round(Math.cos(a) * (r + 6)), y = cy + (int) Math.round(Math.sin(a) * (r + 6));
			g.fill(x, y, x + 1, y + 1, 0xA0F08A2A);
		}
		// El Brazalete arriba del aro y el porcentaje grande en el medio.
		if (!brazalete.isEmpty()) g.renderItem(brazalete, cx - 8, cy - r - 20);
		String texto = porcentaje + "%";
		g.pose().pushPose();
		g.pose().translate(cx, cy - 6, 0);
		g.pose().scale(1.6f, 1.6f, 1f);
		g.drawString(f, texto, -f.width(texto) / 2, 0, porcentaje <= 15 ? ROJO : CELESTE, false);
		g.pose().popPose();
		int y = cy + r + 8;
		g.drawString(f, porcentaje + "/100", cx - 30, y, GRIS, false);
		long dia = p.level().getDayTime() % 24000;
		int hora = (int) ((dia / 1000 + 6) % 24), minutos = (int) (dia % 1000 * 60 / 1000);
		g.drawString(f, String.format("%02d:%02d", hora, minutos), cx - 30, y + 10, BLANCO, false);
		boolean volando = p.getAbilities().flying;
		g.drawString(f, volando ? "THRUSTERS ONLINE" : "THRUSTERS STANDBY", cx - 30, y + 20, volando ? CELESTE : GRIS, false);
		cardiaco(g, f, p, cx - 30, y + 34);
	}

	/** El monitor cardíaco: las pulsaciones suben si vuela rápido o si le queda poca vida. */
	private static void cardiaco(GuiGraphics g, Font f, Player p, int x, int y) {
		double velocidad = p.getDeltaMovement().length() * 20;
		int bpm = Mth.clamp((int) (72 + velocidad * 3 + (1 - p.getHealth() / p.getMaxHealth()) * 60), 60, 190);
		g.drawString(f, "CARDIAC", x, y, GRIS, false);
		String b = bpm + " BPM";
		g.drawString(f, b, x + 84 - f.width(b), y, BLANCO, false);
		int x0 = x, y0 = y + 11, ancho = 84, alto = 18;
		g.fill(x0, y0, x0 + ancho, y0 + 1, 0x60404850);
		g.fill(x0, y0 + alto, x0 + ancho, y0 + alto + 1, 0x60404850);
		g.fill(x0, y0, x0 + 1, y0 + alto, 0x60404850);
		g.fill(x0 + ancho - 1, y0, x0 + ancho, y0 + alto, 0x60404850);
		// La línea: plana con un latido que se repite según las pulsaciones.
		float t = (Util_millis() / 1000f) * bpm / 60f;
		int medio = y0 + alto / 2;
		for (int i = 1; i < ancho - 1; i++) {
			float fase = (t + i / (float) ancho * 1.5f) % 1f;
			int dy = fase < 0.06f ? -Math.round(Mth.sin(fase / 0.06f * Mth.PI) * 7)
					: fase < 0.10f ? Math.round(Mth.sin((fase - 0.06f) / 0.04f * Mth.PI) * 3) : 0;
			g.fill(x0 + i, medio + dy, x0 + i + 1, medio + dy + 1, CELESTE);
		}
	}

	private static long Util_millis() {
		return net.minecraft.Util.getMillis();
	}

	// ---------------------------------------------------------------- Abajo: INTEGRITY y BIO-SUSTAIN

	private static void inferior(GuiGraphics g, Font f, Player p, int w, int h) {
		int y = h - 54, mitad = w / 2;
		// INTEGRITY: la vida.
		int xi = mitad - 92;
		g.drawString(f, "INTEGRITY", xi, y, GRIS, false);
		String vida = String.format("%.1f", p.getHealth() + p.getAbsorptionAmount());
		g.pose().pushPose();
		g.pose().translate(xi, y + 10, 0);
		g.pose().scale(1.5f, 1.5f, 1f);
		g.drawString(f, vida, 0, 0, p.getHealth() <= p.getMaxHealth() / 4 ? ROJO : CELESTE, false);
		g.pose().popPose();
		g.drawString(f, "/ " + Math.round(p.getMaxHealth()), xi + Math.round(f.width(vida) * 1.5f) + 3, y + 14, GRIS, false);
		// BIO-SUSTAIN: la comida (10 puntos) y el nivel.
		int xb = mitad + 6;
		g.drawString(f, "BIO-SUSTAIN", xb, y, GRIS, false);
		String nivel = "LVL " + p.experienceLevel;
		g.drawString(f, nivel, xb + 112 - f.width(nivel), y, BLANCO, false);
		int comida = p.getFoodData().getFoodLevel();
		for (int i = 0; i < 10; i++) {
			int x = xb + i * 9, yy = y + 13;
			int color = comida >= (i + 1) * 2 ? 0xFFF2C14E : comida == i * 2 + 1 ? 0xFFA07A30 : 0x60404850;
			g.fill(x + 1, yy, x + 5, yy + 6, color);
			g.fill(x, yy + 1, x + 6, yy + 5, color);
		}
	}

	// ---------------------------------------------------------------- Panel de la derecha

	private static void panel(GuiGraphics g, Font f, Player p, int w, int h) {
		int x0 = w - 96, y0 = h - 108, x1 = w - 6, y1 = h - 52;
		g.fill(x0, y0, x1, y1, OSCURO);
		g.fill(x0, y0, x1, y0 + 1, CELESTE_SUAVE);
		g.fill(x0, y0, x0 + 1, y1, CELESTE_SUAVE);
		g.fill(x0 + 1, y0 + 1, x0 + 10, y0 + 2, NARANJA);
		String modo = p.getAbilities().flying ? (p.getDeltaMovement().horizontalDistance() > 0.05 ? "THRUST" : "HOVER") : "GROUND";
		g.drawString(f, "MODE", x0 + 6, y0 + 7, GRIS, false);
		g.drawString(f, modo, x1 - 6 - f.width(modo), y0 + 7, CELESTE, false);
		String vel = String.format("%.1f m/s", p.getDeltaMovement().length() * 20);
		g.drawString(f, "VEL", x0 + 6, y0 + 20, GRIS, false);
		g.drawString(f, vel, x1 - 6 - f.width(vel), y0 + 20, BLANCO, false);
		g.drawString(f, "mitriOS v7.3.1", x0 + 6, y1 - 12, CELESTE_SUAVE, false);
	}

	// ---------------------------------------------------------------- Barra de ítems

	private static void barra(GuiGraphics g, Font f, Player p, int w, int h, DeltaTracker tiempo) {
		int lado = 18, separacion = 3, total = 9 * lado + 8 * separacion;
		int x0 = (w - total) / 2, y = h - 30;
		int elegido = p.getInventory().selected;
		for (int i = 0; i < 9; i++) {
			int x = x0 + i * (lado + separacion);
			casilla(g, x, y, lado, i == elegido);
			ItemStack pila = p.getInventory().items.get(i);
			if (!pila.isEmpty()) {
				g.renderItem(p, pila, x + 1, y + 1, i);
				if (!pila.is(ModItems.BRAZALETE_INTERDIMENSIONAL)) g.renderItemDecorations(f, pila, x + 1, y + 1);
			}
		}
		// (Al Brazalete no se le dibuja la barrita de carga: la batería ya se ve en el círculo de la izquierda.)
		// La segunda mano, a la izquierda (separada).
		ItemStack segunda = p.getOffhandItem();
		if (!segunda.isEmpty()) {
			int x = x0 - lado - 10;
			casilla(g, x, y, lado, false);
			g.renderItem(p, segunda, x + 1, y + 1, 9);
			if (!segunda.is(ModItems.BRAZALETE_INTERDIMENSIONAL)) g.renderItemDecorations(f, segunda, x + 1, y + 1);
		}
	}

	/** Una casilla: fondo oscuro con borde; la elegida, con borde celeste y un brillo. */
	private static void casilla(GuiGraphics g, int x, int y, int lado, boolean elegida) {
		int borde = elegida ? CELESTE : 0xFF3A4450;
		if (elegida) g.fill(x - 2, y - 2, x + lado + 2, y + lado + 2, 0x3049C8E5);
		g.fill(x + 1, y, x + lado - 1, y + lado, OSCURO);
		g.fill(x, y + 1, x + lado, y + lado - 1, OSCURO);
		// El borde, con las esquinas cortadas (queda redondeado).
		g.fill(x + 2, y, x + lado - 2, y + 1, borde);
		g.fill(x + 2, y + lado - 1, x + lado - 2, y + lado, borde);
		g.fill(x, y + 2, x + 1, y + lado - 2, borde);
		g.fill(x + lado - 1, y + 2, x + lado, y + lado - 2, borde);
		g.fill(x + 1, y + 1, x + 2, y + 2, borde);
		g.fill(x + lado - 2, y + 1, x + lado - 1, y + 2, borde);
		g.fill(x + 1, y + lado - 2, x + 2, y + lado - 1, borde);
		g.fill(x + lado - 2, y + lado - 2, x + lado - 1, y + lado - 1, borde);
	}
}
