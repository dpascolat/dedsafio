package com.dedsafio4.client;

import com.dedsafio4.temporizador.Temporizador;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Cuenta regresiva arriba en el centro de la pantalla (/tiempo). */
public final class TemporizadorCliente {
	private TemporizadorCliente() {}

	/** Segundos que se sigue mostrando "0:00" parpadeando al terminar. */
	private static final float MOSTRAR_AL_TERMINAR = 3f;
	private static final float ESCALA = 2f;
	private static final int BLANCO = 0xFFFFFFFF, ROJO = 0xFFFF5555;

	/** Momento (System.nanoTime) en que llega a 0; Long.MIN_VALUE = no hay temporizador. */
	private static long finNanos = Long.MIN_VALUE;

	public static void actualizar(int segundos) {
		finNanos = segundos < 0 ? Long.MIN_VALUE : System.nanoTime() + segundos * 1_000_000_000L;
	}

	public static void limpiar() {
		finNanos = Long.MIN_VALUE;
	}

	public static void dibujar(GuiGraphics graphics, Minecraft mc) {
		if (finNanos == Long.MIN_VALUE) return;
		float faltan = (finNanos - System.nanoTime()) / 1e9f;
		if (faltan < -MOSTRAR_AL_TERMINAR) {
			finNanos = Long.MIN_VALUE;
			return;
		}
		boolean terminado = faltan <= 0;
		// Al terminar, "0:00" parpadea en rojo.
		if (terminado && (int) (-faltan * 3) % 2 == 1) return;

		String texto = Temporizador.formato(terminado ? 0 : (int) Math.ceil(faltan));
		int color = terminado || faltan <= 60 ? ROJO : BLANCO;

		int ancho = mc.font.width(texto);
		float x = graphics.guiWidth() / 2f / ESCALA, y = 4 / ESCALA + 1;
		graphics.pose().pushPose();
		graphics.pose().scale(ESCALA, ESCALA, 1f);
		int x1 = (int) (x - ancho / 2f) - 3, y1 = (int) y - 2;
		graphics.fill(x1, y1, x1 + ancho + 6, y1 + mc.font.lineHeight + 3, 0xA0000000);
		graphics.drawString(mc.font, texto, (int) (x - ancho / 2f), (int) y, color, true);
		graphics.pose().popPose();
	}
}
