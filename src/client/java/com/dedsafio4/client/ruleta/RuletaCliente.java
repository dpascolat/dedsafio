package com.dedsafio4.client.ruleta;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.concurrent.CompletableFuture;

/**
 * La ruleta de /ruleta verde: 415 cuadros (30 por segundo) en el centro de la pantalla, con su sonido.
 * Los cuadros están en 7 hojas (textures/gui/ruleta/verde_N.png) de 8×8 cuadros de 256 px. Las hojas se cargan
 * recién la primera vez que se usa la ruleta (en segundo plano), y la animación arranca cuando están listas.
 */
public final class RuletaCliente {
	private RuletaCliente() {}

	private static final int CUADROS = 415, MS_POR_CUADRO = 33;
	private static final int TAM = 256, COLUMNAS = 8, POR_HOJA = COLUMNAS * COLUMNAS, HOJAS = 7;
	/** Qué parte del alto (o del ancho, si es más chico) de la pantalla ocupa la ruleta. */
	private static final float TAMANIO_EN_PANTALLA = 0.6f;

	private static final ResourceLocation[] HOJA = new ResourceLocation[HOJAS];
	static {
		for (int i = 0; i < HOJAS; i++) HOJA[i] = id("textures/gui/ruleta/verde_" + i + ".png");
	}
	private static final SoundEvent SONIDO = SoundEvent.createVariableRangeEvent(id("ruleta_verde"));

	private static CompletableFuture<Void> cargadas;
	/** Momento (System.nanoTime) en que empezó la animación; -1 = no se está mostrando. */
	private static long inicio = -1;
	private static SoundInstance sonando;

	private static ResourceLocation id(String ruta) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, ruta);
	}

	public static void mostrar(String color) {
		if (!"verde".equals(color)) return;
		Minecraft mc = Minecraft.getInstance();
		if (cargadas == null) {
			CompletableFuture<?>[] futuros = new CompletableFuture<?>[HOJAS];
			for (int i = 0; i < HOJAS; i++) futuros[i] = mc.getTextureManager().preload(HOJA[i], Util.backgroundExecutor());
			cargadas = CompletableFuture.allOf(futuros);
		}
		cargadas.thenRunAsync(RuletaCliente::empezar, mc);
	}

	private static void empezar() {
		Minecraft mc = Minecraft.getInstance();
		if (sonando != null) mc.getSoundManager().stop(sonando);
		sonando = SimpleSoundInstance.forUI(SONIDO, 1f);
		mc.getSoundManager().play(sonando);
		inicio = System.nanoTime();
	}

	public static void limpiar() {
		inicio = -1;
		if (sonando != null) Minecraft.getInstance().getSoundManager().stop(sonando);
		sonando = null;
	}

	public static void dibujar(GuiGraphics graphics) {
		if (inicio < 0) return;
		int cuadro = (int) ((System.nanoTime() - inicio) / 1_000_000L / MS_POR_CUADRO);
		if (cuadro >= CUADROS) {
			inicio = -1;
			return;
		}
		int hoja = cuadro / POR_HOJA, k = cuadro % POR_HOJA;
		int altoHoja = hoja == HOJAS - 1 ? (CUADROS - hoja * POR_HOJA + COLUMNAS - 1) / COLUMNAS * TAM : COLUMNAS * TAM;
		int lado = (int) (Math.min(graphics.guiWidth(), graphics.guiHeight()) * TAMANIO_EN_PANTALLA);
		int x = (graphics.guiWidth() - lado) / 2, y = (graphics.guiHeight() - lado) / 2;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		graphics.blit(HOJA[hoja], x, y, lado, lado, (k % COLUMNAS) * TAM, (k / COLUMNAS) * TAM, TAM, TAM,
				COLUMNAS * TAM, altoHoja);
		RenderSystem.disableBlend();
	}
}
