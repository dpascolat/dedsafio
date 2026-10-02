package com.dedsafio4.neocompat.fabric.api.networking.v1;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerPlayNetworking {
	private ServerPlayNetworking() {}

	public interface PlayPayloadHandler<T extends CustomPacketPayload> {
		void receive(T payload, Context context);
	}

	public interface Context {
		ServerPlayer player();

		MinecraftServer server();

		PacketSender responseSender();
	}

	public static final Map<CustomPacketPayload.Type<?>, PlayPayloadHandler<?>> RECEPTORES = new ConcurrentHashMap<>();

	public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> tipo, PlayPayloadHandler<T> receptor) {
		RECEPTORES.put(tipo, receptor);
		return true;
	}

	public static void send(ServerPlayer jugador, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayer(jugador, payload);
	}

	public static boolean canSend(ServerPlayer jugador, CustomPacketPayload.Type<?> tipo) {
		return jugador.connection != null && jugador.connection.hasChannel(tipo);
	}
}
