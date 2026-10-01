package com.dedsafio4.temporizador;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * /tiempo 5:00   cuenta regresiva de 5 minutos arriba de la pantalla de todos
 * /tiempo 45     también acepta solo segundos
 * /tiempo parar  lo saca
 * El servidor lleva la cuenta (en ticks del mundo) y avisa a los clientes; al llegar a 0 suena una campana.
 */
public final class Temporizador {
	private Temporizador() {}

	/** Tick del mundo en que termina; -1 = no hay temporizador. */
	private static long fin = -1;

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tiempo").requires(s -> s.hasPermission(2))
				.then(Commands.literal("parar").executes(ctx -> {
					fin = -1;
					enviarATodos(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> Component.literal("Temporizador detenido."), true);
					return 1;
				}))
				.then(Commands.argument("duracion", StringArgumentType.greedyString()).executes(ctx -> {
					int segundos = leerDuracion(StringArgumentType.getString(ctx, "duracion"));
					if (segundos <= 0) {
						ctx.getSource().sendFailure(Component.literal("Usá minutos:segundos, por ejemplo /tiempo 5:00 o /tiempo 1:30"));
						return 0;
					}
					MinecraftServer server = ctx.getSource().getServer();
					fin = server.overworld().getGameTime() + segundos * 20L;
					enviarATodos(server);
					ctx.getSource().sendSuccess(() -> Component.literal("Temporizador: " + formato(segundos)), true);
					return 1;
				})));
	}

	/** "5:00" → 300, "1:30" → 90, "45" → 45. Devuelve -1 si no se entiende. */
	static int leerDuracion(String texto) {
		String[] partes = texto.strip().split(":");
		try {
			if (partes.length == 1) return Integer.parseInt(partes[0]);
			if (partes.length == 2) {
				int minutos = Integer.parseInt(partes[0].strip()), segundos = Integer.parseInt(partes[1].strip());
				if (minutos < 0 || segundos < 0 || segundos >= 60) return -1;
				return minutos * 60 + segundos;
			}
		} catch (NumberFormatException e) {
			return -1;
		}
		return -1;
	}

	public static String formato(int segundos) {
		return segundos / 60 + ":" + String.format("%02d", segundos % 60);
	}

	private static int restantes(MinecraftServer server) {
		if (fin < 0) return -1;
		return (int) Math.max(0, (fin - server.overworld().getGameTime() + 19) / 20);
	}

	private static void enviarATodos(MinecraftServer server) {
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) sincronizar(jugador);
	}

	/** Al entrar al mundo, el jugador recibe cuánto falta. */
	public static void sincronizar(ServerPlayer jugador) {
		ServerPlayNetworking.send(jugador, new TemporizadorPayload(restantes(jugador.server)));
	}

	/** Cada tick del servidor: cuando llega a 0 suena una campana para todos. */
	public static void tick(MinecraftServer server) {
		if (fin < 0 || server.overworld().getGameTime() < fin) return;
		fin = -1;
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
			jugador.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.MASTER, 1f, 1f);
			ServerPlayNetworking.send(jugador, new TemporizadorPayload(0));
		}
	}
}
