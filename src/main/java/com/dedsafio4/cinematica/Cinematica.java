package com.dedsafio4.cinematica;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * /cinematica <número> [jugadores]: les pasa un video a pantalla completa (sin manos, chat ni inventario) a todos
 * los que están conectados, o a los jugadores que se digan. El video lo baja cada cliente una sola vez y lo
 * reproduce CinematicaScreen.
 */
public final class Cinematica {
	private Cinematica() {}

	/** Cuántas cinemáticas hay. */
	public static final int CANTIDAD = 1;

	public record Payload(int numero) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cinematica"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Payload::numero, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		for (String nombre : new String[]{"cinematica"}) {
			dispatcher.register(Commands.literal(nombre).requires(s -> s.hasPermission(2))
					.then(Commands.argument("numero", IntegerArgumentType.integer(1))
							.executes(c -> pasar(c, c.getSource().getServer().getPlayerList().getPlayers()))
							.then(Commands.argument("jugadores", EntityArgument.players())
									.executes(c -> pasar(c, EntityArgument.getPlayers(c, "jugadores"))))));
		}
	}

	private static int pasar(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> jugadores) throws CommandSyntaxException {
		int numero = IntegerArgumentType.getInteger(c, "numero");
		if (numero > CANTIDAD) {
			c.getSource().sendFailure(Component.literal("Todavía no existe la cinemática " + numero + "."));
			return 0;
		}
		for (ServerPlayer jugador : jugadores) ServerPlayNetworking.send(jugador, new Payload(numero));
		c.getSource().sendSuccess(() -> Component.literal("Cinemática " + numero + " para " + jugadores.size()
				+ (jugadores.size() == 1 ? " jugador." : " jugadores.")), true);
		return jugadores.size();
	}
}
