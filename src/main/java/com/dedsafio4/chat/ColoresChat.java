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
 * Queda guardado en el mundo y se les avisa a todos (el formato del chat también lo arma el juego de cada uno).
 */
public final class ColoresChat {
	private ColoresChat() {}

	public static final int VERDE_AZULADO = 0x55FFA0, ROJO = 0xFF5555;

	static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		/** Los admins: uuid → nombre. */
		final Map<UUID, String> admins = new HashMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			CompoundTag a = tag.getCompound("admins");
			for (String k : a.getAllKeys()) {
				try {
					d.admins.put(UUID.fromString(k), a.getString(k));
				} catch (IllegalArgumentException ignorado) {
				}
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			CompoundTag a = new CompoundTag();
			admins.forEach((u, n) -> a.putString(u.toString(), n));
			tag.put("admins", a);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_colores_chat");
	}

	/** Servidor → todos: los nombres que van en rojo. */
	public record Payload(List<String> rojos) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "colores_chat"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::rojos, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** En el cliente: los nombres que van en rojo (lo manda el servidor). */
	public static final Set<String> ROJOS_CLIENTE = ConcurrentHashMap.newKeySet();

	/** El color del nombre en el juego de cada uno. */
	public static int colorCliente(String nombre) {
		return ROJOS_CLIENTE.contains(nombre.toLowerCase(java.util.Locale.ROOT)) ? ROJO : VERDE_AZULADO;
	}

	/** El color del nombre en el servidor. */
	public static int colorServidor(ServerPlayer jugador) {
		return datos(jugador.server).admins.containsKey(jugador.getUUID()) ? ROJO : VERDE_AZULADO;
	}

	private static Payload payload(MinecraftServer server) {
		List<String> nombres = new ArrayList<>();
		for (String n : datos(server).admins.values()) nombres.add(n.toLowerCase(java.util.Locale.ROOT));
		return new Payload(nombres);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// Si cambió de nombre, se actualiza.
			Datos d = datos(server);
			if (d.admins.containsKey(handler.player.getUUID())) {
				d.admins.put(handler.player.getUUID(), handler.player.getGameProfile().getName());
				d.setDirty();
			}
			ServerPlayNetworking.send(handler.player, payload(server));
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("colorsito").requires(s -> s.hasPermission(2))
				.then(Commands.literal("admin").then(Commands.argument("jugador", GameProfileArgument.gameProfile())
						.executes(c -> cambiar(c, true))))
				.then(Commands.literal("normal").then(Commands.argument("jugador", GameProfileArgument.gameProfile())
						.executes(c -> cambiar(c, false)))));
	}

	private static int cambiar(CommandContext<CommandSourceStack> c, boolean admin) throws CommandSyntaxException {
		MinecraftServer server = c.getSource().getServer();
		Datos d = datos(server);
		int cuantos = 0;
		for (GameProfile perfil : GameProfileArgument.getGameProfiles(c, "jugador")) {
			if (perfil.getId() == null) continue;
			if (admin) d.admins.put(perfil.getId(), perfil.getName());
			else d.admins.remove(perfil.getId());
			cuantos++;
			String nombre = perfil.getName();
			c.getSource().sendSuccess(() -> Component.literal(admin ? "El nombre de " + nombre + " ahora sale en rojo en el chat."
					: "El nombre de " + nombre + " vuelve a salir en verde azulado en el chat.").withStyle(ChatFormatting.AQUA), true);
		}
		d.setDirty();
		Payload p = payload(server);
		for (ServerPlayer j : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(j, p);
		return cuantos;
	}
}
