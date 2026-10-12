package com.dedsafio4.cielo;

import com.dedsafio4.cambios.Cambios;
import com.dedsafio4.ruleta.Ruleta;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * /cielo rojo      el cielo del Overworld se vuelve rojo, con grietas y una galaxia arriba
 * /cielo rojo 2    sale la animación de Reviil, el cielo se cubre de rojo, se abre una grieta y Reviil espía por ella
 * /cielo cabezazo  con el cielo rojo 2: Reviil da un cabezazo contra la grieta
 * /cielo normal    vuelve al cielo de siempre
 * Queda guardado en el mundo.
 */
public final class Cielo {
	private Cielo() {}

	private static final String CLAVE = "cielo_rojo";

	/** 0 normal, 1 rojo, 2 rojo 2. */
	public static int modo(MinecraftServer server) {
		return Cambios.nivel(server, CLAVE);
	}

	public static boolean rojo(MinecraftServer server) {
		return modo(server) >= 1;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cielo").requires(s -> s.hasPermission(2))
				.then(Commands.literal("rojo").executes(ctx -> cambiar(ctx.getSource(), CieloPayload.ROJO))
						.then(Commands.literal("2").executes(ctx -> cambiar(ctx.getSource(), CieloPayload.ROJO_2))))
				.then(Commands.literal("normal").executes(ctx -> cambiar(ctx.getSource(), CieloPayload.NORMAL)))
				.then(Commands.literal("cabezazo").executes(ctx -> {
					MinecraftServer server = ctx.getSource().getServer();
					if (modo(server) != CieloPayload.ROJO_2) {
						ctx.getSource().sendFailure(Component.literal("Primero pon el cielo con /cielo rojo 2."));
						return 0;
					}
					enviar(server, new CieloPayload(CieloPayload.CABEZAZO, true));
					ctx.getSource().sendSuccess(() -> Component.literal("Reviil da un cabezazo."), true);
					return 1;
				})));
	}

	private static int cambiar(CommandSourceStack fuente, int modo) {
		MinecraftServer server = fuente.getServer();
		Cambios.data(server).setNivel(CLAVE, modo);
		if (modo == CieloPayload.ROJO_2) Ruleta.mostrarATodos(server, "reviil");
		enviar(server, new CieloPayload(modo, true));
		fuente.sendSuccess(() -> Component.literal(switch (modo) {
			case CieloPayload.ROJO -> "El cielo se tiñó de rojo.";
			case CieloPayload.ROJO_2 -> "Reviil se acerca: el cielo se cubre de rojo.";
			default -> "El cielo volvió a la normalidad.";
		}), true);
		return 1;
	}

	private static void enviar(MinecraftServer server, CieloPayload payload) {
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(jugador, payload);
		}
	}

	/** Al entrar al mundo: el cielo aparece como está, sin animación. */
	public static void sincronizar(ServerPlayer jugador) {
		ServerPlayNetworking.send(jugador, new CieloPayload(modo(jugador.server), false));
	}
}
