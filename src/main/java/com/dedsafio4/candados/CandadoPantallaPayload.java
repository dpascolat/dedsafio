package com.dedsafio4.candados;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Servidor → cliente: abrir la pantalla del candado.
 *
 * @param cerrar true para poner el candado (pide un código nuevo), false para pedir el código
 */
public record CandadoPantallaPayload(BlockPos pos, boolean cerrar, String aviso) implements CustomPacketPayload {
	public static final Type<CandadoPantallaPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "candado_pantalla"));
	public static final StreamCodec<ByteBuf, CandadoPantallaPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, CandadoPantallaPayload::pos,
			ByteBufCodecs.BOOL, CandadoPantallaPayload::cerrar,
			ByteBufCodecs.stringUtf8(64), CandadoPantallaPayload::aviso,
			CandadoPantallaPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
