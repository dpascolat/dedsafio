package com.dedsafio4.disfraz;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /cambiarmob <mob> [jugadores]: el jugador se ve como ese mob (de Minecraft o de cualquier mod) y tiene su tamaño
 * (la cámara queda a la altura de sus ojos). También los modelos del Skin Pack Dedsafío: /cambiarmob skin:nutria
 * (ver SkinsDedsafio; esos tienen el tamaño normal del jugador). /cambiarmob quitar [jugadores] lo vuelve a la normalidad.
 * Se guarda en el mundo (sigue transformado aunque salga y vuelva a entrar) y se les manda a todos los clientes,
 * que dibujan el mob en lugar del jugador (ver DisfracesCliente).
 */
public final class Disfraces {
	private Disfraces() {}

	/** Lo que sabe el cliente: jugador → mob (lo llena DisfracesCliente con el Payload). */
	public static final Map<UUID, ResourceLocation> CLIENTE = new ConcurrentHashMap<>();

	public static final class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		final Map<UUID, ResourceLocation> mobs = new HashMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (String clave : tag.getAllKeys()) {
				ResourceLocation id = ResourceLocation.tryParse(tag.getString(clave));
				if (id != null) d.mobs.put(UUID.fromString(clave), id);
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			mobs.forEach((jugador, id) -> tag.putString(jugador.toString(), id.toString()));
			return tag;
		}
	}

	public static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_disfraces");
	}

	/** Servidor → clientes: todos los jugadores transformados (jugador → id del mob). */
	public record Payload(Map<UUID, String> mobs) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "disfraces"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.STRING_UTF8), Payload::mobs, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** El mob en que está transformado (o null si es él mismo). Sirve del lado del servidor y del cliente. */
	public static EntityType<?> de(Player jugador) {
		ResourceLocation id;
		if (jugador.level().isClientSide) id = CLIENTE.get(jugador.getUUID());
		else if (jugador.getServer() != null) id = datos(jugador.getServer()).mobs.get(jugador.getUUID());
		else return null;
		return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
	}

	/** El modelo del Skin Pack en que está transformado (por ejemplo "nutria"), o null. */
	public static String skin(Player jugador) {
		ResourceLocation id;
		if (jugador.level().isClientSide) id = CLIENTE.get(jugador.getUUID());
		else if (jugador.getServer() != null) id = datos(jugador.getServer()).mobs.get(jugador.getUUID());
		else return null;
		return id != null && id.getNamespace().equals(SkinsDedsafio.ESPACIO) ? id.getPath() : null;
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			enviarATodos(server);
			handler.player.refreshDimensions();
		});
	}

	private static void enviarATodos(MinecraftServer server) {
		Map<UUID, String> mobs = new HashMap<>();
		datos(server).mobs.forEach((jugador, id) -> mobs.put(jugador, id.toString()));
		for (ServerPlayer p : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(p, new Payload(mobs));
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cambiarmob").requires(s -> s.hasPermission(2))
				.then(Commands.literal("quitar")
						.executes(c -> quitar(c, List.of(c.getSource().getPlayerOrException())))
						.then(Commands.argument("jugadores", EntityArgument.players())
								.executes(c -> quitar(c, EntityArgument.getPlayers(c, "jugadores")))))
				.then(Commands.argument("mob", ResourceLocationArgument.id())
						.suggests((c, b) -> SharedSuggestionProvider.suggestResource(java.util.stream.Stream.concat(
								SkinsDedsafio.NOMBRES.stream().map(n -> ResourceLocation.fromNamespaceAndPath(SkinsDedsafio.ESPACIO, n)),
								BuiltInRegistries.ENTITY_TYPE.stream().filter(Disfraces::sirve).map(EntityType::getKey)), b))
						.executes(c -> cambiar(c, List.of(c.getSource().getPlayerOrException())))
						.then(Commands.argument("jugadores", EntityArgument.players())
								.executes(c -> cambiar(c, EntityArgument.getPlayers(c, "jugadores"))))));
	}

	/** Los mobs en los que se puede transformar: los que se pueden invocar (no el jugador). */
	private static boolean sirve(EntityType<?> tipo) {
		return tipo.canSummon() && tipo != EntityType.PLAYER;
	}

	private static int cambiar(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> jugadores) throws CommandSyntaxException {
		ResourceLocation id = ResourceLocationArgument.getId(c, "mob");
		Component nombre;
		if (id.getNamespace().equals(SkinsDedsafio.ESPACIO)) {
			if (!SkinsDedsafio.NOMBRES.contains(id.getPath())) {
				c.getSource().sendFailure(Component.literal("No existe la skin \"" + id.getPath() + "\"."));
				return 0;
			}
			nombre = Component.literal("la skin " + id.getPath());
		} else {
			EntityType<?> tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
			if (tipo == null || !sirve(tipo)) {
				c.getSource().sendFailure(Component.literal("No existe el mob \"" + id + "\"."));
				return 0;
			}
			nombre = tipo.getDescription();
		}
		MinecraftServer server = c.getSource().getServer();
		for (ServerPlayer p : jugadores) datos(server).mobs.put(p.getUUID(), id);
		listo(server, jugadores);
		c.getSource().sendSuccess(() -> Component.literal(cuantos(jugadores) + " ahora " + (jugadores.size() == 1 ? "es " : "son ")
				+ nombre.getString() + ".").withStyle(ChatFormatting.GOLD), true);
		return jugadores.size();
	}

	private static int quitar(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> jugadores) {
		MinecraftServer server = c.getSource().getServer();
		for (ServerPlayer p : jugadores) datos(server).mobs.remove(p.getUUID());
		listo(server, jugadores);
		c.getSource().sendSuccess(() -> Component.literal(cuantos(jugadores) + " volvió a la normalidad.")
				.withStyle(ChatFormatting.GOLD), true);
		return jugadores.size();
	}

	private static String cuantos(Collection<ServerPlayer> jugadores) {
		return jugadores.size() == 1 ? jugadores.iterator().next().getName().getString() : jugadores.size() + " jugadores";
	}

	private static void listo(MinecraftServer server, Collection<ServerPlayer> jugadores) {
		datos(server).setDirty();
		enviarATodos(server);
		for (ServerPlayer p : jugadores) p.refreshDimensions();
	}
}
