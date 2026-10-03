package com.dedsafio4.ruleta;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
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

/**
 * /ruleta verde  a todos los jugadores les aparece en el centro de la pantalla la ruleta girando (cae en verde),
 * con su sonido. La animación la dibuja el cliente (RuletaCliente).
 */
public final class Ruleta {
	private Ruleta() {}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ruleta").requires(s -> s.hasPermission(2))
				.then(Commands.literal("verde").executes(ctx -> {
					for (ServerPlayer jugador : ctx.getSource().getServer().getPlayerList().getPlayers())
						ServerPlayNetworking.send(jugador, new Payload("verde"));
					ctx.getSource().sendSuccess(() -> Component.literal("Ruleta verde."), true);
					return 1;
				})));
	}

	/** Servidor → cliente: mostrar la ruleta de ese color. */
	public record Payload(String color) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "ruleta"));
		public static final StreamCodec<ByteBuf, Payload> CODEC =
				ByteBufCodecs.STRING_UTF8.map(Payload::new, Payload::color);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
