package com.dedsafio4.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CaveWorldCarver;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Las minas de Gleba: una sola por isla flotante. Cada isla es la zona donde el ruido "continentalness" (el mismo que
 * arma las islas en noise_settings/organos.json) pasa del borde; la mina sale en el chunk donde ese ruido es más alto,
 * o sea en la parte más gruesa de la isla. Ahí se cavan varios túneles juntos, como las cuevas normales.
 */
public class MinaGlebaCarver extends CaveWorldCarver {
	/** Desde dónde es isla (a la altura del medio de las islas) y cuántas veces se cavan túneles en la mina. */
	private static final double BORDE = 0.1;
	private static final int TANDAS = 6;
	/** Para no recorrer de más si muchas islas quedan pegadas: como mucho tantos chunks por isla. */
	private static final int MAXIMO = 20000;

	/** Por cada mundo (su RandomState): chunk → chunk de la mina de su isla (o Long.MIN_VALUE si no es isla). */
	private static final Map<RandomState, Map<Long, Long>> MINAS = new WeakHashMap<>();

	public MinaGlebaCarver() {
		super(CaveCarverConfiguration.CODEC);
	}

	@Override
	public boolean carve(CarvingContext contexto, CaveCarverConfiguration config, ChunkAccess chunk,
						 Function<BlockPos, Holder<Biome>> biomas, RandomSource azar, Aquifer acuifero, ChunkPos desde, CarvingMask mascara) {
		if (minaDe(contexto.randomState(), desde.x, desde.z) != desde.toLong()) return false;
		boolean algo = false;
		for (int i = 0; i < TANDAS; i++) algo |= super.carve(contexto, config, chunk, biomas, azar, acuifero, desde, mascara);
		return algo;
	}

	private static double valor(NormalNoise ruido, int cx, int cz) {
		return ruido.getValue(cx * 16 + 8, 0, cz * 16 + 8);
	}

	/** El chunk donde va la mina de la isla de este chunk (Long.MIN_VALUE si acá no hay isla). */
	public static long minaDe(RandomState estado, int cx, int cz) {
		Map<Long, Long> minas;
		synchronized (MINAS) {
			minas = MINAS.computeIfAbsent(estado, e -> new ConcurrentHashMap<>());
		}
		long clave = ChunkPos.asLong(cx, cz);
		Long ya = minas.get(clave);
		if (ya != null) return ya;
		NormalNoise ruido = estado.getOrCreateNoise(Noises.CONTINENTALNESS);
		if (valor(ruido, cx, cz) <= BORDE) {
			minas.put(clave, Long.MIN_VALUE);
			return Long.MIN_VALUE;
		}
		// Recorre toda la isla (los chunks pegados que también son isla) y se queda con el más alto.
		List<Long> isla = new ArrayList<>();
		ArrayDeque<Long> pendientes = new ArrayDeque<>();
		java.util.Set<Long> vistos = new java.util.HashSet<>();
		pendientes.add(clave);
		vistos.add(clave);
		long mejor = clave;
		double mejorValor = -9;
		while (!pendientes.isEmpty() && isla.size() < MAXIMO) {
			long c = pendientes.poll();
			int x = ChunkPos.getX(c), z = ChunkPos.getZ(c);
			double v = valor(ruido, x, z);
			if (v <= BORDE) continue;
			isla.add(c);
			if (v > mejorValor) {
				mejorValor = v;
				mejor = c;
			}
			for (long vecino : new long[]{ChunkPos.asLong(x + 1, z), ChunkPos.asLong(x - 1, z), ChunkPos.asLong(x, z + 1), ChunkPos.asLong(x, z - 1)}) {
				if (vistos.add(vecino)) pendientes.add(vecino);
			}
		}
		for (long c : isla) minas.put(c, mejor);
		return mejor;
	}
}
