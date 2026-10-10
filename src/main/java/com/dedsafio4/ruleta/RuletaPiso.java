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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * /mision 1 (por ahora), y también al entregar la Dedita de la Misión: aparece la ruleta de 8 colores acostada en el
 * piso (18 bloques de diámetro) con la esfera aurora. Salen los 8 colores a la vez, queda completa 3 s, se encoge y
 * sale un anillo de colores (11,8 s en total). Con /mision 1 sale a los pies del jugador, con la esfera grande en el
 * centro; con el bloque de misión, sale en el bloque y la esfera chiquita arriba del bloque. La dibuja
 * cada cliente (RuletaPisoCliente) como un círculo liso, no de bloques. "Abajo" de la ruleta es la espalda del
 * jugador: la ve como si estuviera parado mirando hacia adelante.
 */
public final class RuletaPiso {
	private RuletaPiso() {}

	/** Servidor → jugadores: la ruleta en ese punto, mirando hacia yaw, y la esfera aurora (su centro y su radio). */
	public record Payload(double x, double y, double z, float yaw, double esferaX, double esferaY, double esferaZ, float radioEsfera)
			implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "ruleta_piso"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeDouble(p.x);
			buf.writeDouble(p.y);
			buf.writeDouble(p.z);
			buf.writeFloat(p.yaw);
			buf.writeDouble(p.esferaX);
			buf.writeDouble(p.esferaY);
			buf.writeDouble(p.esferaZ);
			buf.writeFloat(p.radioEsfera);
		}, buf -> new Payload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readDouble(), buf.readDouble(),
				buf.readDouble(), buf.readFloat()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Pone la ruleta a los pies de ese jugador, con la esfera grande (2,2 de alto) en el centro. */
	public static void mostrar(ServerPlayer p) {
		Vec3 pies = p.position();
		mostrar(p.serverLevel(), pies, p.getYRot(), pies.add(0, 0.12 + 1.1, 0), 1.1f);
	}

	/** Pone la ruleta en ese punto del piso, mirando hacia yaw (-90 = hacia +X), y la esfera (la ven todos los de ese mundo). */
	public static void mostrar(net.minecraft.server.level.ServerLevel level, Vec3 centro, float yaw, Vec3 esfera, float radioEsfera) {
		Payload payload = new Payload(centro.x, centro.y, centro.z, yaw, esfera.x, esfera.y, esfera.z, radioEsfera);
		for (ServerPlayer otro : level.players()) ServerPlayNetworking.send(otro, payload);
	}

	/** Cuánto dura la animación (en ticks): 80 salen, 60 quieta, 16 vuelve, 80 el anillo. */
	public static final int DURACION = 236;

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
