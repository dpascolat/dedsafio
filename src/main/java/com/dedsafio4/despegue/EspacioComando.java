package com.dedsafio4.despegue;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/**
 * El Espacio a mano (admins):
 *   /espacio          te lleva al Espacio (arriba de una plataforma invisible de Barrera, para no caer al vacío).
 *                     Quedas como "visitante": no te devuelve al Overworld como a los que llegan sin la nave.
 *   /espacio cargar   obliga a cargar los chunks de alrededor y te los vuelve a mandar (si no se veía).
 *   /espacio salir    vuelves al Overworld.
 * A los 10 segundos de llegar, el cliente revisa si el Espacio cargó; si no, arma un reporte del error
 * (EspacioCliente) para mandárselo a Claude.
 */
public final class EspacioComando {
	private EspacioComando() {}

	/** Etiqueta de los que están de visita en el Espacio (con /espacio). */
	public static final String VISITA = "dedsafio4_espacio_visita";
	/** A qué altura se llega. */
	private static final int ALTURA = 120;
	/** Cuántos chunks alrededor carga /espacio cargar. */
	private static final int RADIO_CARGA = 2;

	/** Servidor → jugador: revisa en tantos segundos si el Espacio cargó en tu pantalla. */
	public record RevisarPayload(int segundos) implements CustomPacketPayload {
		public static final Type<RevisarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "espacio_revisar"));
		public static final StreamCodec<ByteBuf, RevisarPayload> CODEC = ByteBufCodecs.VAR_INT.map(RevisarPayload::new, RevisarPayload::segundos);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Los que /espacio cargar se llevó lejos un momento: adónde vuelven y en qué tick. */
	private record Volver(net.minecraft.world.phys.Vec3 lugar, int tick) {}
	private static final java.util.Map<java.util.UUID, Volver> VOLVER = new java.util.HashMap<>();

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(RevisarPayload.TYPE, RevisarPayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (VOLVER.isEmpty()) return;
			VOLVER.entrySet().removeIf(e -> {
				if (server.getTickCount() < e.getValue().tick()) return false;
				ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
				if (p != null && p.level().dimension().equals(ModDespegue.ESPACIO)) {
					var l = e.getValue().lugar();
					p.teleportTo(p.serverLevel(), l.x, l.y, l.z, p.getYRot(), p.getXRot());
					p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
					p.resetFallDistance();
				}
				return true;
			});
		});
		// El que se fue del Espacio (por la nave, muriendo, otro comando...) deja de ser visitante.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (p.getTags().contains(VISITA) && !p.level().dimension().equals(ModDespegue.ESPACIO)) p.removeTag(VISITA);
			}
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("espacio").requires(s -> s.hasPermission(2))
				.executes(c -> ir(c.getSource().getPlayerOrException()))
				.then(Commands.literal("cargar").executes(c -> cargar(c.getSource().getPlayerOrException())))
				.then(Commands.literal("salir").executes(c -> salir(c.getSource().getPlayerOrException()))));
	}

	private static int ir(ServerPlayer p) {
		ServerLevel espacio = p.server.getLevel(ModDespegue.ESPACIO);
		if (espacio == null) {
			p.sendSystemMessage(Component.literal("No se encontró la dimensión del Espacio.").withStyle(ChatFormatting.RED));
			return 0;
		}
		BlockPos llegada = new BlockPos(p.getBlockX(), ALTURA, p.getBlockZ());
		// La plataforma invisible (3x3 de Barrera) para no caer.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos piso = llegada.offset(dx, -1, dz);
				if (espacio.getBlockState(piso).isAir()) espacio.setBlock(piso, Blocks.BARRIER.defaultBlockState(), 3);
			}
		}
		p.addTag(VISITA);
		p.teleportTo(espacio, llegada.getX() + 0.5, llegada.getY(), llegada.getZ() + 0.5, p.getYRot(), p.getXRot());
		p.resetFallDistance();
		ServerPlayNetworking.send(p, new RevisarPayload(10));
		p.sendSystemMessage(Component.literal("Llegaste al Espacio. Si no se ve: /espacio cargar. Para volver: /espacio salir.")
				.withStyle(ChatFormatting.AQUA));
		return 1;
	}

	private static int cargar(ServerPlayer p) {
		if (!p.level().dimension().equals(ModDespegue.ESPACIO)) {
			p.sendSystemMessage(Component.literal("No estás en el Espacio (usa /espacio).").withStyle(ChatFormatting.RED));
			return 0;
		}
		ServerLevel espacio = p.serverLevel();
		ChunkPos centro = p.chunkPosition();
		int cargados = 0;
		for (int dx = -RADIO_CARGA; dx <= RADIO_CARGA; dx++) {
			for (int dz = -RADIO_CARGA; dz <= RADIO_CARGA; dz++) {
				espacio.setChunkForced(centro.x + dx, centro.z + dz, true);
				espacio.getChunk(centro.x + dx, centro.z + dz);   // que se carguen ya
				cargados++;
			}
		}
		// Se los vuelve a mandar: se lo lleva un momento lejos (el cliente suelta esos chunks) y se lo trae de vuelta.
		VOLVER.put(p.getUUID(), new Volver(p.position(), p.server.getTickCount() + 3));
		p.teleportTo(espacio, p.getX() + 2048, p.getY(), p.getZ(), p.getYRot(), p.getXRot());
		p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		ServerPlayNetworking.send(p, new RevisarPayload(10));
		int total = cargados;
		p.sendSystemMessage(Component.literal("Cargados " + total + " chunks alrededor tuyo en el Espacio.").withStyle(ChatFormatting.AQUA));
		return 1;
	}

	private static int salir(ServerPlayer p) {
		ServerLevel overworld = p.server.overworld();
		if (p.level().dimension().equals(ModDespegue.ESPACIO)) {
			// Se sueltan los chunks que se habían obligado a cargar.
			ServerLevel espacio = p.serverLevel();
			ChunkPos centro = p.chunkPosition();
			for (int dx = -RADIO_CARGA; dx <= RADIO_CARGA; dx++) {
				for (int dz = -RADIO_CARGA; dz <= RADIO_CARGA; dz++) espacio.setChunkForced(centro.x + dx, centro.z + dz, false);
			}
		}
		p.removeTag(VISITA);
		BlockPos lugar = com.dedsafio4.dimension.Portales.lugarSeguro(overworld, p.blockPosition());
		p.teleportTo(overworld, lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, p.getYRot(), p.getXRot());
		return 1;
	}
}
