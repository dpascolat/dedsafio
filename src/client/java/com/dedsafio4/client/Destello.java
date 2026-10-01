package com.dedsafio4.client;

import net.minecraft.client.gui.GuiGraphics;

/** Destello blanco en toda la pantalla al volver al cielo normal (/cielo normal). */
public final class Destello {
	private Destello() {}

	/** Segundos a pleno brillo y segundos que tarda en desvanecerse. */
	private static final float PLENO = 0.25f, DESVANECER = 1.6f;
	private static long inicioNanos = -1;

	public static void iniciar() {
		inicioNanos = System.nanoTime();
	}

	public static void dibujar(GuiGraphics graphics) {
		if (inicioNanos < 0) return;
		float segundos = (System.nanoTime() - inicioNanos) / 1e9f;
		if (segundos > PLENO + DESVANECER) {
			inicioNanos = -1;
			return;
		}
		float alfa = segundos < PLENO ? 1f : 1f - (segundos - PLENO) / DESVANECER;
		int a = (int) (alfa * alfa * 255f);   // se apaga rápido al principio y suave al final
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (a << 24) | 0xFFFFFF);
	}
}
