package com.dedsafio4.dimension;

import com.dedsafio4.bloques.ModBloques;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Árbol de Grasa de la Dimensión de los Órganos: un tronco de Carne con Venas que sube un poco torcido y abre 3 a 5
 * ramas; arriba del tronco y en la punta de cada rama hay una flor de grasa: el centro de Grasa Líquida (amarilla) y
 * los pétalos de Grasa Coagulada (clarita), la de arriba más grande.
 */
public class ArbolGrasaFeature extends Feature<NoneFeatureConfiguration> {
	public ArbolGrasaFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contexto) {
		WorldGenLevel level = contexto.level();
		RandomSource azar = contexto.random();
		BlockPos base = contexto.origin();
		BlockState suelo = level.getBlockState(base.below());
		if (!suelo.is(ModBloques.CARNE_ROSA) && !suelo.is(ModBloques.CARNE_ROJA) && !suelo.is(ModBloques.CARNE_CON_VENAS)) {
			return false;
		}
		if (!level.getBlockState(base).isAir() || base.getY() + 20 > level.getMaxBuildHeight()) return false;

		BlockState tronco = ModBloques.CARNE_CON_VENAS.defaultBlockState();
		int alto = 6 + azar.nextInt(4);
		double x = 0, z = 0, angulo = azar.nextDouble() * Math.PI * 2;
		for (int y = 0; y < alto; y++) {
			int antesX = (int) Math.round(x), antesZ = (int) Math.round(z);
			if (y > 2 && azar.nextFloat() < 0.3f) {
				x += Math.cos(angulo) * 0.6;
				z += Math.sin(angulo) * 0.6;
			}
			// Si se corrió, también abajo del lugar nuevo, así el tronco queda pegado (no en escalones sueltos).
			if ((int) Math.round(x) != antesX || (int) Math.round(z) != antesZ) {
				poner(level, base.offset((int) Math.round(x), y - 1, (int) Math.round(z)), tronco);
			}
			poner(level, base.offset((int) Math.round(x), y, (int) Math.round(z)), tronco);
			// La base, más ancha.
			if (y == 0) for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				if (azar.nextFloat() < 0.7f) poner(level, base.offset(d[0], 0, d[1]), tronco);
			}
		}
		BlockPos punta = base.offset((int) Math.round(x), alto, (int) Math.round(z));
		flor(level, punta, 2, azar);

		// Las ramas: salen de la mitad para arriba, en distintas direcciones, y terminan en una flor chica.
		int ramas = 3 + azar.nextInt(3);
		double giro = azar.nextDouble() * Math.PI * 2;
		for (int i = 0; i < ramas; i++) {
			double a = giro + i * (Math.PI * 2 / ramas) + (azar.nextDouble() - 0.5) * 0.6;
			int desde = alto / 2 + azar.nextInt(Math.max(1, alto / 2 - 1));
			int largo = 2 + azar.nextInt(2);
			double rx = x, rz = z;
			int ry = desde;
			for (int j = 1; j <= largo; j++) {
				rx += Math.cos(a);
				rz += Math.sin(a);
				// Primero al costado (pegado a lo anterior) y, si sube, el de arriba.
				poner(level, base.offset((int) Math.round(rx), ry, (int) Math.round(rz)), tronco);
				if (j % 2 == 0 || j == largo) {
					ry++;
					poner(level, base.offset((int) Math.round(rx), ry, (int) Math.round(rz)), tronco);
				}
			}
			flor(level, base.offset((int) Math.round(rx), ry + 1, (int) Math.round(rz)), 1, azar);
		}
		return true;
	}

	/** Una flor de grasa con el centro en {@code centro}: tamaño 1 (chica) o 2 (la de arriba del tronco). */
	private static void flor(WorldGenLevel level, BlockPos centro, int tamano, RandomSource azar) {
		BlockState liquida = ModBloques.GRASA_LIQUIDA.defaultBlockState();
		BlockState coagulada = ModBloques.GRASA_COAGULADA.defaultBlockState();
		if (tamano == 1) {
			poner(level, centro, liquida);
			for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) poner(level, centro.offset(d[0], 0, d[1]), coagulada);
			if (azar.nextBoolean()) poner(level, centro.above(), liquida);
			return;
		}
		// Abajo, el cáliz: un aro chico de coagulada.
		for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) poner(level, centro.offset(d[0], 0, d[1]), coagulada);
		poner(level, centro, liquida);
		// La flor abierta: el centro de líquida (3 x 3) y los pétalos de coagulada alrededor (sin las esquinas).
		BlockPos arriba = centro.above();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				int d = Math.abs(dx) + Math.abs(dz), m = Math.max(Math.abs(dx), Math.abs(dz));
				if (m <= 1) poner(level, arriba.offset(dx, 0, dz), liquida);
				else if (d <= 4 && m <= 3 && !(m == 3 && d > 3) && azar.nextFloat() > 0.1f) poner(level, arriba.offset(dx, 0, dz), coagulada);
			}
		}
		// Las puntas de los pétalos se levantan un poco, y el centro sobresale.
		for (int[] d : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
			if (azar.nextFloat() < 0.6f) poner(level, arriba.offset(d[0], 1, d[1]), coagulada);
		}
		poner(level, arriba.above(), liquida);
	}

	private static void poner(WorldGenLevel level, BlockPos pos, BlockState bloque) {
		if (level.getBlockState(pos).canBeReplaced()) level.setBlock(pos, bloque, 2);
	}
}
