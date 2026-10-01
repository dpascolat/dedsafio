package com.dedsafio4.candados;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Servidor → cliente: los cofres con candado de la dimensión donde está el jugador (para dibujarlos). */
public record CandadosListaPayload(List<BlockPos> posiciones) implements CustomPacketPayload {
	public static final Type<CandadosListaPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "candados_lista"));
	public static final StreamCodec<ByteBuf, CandadosListaPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), CandadosListaPayload::posiciones,
			CandadosListaPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
