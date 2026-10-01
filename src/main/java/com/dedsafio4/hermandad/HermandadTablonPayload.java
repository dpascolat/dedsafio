package com.dedsafio4.hermandad;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Servidor → miembros: los anuncios del Tablón de su Hermandad (el más nuevo primero). */
public record HermandadTablonPayload(List<Anuncio> anuncios) implements CustomPacketPayload {
	/**
	 * @param fecha "dd/MM HH:mm" del servidor
	 * @param autor quién lo publicó
	 */
	public record Anuncio(String fecha, String autor, String texto) {
		public static final StreamCodec<ByteBuf, Anuncio> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Anuncio::fecha,
				ByteBufCodecs.STRING_UTF8, Anuncio::autor,
				ByteBufCodecs.STRING_UTF8, Anuncio::texto,
				Anuncio::new);
	}

	public static final Type<HermandadTablonPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "hermandad_tablon"));
	public static final StreamCodec<ByteBuf, HermandadTablonPayload> CODEC = StreamCodec.composite(
			Anuncio.CODEC.apply(ByteBufCodecs.list()), HermandadTablonPayload::anuncios,
			HermandadTablonPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
