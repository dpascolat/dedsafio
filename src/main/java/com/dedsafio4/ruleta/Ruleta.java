package com.dedsafio4.ruleta;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.netty.buffer.ByteBuf;
import com.dedsafio4.pociones.ModPociones;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Animaciones en el centro de la pantalla de todos los jugadores, con su sonido (las dibuja el cliente, AnimacionesCliente):
 * /ruleta verde|morado|rojo|celeste|azul|naranja|amarillo|rosa  la ruleta gira y cae en ese color (la roja termina con la
 *                                                          criatura, que les da Rojizo 1 minuto, y la rosa con la nutria).
 * Cuando muere un jugador, a todos les aparece la animación de muerte.
 */
public final class Ruleta {
	private Ruleta() {}

	public static final String[] COLORES = {"verde", "morado", "rojo", "celeste", "azul", "naranja", "amarillo", "rosa"};

	/**
	 * Ticks desde /ruleta rojo hasta que aparece la criatura (ahí empieza el Rojizo): la ruleta roja dura 417
	 * cuadros de 30 ms (250 ticks) y el cliente tarda un poquito en cargarla.
	 */
	private static final int TICKS_HASTA_CRIATURA = 264;
	/** Con la criatura, a todos les da Rojizo por 1 minuto (sin partículas ni ícono). */
	private static final int TICKS_ROJIZO = 60 * 20;
	/** Tick del servidor en que hay que dar el Rojizo; -1 = nada pendiente. */
	private static long darRojizoEn = -1;

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerLivingEntityEvents.AFTER_DEATH.register((entidad, fuente) -> {
			if (entidad instanceof ServerPlayer jugador) mostrarATodos(jugador.server, "muerte");
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (darRojizoEn < 0 || server.getTickCount() < darRojizoEn) return;
			darRojizoEn = -1;
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				jugador.addEffect(new MobEffectInstance(ModPociones.EFECTO_ROJIZO, TICKS_ROJIZO, 0, false, false, false));
			}
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> comando = Commands.literal("ruleta").requires(s -> s.hasPermission(2));
		for (String color : COLORES) {
			comando.then(Commands.literal(color).executes(ctx -> {
				MinecraftServer server = ctx.getSource().getServer();
				mostrarATodos(server, "ruleta_" + color);
				darRojizoEn = color.equals("rojo") ? server.getTickCount() + TICKS_HASTA_CRIATURA : -1;
				ctx.getSource().sendSuccess(() -> Component.literal("Ruleta " + color + "."), true);
				return 1;
			}));
		}
		dispatcher.register(comando);
	}

	public static void mostrarATodos(MinecraftServer server, String animacion) {
		for (ServerPlayer jugador : server.getPlayerList().getPlayers())
			ServerPlayNetworking.send(jugador, new Payload(animacion));
	}

	/** Servidor → cliente: mostrar esa animación (el nombre de un archivo de assets/dedsafio4/animaciones). */
	public record Payload(String animacion) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "ruleta"));
		public static final StreamCodec<ByteBuf, Payload> CODEC =
				ByteBufCodecs.STRING_UTF8.map(Payload::new, Payload::animacion);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
