package com.dedsafio4.hermandad;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.marcas.MarcasPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Servidor → miembros: las ubicaciones marcadas por su Hermandad (en todas las dimensiones). */
public record HermandadUbicacionesPayload(List<MarcasPayload.Marca> ubicaciones) implements CustomPacketPayload {
	public static final Type<HermandadUbicacionesPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "hermandad_ubicaciones"));
	public static final StreamCodec<ByteBuf, HermandadUbicacionesPayload> CODEC =
			MarcasPayload.Marca.CODEC.apply(ByteBufCodecs.list()).map(HermandadUbicacionesPayload::new, HermandadUbicacionesPayload::ubicaciones);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
