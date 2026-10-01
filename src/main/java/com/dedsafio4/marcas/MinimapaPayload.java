package com.dedsafio4.marcas;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor → cliente: si los admins pueden usar el minimapa (si no, nadie puede). */
public record MinimapaPayload(boolean adminsLoUsan) implements CustomPacketPayload {
	public static final Type<MinimapaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "minimapa"));
	public static final StreamCodec<ByteBuf, MinimapaPayload> CODEC =
			ByteBufCodecs.BOOL.map(MinimapaPayload::new, MinimapaPayload::adminsLoUsan);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
