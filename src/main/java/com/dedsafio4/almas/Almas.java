package com.dedsafio4.almas;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;

/**
 * Cada jugador tiene alma o no tiene alma. Todos empiezan con alma.
 * /alma obtener <jugador> → le da el alma.
 * /alma sacar <jugador>   → le saca el alma.
 * /alma ver <jugador>     → dice si la tiene.
 * Se guarda en el jugador (sigue igual después de morir o reiniciar el servidor).
 * El cliente lo recibe con {@link Payload} para mostrar el Alma (o el Sin Alma) en el inventario.
 */
public final class Almas {
	private Almas() {}

	private static final String SIN_ALMA = "dedsafio4_sin_alma";
	private static final int VIOLETA = 0xC77DFF;

	public static boolean tieneAlma(Player p) {
		return !p.getTags().contains(SIN_ALMA);
	}

	public static void ponerAlma(ServerPlayer p, boolean tiene) {
		if (tiene) p.removeTag(SIN_ALMA);
		else p.addTag(SIN_ALMA);
		sincronizar(p);
	}

	public static void sincronizar(ServerPlayer p) {
		if (ServerPlayNetworking.canSend(p, Payload.TYPE)) ServerPlayNetworking.send(p, new Payload(tieneAlma(p)));
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		// Al morir o volver del End se pierden las marcas del jugador: el estado del alma se copia.
		ServerPlayerEvents.COPY_FROM.register((viejo, nuevo, vivo) -> ponerAlma(nuevo, tieneAlma(viejo)));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sincronizar(handler.player));
	}

	/** Servidor → cliente: si el jugador tiene alma. */
	public record Payload(boolean tiene) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "alma"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, Payload::tiene, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("alma").requires(s -> s.hasPermission(2))
				.then(Commands.literal("obtener").then(Commands.argument("jugador", EntityArgument.players())
						.executes(ctx -> cambiar(ctx, true))))
				.then(Commands.literal("sacar").then(Commands.argument("jugador", EntityArgument.players())
						.executes(ctx -> cambiar(ctx, false))))
				.then(Commands.literal("ver").then(Commands.argument("jugador", EntityArgument.player())
						.executes(ctx -> {
							ServerPlayer p = EntityArgument.getPlayer(ctx, "jugador");
							ctx.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName()
									+ (tieneAlma(p) ? " tiene alma." : " no tiene alma.")).withColor(VIOLETA), false);
							return tieneAlma(p) ? 1 : 0;
						}))));
	}

	private static int cambiar(CommandContext<CommandSourceStack> ctx, boolean tiene) throws CommandSyntaxException {
		Collection<ServerPlayer> jugadores = EntityArgument.getPlayers(ctx, "jugador");
		for (ServerPlayer p : jugadores) {
			ponerAlma(p, tiene);
			p.level().playSound(null, p.blockPosition(),
					tiene ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS,
					tiene ? 1f : 0.4f, tiene ? 1f : 1.6f);
			p.sendSystemMessage(Component.literal(tiene ? "Obtuviste tu alma." : "Te sacaron el alma.").withColor(VIOLETA));
		}
		String quienes = jugadores.size() == 1
				? jugadores.iterator().next().getGameProfile().getName()
				: jugadores.size() + " jugadores";
		ctx.getSource().sendSuccess(() -> Component.literal(tiene
				? quienes + " ahora tiene alma."
				: quienes + " ahora no tiene alma.").withColor(VIOLETA), true);
		return jugadores.size();
	}
}
