package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.ZarinosaEntity;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Con una Zarinosa prendida en la cabeza, los bordes de la pantalla se ponen verdes y laten (tapa parte de la vista). */
public final class ZarinosaPantalla {
	private ZarinosaPantalla() {}

	public static void registrar() {
		HudRenderCallback.EVENT.register((g, contador) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || mc.player.getPassengers().stream().noneMatch(p -> p instanceof ZarinosaEntity)) return;
			int w = g.guiWidth(), h = g.guiHeight();
			float pulso = 0.5f + 0.5f * Mth.sin((mc.player.tickCount + contador.getGameTimeDeltaPartialTick(true)) / 20f * 8);
			int fuerte = ((int) (150 + 60 * pulso) << 24) | 0x1E7A1C, nada = 0x001E7A1C;
			int borde = Math.max(30, h / 4);
			g.fillGradient(0, 0, w, borde, fuerte, nada);
			g.fillGradient(0, h - borde, w, h, nada, fuerte);
			// Los costados: tiras verticales que se van aclarando hacia el centro.
			for (int i = 0; i < borde; i += 2) {
				int a = (int) (((150 + 60 * pulso)) * (1 - i / (float) borde));
				g.fill(i, 0, i + 2, h, (a << 24) | 0x1E7A1C);
				g.fill(w - i - 2, 0, w - i, h, (a << 24) | 0x1E7A1C);
			}
		});
	}
}
