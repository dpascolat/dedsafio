package com.dedsafio4.temporizador;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor → cliente: segundos que le quedan al temporizador (-1 = no hay temporizador). */
public record TemporizadorPayload(int segundos) implements CustomPacketPayload {
	public static final Type<TemporizadorPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "temporizador"));
	public static final StreamCodec<ByteBuf, TemporizadorPayload> CODEC =
			ByteBufCodecs.VAR_INT.map(TemporizadorPayload::new, TemporizadorPayload::segundos);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
