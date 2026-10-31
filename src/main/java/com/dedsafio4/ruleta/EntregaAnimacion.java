package com.dedsafio4.ruleta;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * La animación de entregar la Dedita de la Misión (la dibuja cada cliente: EntregaCliente). Animación de NavithLabs /
 * AlieRequiem, usada con su permiso.
 */
public final class EntregaAnimacion {
	private EntregaAnimacion() {}

	/** Cuándo llega el premio y el aviso en el chat (en ticks), y cuándo termina todo. */
	public static final int AVISO = 180, DURACION = 216;

	/** Servidor → jugadores: la animación en ese punto del piso, con la luz a esa altura, y quién entregó. */
	public record Payload(double x, double y, double z, double luzY, UUID quien) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "entrega_mision"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeDouble(p.x);
			buf.writeDouble(p.y);
			buf.writeDouble(p.z);
			buf.writeDouble(p.luzY);
			buf.writeUUID(p.quien);
		}, buf -> new Payload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readUUID()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void mostrar(ServerLevel level, Payload payload) {
		for (ServerPlayer otro : level.players()) ServerPlayNetworking.send(otro, payload);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
	}
}
