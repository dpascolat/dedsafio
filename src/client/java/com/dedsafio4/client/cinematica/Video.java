package com.dedsafio4.client.cinematica;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;

/**
 * Un video de cinemática: el archivo tiene un encabezado ("DCN1", ancho, alto, cuadros por segundo × 1000,
 * cantidad de cuadros), el largo de cada cuadro y después los cuadros en JPEG. Un hilo aparte decodifica el
 * cuadro que toca (si se atrasa, saltea) y el hilo del dibujo lo sube a una textura.
 */
final class Video implements AutoCloseable {
	static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cinematica/video");

	final int ancho, alto, cuadros;
	final float fps;
	private final long[] posicion;
	private final int[] largo;
	private final RandomAccessFile archivo;
	private final NativeImage imagen;
	private Textura textura;
	private final Thread hilo;

	/** El cuadro que hay que mostrar ahora (lo pone el dibujo). */
	private volatile int pedido = 0;
	/** La imagen lista para subir (o null); el hilo no la vuelve a tocar hasta que se sube. */
	private volatile boolean listo;
	private volatile boolean cerrado;
	/** Si ya se subió al menos un cuadro. */
	boolean hayCuadro;

	Video(Path ruta) throws IOException {
		archivo = new RandomAccessFile(ruta.toFile(), "r");
		byte[] magia = new byte[4];
		archivo.readFully(magia);
		if (!new String(magia, java.nio.charset.StandardCharsets.US_ASCII).equals("DCN1")) throw new IOException("No es un video de cinemática");
		ancho = archivo.readInt();
		alto = archivo.readInt();
		fps = archivo.readInt() / 1000f;
		cuadros = archivo.readInt();
		posicion = new long[cuadros];
		largo = new int[cuadros];
		long p = 4 + 16 + 4L * cuadros;
		for (int i = 0; i < cuadros; i++) {
			largo[i] = archivo.readInt();
			posicion[i] = p;
			p += largo[i];
		}
		imagen = new NativeImage(NativeImage.Format.RGBA, ancho, alto, false);
		hilo = new Thread(this::decodificar, "Dedsafio4 video");
		hilo.setDaemon(true);
		hilo.start();
	}

	void pedir(int cuadro) {
		pedido = cuadro;
	}

	private void decodificar() {
		int ultimo = -1;
		while (!cerrado) {
			int f = Math.min(pedido, cuadros - 1);
			if (f == ultimo) {
				dormir(2);
				continue;
			}
			try {
				byte[] jpg = new byte[largo[f]];
				synchronized (archivo) {
					archivo.seek(posicion[f]);
					archivo.readFully(jpg);
				}
				ByteBuffer datos = MemoryUtil.memAlloc(jpg.length);
				datos.put(jpg).flip();
				try (MemoryStack pila = MemoryStack.stackPush()) {
					IntBuffer w = pila.mallocInt(1), h = pila.mallocInt(1), c = pila.mallocInt(1);
					ByteBuffer px = STBImage.stbi_load_from_memory(datos, w, h, c, 4);
					if (px != null) {
						// Espera a que el dibujo suba el cuadro anterior antes de pisar la imagen.
						while (listo && !cerrado) dormir(1);
						if (cerrado) {
							STBImage.stbi_image_free(px);
							break;
						}
						int ww = Math.min(w.get(0), ancho), hh = Math.min(h.get(0), alto), anchoFuente = w.get(0);
						for (int y = 0; y < hh; y++) {
							for (int x = 0; x < ww; x++) imagen.setPixelRGBA(x, y, px.getInt((y * anchoFuente + x) * 4));
						}
						STBImage.stbi_image_free(px);
						listo = true;
					}
				} finally {
					MemoryUtil.memFree(datos);
				}
				ultimo = f;
			} catch (Exception e) {
				Dedsafio4.LOGGER.error("Error leyendo el cuadro " + f + " de la cinemática", e);
				ultimo = f;
			}
		}
	}

	private static void dormir(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ignorado) {
			Thread.currentThread().interrupt();
		}
	}

	/** En el hilo del dibujo: si hay un cuadro nuevo, lo sube a la textura. */
	void subir() {
		if (textura == null) {
			textura = new Textura(ancho, alto);
			Minecraft.getInstance().getTextureManager().register(TEXTURA, textura);
		}
		if (!listo) return;
		textura.bind();
		imagen.upload(0, 0, 0, false);
		listo = false;
		hayCuadro = true;
	}

	@Override
	public void close() {
		cerrado = true;
		try {
			hilo.join(500);
		} catch (InterruptedException ignorado) {
			Thread.currentThread().interrupt();
		}
		try {
			archivo.close();
		} catch (IOException ignorado) {
		}
		imagen.close();
		if (textura != null) Minecraft.getInstance().getTextureManager().release(TEXTURA);
	}

	/** La textura del video (se llena cuadro por cuadro, no viene de un archivo). */
	private static final class Textura extends AbstractTexture {
		Textura(int ancho, int alto) {
			TextureUtil.prepareImage(getId(), ancho, alto);
		}

		@Override
		public void load(ResourceManager recursos) {
		}
	}
}
