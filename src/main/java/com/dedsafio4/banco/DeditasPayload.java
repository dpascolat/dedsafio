package com.dedsafio4.banco;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor → cliente: saldo actual de deditas del jugador. */
public record DeditasPayload(long saldo) implements CustomPacketPayload {
	public static final Type<DeditasPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "deditas"));
	public static final StreamCodec<ByteBuf, DeditasPayload> CODEC =
			ByteBufCodecs.VAR_LONG.map(DeditasPayload::new, DeditasPayload::saldo);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
