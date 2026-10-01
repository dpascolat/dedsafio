package com.dedsafio4.hermandad;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/** Servidor → cliente: datos de la Hermandad del jugador para la interfaz (tecla H). */
public record HermandadInfoPayload(String nombre, int color, UUID maestro, List<Miembro> miembros,
								   long balance, int nivelBanco, String motd) implements CustomPacketPayload {
	/** @param lider si tiene el rango de Líder (puede publicar en el Tablón) */
	public record Miembro(UUID uuid, String nombre, boolean lider) {
		public static final StreamCodec<ByteBuf, Miembro> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Miembro::uuid,
				ByteBufCodecs.STRING_UTF8, Miembro::nombre,
				ByteBufCodecs.BOOL, Miembro::lider,
				Miembro::new);
	}

	private static final StreamCodec<ByteBuf, List<Miembro>> MIEMBROS = Miembro.CODEC.apply(ByteBufCodecs.list());

	public static final Type<HermandadInfoPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "hermandad_info"));
	public static final StreamCodec<ByteBuf, HermandadInfoPayload> CODEC = StreamCodec.of(
			(buf, p) -> {
				ByteBufCodecs.STRING_UTF8.encode(buf, p.nombre());
				ByteBufCodecs.INT.encode(buf, p.color());
				UUIDUtil.STREAM_CODEC.encode(buf, p.maestro());
				MIEMBROS.encode(buf, p.miembros());
				ByteBufCodecs.VAR_LONG.encode(buf, p.balance());
				ByteBufCodecs.VAR_INT.encode(buf, p.nivelBanco());
				ByteBufCodecs.STRING_UTF8.encode(buf, p.motd());
			},
			buf -> new HermandadInfoPayload(
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.INT.decode(buf),
					UUIDUtil.STREAM_CODEC.decode(buf),
					MIEMBROS.decode(buf),
					ByteBufCodecs.VAR_LONG.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf)));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
