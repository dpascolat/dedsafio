package com.dedsafio4.eclipse;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.cambios.Cambios;
import com.dedsafio4.herobrine.ModHerobrine;
import com.mojang.brigadier.CommandDispatcher;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Momento Reviil: el Eclipse (en el Overworld).
 * /momento eclipse        empieza (con animación: la luna tapa el sol, todo se oscurece, se abre una
 *                         grieta en el cielo y se asoma Reviil); ninguna luz ilumina salvo la Linterna,
 *                         y Herobrine aparece entre los jugadores.
 * /momento eclipse parar  termina (y Herobrine se va).
 * Queda guardado en el mundo.
 */
public final class Eclipse {
	private Eclipse() {}

	private static final String CLAVE = "eclipse";
	private static final ResourceLocation FUENTE_ICONOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos");
	private static final String ICONO = String.valueOf((char) 0xE001);
	private static final String ANUNCIO = "¡Ha llegado el Eclipse! Deben utilizar linternas para abrirse paso por el mundo. "
			+ "CUIDADO: Herobrine está entre nosotros. NO lo mires a los ojos o te atacará y te hará mucho daño. "
			+ "Puedes apuntarle con la Linterna para que desaparezca.";

	/** Servidor → cliente: si hay eclipse, y si hay que mostrar la animación de cómo empieza. */
	public record Payload(boolean activo, boolean animar) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "eclipse"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, Payload::activo, ByteBufCodecs.BOOL, Payload::animar, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static boolean activo(MinecraftServer server) {
		return Cambios.nivel(server, CLAVE) >= 1;
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
	}

	/** Se engancha en "/momento": /momento eclipse y /momento eclipse parar. */
	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("momento").requires(s -> s.hasPermission(2))
				.then(Commands.literal("eclipse")
						.executes(ctx -> cambiar(ctx.getSource(), true))
						.then(Commands.literal("parar").executes(ctx -> cambiar(ctx.getSource(), false)))));
	}

	private static int cambiar(CommandSourceStack fuente, boolean activo) {
		MinecraftServer server = fuente.getServer();
		Cambios.data(server).setNivel(CLAVE, activo ? 1 : 0);
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(jugador, new Payload(activo, true));
		}
		if (activo) {
			anunciar(server);
			ModHerobrine.alEmpezarEclipse(server);
		} else {
			ModHerobrine.sacarTodos(server);
			fuente.sendSuccess(() -> Component.literal("El Eclipse terminó."), true);
		}
		return 1;
	}

	private static void anunciar(MinecraftServer server) {
		Component mensaje = Component.empty()
				.append(Component.literal(ICONO).withStyle(Style.EMPTY.withFont(FUENTE_ICONOS)))
				.append(Component.literal(" Momento Reviil").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF3B3B)).withBold(true)))
				.append(Component.literal("\n" + ANUNCIO).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF9A9A))));
		server.getPlayerList().broadcastSystemMessage(mensaje, false);
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
			jugador.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.6f, 0.5f);
		}
	}

	/** Al entrar al mundo. */
	public static void sincronizar(ServerPlayer jugador) {
		ServerPlayNetworking.send(jugador, new Payload(activo(jugador.server), false));
	}
}
