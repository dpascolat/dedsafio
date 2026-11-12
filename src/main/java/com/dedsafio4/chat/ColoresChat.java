package com.dedsafio4.chat;

import com.dedsafio4.Dedsafio4;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El color del nombre en el chat: verde azulado para todos y rojo para los admins.
 * /colorsito admin <jugador>: su nombre sale en rojo.   /colorsito normal <jugador>: vuelve al verde azulado.
 * /rolplayer_1 <jugador>: su nombre y lo que escribe salen en verde claro (/colorsito normal lo saca).
 * Queda guardado en el mundo y se les avisa a todos (el formato del chat también lo arma el juego de cada uno).
 */
public final class ColoresChat {
	private ColoresChat() {}

	public static final int VERDE_AZULADO = 0x55FFA0, ROJO = 0xFF5555, VERDE_CLARO = 0x8CFF8C;

	static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		/** Los admins: uuid → nombre. */
		final Map<UUID, String> admins = new HashMap<>();
		/** Los de /rolplayer_1: uuid → nombre. */
		final Map<UUID, String> rolplayers1 = new HashMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			leerMapa(tag.getCompound("admins"), d.admins);
			leerMapa(tag.getCompound("rolplayer1"), d.rolplayers1);
			return d;
		}

		private static void leerMapa(CompoundTag a, Map<UUID, String> mapa) {
			for (String k : a.getAllKeys()) {
				try {
					mapa.put(UUID.fromString(k), a.getString(k));
				} catch (IllegalArgumentException ignorado) {
				}
			}
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			CompoundTag a = new CompoundTag();
			admins.forEach((u, n) -> a.putString(u.toString(), n));
			tag.put("admins", a);
			CompoundTag r = new CompoundTag();
			rolplayers1.forEach((u, n) -> r.putString(u.toString(), n));
			tag.put("rolplayer1", r);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_colores_chat");
	}

	/** Servidor → todos: los nombres que van en rojo y los que van en verde claro (/rolplayer_1). */
	public record Payload(List<String> rojos, List<String> verdes) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "colores_chat"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::rojos,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::verdes, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** En el cliente: los nombres que van en rojo (lo manda el servidor). */
	public static final Set<String> ROJOS_CLIENTE = ConcurrentHashMap.newKeySet();
	/** En el cliente: los nombres de /rolplayer_1 (lo manda el servidor). */
	public static final Set<String> VERDES_CLIENTE = ConcurrentHashMap.newKeySet();

	/** El color del nombre en el juego de cada uno. */
	public static int colorCliente(String nombre) {
		String n = nombre.toLowerCase(java.util.Locale.ROOT);
		return ROJOS_CLIENTE.contains(n) ? ROJO : VERDES_CLIENTE.contains(n) ? VERDE_CLARO : VERDE_AZULADO;
	}

	/** El mensaje de este jugador, con el color que le toca (verde claro si es de /rolplayer_1; si no, igual). */
	public static Component mensajeCliente(String nombre, Component mensaje) {
		if (!VERDES_CLIENTE.contains(nombre.toLowerCase(java.util.Locale.ROOT))) return mensaje;
		return mensaje.copy().withStyle(s -> s.withColor(VERDE_CLARO));
	}

	/** El color del nombre en el servidor. */
	public static int colorServidor(ServerPlayer jugador) {
		Datos d = datos(jugador.server);
		return d.admins.containsKey(jugador.getUUID()) ? ROJO : d.rolplayers1.containsKey(jugador.getUUID()) ? VERDE_CLARO : VERDE_AZULADO;
	}

	private static Payload payload(MinecraftServer server) {
		List<String> rojos = new ArrayList<>(), verdes = new ArrayList<>();
		for (String n : datos(server).admins.values()) rojos.add(n.toLowerCase(java.util.Locale.ROOT));
		for (String n : datos(server).rolplayers1.values()) verdes.add(n.toLowerCase(java.util.Locale.ROOT));
		return new Payload(rojos, verdes);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// Si cambió de nombre, se actualiza.
			Datos d = datos(server);
			for (Map<UUID, String> mapa : List.of(d.admins, d.rolplayers1)) {
				if (mapa.containsKey(handler.player.getUUID())) {
					mapa.put(handler.player.getUUID(), handler.player.getGameProfile().getName());
					d.setDirty();
				}
			}
			ServerPlayNetworking.send(handler.player, payload(server));
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("colorsito").requires(s -> s.hasPermission(2))
				.then(Commands.literal("admin").then(Commands.argument("jugador", GameProfileArgument.gameProfile())
						.executes(c -> cambiar(c, true))))
				.then(Commands.literal("normal").then(Commands.argument("jugador", GameProfileArgument.gameProfile())
						.executes(c -> cambiar(c, NORMAL)))));
		dispatcher.register(Commands.literal("rolplayer_1").requires(s -> s.hasPermission(2))
				.then(Commands.argument("jugador", GameProfileArgument.gameProfile()).executes(c -> cambiar(c, ROLPLAYER_1))));
	}

	private static final int NORMAL = 0, ADMIN = 1, ROLPLAYER_1 = 2;

	private static int cambiar(CommandContext<CommandSourceStack> c, boolean admin) throws CommandSyntaxException {
		return cambiar(c, admin ? ADMIN : NORMAL);
	}

	private static int cambiar(CommandContext<CommandSourceStack> c, int que) throws CommandSyntaxException {
		MinecraftServer server = c.getSource().getServer();
		Datos d = datos(server);
		int cuantos = 0;
		for (GameProfile perfil : GameProfileArgument.getGameProfiles(c, "jugador")) {
			if (perfil.getId() == null) continue;
			// Un jugador tiene un solo color: se lo saca de los otros.
			d.admins.remove(perfil.getId());
			d.rolplayers1.remove(perfil.getId());
			if (que == ADMIN) d.admins.put(perfil.getId(), perfil.getName());
			if (que == ROLPLAYER_1) d.rolplayers1.put(perfil.getId(), perfil.getName());
			cuantos++;
			String nombre = perfil.getName();
			String texto = switch (que) {
				case ADMIN -> "El nombre de " + nombre + " ahora sale en rojo en el chat.";
				case ROLPLAYER_1 -> nombre + " ahora es rolplayer 1: su nombre y lo que escribe salen en verde claro en el chat.";
				default -> "El nombre de " + nombre + " vuelve a salir en verde azulado en el chat.";
			};
			c.getSource().sendSuccess(() -> Component.literal(texto).withStyle(ChatFormatting.AQUA), true);
		}
		d.setDirty();
		Payload p = payload(server);
		for (ServerPlayer j : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(j, p);
		return cuantos;
	}
}
