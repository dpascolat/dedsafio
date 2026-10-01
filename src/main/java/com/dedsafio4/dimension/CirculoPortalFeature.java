package com.dedsafio4.dimension;

import com.dedsafio4.bloques.ModBloques;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Disco de Agua Rara de 20 bloques de ancho. Los mundos ya creados lo tienen guardado, por eso
 * sigue registrado, pero el círculo del cielo se arma al llegar (ver {@link Portales}): puesto en
 * la generación del mundo escribía fuera del trozo que se estaba generando y trababa la carga.
 */
public class CirculoPortalFeature extends Feature<NoneFeatureConfiguration> {
	private static final int RADIO = 10;

	public CirculoPortalFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contexto) {
		WorldGenLevel level = contexto.level();
		BlockPos centro = contexto.origin();
		for (int dx = -RADIO; dx <= RADIO; dx++) {
			for (int dz = -RADIO; dz <= RADIO; dz++) {
				if (dx * dx + dz * dz > RADIO * RADIO) continue;
				BlockPos pos = centro.offset(dx, 0, dz);
				if (level.getBlockState(pos).isAir()) {
					level.setBlock(pos, ModBloques.AGUA_PORTAL.defaultBlockState(), 2);
				}
			}
		}
		return true;
	}
}
