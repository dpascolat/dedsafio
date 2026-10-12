package com.dedsafio4.cielo;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Servidor → cliente: cómo está el cielo ({@code modo}: 0 normal, 1 rojo, 2 rojo 2 con Reviil; 3 = Reviil da un
 * cabezazo, sin cambiar el cielo).
 * {@code animar} es true cuando cambió con el comando (se muestra la animación) y false al entrar al mundo.
 */
public record CieloPayload(int modo, boolean animar) implements CustomPacketPayload {
	public static final int NORMAL = 0, ROJO = 1, ROJO_2 = 2, CABEZAZO = 3;

	public static final Type<CieloPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cielo"));
	public static final StreamCodec<ByteBuf, CieloPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CieloPayload::modo,
			ByteBufCodecs.BOOL, CieloPayload::animar,
			CieloPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
