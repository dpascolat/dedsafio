package com.dedsafio4.marcas;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Servidor → cliente: todas las marcas que pusieron los admins (se ven en el mundo). */
public record MarcasPayload(List<Marca> marcas) implements CustomPacketPayload {
	public record Marca(String nombre, String dimension, int x, int y, int z, int color) {
		public static final StreamCodec<ByteBuf, Marca> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Marca::nombre,
				ByteBufCodecs.STRING_UTF8, Marca::dimension,
				ByteBufCodecs.VAR_INT, Marca::x,
				ByteBufCodecs.VAR_INT, Marca::y,
				ByteBufCodecs.VAR_INT, Marca::z,
				ByteBufCodecs.INT, Marca::color,
				Marca::new);
	}

	public static final Type<MarcasPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "marcas"));
	public static final StreamCodec<ByteBuf, MarcasPayload> CODEC =
			Marca.CODEC.apply(ByteBufCodecs.list()).map(MarcasPayload::new, MarcasPayload::marcas);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
