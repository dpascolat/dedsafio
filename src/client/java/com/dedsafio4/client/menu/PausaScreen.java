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
 * La pausa del Dedsafío (Esc), en lugar de la de Minecraft: el juego se ve detrás desenfocado, arriba "EUFONIA
 * STUDIO" y el logo de Dedsafío 4, y abajo tres palabras con letras futuristas: VOLVER AL JUEGO (verde), OPCIONES
 * (amarillo) y DESCONECTARSE (rojo). Al pasar el mouse las palabras crecen un poco y brillan.
 * (Si no está la imagen de Eufonia, como en el Abelsafío, no se dibuja.)
 */
public class PausaScreen extends Screen {
	private static ResourceLocation menu(String nombre) {
		return ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/menu/" + nombre + ".png");
	}

	private static final ResourceLocation LOGO = menu("logo");
	private static final ResourceLocation POLVO = menu("polvo");
	private static final ResourceLocation EUFONIA = menu("pausa_eufonia");
	private static final int LOGO_W = 556, LOGO_H = 284;
	private static final int EUFONIA_W = 245, EUFONIA_H = 26;
	private static final ResourceLocation[] PALABRAS = {menu("pausa_volver"), menu("pausa_opciones"), menu("pausa_desconectarse")};
	/** Tamaño de cada imagen de palabra. */
	private static final int[][] TAMANOS = {{466, 32}, {238, 32}, {426, 32}};
	private static final int[] COLORES = {0x3CE65A, 0xF2E040, 0xF0505A};
	/** Dónde va cada palabra (alto de la pantalla, de 0 a 1). */
	private static final float[] ALTURAS = {0.525f, 0.6f, 0.675f};

	private final float[] brillo = new float[PALABRAS.length];
	private int elegido = -1;
	/** Píxeles de pantalla por píxel de las imágenes de las palabras. */
	private float escala;
	private boolean conEufonia;

	public PausaScreen() {
		super(Component.translatable("menu.game"));
	}

	@Override
	protected void init() {
		// "VOLVER AL JUEGO" ocupa más o menos el 27% del alto de la pantalla a lo ancho.
		escala = Math.min(height * 0.27f, width * 0.6f) / TAMANOS[0][0];
		conEufonia = minecraft.getResourceManager().getResource(EUFONIA).isPresent();
		// Las imágenes se ven más chicas o más grandes según la pantalla: suavizadas, para que no se vean serruchadas.
		for (ResourceLocation r : PALABRAS) minecraft.getTextureManager().getTexture(r).setFilter(true, false);
		if (conEufonia) minecraft.getTextureManager().getTexture(EUFONIA).setFilter(true, false);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		// El juego queda atrás, desenfocado y apenas oscurecido.
		if (minecraft.level != null) renderBlurredBackground(parcial);
		g.fill(0, 0, width, height, 0x30000000);
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
		if (conEufonia) {
			float se = height * 0.155f / EUFONIA_W;
			g.pose().pushPose();
			g.pose().translate(width / 2f, height * 0.05f, 0);
			g.pose().scale(se, se, 1);
			g.blit(EUFONIA, -EUFONIA_W / 2, -EUFONIA_H / 2, 0, 0, EUFONIA_W, EUFONIA_H, EUFONIA_W, EUFONIA_H);
			g.pose().popPose();
		}
		// El logo, que late muy suave.
		float t = (Util.getMillis() % 4000L) / 4000f;
		float logoAncho = Math.min(width * 0.5f, height * 0.33f);
		float s = logoAncho / LOGO_W * (1 + 0.012f * (float) Math.sin(t * Math.PI * 2));
		g.pose().pushPose();
		g.pose().translate(width / 2f, height * (conEufonia ? 0.165f : 0.15f), 0);
		g.pose().scale(s, s, 1);
		g.blit(LOGO, -LOGO_W / 2, -LOGO_H / 2, 0, 0, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
		g.pose().popPose();
		for (int i = 0; i < PALABRAS.length; i++) boton(g, i);
		g.setColor(1, 1, 1, 1);
	}

	private void boton(GuiGraphics g, int i) {
		float a = brillo[i];
		int w = TAMANOS[i][0], h = TAMANOS[i][1];
		float s = escala * (1 + 0.08f * a);
		int color = COLORES[i];
		float cy = height * ALTURAS[i];
		if (a > 0.01f) {
			// Resplandor del color del botón.
			float ancho = w * s;
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			g.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 0.45f * a);
			g.pose().pushPose();
			g.pose().translate(width / 2f - ancho * 0.6f, cy - h * s, 0);
			g.pose().scale(ancho * 1.2f / 64f, h * s * 2 / 64f, 1);
			g.blit(POLVO, 0, 0, 0, 0, 64, 64, 64, 64);
			g.pose().popPose();
			RenderSystem.defaultBlendFunc();
			g.setColor(1, 1, 1, 1);
		}
		g.pose().pushPose();
		g.pose().translate(width / 2f, cy, 0);
		g.pose().scale(s, s, 1);
		g.blit(PALABRAS[i], -w / 2, -h / 2, 0, 0, w, h, w, h);
		if (a > 0.01f) {
			// Con el mouse encima, la palabra se aclara (se suma de nuevo encima).
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			g.setColor(1, 1, 1, 0.4f * a);
			g.blit(PALABRAS[i], -w / 2, -h / 2, 0, 0, w, h, w, h);
			RenderSystem.defaultBlendFunc();
			g.setColor(1, 1, 1, 1);
		}
		g.pose().popPose();
	}

	/** Qué botón hay bajo el mouse (o -1). */
	private int botonEn(double mx, double my) {
		for (int i = 0; i < PALABRAS.length; i++) {
			float ancho = TAMANOS[i][0] * escala, alto = TAMANOS[i][1] * escala;
			if (Math.abs(mx - width / 2f) <= ancho / 2 + 6 && Math.abs(my - height * ALTURAS[i]) <= alto / 2 + 3) return i;
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
				elegido = (elegido + 1) % PALABRAS.length;
				return true;
			}
			case GLFW.GLFW_KEY_UP -> {
				elegido = (elegido + PALABRAS.length - 1) % PALABRAS.length;
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
