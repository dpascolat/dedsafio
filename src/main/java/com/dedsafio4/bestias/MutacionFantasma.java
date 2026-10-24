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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /mutacion pantome: todos los fantasmas (phantoms; los que ya están y los que aparezcan) se ven como el Fantasma
 * Mutante (el modelo y las texturas los dibuja el cliente: PhantomMutadoRenderer) y cuando le pegan a un jugador le
 * mezclan el inventario (todo menos la armadura y la segunda mano, donde va el tótem). /mutacion pantome desactivar
 * los vuelve normales. Queda guardado en el mundo y se avisa a todos los jugadores.
 */
public final class MutacionFantasma {
	private MutacionFantasma() {}

	/** En el cliente: si la mutación está activa (lo manda el servidor). */
	public static volatile boolean ACTIVA_CLIENTE;

	/** Las casillas que se mezclan: la barra y el inventario (de la 36 en adelante van la armadura y la segunda mano). */
	private static final int CASILLAS = 36;

	/** Servidor → cliente: si los fantasmas están mutados. */
	public record Payload(boolean activa) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "mutacion_fantasma"));
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
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_mutacion_fantasma");
	}

	public static boolean activa(MinecraftServer server) {
		return datos(server).activa;
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				ServerPlayNetworking.send(handler.player, new Payload(datos(server).activa)));
	}

	/** Se suma a /mutacion (el de las arañas): /mutacion pantome [activar|desactivar]. */
	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mutacion").requires(s -> s.hasPermission(2))
				.then(Commands.literal("pantome")
						.executes(c -> cambiar(c.getSource(), true))
						.then(Commands.literal("activar").executes(c -> cambiar(c.getSource(), true)))
						.then(Commands.literal("desactivar").executes(c -> cambiar(c.getSource(), false)))));
	}

	private static int cambiar(CommandSourceStack fuente, boolean activa) {
		MinecraftServer server = fuente.getServer();
		Datos d = datos(server);
		d.activa = activa;
		d.setDirty();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(p, new Payload(activa));
		fuente.sendSuccess(() -> Component.literal(activa ? "Los fantasmas mutaron: si te pegan, te mezclan el inventario."
				: "Los fantasmas volvieron a la normalidad.").withStyle(activa ? ChatFormatting.DARK_PURPLE : ChatFormatting.GOLD), true);
		return 1;
	}

	/** Un fantasma mutado le pegó: le mezcla el inventario (no la armadura ni la segunda mano). */
	public static void alPegar(ServerPlayer jugador) {
		if (!activa(jugador.server)) return;
		var inv = jugador.getInventory();
		List<ItemStack> cosas = new ArrayList<>(CASILLAS);
		for (int i = 0; i < CASILLAS; i++) cosas.add(inv.getItem(i));
		Collections.shuffle(cosas, new java.util.Random(jugador.getRandom().nextLong()));
		for (int i = 0; i < CASILLAS; i++) inv.setItem(i, cosas.get(i));
		inv.setChanged();
		jugador.inventoryMenu.broadcastChanges();
	}
}
