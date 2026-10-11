package com.dedsafio4.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * El bioma Phora: con agua verde y donde aparecen la Nave y la Nave Veloz (worldgen/biome/phora.json). No sale solo:
 * se pone con "/phora <desde> <hasta>" en todo ese espacio (de punta a punta, de arriba a abajo del mundo).
 * Ahí no se pueden romper los bloques que ya estaban (en creativo sí); poner bloques y agua, sí, y lo que pone un
 * jugador después se puede volver a romper (se anotan esos lugares, ver BlockItemPhoraMixin).
 */
public final class Phora {
	private Phora() {}

	public static final ResourceKey<Biome> BIOMA = ResourceKey.create(Registries.BIOME,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "phora"));

	public static boolean enPhora(Level mundo, BlockPos pos) {
		return mundo.getBiome(pos).is(BIOMA);
	}

	/** Los lugares de Phora donde un jugador puso un bloque (por mundo): esos sí se pueden romper. */
	private static final class Puestos extends net.minecraft.world.level.saveddata.SavedData {
		static final Factory<Puestos> FACTORY = new Factory<>(Puestos::new, Puestos::leer, null);
		final java.util.Set<Long> lugares = new java.util.HashSet<>();

		private static Puestos leer(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			Puestos p = new Puestos();
			for (long l : tag.getLongArray("lugares")) p.lugares.add(l);
			return p;
		}

		@Override
		public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			tag.putLongArray("lugares", lugares.stream().mapToLong(Long::longValue).toArray());
			return tag;
		}
	}

	private static Puestos puestos(net.minecraft.server.level.ServerLevel mundo) {
		return mundo.getDataStorage().computeIfAbsent(Puestos.FACTORY, "dedsafio4_phora_puestos");
	}

	/** Un jugador puso un bloque ahí (lo llama BlockItemPhoraMixin): si es Phora, se anota. */
	public static void alPoner(Level mundo, BlockPos pos, net.minecraft.world.level.block.state.BlockState estado) {
		if (!(mundo instanceof net.minecraft.server.level.ServerLevel servidor) || !enPhora(mundo, pos)) return;
		Puestos p = puestos(servidor);
		p.lugares.add(pos.asLong());
		// Puertas, camas y plantas altas ocupan dos lugares.
		if (estado.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)) p.lugares.add(pos.above().asLong());
		if (estado.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
			p.lugares.add(pos.relative(estado.getValue(net.minecraft.world.level.block.BedBlock.FACING)).asLong());
		}
		p.setDirty();
	}

	private static boolean puesto(Level mundo, BlockPos pos) {
		return mundo instanceof net.minecraft.server.level.ServerLevel servidor && puestos(servidor).lugares.contains(pos.asLong());
	}

	public static void registrarComandos(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
		dispatcher.register(net.minecraft.commands.Commands.literal("phora").requires(s -> s.hasPermission(2))
				.then(net.minecraft.commands.Commands.argument("desde", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
						.then(net.minecraft.commands.Commands.argument("hasta", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
								.executes(c -> {
									BlockPos desde = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "desde");
									BlockPos hasta = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "hasta");
									int chunks = poner(c.getSource().getLevel(), desde, hasta);
									c.getSource().sendSuccess(() -> Component.literal("Bioma Phora puesto de " + desde.getX() + " " + desde.getZ()
											+ " a " + hasta.getX() + " " + hasta.getZ() + " (" + chunks + " chunks).").withColor(0x5FD96A), true);
									return chunks;
								}))));
	}

	/** Pone el bioma Phora en ese rectángulo (toda la altura del mundo) y se lo manda a los jugadores. */
	public static int poner(net.minecraft.server.level.ServerLevel mundo, BlockPos a, BlockPos b) {
		int x1 = Math.min(a.getX(), b.getX()), x2 = Math.max(a.getX(), b.getX());
		int z1 = Math.min(a.getZ(), b.getZ()), z2 = Math.max(a.getZ(), b.getZ());
		net.minecraft.core.Holder<Biome> phora = mundo.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(BIOMA);
		// Los biomas se guardan de a 4×4×4 bloques ("quarts").
		int qx1 = x1 >> 2, qx2 = x2 >> 2, qz1 = z1 >> 2, qz2 = z2 >> 2;
		java.util.List<net.minecraft.world.level.chunk.ChunkAccess> cambiados = new java.util.ArrayList<>();
		for (int cx = x1 >> 4; cx <= x2 >> 4; cx++) {
			for (int cz = z1 >> 4; cz <= z2 >> 4; cz++) {
				net.minecraft.world.level.chunk.ChunkAccess chunk = mundo.getChunk(cx, cz);
				chunk.fillBiomesFromNoise((qx, qy, qz, muestreo) -> qx >= qx1 && qx <= qx2 && qz >= qz1 && qz <= qz2
						? phora : chunk.getNoiseBiome(qx, qy, qz), mundo.getChunkSource().randomState().sampler());
				chunk.setUnsaved(true);
				cambiados.add(chunk);
			}
		}
		mundo.getChunkSource().chunkMap.resendBiomesForChunks(cambiados);
		return cambiados.size();
	}

	public static void registrar() {
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((mundo, jugador, pos, estado, entidad) -> {
			if (jugador.isCreative() || !enPhora(mundo, pos) || puesto(mundo, pos)) return true;
			jugador.displayClientMessage(Component.literal("En Phora solo puedes romper los bloques que pusiste.").withColor(0x5FD96A), true);
			return false;
		});
		// Lo que se rompió deja de estar anotado.
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((mundo, jugador, pos, estado, entidad) -> {
			if (mundo instanceof net.minecraft.server.level.ServerLevel servidor && puestos(servidor).lugares.remove(pos.asLong())) {
				puestos(servidor).setDirty();
			}
		});
	}
}
