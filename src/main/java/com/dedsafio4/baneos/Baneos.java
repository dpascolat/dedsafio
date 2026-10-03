package com.dedsafio4.baneos;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * El Bloque de Baneados (con la textura del bloque barrera): con click derecho muestra a todos los baneados del
 * servidor; eligiendo uno y teniendo una cuchara o un tenedor en la mano, se lo desbanea (se gasta el cubierto).
 */
public final class Baneos {
	private Baneos() {}

	/** Hasta qué distancia del bloque se puede desbanear (para que no lo hagan de lejos). */
	private static final double DISTANCIA = 8;

	public static final Block BLOQUE = Registry.register(BuiltInRegistries.BLOCK, id("bloque_baneados"),
			new BloqueBaneados(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).noOcclusion()
					.isValidSpawn(Blocks::never).isRedstoneConductor((e, m, p) -> false)
					.isSuffocating((e, m, p) -> false).isViewBlocking((e, m, p) -> false)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, id("bloque_baneados"),
			new BlockItem(BLOQUE, new Item.Properties()));

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Las cucharas y tenedores que sirven para desbanear. */
	public static boolean esCubierto(ItemStack item) {
		return item.is(ModItems.CUCHARA_MADERA) || item.is(ModItems.CUCHARA_HOJAS) || item.is(ModItems.TENEDOR_HOJAS)
				|| item.is(ModItems.CUCHARA_DORADA_HOJAS);
	}

	// --- El bloque ---

	public static class BloqueBaneados extends Block {
		public static final MapCodec<BloqueBaneados> CODEC = simpleCodec(BloqueBaneados::new);

		public BloqueBaneados(Properties propiedades) {
			super(propiedades);
		}

		@Override
		protected MapCodec<? extends Block> codec() {
			return CODEC;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos pos, Player jugador, BlockHitResult golpe) {
			if (jugador instanceof ServerPlayer servidor) enviarLista(servidor, pos);
			return InteractionResult.sidedSuccess(mundo.isClientSide);
		}
	}

	// --- Los mensajes entre servidor y cliente ---

	/** Servidor → cliente: abrir (o actualizar) la pantalla con los baneados. */
	public record ListaPayload(BlockPos pos, List<String> ids, List<String> nombres) implements CustomPacketPayload {
		public static final Type<ListaPayload> TYPE = new Type<>(id("baneados_lista"));
		public static final StreamCodec<ByteBuf, ListaPayload> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, ListaPayload::pos,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ListaPayload::ids,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ListaPayload::nombres,
				ListaPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: desbanear a ese jugador (con el cubierto que tiene en la mano). */
	public record DesbanearPayload(BlockPos pos, String jugador) implements CustomPacketPayload {
		public static final Type<DesbanearPayload> TYPE = new Type<>(id("baneados_desbanear"));
		public static final StreamCodec<ByteBuf, DesbanearPayload> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, DesbanearPayload::pos,
				ByteBufCodecs.STRING_UTF8, DesbanearPayload::jugador,
				DesbanearPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(ListaPayload.TYPE, ListaPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DesbanearPayload.TYPE, DesbanearPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(DesbanearPayload.TYPE, (payload, contexto) ->
				contexto.server().execute(() -> desbanear(contexto.player(), payload.pos(), payload.jugador())));
	}

	/** Los baneados del servidor (los que tienen uuid), ordenados por nombre. */
	private static List<GameProfile> baneados(ServerPlayer jugador) {
		List<GameProfile> lista = new ArrayList<>();
		for (UserBanListEntry entrada : jugador.server.getPlayerList().getBans().getEntries()) {
			Object usuario = ((com.dedsafio4.mixin.StoredUserEntryAccessor) entrada).dedsafio4$usuario();
			if (usuario instanceof GameProfile perfil && perfil.getId() != null) lista.add(perfil);
		}
		lista.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName() == null ? "" : a.getName(), b.getName() == null ? "" : b.getName()));
		return lista;
	}

	public static void enviarLista(ServerPlayer jugador, BlockPos pos) {
		List<String> ids = new ArrayList<>(), nombres = new ArrayList<>();
		for (GameProfile perfil : baneados(jugador)) {
			ids.add(perfil.getId().toString());
			nombres.add(perfil.getName() == null ? perfil.getId().toString().substring(0, 8) : perfil.getName());
		}
		ServerPlayNetworking.send(jugador, new ListaPayload(pos, ids, nombres));
	}

	private static void desbanear(ServerPlayer jugador, BlockPos pos, String id) {
		if (!jugador.level().getBlockState(pos).is(BLOQUE) || jugador.distanceToSqr(pos.getCenter()) > DISTANCIA * DISTANCIA) return;
		InteractionHand mano = esCubierto(jugador.getMainHandItem()) ? InteractionHand.MAIN_HAND
				: esCubierto(jugador.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
		if (mano == null) {
			jugador.displayClientMessage(Component.literal("Necesitas una cuchara o un tenedor en la mano.").withStyle(ChatFormatting.RED), true);
			return;
		}
		UUID uuid;
		try {
			uuid = UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			return;
		}
		UserBanList bans = jugador.server.getPlayerList().getBans();
		GameProfile perfil = baneados(jugador).stream().filter(p -> p.getId().equals(uuid)).findFirst().orElse(null);
		if (perfil == null) {
			enviarLista(jugador, pos);
			return;
		}
		bans.remove(perfil);
		jugador.getItemInHand(mano).consume(1, jugador);
		jugador.level().playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
		jugador.server.getPlayerList().broadcastSystemMessage(Component.literal(jugador.getGameProfile().getName() + " revivió a "
				+ (perfil.getName() != null ? perfil.getName() : id) + ".").withStyle(ChatFormatting.GREEN), false);
		enviarLista(jugador, pos);
	}
}
