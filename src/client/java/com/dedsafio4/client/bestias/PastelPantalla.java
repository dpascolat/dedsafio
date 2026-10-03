package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperPastelEntity;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Las manchas de pastel en la pantalla cuando explota un Creeper Pastel cerca: unas 7 manchas pixel-art
 * de crema con borde de bizcocho, confites rojos, chorreados y migas (distintas cada vez). Se ven 3,5
 * segundos, después se van borrando en 1,5 y mientras tanto chorrean un poco hacia abajo.
 */
public final class PastelPantalla {
	private PastelPantalla() {}

	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "pastel_pantalla");
	private static final long LLENO = 3500, BORRANDO = 1500;
	private static final int CREMA = 0xF4F0EA, CREMA_SOMBRA = 0xE6DFD6, BIZCOCHO = 0xB8692E, BIZCOCHO_OSCURO = 0x7E4220, CONFITE = 0xE0262C;

	private static long desde = -1;
	private static int ancho, alto;
	private static DynamicTexture textura;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(CreeperPastelEntity.PastelPayload.TYPE, (payload, context) ->
				context.client().execute(PastelPantalla::manchar));
		HudRenderCallback.EVENT.register((g, contador) -> {
			if (desde < 0 || textura == null) return;
			long pasaron = Util.getMillis() - desde;
			if (pasaron > LLENO + BORRANDO) {
				desde = -1;
				return;
			}
			float opacidad = pasaron < 120 ? pasaron / 120f : pasaron < LLENO ? 1 : 1 - (pasaron - LLENO) / (float) BORRANDO;
			int bajada = (int) (Math.max(0, pasaron - 500) / 1000f * 3);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			g.setColor(1, 1, 1, opacidad);
			g.blit(TEXTURA, 0, bajada, g.guiWidth(), g.guiHeight(), 0, 0, ancho, alto, ancho, alto);
			g.setColor(1, 1, 1, 1);
		});
	}

	private static int abgr(int rgb) {
		return 0xFF000000 | (rgb & 0xFF) << 16 | (rgb & 0xFF00) | (rgb >> 16 & 0xFF);
	}

	/** Arma las manchas nuevas (al azar, en una imagen chiquita de 96 de ancho que después se agranda). */
	private static void manchar() {
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		ancho = 96;
		alto = Math.max(32, Math.round(96f * h / Math.max(1, w)));
		NativeImage imagen = new NativeImage(ancho, alto, true);
		RandomSource azar = RandomSource.create();
		for (int b = 0; b < 7; b++) {
			float cx = azar.nextFloat() * ancho, cy = azar.nextFloat() * alto * 0.85f, r = 6 + azar.nextFloat() * 10;
			for (int y = (int) (-r - 2); y <= r + 2; y++) {
				for (int x = (int) (-r - 2); x <= r + 2; x++) {
					float d = (float) Math.hypot(x, y) + (azar.nextFloat() - 0.5f) * 3;
					if (d > r) continue;
					float k = d / r;
					int color = k < 0.55f ? CREMA : k < 0.8f ? (azar.nextBoolean() ? CREMA_SOMBRA : CREMA)
							: (azar.nextBoolean() ? BIZCOCHO : BIZCOCHO_OSCURO);
					pintar(imagen, Math.round(cx + x), Math.round(cy + y), color);
				}
			}
			for (int i = 0; i < 4; i++) {
				pintar(imagen, Math.round(cx + (azar.nextFloat() - 0.5f) * r), Math.round(cy + (azar.nextFloat() - 0.5f) * r), CONFITE);
			}
			for (int i = 0; i < 3; i++) {
				int dx = Math.round(cx + (azar.nextFloat() - 0.5f) * r * 1.2f), largo = 3 + azar.nextInt(8);
				for (int j = 0; j < largo; j++) pintar(imagen, dx, Math.round(cy + r * 0.6f) + j, CREMA);
			}
			for (int i = 0; i < 10; i++) {
				pintar(imagen, Math.round(cx + (azar.nextFloat() - 0.5f) * r * 3), Math.round(cy + (azar.nextFloat() - 0.5f) * r * 3),
						azar.nextBoolean() ? BIZCOCHO : BIZCOCHO_OSCURO);
			}
		}
		textura = new DynamicTexture(imagen);
		mc.getTextureManager().register(TEXTURA, textura);
		desde = Util.getMillis();
	}

	private static void pintar(NativeImage imagen, int x, int y, int rgb) {
		if (x >= 0 && y >= 0 && x < imagen.getWidth() && y < imagen.getHeight()) imagen.setPixelRGBA(x, y, abgr(rgb));
	}
}
