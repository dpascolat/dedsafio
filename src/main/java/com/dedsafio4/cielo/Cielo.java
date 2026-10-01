package com.dedsafio4.cielo;

import com.dedsafio4.cambios.Cambios;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * /cielo rojo    el cielo del Overworld se vuelve rojo, con grietas y una galaxia arriba
 * /cielo normal  vuelve al cielo de siempre
 * Queda guardado en el mundo.
 */
public final class Cielo {
	private Cielo() {}

	private static final String CLAVE = "cielo_rojo";

	public static boolean rojo(MinecraftServer server) {
		return Cambios.nivel(server, CLAVE) >= 1;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cielo").requires(s -> s.hasPermission(2))
				.then(Commands.literal("rojo").executes(ctx -> cambiar(ctx.getSource(), true)))
				.then(Commands.literal("normal").executes(ctx -> cambiar(ctx.getSource(), false))));
	}

	private static int cambiar(CommandSourceStack fuente, boolean rojo) {
		MinecraftServer server = fuente.getServer();
		Cambios.data(server).setNivel(CLAVE, rojo ? 1 : 0);
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(jugador, new CieloPayload(rojo, true));
		}
		fuente.sendSuccess(() -> Component.literal(rojo ? "El cielo se tiñó de rojo." : "El cielo volvió a la normalidad."), true);
		return 1;
	}

	/** Al entrar al mundo: el cielo aparece como está, sin animación. */
	public static void sincronizar(ServerPlayer jugador) {
		ServerPlayNetworking.send(jugador, new CieloPayload(rojo(jugador.server), false));
	}
}
