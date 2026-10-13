package com.dedsafio4.cielo;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * El cielo de noche de las animaciones: mientras dura (la de revivir, la de la Dedita de la Misión), los que están
 * cerca del lugar (a menos de {@code radio} bloques) ven el cielo oscuro con estrellas. Lo dibuja CieloNocheCliente.
 */
public final class CieloNoche {
	private CieloNoche() {}

	/** Servidor → jugadores: cielo de noche alrededor de ese punto, con ese radio, durante tantos ticks. */
	public record Payload(double x, double y, double z, float radio, int ticks) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cielo_noche"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.DOUBLE, Payload::x, ByteBufCodecs.DOUBLE, Payload::y, ByteBufCodecs.DOUBLE, Payload::z,
				ByteBufCodecs.FLOAT, Payload::radio, ByteBufCodecs.VAR_INT, Payload::ticks, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
	}

	/** Lo manda a todos los de ese mundo (cada cliente mira si está cerca). */
	public static void mostrar(ServerLevel mundo, Vec3 centro, float radio, int ticks) {
		Payload p = new Payload(centro.x, centro.y, centro.z, radio, ticks);
		for (ServerPlayer jugador : mundo.players()) ServerPlayNetworking.send(jugador, p);
	}
}
