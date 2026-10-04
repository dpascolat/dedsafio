package com.dedsafio4.hermandad;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cliente → servidor: acciones desde la interfaz de la Hermandad. */
public record HermandadAccionPayload(int accion, String texto) implements CustomPacketPayload {
	/** Pedir los datos para abrir o refrescar la interfaz. */
	public static final int PEDIR_INFO = 0;
	/** texto = nombre del jugador a invitar. */
	public static final int INVITAR = 1;
	/** texto = UUID del miembro a expulsar. */
	public static final int EXPULSAR = 2;
	/** texto = color RGB del nombre de la Hermandad, en hexadecimal. */
	public static final int COLOR = 3;
	/** texto = mensaje para el chat de la Hermandad. */
	public static final int CHAT = 4;
	/** Guardar en la colección el estandarte que el jugador tiene en la mano. */
	public static final int ESTANDARTE_GUARDAR = 6;
	/** texto = número del lugar en la colección. Los miembros llevan ese estandarte de capa. */
	public static final int ESTANDARTE_ACTIVAR = 7;
	/** texto = número del lugar. El estandarte vuelve al inventario del Maestro. */
	public static final int ESTANDARTE_RECUPERAR = 8;
	/** texto = número del lugar. Se borra de la colección. */
	public static final int ESTANDARTE_BORRAR = 9;
	/** Ya no hay estandarte activo (se van las capas). */
	public static final int ESTANDARTE_QUITAR = 10;
	/** texto = el anuncio para el Tablón (Maestro o Líderes). */
	public static final int ANUNCIO_PUBLICAR = 11;
	/** texto = número del anuncio a borrar (Maestro o Líderes). */
	public static final int ANUNCIO_BORRAR = 12;
	/** texto = UUID del miembro que pasa a ser Líder (solo el Maestro). */
	public static final int SUBIR_RANGO = 13;
	/** texto = UUID del Líder que vuelve a ser miembro (solo el Maestro). */
	public static final int BAJAR_RANGO = 14;
	/** El Maestro disuelve la Hermandad (se borra para todos). */
	public static final int DISOLVER = 15;
	/** Un miembro (que no sea el Maestro) se sale de la Hermandad. */
	public static final int SALIR = 16;

	/** Largo máximo de un mensaje del chat, como el chat normal. */
	public static final int LARGO_CHAT = 256;

	public static final Type<HermandadAccionPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "hermandad_accion"));
	public static final StreamCodec<ByteBuf, HermandadAccionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HermandadAccionPayload::accion,
			ByteBufCodecs.stringUtf8(LARGO_CHAT), HermandadAccionPayload::texto,
			HermandadAccionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
