package com.dedsafio4.client;

import com.dedsafio4.client.mixin.AbstractTextureAccessor;
import com.dedsafio4.pociones.ModPociones;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.List;

/** Lo que se ve con las pociones Rojizo y Negro Puro. */
public final class PocionesCliente {
	private PocionesCliente() {}

	// ---------- Rojizo: el mundo teñido de rojo (el HUD no) ----------

	/** Se multiplica la pantalla por este color y encima va un velo rojo suave. */
	private static final float ROJO = 1f, VERDE = 0.62f, AZUL = 0.56f;
	private static final int VELO = 0x18B00000;

	/** Se llama al empezar a dibujar el HUD: en ese momento en la pantalla solo está el mundo. */
	public static void dibujarRojizo(GuiGraphics graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !mc.player.hasEffect(ModPociones.EFECTO_ROJIZO)) return;
		graphics.flush();
		float w = graphics.guiWidth(), h = graphics.guiHeight();
		Matrix4f m = graphics.pose().last().pose();
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		// color final = color de la pantalla × color del cuadro
		RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		b.addVertex(m, 0, h, 0).setColor(ROJO, VERDE, AZUL, 1f);
		b.addVertex(m, w, h, 0).setColor(ROJO, VERDE, AZUL, 1f);
		b.addVertex(m, w, 0, 0).setColor(ROJO, VERDE, AZUL, 1f);
		b.addVertex(m, 0, 0, 0).setColor(ROJO, VERDE, AZUL, 1f);
		BufferUploader.drawWithShader(b.buildOrThrow());
		RenderSystem.defaultBlendFunc();
		graphics.fill(0, 0, (int) w, (int) h, VELO);
		graphics.flush();
	}

	// ---------- Negro Puro: todos los bloques negros ----------
	// Se arma una copia de la textura de los bloques con todos los píxeles en negro (dejando la
	// transparencia, así las hojas, el pasto y el vidrio siguen teniendo su forma). Mientras se dibuja
	// el mundo, la textura de los bloques apunta a esa copia; para el HUD, el inventario y la mano vuelve
	// a la original. Funciona también con Sodium, que pide la textura de los bloques al dibujar.

	private static int texturaNegra = -1;
	private static int idOriginal = -1;

	private static boolean activo(Minecraft mc) {
		return mc.player != null && mc.player.hasEffect(ModPociones.EFECTO_NEGRO_PURO);
	}

	/** Al empezar a dibujar el mundo. */
	public static void antesDelMundo() {
		Minecraft mc = Minecraft.getInstance();
		if (!activo(mc)) return;
		AbstractTexture atlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
		if (texturaNegra < 0) texturaNegra = crearCopiaNegra(atlas.getId());
		idOriginal = atlas.getId();
		((AbstractTextureAccessor) atlas).dedsafio4$setId(texturaNegra);
	}

	/** Al terminar de dibujar el mundo (antes de la mano y el HUD). */
	public static void despuesDelMundo() {
		if (idOriginal < 0) return;
		AbstractTexture atlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
		((AbstractTextureAccessor) atlas).dedsafio4$setId(idOriginal);
		idOriginal = -1;
	}

	/** Si ya no tiene el efecto, se libera la copia. */
	public static void tick(Minecraft mc) {
		if (texturaNegra >= 0 && !activo(mc)) borrarCopia();
	}

	/** La textura de los bloques cambió (F3+T, paquete de recursos): la copia se vuelve a armar. */
	public static void atlasRecargado() {
		if (texturaNegra >= 0) borrarCopia();
	}

	private static void borrarCopia() {
		TextureUtil.releaseTextureId(texturaNegra);
		texturaNegra = -1;
	}

	private static int crearCopiaNegra(int original) {
		RenderSystem.bindTexture(original);
		int niveles = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL);
		List<NativeImage> negras = new ArrayList<>();
		for (int nivel = 0; nivel <= niveles; nivel++) {
			int w = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, nivel, GL11.GL_TEXTURE_WIDTH);
			int h = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, nivel, GL11.GL_TEXTURE_HEIGHT);
			if (w <= 0 || h <= 0) break;
			NativeImage imagen = new NativeImage(w, h, false);
			imagen.downloadTexture(nivel, false);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) imagen.setPixelRGBA(x, y, imagen.getPixelRGBA(x, y) & 0xFF000000);
			}
			negras.add(imagen);
		}
		int id = TextureUtil.generateTextureId();
		TextureUtil.prepareImage(id, negras.size() - 1, negras.get(0).getWidth(), negras.get(0).getHeight());
		for (int nivel = 0; nivel < negras.size(); nivel++) negras.get(nivel).upload(nivel, 0, 0, true);
		return id;
	}
}
