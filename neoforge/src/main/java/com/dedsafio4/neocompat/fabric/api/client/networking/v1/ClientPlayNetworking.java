package com.dedsafio4.neocompat.fabric.api.client.networking.v1;

import com.dedsafio4.neocompat.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPlayNetworking {
	private ClientPlayNetworking() {}

	public interface PlayPayloadHandler<T extends CustomPacketPayload> {
		void receive(T payload, Context context);
	}

	public interface Context {
		Minecraft client();

		LocalPlayer player();

		PacketSender responseSender();
	}

	public static final Map<CustomPacketPayload.Type<?>, PlayPayloadHandler<?>> RECEPTORES = new ConcurrentHashMap<>();

	public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> tipo, PlayPayloadHandler<T> receptor) {
		RECEPTORES.put(tipo, receptor);
		return true;
	}

	public static void send(CustomPacketPayload payload) {
		PacketDistributor.sendToServer(payload);
	}

	public static boolean canSend(CustomPacketPayload.Type<?> tipo) {
		var conexion = Minecraft.getInstance().getConnection();
		return conexion != null && conexion.hasChannel(tipo);
	}
}
