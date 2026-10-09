package com.dedsafio4.ruleta;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * /mision 1 (por ahora), y también al entregar la Dedita de la Misión: a los pies del jugador aparece la ruleta de
 * 8 colores acostada en el piso (18 bloques de diámetro). Las porciones salen una tras otra creciendo desde el centro,
 * queda completa 3 s y se encoge. La dibuja
 * cada cliente (RuletaPisoCliente) como un círculo liso, no de bloques. "Abajo" de la ruleta es la espalda del
 * jugador: la ve como si estuviera parado mirando hacia adelante.
 */
public final class RuletaPiso {
	private RuletaPiso() {}

	/** Servidor → jugadores: la ruleta en ese punto, mirando hacia yaw. */
	public record Payload(double x, double y, double z, float yaw) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "ruleta_piso"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.DOUBLE, Payload::x, ByteBufCodecs.DOUBLE, Payload::y, ByteBufCodecs.DOUBLE, Payload::z,
				ByteBufCodecs.FLOAT, Payload::yaw, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Pone la ruleta a los pies de ese jugador (la ven todos los de su mundo). */
	public static void mostrar(ServerPlayer p) {
		Vec3 pies = p.position();
		Payload payload = new Payload(pies.x, pies.y, pies.z, p.getYRot());
		for (ServerPlayer otro : p.serverLevel().players()) ServerPlayNetworking.send(otro, payload);
	}

	/** Cuánto dura la animación (en ticks). */
	public static final int DURACION = 360;

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mision").requires(s -> s.hasPermission(2))
				.then(Commands.argument("numero", IntegerArgumentType.integer(1, 1)).executes(c -> {
					mostrar(c.getSource().getPlayerOrException());
					c.getSource().sendSuccess(() -> Component.literal("Ruleta de la misión 1.").withStyle(ChatFormatting.AQUA), true);
					return 1;
				})));
	}
}
