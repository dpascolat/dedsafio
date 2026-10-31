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
 * La Cuchara de la Resurrección además gasta el Alma del que revive (sin Alma no se puede usar); el Tenedor de
 * Hierro con Hojas no la gasta.
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

	/**
	 * /baneoperma <jugador> [motivo] (admins): lo banea para siempre con la lista de baneados de Minecraft (la misma
	 * de /ban), así aparece en el Bloque de Baneados y se lo puede revivir. Si está conectado, lo saca del servidor.
	 */
	public static void registrarComandos(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
		var jugadores = net.minecraft.commands.Commands.argument("jugador", net.minecraft.commands.arguments.GameProfileArgument.gameProfile());
		dispatcher.register(net.minecraft.commands.Commands.literal("baneoperma").requires(s -> s.hasPermission(3))
				.then(jugadores
						.executes(c -> banear(c, null))
						.then(net.minecraft.commands.Commands.argument("motivo", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
								.executes(c -> banear(c, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "motivo"))))));
	}

	private static int banear(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c, String motivo)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		var fuente = c.getSource();
		UserBanList bans = fuente.getServer().getPlayerList().getBans();
		int cuantos = 0;
		for (GameProfile perfil : net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles(c, "jugador")) {
			if (perfil.getId() == null || bans.isBanned(perfil)) continue;
			String razon = motivo == null || motivo.isBlank() ? "Baneado para siempre." : motivo;
			bans.add(new UserBanListEntry(perfil, null, fuente.getTextName(), null, razon));
			cuantos++;
			String nombre = perfil.getName();
			fuente.sendSuccess(() -> Component.literal(nombre + " fue baneado para siempre. Aparece en el Bloque de Baneados.")
					.withStyle(ChatFormatting.RED), true);
			ServerPlayer conectado = fuente.getServer().getPlayerList().getPlayer(perfil.getId());
			if (conectado != null) conectado.connection.disconnect(Component.literal("Fuiste baneado: " + razon));
		}
		if (cuantos == 0) fuente.sendFailure(Component.literal("Ese jugador ya estaba baneado."));
		return cuantos;
	}

	/** Las cucharas y tenedores que sirven para desbanear (los simples, sin hojas, no). */
	public static boolean esCubierto(ItemStack item) {
		return item.is(ModItems.CUCHARA_HOJAS) || item.is(ModItems.TENEDOR_HOJAS)
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

	/** El azul de los nombres en el mensaje de revivir. */
	private static final int AZUL_NOMBRE = 0x5AA0FF;

	/** Servidor → todos: arriba de la pantalla, "(cara) X ha resucitado" (lo dibuja ResucitadoCliente). */
	public record ResucitadoPayload(String nombre, String uuid) implements CustomPacketPayload {
		public static final Type<ResucitadoPayload> TYPE = new Type<>(id("resucitado"));
		public static final StreamCodec<ByteBuf, ResucitadoPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, ResucitadoPayload::nombre, ByteBufCodecs.STRING_UTF8, ResucitadoPayload::uuid,
				ResucitadoPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(ListaPayload.TYPE, ListaPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ResucitadoPayload.TYPE, ResucitadoPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DesbanearPayload.TYPE, DesbanearPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(DesbanearPayload.TYPE, (payload, contexto) ->
				contexto.server().execute(() -> desbanear(contexto.player(), payload.pos(), payload.jugador())));
	}

	/** Los baneados del servidor (los que tienen uuid), ordenados por nombre. */
	public static List<GameProfile> baneados(ServerPlayer jugador) {
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

	/** La cuchara o el tenedor que tiene en la mano (o nada). */
	public static ItemStack cubierto(Player jugador) {
		return esCubierto(jugador.getMainHandItem()) ? jugador.getMainHandItem() : esCubierto(jugador.getOffhandItem()) ? jugador.getOffhandItem() : ItemStack.EMPTY;
	}

	/** Cobra el revivir: gasta el cubierto y, si es la Cuchara de la Resurrección, el Alma. Devuelve false si no puede. */
	public static boolean pagar(ServerPlayer jugador) {
		ItemStack item = cubierto(jugador);
		if (item.isEmpty()) {
			jugador.displayClientMessage(Component.literal("Necesitas una cuchara o un tenedor en la mano.").withStyle(ChatFormatting.RED), true);
			return false;
		}
		boolean usaAlma = item.is(ModItems.CUCHARA_HOJAS);
		if (usaAlma && !com.dedsafio4.almas.Almas.tieneAlma(jugador)) {
			jugador.displayClientMessage(Component.literal("La Cuchara de la Resurrección necesita tu Alma.").withStyle(ChatFormatting.RED), true);
			return false;
		}
		if (usaAlma) com.dedsafio4.almas.Almas.ponerAlma(jugador, false);
		item.consume(1, jugador);
		return true;
	}

	private static void desbanear(ServerPlayer jugador, BlockPos pos, String id) {
		if (!jugador.level().getBlockState(pos).is(BLOQUE) || jugador.distanceToSqr(pos.getCenter()) > DISTANCIA * DISTANCIA) return;
		if (cubierto(jugador).isEmpty()) {
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
		if (!pagar(jugador)) return;
		bans.remove(perfil);
		jugador.level().playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
		// El mensaje y el cartel salen cuando termina su animación de revivir (o ya, si no hay lugar de /revivir).
		String revivido = perfil.getName() != null ? perfil.getName() : id;
		com.dedsafio4.revivir.Revivir.alDesbanear(jugador.server, uuid, jugador.getGameProfile().getName(), revivido);
		enviarLista(jugador, pos);
	}

	/** En el chat: "(lápida) El jugador X ha revivido a Y." y arriba en la pantalla de todos "(cara) Y ha resucitado". */
	public static void anunciarRevivido(net.minecraft.server.MinecraftServer server, String quien, String revivido, UUID uuid) {
		server.getPlayerList().broadcastSystemMessage(Component.empty()
				.append(Component.literal("").withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(
						ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos"))))
				.append(Component.literal(" El jugador ").withStyle(ChatFormatting.WHITE))
				.append(Component.literal(quien).withColor(AZUL_NOMBRE))
				.append(Component.literal(" ha revivido a ").withStyle(ChatFormatting.WHITE))
				.append(Component.literal(revivido).withColor(AZUL_NOMBRE))
				.append(Component.literal(".").withStyle(ChatFormatting.WHITE)), false);
		ResucitadoPayload cartel = new ResucitadoPayload(revivido, uuid.toString());
		for (ServerPlayer p : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(p, cartel);
	}
}
