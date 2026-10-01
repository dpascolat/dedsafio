package com.dedsafio4.candados;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cliente → servidor: el código que escribió el jugador. */
public record CandadoCodigoPayload(BlockPos pos, boolean cerrar, String codigo) implements CustomPacketPayload {
	public static final Type<CandadoCodigoPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "candado_codigo"));
	public static final StreamCodec<ByteBuf, CandadoCodigoPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, CandadoCodigoPayload::pos,
			ByteBufCodecs.BOOL, CandadoCodigoPayload::cerrar,
			ByteBufCodecs.stringUtf8(Candados.LARGO_MAXIMO_CODIGO), CandadoCodigoPayload::codigo,
			CandadoCodigoPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
