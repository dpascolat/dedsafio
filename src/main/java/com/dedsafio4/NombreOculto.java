package com.dedsafio4;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * /nombre esconder → arriba de tu cabeza ya no se ve nada (ni la vida, ni el nombre, ni la Hermandad).
 * /nombre mostrar  → vuelve a verse.
 * Cada uno lo hace para sí mismo; los admins también pueden ponerle jugadores: /nombre esconder <jugadores>.
 * Queda guardado en el mundo.
 */
public final class NombreOculto {
	private NombreOculto() {}

	/** En el cliente: los jugadores que tienen el nombre escondido. */
	public static volatile Set<UUID> OCULTOS_CLIENTE = Set.of();

	public static boolean ocultoEnCliente(UUID jugador) {
		return OCULTOS_CLIENTE.contains(jugador);
	}

	/** Servidor → todos: quiénes tienen el nombre escondido. */
	public record Payload(List<UUID> ocultos) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "nombre_oculto"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC =
				UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).<RegistryFriendlyByteBuf>cast().map(Payload::new, Payload::ocultos);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::cargar, null);
		final Set<UUID> ocultos = new HashSet<>();

		private static Datos cargar(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (Tag t : tag.getList("ocultos", Tag.TAG_STRING)) d.ocultos.add(UUID.fromString(t.getAsString()));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag l = new ListTag();
			for (UUID u : ocultos) l.add(StringTag.valueOf(u.toString()));
			tag.put("ocultos", l);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_nombre_oculto");
	}

	private static void enviarATodos(MinecraftServer server) {
		Payload p = new Payload(new ArrayList<>(datos(server).ocultos));
		for (ServerPlayer j : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(j, p);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				ServerPlayNetworking.send(handler.player, new Payload(new ArrayList<>(datos(server).ocultos))));
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("nombre")
				.then(Commands.literal("esconder")
						.executes(c -> cambiar(c.getSource(), List.of(c.getSource().getPlayerOrException()), true))
						.then(Commands.argument("jugadores", EntityArgument.players()).requires(s -> s.hasPermission(2))
								.executes(c -> cambiar(c.getSource(), EntityArgument.getPlayers(c, "jugadores"), true))))
				.then(Commands.literal("mostrar")
						.executes(c -> cambiar(c.getSource(), List.of(c.getSource().getPlayerOrException()), false))
						.then(Commands.argument("jugadores", EntityArgument.players()).requires(s -> s.hasPermission(2))
								.executes(c -> cambiar(c.getSource(), EntityArgument.getPlayers(c, "jugadores"), false)))));
	}

	private static int cambiar(CommandSourceStack fuente, Collection<ServerPlayer> jugadores, boolean esconder) {
		Datos d = datos(fuente.getServer());
		for (ServerPlayer j : jugadores) {
			if (esconder) d.ocultos.add(j.getUUID());
			else d.ocultos.remove(j.getUUID());
		}
		d.setDirty();
		enviarATodos(fuente.getServer());
		String quien = jugadores.size() == 1 ? jugadores.iterator().next().getGameProfile().getName() : jugadores.size() + " jugadores";
		fuente.sendSuccess(() -> Component.literal(esconder
				? "Nombre escondido (" + quien + "): ya no se ve nada arriba de la cabeza."
				: "Nombre visible otra vez (" + quien + ").").withStyle(ChatFormatting.GRAY), false);
		return jugadores.size();
	}
}
