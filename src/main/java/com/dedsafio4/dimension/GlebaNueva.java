package com.dedsafio4.dimension;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * /gleba_nuevo: borra todo lo generado de Gleba (la Dimensión de los Órganos) para que se genere de nuevo, con las
 * islas y las minas como están ahora. Con el servidor prendido no se pueden borrar los archivos (están en uso), así
 * que el comando saca a todos de Gleba y deja una marca; el borrado se hace al apagar el servidor (o al prenderlo, si
 * se cortó de golpe). Lo de afuera de Gleba (/spawn_gleba, el Overworld, los inventarios) no se toca.
 *   /gleba_nuevo            marca Gleba para borrar;   /gleba_nuevo cancelar   saca la marca.
 */
public final class GlebaNueva {
	private GlebaNueva() {}

	private static Path marca(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("dedsafio4_gleba_nuevo");
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTING.register(GlebaNueva::borrarSiHayMarca);
		ServerLifecycleEvents.SERVER_STOPPED.register(GlebaNueva::borrarSiHayMarca);
	}

	private static void borrarSiHayMarca(MinecraftServer server) {
		Path marca = marca(server);
		if (!Files.exists(marca)) return;
		Path carpeta = DimensionType.getStorageFolder(Organos.DIMENSION, server.getWorldPath(LevelResource.ROOT));
		try {
			if (Files.exists(carpeta)) {
				try (Stream<Path> todo = Files.walk(carpeta)) {
					for (Path p : todo.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
				}
			}
			Files.delete(marca);
			com.dedsafio4.Dedsafio4.LOGGER.info("Gleba borrada: se genera de nuevo ({})", carpeta);
		} catch (IOException e) {
			com.dedsafio4.Dedsafio4.LOGGER.error("No se pudo borrar Gleba ({})", carpeta, e);
		}
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("gleba_nuevo").requires(s -> s.hasPermission(2))
				.executes(c -> {
					MinecraftServer server = c.getSource().getServer();
					// Nadie se queda en Gleba: todos vuelven al Overworld (si no, aparecerían en el aire de la Gleba nueva).
					ServerLevel gleba = server.getLevel(Organos.DIMENSION);
					ServerLevel overworld = server.overworld();
					if (gleba != null) {
						for (ServerPlayer jugador : new ArrayList<>(gleba.players())) {
							BlockPos lugar = Portales.lugarSeguro(overworld, overworld.getSharedSpawnPos());
							jugador.teleportTo(overworld, lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, jugador.getYRot(), jugador.getXRot());
							jugador.sendSystemMessage(Component.literal("Gleba se va a generar de nuevo: volviste al Overworld.")
									.withStyle(ChatFormatting.LIGHT_PURPLE));
						}
					}
					try {
						Files.writeString(marca(server), "Gleba se borra al apagar el servidor (/gleba_nuevo).");
					} catch (IOException e) {
						c.getSource().sendFailure(Component.literal("No se pudo marcar Gleba para borrar: " + e.getMessage()));
						return 0;
					}
					c.getSource().sendSuccess(() -> Component.literal("Listo: Gleba se borra y se genera de nuevo cuando se reinicie el servidor."
							+ " Hasta entonces no entren a Gleba (lo que hagan ahí se pierde). Para cancelar: /gleba_nuevo cancelar")
							.withStyle(ChatFormatting.LIGHT_PURPLE), true);
					return 1;
				})
				.then(Commands.literal("cancelar").executes(c -> {
					try {
						boolean habia = Files.deleteIfExists(marca(c.getSource().getServer()));
						c.getSource().sendSuccess(() -> Component.literal(habia ? "Listo: Gleba no se borra."
								: "Gleba no estaba marcada para borrar.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
						return habia ? 1 : 0;
					} catch (IOException e) {
						c.getSource().sendFailure(Component.literal("No se pudo cancelar: " + e.getMessage()));
						return 0;
					}
				})));
	}
}
