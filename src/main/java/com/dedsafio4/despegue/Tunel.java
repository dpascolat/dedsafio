package com.dedsafio4.despegue;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * /tptp [segundos]  a todos los jugadores en supervivencia les aparece en la pantalla el túnel del
 * hiperespacio (rayos blancos sobre negro que vienen hacia uno) por esos segundos (5 si no se dice).
 * La animación la dibuja el cliente (TunelPantalla).
 */
public final class Tunel {
	private Tunel() {}

	/** Servidor → cliente: mostrar el túnel por tantos ticks. */
	public record Payload(int ticks) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "tunel"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = ByteBufCodecs.VAR_INT.map(Payload::new, Payload::ticks);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tptp").requires(s -> s.hasPermission(2))
				.executes(ctx -> mostrar(ctx, 5))
				.then(Commands.argument("segundos", IntegerArgumentType.integer(1, 120))
						.executes(ctx -> mostrar(ctx, IntegerArgumentType.getInteger(ctx, "segundos")))));
	}

	private static int mostrar(CommandContext<CommandSourceStack> ctx, int segundos) {
		int cuantos = 0;
		for (ServerPlayer jugador : ctx.getSource().getServer().getPlayerList().getPlayers()) {
			if (jugador.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) continue;
			ServerPlayNetworking.send(jugador, new Payload(segundos * 20));
			cuantos++;
		}
		int total = cuantos;
		ctx.getSource().sendSuccess(() -> Component.literal("Túnel para " + total + " jugadores en supervivencia ("
				+ segundos + " s)."), true);
		return total;
	}
}
