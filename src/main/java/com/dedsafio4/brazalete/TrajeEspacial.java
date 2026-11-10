package com.dedsafio4.brazalete;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * El Traje Espacial (modelo "outer_armor_astronauta" de Patricio): aparece puesto sobre el jugador mientras tiene el
 * Modo de Vuelo del Brazalete activado (J), con la pulsera en las dos muñecas. Volando tiene su propia animación
 * (VueloTraje, en el cliente). Lo dibuja TrajeLayer.
 * El servidor le avisa a todos quién lo tiene puesto (las marcas de los jugadores no llegan a los otros clientes).
 */
public final class TrajeEspacial {
	private TrajeEspacial() {}

	/** La velocidad de vuelo normal de Minecraft (la de creativo). */
	public static final float VELOCIDAD_NORMAL = 0.05f;

	/** Cuántas veces más rápido vuelan todos los trajes (/traje velocidad <veces>), guardado en el mundo. */
	static final class Datos extends net.minecraft.world.level.saveddata.SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		float veces = 1f;

		private static Datos leer(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("veces")) d.veces = tag.getFloat("veces");
			return d;
		}

		@Override
		public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			tag.putFloat("veces", veces);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_traje");
	}

	public static float velocidadVuelo(MinecraftServer server) {
		return VELOCIDAD_NORMAL * datos(server).veces;
	}

	public static void registrarComandos(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
		dispatcher.register(net.minecraft.commands.Commands.literal("traje").requires(s -> s.hasPermission(2))
				.then(net.minecraft.commands.Commands.literal("velocidad")
						.executes(c -> {
							float v = datos(c.getSource().getServer()).veces;
							c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Los trajes vuelan a " + v
									+ " veces la velocidad normal.").withColor(0x49C8E5), false);
							return 1;
						})
						.then(net.minecraft.commands.Commands.argument("veces", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.1f, 20f))
								.executes(c -> {
									float v = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c, "veces");
									Datos d = datos(c.getSource().getServer());
									d.veces = v;
									d.setDirty();
									c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Listo: todos los trajes vuelan a "
											+ v + " veces la velocidad normal (1 = normal).").withColor(0x49C8E5), true);
									return 1;
								}))));
	}

	/** Del lado del cliente: los jugadores que tienen el traje puesto (lo que mandó el servidor). */
	public static final Set<UUID> PUESTOS_CLIENTE = new HashSet<>();

	public record Payload(UUID jugador, boolean puesto) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "traje_espacial"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Payload::jugador, ByteBufCodecs.BOOL, Payload::puesto, Payload::new);

		@Override
		public Type<Payload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		// Al entrar, se entera de quiénes lo tienen puesto.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			for (ServerPlayer otro : server.getPlayerList().getPlayers()) {
				if (otro.getTags().contains(Brazalete.MARCA)) ServerPlayNetworking.send(handler.getPlayer(), new Payload(otro.getUUID(), true));
			}
		});
	}

	/** ¿Lo tiene puesto? En el servidor mira la marca del Brazalete; en el cliente, lo que avisó el servidor. */
	public static boolean puesto(Player jugador) {
		if (jugador.level().isClientSide) return PUESTOS_CLIENTE.contains(jugador.getUUID());
		return jugador.getTags().contains(Brazalete.MARCA);
	}

	/** Lo llama el Brazalete al entrar o salir del Modo de Vuelo. */
	public static void avisar(MinecraftServer server, UUID jugador, boolean puesto) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(p, new Payload(jugador, puesto));
	}
}
