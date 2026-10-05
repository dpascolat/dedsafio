package com.dedsafio4.dimension;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bloques.ModBloques;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El techo de roca del Centro de Quiu. Se pone al final de todo (después de los árboles, por eso los árboles salen
 * igual que siempre) y sigue la forma del terreno, como el techo de una cueva gigante: más o menos 40 bloques arriba
 * del piso, con bultos y "estalactitas", y macizo hasta arriba de todo. Es Roca del Techo, que deja pasar la luz.
 */
public class TechoFeature extends Feature<NoneFeatureConfiguration> {
	/** Cuántos bloques de aire quedan (más o menos) entre el piso y el techo. */
	public static final int ALTO_CUEVA = 40;
	/** Cada cuántos bloques se mide el piso (y se toma lo más alto de alrededor, para que el techo no corte los cerros). */
	private static final int GRILLA = 16;

	public static final TechoFeature TECHO = Registry.register(BuiltInRegistries.FEATURE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "techo"), new TechoFeature());

	/** La altura del piso en cada punto de la grilla (lo que da el terreno, sin árboles). */
	private static final Map<Long, Integer> PISO = new ConcurrentHashMap<>();
	private static final Map<Long, SimplexNoise[]> RUIDOS = new ConcurrentHashMap<>();

	public static void registrar() {}

	public TechoFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	private static int piso(ChunkGenerator generador, WorldGenLevel mundo, RandomState estado, int gx, int gz) {
		long clave = ((long) gx << 32) ^ (gz & 0xFFFFFFFFL);
		Integer y = PISO.get(clave);
		if (y == null) {
			if (PISO.size() > 200_000) PISO.clear();
			y = generador.getBaseHeight(gx * GRILLA, gz * GRILLA, Heightmap.Types.OCEAN_FLOOR_WG, mundo, estado);
			PISO.put(clave, y);
		}
		return y;
	}

	/** Lo más alto del piso en la celda y sus vecinas. */
	private static int alto(ChunkGenerator generador, WorldGenLevel mundo, RandomState estado, int gx, int gz) {
		int m = Integer.MIN_VALUE;
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) m = Math.max(m, piso(generador, mundo, estado, gx + dx, gz + dz));
		return m;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
		WorldGenLevel mundo = ctx.level();
		ChunkGenerator generador = ctx.chunkGenerator();
		ServerLevel servidor = mundo.getLevel();
		RandomState estado = servidor.getChunkSource().randomState();
		SimplexNoise[] ruido = RUIDOS.computeIfAbsent(mundo.getSeed(), s -> {
			RandomSource r = RandomSource.create(s ^ 0x7EC0L);
			return new SimplexNoise[]{new SimplexNoise(r), new SimplexNoise(r)};
		});
		int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
		ChunkAccess chunk = mundo.getChunk(x0 >> 4, z0 >> 4);
		BlockState roca = ModBloques.ROCA_TECHO.defaultBlockState();
		int arriba = mundo.getMaxBuildHeight() - 1;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = x0 + dx, z = z0 + dz;
				// El piso suavizado (interpolando la grilla) más la altura de la cueva, con bultos y estalactitas.
				int gx = Math.floorDiv(x, GRILLA), gz = Math.floorDiv(z, GRILLA);
				float fx = (x - gx * GRILLA) / (float) GRILLA, fz = (z - gz * GRILLA) / (float) GRILLA;
				float base = Mth.lerp(fz,
						Mth.lerp(fx, alto(generador, mundo, estado, gx, gz), alto(generador, mundo, estado, gx + 1, gz)),
						Mth.lerp(fx, alto(generador, mundo, estado, gx, gz + 1), alto(generador, mundo, estado, gx + 1, gz + 1)));
				double bultos = ruido[0].getValue(x / 40.0, z / 40.0) * 6;
				double picos = Math.max(0, ruido[1].getValue(x / 5.0, z / 5.0) - 0.55) * 14;
				int techo = Mth.clamp((int) (base + ALTO_CUEVA + bultos - picos), mundo.getMinBuildHeight() + 20, arriba - 10);
				for (int y = techo; y <= arriba; y++) chunk.setBlockState(pos.set(x, y, z), roca, false);
			}
		}
		return true;
	}
}
