package com.dedsafio4.client.ruleta;

import com.dedsafio4.Dedsafio4;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Las animaciones de pantalla (ruletas, la criatura, la nutria, la de muerte): en el centro de la pantalla (o abajo), con su
 * sonido. Cada una tiene un JSON en assets/dedsafio4/animaciones/ (tamaño de los cuadros, cuántos hay por hoja, cuánto
 * dura cada cuadro, qué tan grande se ve, si va abajo, el sonido y la animación que va después) y sus hojas de cuadros en
 * textures/gui/animaciones/NOMBRE_N.png. Las hojas se cargan en segundo plano cuando llega la animación (arranca
 * cuando están listas) y se sueltan al terminar, porque ocupan mucha memoria de video.
 */
public final class AnimacionesCliente {
	private AnimacionesCliente() {}

	private record Info(String nombre, int ancho, int alto, int columnas, int porHoja, int hojas, int[] fin,
						ResourceLocation sonido, float altoEnPantalla, boolean abajo, String siguiente) {
		ResourceLocation hoja(int i) {
			return id("textures/gui/animaciones/" + nombre + "_" + i + ".png");
		}

		int cuadros() {
			return fin.length;
		}
	}

	private static final Map<String, Info> INFOS = new HashMap<>();
	private static Info actual;
	/** Momento (Util.getMillis) en que empezó la animación actual. */
	private static long inicio;
	private static SoundInstance sonando;
	/** Cuenta cuántas veces se pidió una animación, para ignorar cargas viejas si llega otra antes. */
	private static int pedido;

	private static ResourceLocation id(String ruta) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, ruta);
	}

	public static void mostrar(String nombre) {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> {
			Info info = info(nombre);
			if (info == null) return;
			int esta = ++pedido;
			cargar(info).thenRunAsync(() -> {
				if (esta == pedido) empezar(info);
			}, mc);
		});
	}

	/** Carga las hojas de la animación y de las que van después (para que no haya pausa entre una y otra). */
	private static CompletableFuture<Void> cargar(Info info) {
		Minecraft mc = Minecraft.getInstance();
		CompletableFuture<?>[] futuros = new CompletableFuture<?>[info.hojas];
		for (int i = 0; i < info.hojas; i++) futuros[i] = mc.getTextureManager().preload(info.hoja(i), Util.backgroundExecutor());
		CompletableFuture<Void> todo = CompletableFuture.allOf(futuros);
		Info despues = info.siguiente == null ? null : info(info.siguiente);
		return despues == null ? todo : todo.thenCompose(v -> cargar(despues));
	}

	private static void empezar(Info info) {
		Minecraft mc = Minecraft.getInstance();
		if (actual != null && actual.nombre.equals(info.nombre)) {
			// La misma otra vez: sus hojas ya están cargadas, solo vuelve a empezar.
			if (sonando != null) mc.getSoundManager().stop(sonando);
			actual = null;
		} else {
			terminar(false);
		}
		actual = info;
		inicio = Util.getMillis();
		sonando = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(info.sonido), 1f);
		mc.getSoundManager().play(sonando);
	}

	/**
	 * Deja de mostrar la animación actual y suelta sus hojas. Si terminó sola (dejarSonido) solo suelta las suyas; si
	 * se cortó, también las de las que iban después, que ya no se van a mostrar.
	 */
	private static void terminar(boolean dejarSonido) {
		Minecraft mc = Minecraft.getInstance();
		for (Info i = actual; i != null; i = dejarSonido || i.siguiente == null ? null : info(i.siguiente)) {
			for (int h = 0; h < i.hojas; h++) mc.getTextureManager().release(i.hoja(h));
		}
		if (sonando != null && !dejarSonido) mc.getSoundManager().stop(sonando);
		sonando = null;
		actual = null;
	}

	public static void limpiar() {
		pedido++;
		terminar(false);
	}

	private static Info info(String nombre) {
		if (INFOS.containsKey(nombre)) return INFOS.get(nombre);
		Info info = null;
		try (Reader lector = new InputStreamReader(Minecraft.getInstance().getResourceManager()
				.open(id("animaciones/" + nombre + ".json")), StandardCharsets.UTF_8)) {
			JsonObject j = JsonParser.parseReader(lector).getAsJsonObject();
			var duraciones = j.getAsJsonArray("duraciones");
			int[] fin = new int[duraciones.size()];
			int suma = 0;
			for (int i = 0; i < fin.length; i++) fin[i] = suma += duraciones.get(i).getAsInt();
			info = new Info(nombre, j.get("ancho").getAsInt(), j.get("alto").getAsInt(), j.get("columnas").getAsInt(),
					j.get("por_hoja").getAsInt(), j.get("hojas").getAsInt(), fin, id(j.get("sonido").getAsString()),
					j.get("alto_en_pantalla").getAsFloat(), j.has("posicion") && "abajo".equals(j.get("posicion").getAsString()), j.has("siguiente") ? j.get("siguiente").getAsString() : null);
		} catch (Exception e) {
			Dedsafio4.LOGGER.warn("No se pudo leer la animación {}", nombre, e);
		}
		INFOS.put(nombre, info);
		return info;
	}

	public static void dibujar(GuiGraphics graphics) {
		if (actual == null) return;
		Info info = actual;
		long pasado = Util.getMillis() - inicio;
		int cuadro = 0;
		while (cuadro < info.cuadros() && info.fin[cuadro] <= pasado) cuadro++;
		if (cuadro >= info.cuadros()) {
			// La que sigue arranca enseguida (ya está cargada); el sonido de esta sigue hasta que termine solo.
			String siguiente = info.siguiente;
			terminar(true);
			Info despues = siguiente == null ? null : info(siguiente);
			if (despues != null) empezar(despues);
			return;
		}
		int hoja = cuadro / info.porHoja, k = cuadro % info.porHoja;
		int enHoja = Math.min(info.porHoja, info.cuadros() - hoja * info.porHoja);
		int altoHoja = (enHoja + info.columnas - 1) / info.columnas * info.alto;
		float alto = Math.min(graphics.guiHeight() * info.altoEnPantalla, graphics.guiWidth() * 0.9f * info.alto / info.ancho);
		int h = Math.round(alto), w = Math.round(alto * info.ancho / info.alto);
		int x = (graphics.guiWidth() - w) / 2, y = info.abajo ? graphics.guiHeight() - h : (graphics.guiHeight() - h) / 2;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		graphics.blit(info.hoja(hoja), x, y, w, h, (k % info.columnas) * info.ancho, (k / info.columnas) * info.alto,
				info.ancho, info.alto, info.columnas * info.ancho, altoHoja);
		RenderSystem.disableBlend();
	}
}
