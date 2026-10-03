package com.dedsafio4.banco;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /deditas                                      ver tu saldo
 * /deditas ver <jugador>                        ver el saldo de otro (admin)
 * /deditas dar|quitar|set <jugador> <cantidad>  modificar saldos (admin)
 */
public final class BancoComandos {
	private BancoComandos() {}

	private enum Operacion { DAR, QUITAR, SET }

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("deditas")
				.executes(ctx -> {
					ServerPlayer jugador = ctx.getSource().getPlayerOrException();
					ctx.getSource().sendSuccess(() -> Component.literal("Tienes " + Banco.saldo(jugador) + " deditas"), false);
					return 1;
				})
				.then(Commands.literal("ver").requires(s -> s.hasPermission(2))
						.then(Commands.argument("jugador", EntityArgument.player())
								.executes(ctx -> {
									ServerPlayer jugador = EntityArgument.getPlayer(ctx, "jugador");
									ctx.getSource().sendSuccess(() -> Component.literal(
											jugador.getName().getString() + " tiene " + Banco.saldo(jugador) + " deditas"), false);
									return 1;
								})))
				.then(operacion("dar", Operacion.DAR))
				.then(operacion("quitar", Operacion.QUITAR))
				.then(operacion("set", Operacion.SET)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> operacion(String nombre, Operacion op) {
		return Commands.literal(nombre).requires(s -> s.hasPermission(2))
				.then(Commands.argument("jugador", EntityArgument.player())
						.then(Commands.argument("cantidad", LongArgumentType.longArg(0))
								.executes(ctx -> ejecutar(ctx, op))));
	}

	private static int ejecutar(CommandContext<CommandSourceStack> ctx, Operacion op) throws CommandSyntaxException {
		ServerPlayer jugador = EntityArgument.getPlayer(ctx, "jugador");
		long cantidad = LongArgumentType.getLong(ctx, "cantidad");
		switch (op) {
			case DAR -> Banco.sumar(jugador, cantidad);
			case QUITAR -> Banco.setSaldo(jugador, Banco.saldo(jugador) - cantidad);
			case SET -> Banco.setSaldo(jugador, cantidad);
		}
		ctx.getSource().sendSuccess(() -> Component.literal(
				jugador.getName().getString() + " ahora tiene " + Banco.saldo(jugador) + " deditas"), true);
		return 1;
	}
}
