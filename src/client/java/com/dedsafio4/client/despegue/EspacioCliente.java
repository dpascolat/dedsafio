package com.dedsafio4.client.despegue;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.despegue.EspacioComando;
import com.dedsafio4.despegue.ModDespegue;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Después de /espacio (o /espacio cargar): a los 10 segundos revisa si el Espacio cargó en la pantalla. Si no, arma
 * un reporte con lo que se ve (en qué pantalla está, si llegó el chunk donde está parado, etc.), lo copia al
 * portapapeles, lo guarda en .minecraft/dedsafio4_error_espacio.txt y lo avisa en el chat (con clic para copiarlo).
 */
public final class EspacioCliente {
	private EspacioCliente() {}

	private static long revisarEn = -1;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(EspacioComando.RevisarPayload.TYPE, (payload, context) ->
				revisarEn = Util.getMillis() + payload.segundos() * 1000L);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (revisarEn < 0 || Util.getMillis() < revisarEn) return;
			revisarEn = -1;
			revisar(mc);
		});
	}

	private static void revisar(Minecraft mc) {
		String pantalla = mc.screen == null ? "ninguna (jugando)" : mc.screen.getClass().getSimpleName();
		boolean cargando = mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen;
		boolean enEspacio = mc.level != null && mc.level.dimension().equals(ModDespegue.ESPACIO);
		boolean chunk = false;
		int cx = 0, cz = 0, chunksCargados = -1;
		String lugar = "?";
		if (mc.player != null && mc.level != null) {
			cx = mc.player.getBlockX() >> 4;
			cz = mc.player.getBlockZ() >> 4;
			chunk = mc.level.getChunkSource().hasChunk(cx, cz);
			chunksCargados = mc.level.getChunkSource().getLoadedChunksCount();
			lugar = String.format("%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
		}
		if (enEspacio && chunk && !cargando) {
			if (mc.player != null) mc.player.displayClientMessage(Component.literal("El Espacio cargó bien.").withStyle(ChatFormatting.GREEN), true);
			return;
		}
		String reporte = "ERROR ESPACIO (Dedsafío 4) " + LocalDateTime.now().withNano(0)
				+ "\n- dimensión: " + (mc.level == null ? "ninguna" : mc.level.dimension().location())
				+ "\n- en el Espacio: " + enEspacio
				+ "\n- pantalla: " + pantalla + (cargando ? " (sigue cargando el mundo)" : "")
				+ "\n- lugar: " + lugar + " (chunk " + cx + ", " + cz + ")"
				+ "\n- chunk donde estás llegó: " + chunk
				+ "\n- chunks cargados en el cliente: " + chunksCargados
				+ "\n- distancia de render: " + mc.options.renderDistance().get() + " / simulación: " + mc.options.simulationDistance().get()
				+ "\n- servidor: " + (mc.getSingleplayerServer() != null ? "un jugador" : mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "?")
				+ "\n- FPS: " + mc.getFps()
				+ "\n- versión: Minecraft " + net.minecraft.SharedConstants.getCurrentVersion().getName();
		Dedsafio4.LOGGER.warn("[espacio] {}", reporte.replace('\n', ' '));
		try {
			mc.keyboardHandler.setClipboard(reporte);
		} catch (Exception ignorado) {
		}
		Path archivo = mc.gameDirectory.toPath().resolve("dedsafio4_error_espacio.txt");
		try {
			Files.writeString(archivo, reporte);
		} catch (Exception ignorado) {
		}
		if (mc.player != null) {
			mc.player.sendSystemMessage(Component.literal("El Espacio no cargó en 10 segundos. El reporte ya está copiado (pégalo con Ctrl+V) y guardado en "
							+ archivo.getFileName() + ". ").withStyle(ChatFormatting.RED)
					.append(Component.literal("[Copiar de nuevo]").withStyle(s -> s.withColor(ChatFormatting.YELLOW).withUnderlined(true)
							.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, reporte))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(reporte))))));
		}
	}
}
