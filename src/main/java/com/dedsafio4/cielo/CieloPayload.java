package com.dedsafio4.cielo;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Servidor → cliente: si el cielo rojo está activo.
 * {@code animar} es true cuando cambió con el comando (se muestra la animación) y false al entrar al mundo.
 */
public record CieloPayload(boolean rojo, boolean animar) implements CustomPacketPayload {
	public static final Type<CieloPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cielo"));
	public static final StreamCodec<ByteBuf, CieloPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, CieloPayload::rojo,
			ByteBufCodecs.BOOL, CieloPayload::animar,
			CieloPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
