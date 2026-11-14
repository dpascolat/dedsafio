package com.dedsafio4.hermandad;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Servidor → cliente: el chat de la Hermandad (solo lo reciben sus miembros). */
public record HermandadChatPayload(List<Linea> lineas) implements CustomPacketPayload {
	/** Copia las listas: si no, se pueden cambiar mientras se mandan y el jugador queda desconectado. */
	public HermandadChatPayload {
		lineas = List.copyOf(lineas);
	}

	/**
	 * @param hora  "HH:mm" del servidor
	 * @param autor quién lo escribió; vacío si es un aviso (alguien se unió, lo expulsaron...)
	 */
	public record Linea(String hora, String autor, String texto) {
		public static final StreamCodec<ByteBuf, Linea> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Linea::hora,
				ByteBufCodecs.STRING_UTF8, Linea::autor,
				ByteBufCodecs.STRING_UTF8, Linea::texto,
				Linea::new);
	}

	public static final Type<HermandadChatPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "hermandad_chat"));
	public static final StreamCodec<ByteBuf, HermandadChatPayload> CODEC = StreamCodec.composite(
			Linea.CODEC.apply(ByteBufCodecs.list()), HermandadChatPayload::lineas,
			HermandadChatPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
