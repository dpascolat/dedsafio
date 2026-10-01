package com.dedsafio4.hermandad;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Cliente → servidor: una acción en el Manuscrito de Hermandad. Todas llevan el nombre
 * que está escrito arriba, así nunca se pierde lo que tipeó el jugador.
 */
public record ManuscritoAccionPayload(int accion, String nombre, boolean manoPrincipal) implements CustomPacketPayload {
	/** Solo guardar el nombre (al cerrar el libro). */
	public static final int GUARDAR_NOMBRE = 0;
	public static final int INSCRIBIRSE = 1;
	public static final int CREAR = 2;

	public static final Type<ManuscritoAccionPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "manuscrito_accion"));
	public static final StreamCodec<ByteBuf, ManuscritoAccionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ManuscritoAccionPayload::accion,
			ByteBufCodecs.stringUtf8(64), ManuscritoAccionPayload::nombre,
			ByteBufCodecs.BOOL, ManuscritoAccionPayload::manoPrincipal,
			ManuscritoAccionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
