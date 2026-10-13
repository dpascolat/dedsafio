package com.dedsafio4.baneos;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ítems baneados (admins):
 *   /banear item <ítem>     ese ítem ya no se puede usar (clic derecho: el arco no dispara, la perla no se tira,
 *                           no se come, no se pone como bloque...). En el inventario su casilla se ve rojiza.
 *   /desbanear item <ítem>  se vuelve a poder usar
 *   /banear item            la lista
 * Queda guardado en el mundo. Lo frena ItemBaneadoMixin (en el servidor y en el cliente, por eso se le manda la
 * lista a cada jugador); la casilla rojiza la dibuja el cliente.
 */
public final class ItemsBaneados {
	private ItemsBaneados() {}

	/** El fondo de la casilla de un ítem baneado: rojo a medias (sobre el gris de la casilla queda rojizo). */
	public static final int ROJIZO = 0x90D23C3C;

	/** La lista en el cliente (la manda el servidor). */
	public static volatile Set<Item> CLIENTE = Set.of();

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		final Set<String> ids = new LinkedHashSet<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (Tag t : tag.getList("items", Tag.TAG_STRING)) d.ids.add(t.getAsString());
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag l = new ListTag();
			for (String id : ids) l.add(StringTag.valueOf(id));
			tag.put("items", l);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_items_baneados");
	}

	/** Servidor → jugador: los ítems baneados. */
	public record Payload(List<String> ids) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "items_baneados"));
		public static final StreamCodec<ByteBuf, Payload> CODEC =
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(Payload::new, Payload::ids);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** ¿Ese ítem está baneado? (en el mundo del servidor o, en el cliente, según la lista que mandó el servidor). */
	public static boolean baneado(Item item, Level mundo) {
		if (item == Items.AIR) return false;
		if (mundo.isClientSide) return CLIENTE.contains(item);
		return mundo.getServer() != null && datos(mundo.getServer()).ids.contains(BuiltInRegistries.ITEM.getKey(item).toString());
	}

	/** En el cliente: lo que mandó el servidor. */
	public static void recibir(Payload p) {
		Set<Item> s = new LinkedHashSet<>();
		for (String id : p.ids()) {
			ResourceLocation rl = ResourceLocation.tryParse(id);
			if (rl != null) BuiltInRegistries.ITEM.getOptional(rl).ifPresent(s::add);
		}
		CLIENTE = Set.copyOf(s);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> enviar(handler.player));
	}

	private static void enviar(ServerPlayer jugador) {
		ServerPlayNetworking.send(jugador, new Payload(new ArrayList<>(datos(jugador.server).ids)));
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		var argumento = Commands.argument("item", ResourceLocationArgument.id())
				.suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), b));
		dispatcher.register(Commands.literal("banear").requires(s -> s.hasPermission(2))
				.then(Commands.literal("item")
						.executes(ItemsBaneados::listar)
						.then(argumento.executes(c -> cambiar(c, true)))));
		dispatcher.register(Commands.literal("desbanear").requires(s -> s.hasPermission(2))
				.then(Commands.literal("item")
						.then(Commands.argument("item", ResourceLocationArgument.id())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(datos(c.getSource().getServer()).ids, b))
								.executes(c -> cambiar(c, false)))));
	}

	/** "arco" no existe, pero "bow" sí: se prueba con minecraft: y con dedsafio4:. */
	private static Item item(ResourceLocation id) {
		var item = BuiltInRegistries.ITEM.getOptional(id);
		if (item.isEmpty() && id.getNamespace().equals("minecraft")) {
			item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, id.getPath()));
		}
		return item.filter(i -> i != Items.AIR).orElse(null);
	}

	private static int cambiar(CommandContext<CommandSourceStack> c, boolean banear) {
		Item item = item(ResourceLocationArgument.getId(c, "item"));
		if (item == null) {
			c.getSource().sendFailure(Component.literal("No existe ese ítem."));
			return 0;
		}
		MinecraftServer server = c.getSource().getServer();
		Datos d = datos(server);
		String id = BuiltInRegistries.ITEM.getKey(item).toString();
		boolean cambio = banear ? d.ids.add(id) : d.ids.remove(id);
		d.setDirty();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) enviar(p);
		String nombre = item.getDescription().getString();
		c.getSource().sendSuccess(() -> Component.literal(!cambio
				? nombre + (banear ? " ya estaba baneado." : " no estaba baneado.")
				: banear ? nombre + " está baneado: ya no se puede usar." : nombre + " se puede usar de nuevo.")
				.withStyle(banear ? ChatFormatting.RED : ChatFormatting.GREEN), true);
		return 1;
	}

	private static int listar(CommandContext<CommandSourceStack> c) {
		Set<String> ids = datos(c.getSource().getServer()).ids;
		c.getSource().sendSuccess(() -> Component.literal(ids.isEmpty() ? "No hay ítems baneados."
				: "Ítems baneados: " + String.join(", ", ids)).withStyle(ChatFormatting.GOLD), false);
		return ids.size();
	}
}
