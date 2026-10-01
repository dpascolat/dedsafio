package com.dedsafio4.hermandad;

import com.dedsafio4.Dedsafio4;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Servidor → miembros: la Colección de Estandartes de su Hermandad (pestaña Estandartes)
 * y cuál está activo (-1 si ninguno).
 */
public record HermandadEstandartesPayload(List<ItemStack> coleccion, int activo) implements CustomPacketPayload {
	public static final Type<HermandadEstandartesPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "hermandad_estandartes"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HermandadEstandartesPayload> CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), HermandadEstandartesPayload::coleccion,
			ByteBufCodecs.VAR_INT, HermandadEstandartesPayload::activo,
			HermandadEstandartesPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
