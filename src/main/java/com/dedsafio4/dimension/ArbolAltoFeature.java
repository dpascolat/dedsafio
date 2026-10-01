package com.dedsafio4.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Árbol altísimo: tronco de 2x2 (con la base ensanchada) de unos 70 bloques, ramas con hojas
 * repartidas por arriba y una copa grande. Los árboles de Minecraft no pasan de 32 bloques,
 * por eso este se genera aparte.
 */
public class ArbolAltoFeature extends Feature<ArbolAltoFeature.Config> {

	public record Config(BlockState tronco, BlockState hojas, int alturaMin, int alturaMax,
						 boolean frutas, java.util.Optional<BlockState> fruta,
						 java.util.Optional<BlockState> frutaExtra) implements FeatureConfiguration {
		public static final Codec<Config> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
				BlockState.CODEC.fieldOf("tronco").forGetter(Config::tronco),
				BlockState.CODEC.fieldOf("hojas").forGetter(Config::hojas),
				Codec.INT.fieldOf("altura_min").forGetter(Config::alturaMin),
				Codec.INT.fieldOf("altura_max").forGetter(Config::alturaMax),
				Codec.BOOL.optionalFieldOf("frutas", false).forGetter(Config::frutas),
				BlockState.CODEC.optionalFieldOf("fruta").forGetter(Config::fruta),
				BlockState.CODEC.optionalFieldOf("fruta_extra").forGetter(Config::frutaExtra)
		).apply(instancia, Config::new));
	}

	public ArbolAltoFeature() {
		super(Config.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> contexto) {
		WorldGenLevel level = contexto.level();
		RandomSource azar = contexto.random();
		Config config = contexto.config();
		BlockPos base = contexto.origin();

		// Las cuatro columnas del tronco necesitan piso.
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				BlockState suelo = level.getBlockState(base.offset(dx, -1, dz));
				if (!suelo.is(Blocks.GRASS_BLOCK) && !suelo.is(Blocks.DIRT) && !suelo.is(Blocks.PODZOL)
						&& !suelo.is(Blocks.COARSE_DIRT) && !suelo.is(Blocks.MOSS_BLOCK)) {
					return false;
				}
				if (!level.getBlockState(base.offset(dx, 0, dz)).canBeReplaced()) return false;
			}
		}

		int altura = config.alturaMin() + azar.nextInt(Math.max(1, config.alturaMax() - config.alturaMin() + 1));
		if (base.getY() + altura + 6 > level.getMaxBuildHeight()) return false;

		// Tronco de 2x2.
		for (int y = 0; y < altura; y++) {
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					BlockPos pos = base.offset(dx, y, dz);
					if (level.getBlockState(pos).canBeReplaced()) level.setBlock(pos, config.tronco(), 2);
				}
			}
		}

		// Base ensanchada: las primeras hiladas se abren para los costados.
		for (int y = 0; y < 5; y++) {
			int extra = y < 2 ? 1 : 0;
			if (extra == 0) continue;
			for (int dx = -extra; dx <= 1 + extra; dx++) {
				for (int dz = -extra; dz <= 1 + extra; dz++) {
					if (dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1) continue;
					if (azar.nextInt(4) == 0) continue;   // que no quede un cuadrado perfecto
					BlockPos pos = base.offset(dx, y, dz);
					if (level.getBlockState(pos).canBeReplaced()) level.setBlock(pos, config.tronco(), 2);
				}
			}
		}

		int copa = base.getY() + altura;
		// Ramas con hojas desde la mitad para arriba.
		for (int y = base.getY() + altura / 2; y < copa - 6; y += 6 + azar.nextInt(5)) {
			int ramas = 2 + azar.nextInt(2);
			for (int i = 0; i < ramas; i++) {
				int dx = azar.nextInt(7) - 3, dz = azar.nextInt(7) - 3;
				if (Math.abs(dx) < 2 && Math.abs(dz) < 2) dx += 2;
				BlockPos punta = new BlockPos(base.getX() + dx, y + azar.nextInt(3), base.getZ() + dz);
				// la rama de madera hasta la hoja
				for (int paso = 1; paso <= 2; paso++) {
					BlockPos rama = new BlockPos(base.getX() + dx * paso / 2, punta.getY(), base.getZ() + dz * paso / 2);
					if (level.getBlockState(rama).canBeReplaced()) level.setBlock(rama, config.tronco(), 2);
				}
				bolaHojas(level, azar, punta, 2.6f, 2, config.hojas());
			}
		}

		// Frutas pegadas al tronco, como el cacao (Huevo Eburia si no se dice otra), al alcance de la mano.
		if (config.frutas()) {
			int cuantos = 2 + azar.nextInt(3) + (config.frutaExtra().isPresent() ? 2 : 0);
			for (int i = 0; i < cuantos; i++) {
				BlockState elegida = config.frutaExtra().isPresent() && azar.nextBoolean() ? config.frutaExtra().get()
						: config.fruta().orElse(com.dedsafio4.bloques.ModBloques.HUEVO_EBURIA.defaultBlockState());
				// Las frutas de los diseños (Solaria, Uvina) van bajas, entre 1 y 4 bloques; el Huevo Eburia más repartido.
				boolean baja = elegida.getBlock() instanceof com.dedsafio4.eburia.FrutaTroncoBlock;
				int y = baja ? base.getY() + 1 + azar.nextInt(4)
						: base.getY() + 3 + azar.nextInt(Math.max(1, Math.min(altura - 6, 12)));
				net.minecraft.core.Direction lado = net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(azar);
				// el tronco ocupa 2x2: se elige la columna que da a ese lado
				int tx = base.getX() + (lado == net.minecraft.core.Direction.EAST ? 1 : 0);
				int tz = base.getZ() + (lado == net.minecraft.core.Direction.SOUTH ? 1 : 0);
				BlockPos pos = new BlockPos(tx, y, tz).relative(lado);
				if (!level.getBlockState(pos).canBeReplaced()) continue;
				BlockState fruta = elegida
						.setValue(com.dedsafio4.eburia.HuevoEburiaBlock.FACING, lado)
						.setValue(com.dedsafio4.eburia.HuevoEburiaBlock.EDAD, azar.nextInt(3));
				level.setBlock(pos, fruta, 2);
			}
		}

		// Copa grande arriba de todo.
		BlockPos centro = new BlockPos(base.getX(), copa - 2, base.getZ());
		bolaHojas(level, azar, centro, 5.2f, 5, config.hojas());
		bolaHojas(level, azar, centro.offset(1, 3, 1), 3.6f, 3, config.hojas());
		bolaHojas(level, azar, centro.offset(0, 5, 0), 2.2f, 2, config.hojas());

		return true;
	}

	/** Bola de hojas achatada, con el borde comido para que no quede una pelota perfecta. */
	private static void bolaHojas(WorldGenLevel level, RandomSource azar, BlockPos centro,
								  float radio, int alto, BlockState hojas) {
		int r = (int) Math.ceil(radio);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = -alto; dy <= alto; dy++) {
					float distancia = dx * dx + dz * dz + (dy * dy) * 2.2f;
					if (distancia > radio * radio) continue;
					if (distancia > (radio - 1) * (radio - 1) && azar.nextInt(3) == 0) continue;   // borde irregular
					BlockPos pos = centro.offset(dx, dy, dz);
					if (level.getBlockState(pos).canBeReplaced()) level.setBlock(pos, hojas, 2);
				}
			}
		}
	}
}
