package com.dedsafio4.dimension;

import com.dedsafio4.bloques.ModBloques;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Los Pulmones en las minas de Gleba: en una columna de la isla busca los pisos de cueva (con 2 bloques de aire arriba y
 * la superficie de la isla más arriba, o sea bajo tierra) y lo pone en uno. Como mucho 5 por chunk.
 */
public class PulmonesFeature extends Feature<NoneFeatureConfiguration> {
	public PulmonesFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contexto) {
		WorldGenLevel mundo = contexto.level();
		// Prueba varias columnas del chunk hasta encontrar una mina.
		for (int intento = 0; intento < 12; intento++) {
			BlockPos origen = new BlockPos((contexto.origin().getX() & ~15) + contexto.random().nextInt(16), contexto.origin().getY(),
					(contexto.origin().getZ() & ~15) + contexto.random().nextInt(16));
			if (enColumna(mundo, origen, contexto.random())) return true;
		}
		return false;
	}

	private static boolean enColumna(WorldGenLevel mundo, BlockPos origen, net.minecraft.util.RandomSource azar) {
		int superficie = mundo.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origen.getX(), origen.getZ());
		// Todos los pisos de cueva de esta columna (bajo tierra: con la superficie de la isla bastante más arriba).
		java.util.List<BlockPos> pisos = new java.util.ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(origen.getX(), Math.min(superficie - 4, 210), origen.getZ());
		for (; pos.getY() > 70; pos.move(Direction.DOWN)) {
			if (!mundo.isEmptyBlock(pos) || !mundo.isEmptyBlock(pos.above())) continue;
			BlockPos abajo = pos.below();
			if (mundo.getBlockState(abajo).isFaceSturdy(mundo, abajo, Direction.UP)) pisos.add(pos.immutable());
		}
		if (pisos.isEmpty()) return false;
		mundo.setBlock(pisos.get(azar.nextInt(pisos.size())), ModBloques.PULMONES.defaultBlockState(), 2);
		return true;
	}
}
