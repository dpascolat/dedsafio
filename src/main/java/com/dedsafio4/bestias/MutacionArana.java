package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * /mutacion: todas las arañas de cueva (las que ya están y las que aparezcan) se ven como la Araña Mutante (el
 * modelo y las texturas los dibuja el cliente: AranaCuevaRenderer). /mutacion desactivar las vuelve normales. Queda
 * guardado en el mundo y se avisa a todos los jugadores.
 */
public final class MutacionArana {
	private MutacionArana() {}

	/** En el cliente: si la mutación está activa (lo manda el servidor). */
	public static volatile boolean ACTIVA_CLIENTE;

	/** Servidor → cliente: si las arañas de cueva están mutadas. */
	public record Payload(boolean activa) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "mutacion_arana"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = ByteBufCodecs.BOOL.map(Payload::new, Payload::activa);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean activa;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.activa = tag.getBoolean("activa");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("activa", activa);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_mutacion_arana");
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				ServerPlayNetworking.send(handler.player, new Payload(datos(server).activa)));
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mutacion").requires(s -> s.hasPermission(2))
				.executes(c -> cambiar(c.getSource(), true))
				.then(Commands.literal("activar").executes(c -> cambiar(c.getSource(), true)))
				.then(Commands.literal("desactivar").executes(c -> cambiar(c.getSource(), false))));
	}

	private static int cambiar(CommandSourceStack fuente, boolean activa) {
		MinecraftServer server = fuente.getServer();
		Datos d = datos(server);
		d.activa = activa;
		d.setDirty();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(p, new Payload(activa));
		fuente.sendSuccess(() -> Component.literal(activa ? "Las arañas de cueva mutaron."
				: "Las arañas de cueva volvieron a la normalidad.").withStyle(activa ? ChatFormatting.DARK_PURPLE : ChatFormatting.GOLD), true);
		return 1;
	}
}
