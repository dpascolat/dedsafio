package com.dedsafio4.hermandad;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Servidor → todos los clientes: a qué Hermandad pertenece cada jugador (para el nombre arriba de la
 * cabeza) y el estandarte activo de cada Hermandad (para las capas y la lista de Hermandades).
 */
public record HermandadesJugadoresPayload(List<Entrada> entradas, List<Activo> activos) implements CustomPacketPayload {
	public record Entrada(UUID jugador, String hermandad, int color) {
		public static final StreamCodec<ByteBuf, Entrada> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Entrada::jugador,
				ByteBufCodecs.STRING_UTF8, Entrada::hermandad,
				ByteBufCodecs.INT, Entrada::color,
				Entrada::new);
	}

	/** El estandarte que eligió una Hermandad. */
	public record Activo(String hermandad, ItemStack estandarte) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Activo> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Activo::hermandad,
				ItemStack.STREAM_CODEC, Activo::estandarte,
				Activo::new);
	}

	public static final Type<HermandadesJugadoresPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "hermandades_jugadores"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HermandadesJugadoresPayload> CODEC = StreamCodec.composite(
			Entrada.CODEC.apply(ByteBufCodecs.list()), HermandadesJugadoresPayload::entradas,
			Activo.CODEC.apply(ByteBufCodecs.list()), HermandadesJugadoresPayload::activos,
			HermandadesJugadoresPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
