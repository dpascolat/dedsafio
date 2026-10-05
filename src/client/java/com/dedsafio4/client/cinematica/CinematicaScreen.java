package com.dedsafio4.client.cinematica;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.cinematica.Cinematica;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * /cinematica N: el video a pantalla completa, con su sonido. Tapa todo (no se ven las manos, el chat ni el
 * inventario), no se puede cerrar con Esc y no pausa el juego; al terminar el video se cierra solo. Si el video
 * todavía se está bajando, muestra cuánto falta y empieza cuando termina.
 */
public class CinematicaScreen extends Screen {
	private final int numero;
	private Video video;
	private SoundInstance sonido;
	private long inicio;
	private int ticksError;

	public CinematicaScreen(int numero) {
		super(Component.literal("Cinemática " + numero));
		this.numero = numero;
	}

	public static void registrar() {
		CinematicaArchivos.registrar();
		ClientPlayNetworking.registerGlobalReceiver(Cinematica.Payload.TYPE, (payload, context) -> context.client().execute(() -> {
			if (!CinematicaArchivos.existe(payload.numero())) return;
			CinematicaArchivos.asegurar(payload.numero());
			Minecraft.getInstance().setScreen(new CinematicaScreen(payload.numero()));
		}));
	}

	@Override
	public void tick() {
		if (video != null) return;
		switch (CinematicaArchivos.estado(numero)) {
			case LISTO -> empezar();
			case ERROR -> {
				if (++ticksError > 80) onClose();
			}
			default -> {
			}
		}
	}

	private void empezar() {
		try {
			video = new Video(CinematicaArchivos.archivo(numero));
		} catch (Exception e) {
			Dedsafio4.LOGGER.error("No se pudo abrir la cinemática " + numero, e);
			onClose();
			return;
		}
		inicio = Util.getMillis();
		Minecraft mc = Minecraft.getInstance();
		mc.getMusicManager().stopPlaying();
		sonido = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cinematica" + numero)), 1f, 1f);
		mc.getSoundManager().play(sonido);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		g.fill(0, 0, width, height, 0xFF000000);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		renderBackground(g, mx, my, parcial);
		if (video == null) {
			String texto = switch (CinematicaArchivos.estado(numero)) {
				case ERROR -> "No se pudo bajar la cinemática (revisa tu internet).";
				case LISTO -> "";
				default -> "Cargando la cinemática... " + Math.round(CinematicaArchivos.progreso(numero) * 100) + "%";
			};
			g.drawCenteredString(font, texto, width / 2, height / 2 - 4, 0xFFBBBBBB);
			return;
		}
		int cuadro = (int) ((Util.getMillis() - inicio) / 1000.0 * video.fps);
		if (cuadro >= video.cuadros) {
			onClose();
			return;
		}
		video.pedir(cuadro);
		video.subir();
		if (!video.hayCuadro) return;
		// Lo más grande que entre en la pantalla sin deformarse (franjas negras si sobra).
		float escala = Math.min(width / (float) video.ancho, height / (float) video.alto);
		int w = Math.round(video.ancho * escala), h = Math.round(video.alto * escala);
		g.blit(Video.TEXTURA, (width - w) / 2, (height - h) / 2, w, h, 0, 0, video.ancho, video.alto, video.ancho, video.alto);
	}

	@Override
	public void removed() {
		if (video != null) video.close();
		video = null;
		if (sonido != null) Minecraft.getInstance().getSoundManager().stop(sonido);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
