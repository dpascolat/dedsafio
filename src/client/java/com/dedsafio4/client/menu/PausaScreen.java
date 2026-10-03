package com.dedsafio4.client.menu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * La pausa del Dedsafío (Esc), en lugar de la de Minecraft: el juego se ve detrás oscurecido en rojo, con
 * luces rojas que flotan, el logo de Dedsafío 4 arriba y tres botones: VOLVER AL JUEGO (verde), OPCIONES
 * (amarillo) y DESCONECTARSE (rojo). Al pasar el mouse las palabras crecen un poco y brillan.
 */
public class PausaScreen extends Screen {
	private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/menu/logo.png");
	private static final ResourceLocation POLVO = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/menu/polvo.png");
	private static final int LOGO_W = 556, LOGO_H = 284;
	private static final String[] TEXTOS = {"VOLVER AL JUEGO", "OPCIONES", "DESCONECTARSE"};
	private static final int[] COLORES = {0x3CE65A, 0xF2E040, 0xF0505A};

	private final float[] brillo = new float[TEXTOS.length];
	private int elegido = -1;
	private float escalaTexto;
	private int[] centrosY = new int[TEXTOS.length];

	public PausaScreen() {
		super(Component.translatable("menu.game"));
	}

	@Override
	protected void init() {
		escalaTexto = Math.max(1.5f, Math.min(2.5f, height / 150f));
		int primero = height / 2 + (int) (height * 0.08f), paso = (int) (height * 0.13f);
		for (int i = 0; i < TEXTOS.length; i++) centrosY[i] = primero + i * paso;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		// El juego queda atrás, desenfocado, oscurecido y teñido de rojo.
		if (minecraft.level != null) renderBlurredBackground(parcial);
		g.fillGradient(0, 0, width, height, 0xD0160404, 0xE00A0202);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		super.render(g, mx, my, parcial);
		int sobre = botonEn(mx, my);
		for (int i = 0; i < brillo.length; i++) {
			boolean activo = i == sobre || (sobre < 0 && i == elegido);
			brillo[i] += ((activo ? 1 : 0) - brillo[i]) * 0.25f;
		}
		RenderSystem.enableBlend();
		luces(g);
		// El logo, que late muy suave.
		float t = (Util.getMillis() % 4000L) / 4000f;
		float logoAncho = Math.min(width * 0.62f, height * 0.62f * LOGO_W / LOGO_H);
		float s = logoAncho / LOGO_W * (1 + 0.012f * (float) Math.sin(t * Math.PI * 2));
		g.pose().pushPose();
		g.pose().translate(width / 2f, height * 0.25f, 0);
		g.pose().scale(s, s, 1);
		g.blit(LOGO, -LOGO_W / 2, -LOGO_H / 2, 0, 0, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
		g.pose().popPose();
		for (int i = 0; i < TEXTOS.length; i++) boton(g, i);
		g.setColor(1, 1, 1, 1);
	}

	/** Luces rojas desenfocadas que flotan despacio. */
	private void luces(GuiGraphics g) {
		float t = (Util.getMillis() % 20000L) / 20000f * (float) Math.PI * 2;
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		for (int i = 0; i < 14; i++) {
			float fase = i * 1.7f;
			float x = (0.08f + (i * 0.37f) % 0.84f) * width + (float) Math.sin(t * (1 + i % 3) + fase) * width * 0.03f;
			float y = (0.1f + (i * 0.53f) % 0.8f) * height + (float) Math.cos(t * (1 + i % 2) + fase) * height * 0.04f;
			float tam = (i % 4 == 0 ? 0.09f : 0.04f) * height;
			g.setColor(1f, 0.15f, 0.12f, i % 4 == 0 ? 0.35f : 0.55f);
			g.pose().pushPose();
			g.pose().translate(x - tam / 2, y - tam / 2, 0);
			g.pose().scale(tam / 64f, tam / 64f, 1);
			g.blit(POLVO, 0, 0, 0, 0, 64, 64, 64, 64);
			g.pose().popPose();
		}
		RenderSystem.defaultBlendFunc();
		g.setColor(1, 1, 1, 1);
	}

	private void boton(GuiGraphics g, int i) {
		float a = brillo[i];
		String texto = TEXTOS[i];
		float s = escalaTexto * (1 + 0.08f * a);
		int color = COLORES[i];
		float ancho = font.width(texto) * s;
		if (a > 0.01f) {
			// Resplandor del color del botón.
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			g.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 0.45f * a);
			g.pose().pushPose();
			g.pose().translate(width / 2f - ancho * 0.7f, centrosY[i] - 12 * s, 0);
			g.pose().scale(ancho * 1.4f / 64f, 24 * s / 64f, 1);
			g.blit(POLVO, 0, 0, 0, 0, 64, 64, 64, 64);
			g.pose().popPose();
			RenderSystem.defaultBlendFunc();
			g.setColor(1, 1, 1, 1);
		}
		g.pose().pushPose();
		g.pose().translate(width / 2f - ancho / 2, centrosY[i] - 4 * s, 0);
		g.pose().scale(s, s, 1);
		// Letras con un borde oscuro y, encima, el color (más claro con el mouse encima).
		int oscuro = 0xFF000000 | ((color >> 2) & 0x3F3F3F);
		for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) g.drawString(font, texto, d[0], d[1], oscuro, false);
		int claro = mezclar(color, 0xFFFFFF, 0.35f * a);
		g.drawString(font, texto, 0, 0, 0xFF000000 | claro, false);
		g.pose().popPose();
	}

	private static int mezclar(int a, int b, float k) {
		int r = (int) ((a >> 16 & 255) * (1 - k) + (b >> 16 & 255) * k);
		int v = (int) ((a >> 8 & 255) * (1 - k) + (b >> 8 & 255) * k);
		int az = (int) ((a & 255) * (1 - k) + (b & 255) * k);
		return r << 16 | v << 8 | az;
	}

	/** Qué botón hay bajo el mouse (o -1). */
	private int botonEn(double mx, double my) {
		for (int i = 0; i < TEXTOS.length; i++) {
			float ancho = font.width(TEXTOS[i]) * escalaTexto;
			if (Math.abs(mx - width / 2f) <= ancho / 2 + 8 && Math.abs(my - centrosY[i]) <= 6 * escalaTexto + 4) return i;
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		int i = botonEn(mx, my);
		if (boton == 0 && i >= 0) {
			apretar(i);
			return true;
		}
		return super.mouseClicked(mx, my, boton);
	}

	@Override
	public boolean keyPressed(int tecla, int scancode, int mods) {
		switch (tecla) {
			case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_TAB -> {
				elegido = (elegido + 1) % TEXTOS.length;
				return true;
			}
			case GLFW.GLFW_KEY_UP -> {
				elegido = (elegido + TEXTOS.length - 1) % TEXTOS.length;
				return true;
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				if (elegido >= 0) apretar(elegido);
				return true;
			}
			default -> {
				return super.keyPressed(tecla, scancode, mods);
			}
		}
	}

	private void apretar(int i) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		switch (i) {
			case 0 -> onClose();
			case 1 -> minecraft.setScreen(new OptionsScreen(this, minecraft.options));
			default -> desconectarse();
		}
	}

	/** Igual que el botón de Minecraft: sale del mundo o del servidor y vuelve al menú. */
	private void desconectarse() {
		boolean local = minecraft.isLocalServer();
		if (minecraft.level != null) minecraft.level.disconnect();
		if (local) minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
		else minecraft.disconnect();
		TitleScreen titulo = new TitleScreen();
		minecraft.setScreen(local || !MenuPrincipalScreen.modoNormal ? titulo : new JoinMultiplayerScreen(titulo));
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
