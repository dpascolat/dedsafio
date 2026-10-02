package com.dedsafio4.neocompat.fabric.api.networking.v1;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/** Anota los tipos de paquetes; Puente los registra en NeoForge (RegisterPayloadHandlersEvent). */
public final class PayloadTypeRegistry {
	public record Registro<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> tipo,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {}

	public static final List<Registro<?>> AL_CLIENTE = new ArrayList<>(), AL_SERVIDOR = new ArrayList<>();
	private static final PayloadTypeRegistry S2C = new PayloadTypeRegistry(AL_CLIENTE), C2S = new PayloadTypeRegistry(AL_SERVIDOR);

	private final List<Registro<?>> lista;

	private PayloadTypeRegistry(List<Registro<?>> lista) {
		this.lista = lista;
	}

	public static PayloadTypeRegistry playS2C() {
		return S2C;
	}

	public static PayloadTypeRegistry playC2S() {
		return C2S;
	}

	public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> tipo, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		lista.add(new Registro<>(tipo, codec));
	}
}
