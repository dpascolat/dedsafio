package com.dedsafio4.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.function.IntSupplier;

/**
 * La pantalla de carga de SAO estudios en vez de la de Mojang: fondo casi blanco, el logo del pingüino que aparece
 * suave y una barra fina con borde. Se cambian solo las partes que dibuja Minecraft (el fondo, el logo y la barra);
 * cuándo empieza, el progreso y cuándo se va siguen siendo los de siempre.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
	@Unique private static final ResourceLocation DEDSAFIO4$LOGO = ResourceLocation.fromNamespaceAndPath("dedsafio4", "carga/sao_logo");
	/** 0: todavía no se cargó el logo; 1: listo; -1: no se pudo. */
	@Unique private static int dedsafio4$logoEstado = 0;
	/** Diseño hecho en 1920×1080: el logo (760×760 arriba al centro) y la barra (620×18 abajo). */
	@Unique private static final float DEDSAFIO4$REF_W = 1920, DEDSAFIO4$REF_H = 1080;

	@Shadow @Final private Minecraft minecraft;
	@Shadow private float currentProgress;
	@Shadow private long fadeOutStart;

	@Unique private long dedsafio4$inicio = 0;

	/** El color de fondo: #fbfbfb en vez del rojo de Mojang. */
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"))
	private int dedsafio4$fondo(IntSupplier original) {
		return 0xFBFBFB;
	}

	/** La primera mitad del logo de Mojang → el logo de SAO estudios entero. */
	@Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
	private void dedsafio4$logo(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, float u, float v,
								int uw, int vh, int tw, int th) {
		if (!dedsafio4$cargarLogo()) return;
		float alfaFondo = RenderSystem.getShaderColor()[3];
		float t = dedsafio4$segundos();
		float entrada = dedsafio4$salidaSuave(Mth.clamp(t / 0.8f, 0, 1));
		float alfa = alfaFondo * entrada * dedsafio4$salida();
		if (alfa <= 0.003f) return;
		float s = dedsafio4$escala(g);
		float ox = (g.guiWidth() - DEDSAFIO4$REF_W * s) / 2, oy = (g.guiHeight() - DEDSAFIO4$REF_H * s) / 2;
		float lado = 760 * s * (0.985f + 0.015f * entrada);
		RenderSystem.defaultBlendFunc();
		g.setColor(1, 1, 1, alfa);
		g.pose().pushPose();
		g.pose().translate(ox + 960 * s - lado / 2, oy + (20 + 380) * s - lado / 2, 0);
		g.pose().scale(lado / 1024f, lado / 1024f, 1);
		g.blit(DEDSAFIO4$LOGO, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
		g.pose().popPose();
		// Minecraft sigue con su mezcla y su color después de esto.
		RenderSystem.blendFunc(770, 1);
		g.setColor(1, 1, 1, alfaFondo);
	}

	/** La otra mitad del logo de Mojang: nada. */
	@Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
	private void dedsafio4$sinSegundaMitad(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, float u, float v,
										   int uw, int vh, int tw, int th) {
	}

	/** La barra: borde de 3 px, 3 px de aire y el relleno negro. */
	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"))
	private void dedsafio4$barra(LoadingOverlay yo, GuiGraphics g, int x0, int y0, int x1, int y1, float alfaMinecraft) {
		float t = dedsafio4$segundos();
		float alfa = dedsafio4$salidaSuave(Mth.clamp((t - 0.3f) / 0.7f, 0, 1)) * dedsafio4$salida();
		int a = Math.round(alfa * 255);
		if (a <= 1) return;
		// Cuando ya terminó de cargar, la barra se completa rápido antes de irse.
		float progreso = currentProgress;
		if (fadeOutStart > -1L) progreso += (1 - progreso) * Mth.clamp((Util.getMillis() - fadeOutStart) / 200f, 0, 1);
		float s = dedsafio4$escala(g);
		float ox = (g.guiWidth() - DEDSAFIO4$REF_W * s) / 2, oy = (g.guiHeight() - DEDSAFIO4$REF_H * s) / 2;
		int bx0 = Math.round(ox + (960 - 310) * s), bx1 = Math.round(ox + (960 + 310) * s);
		int by0 = Math.round(oy + 850 * s), by1 = Math.round(oy + 868 * s);
		int borde = Math.max(1, Math.round(3 * s));
		int tinta = (a << 24) | 0x171717;
		g.fill(bx0, by0, bx1, by0 + borde, tinta);
		g.fill(bx0, by1 - borde, bx1, by1, tinta);
		g.fill(bx0, by0 + borde, bx0 + borde, by1 - borde, tinta);
		g.fill(bx1 - borde, by0 + borde, bx1, by1 - borde, tinta);
		int ix0 = bx0 + borde * 2, ix1 = bx1 - borde * 2;
		int lleno = Math.round(ix0 + (ix1 - ix0) * Mth.clamp(progreso, 0, 1));
		if (lleno > ix0) g.fill(ix0, by0 + borde * 2, lleno, by1 - borde * 2, tinta);
	}

	@Unique
	private float dedsafio4$segundos() {
		if (dedsafio4$inicio == 0) dedsafio4$inicio = Util.getMillis();
		return (Util.getMillis() - dedsafio4$inicio) / 1000f;
	}

	/** Al terminar: 0,25 s quieto con la barra llena y después el logo y la barra se desvanecen (0,65 s). */
	@Unique
	private float dedsafio4$salida() {
		if (fadeOutStart <= -1L) return 1;
		float f = (Util.getMillis() - fadeOutStart) / 1000f;
		float k = Mth.clamp((f - 0.25f) / 0.65f, 0, 1);
		return 1 - k * k * k;
	}

	@Unique
	private static float dedsafio4$salidaSuave(float x) {
		float y = 1 - x;
		return 1 - y * y * y;
	}

	@Unique
	private static float dedsafio4$escala(GuiGraphics g) {
		return Math.min(g.guiWidth() / DEDSAFIO4$REF_W, g.guiHeight() / DEDSAFIO4$REF_H);
	}

	/** El logo se carga a mano desde el mod (mientras carga el juego, las texturas de los mods todavía no están). */
	@Unique
	private boolean dedsafio4$cargarLogo() {
		if (dedsafio4$logoEstado != 0) return dedsafio4$logoEstado > 0;
		dedsafio4$logoEstado = -1;
		try {
			var ruta = FabricLoader.getInstance().getModContainer("dedsafio4").orElseThrow()
					.findPath("assets/dedsafio4/textures/gui/carga/sao_logo.png").orElseThrow();
			try (InputStream in = Files.newInputStream(ruta)) {
				DynamicTexture textura = new DynamicTexture(NativeImage.read(in));
				textura.setFilter(true, false);
				minecraft.getTextureManager().register(DEDSAFIO4$LOGO, textura);
			}
			dedsafio4$logoEstado = 1;
			return true;
		} catch (Exception e) {
			com.dedsafio4.Dedsafio4.LOGGER.warn("No se pudo cargar el logo de la pantalla de carga", e);
			return false;
		}
	}
}
