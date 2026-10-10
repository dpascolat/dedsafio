package com.dedsafio4.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Lo que hay en el Limbo, todo en piedra oscura:
 *   ARBOL    árboles muertos (tronco oscuro con ramas, sin hojas)
 *   COLUMNA  columnas rotas, a veces con pedazos caídos al lado
 *   TUMBA    tumbas (lápida de piedra negra), a veces con una vela de almas
 *   FAROL    faroles de almas en un poste (la poca luz azul que hay)
 *   ARCO     arcos en ruinas
 *   RUINA    ruinas de una casa: paredes rotas, telarañas y un farol
 */
public class LimboFeature extends Feature<NoneFeatureConfiguration> {
	public enum Tipo { ARBOL, COLUMNA, TUMBA, FAROL, ARCO, RUINA }

	private final Tipo tipo;

	public LimboFeature(Tipo tipo) {
		super(NoneFeatureConfiguration.CODEC);
		this.tipo = tipo;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contexto) {
		WorldGenLevel mundo = contexto.level();
		RandomSource r = contexto.random();
		BlockPos pos = contexto.origin();
		// Solo sobre piso firme (no en el agua ni en el aire).
		if (!mundo.getBlockState(pos.below()).isSolid() || !mundo.getFluidState(pos).isEmpty() || !mundo.isEmptyBlock(pos)) return false;
		switch (tipo) {
			case ARBOL -> arbol(mundo, r, pos);
			case COLUMNA -> columna(mundo, r, pos, 3 + r.nextInt(6), true);
			case TUMBA -> tumba(mundo, r, pos);
			case FAROL -> farol(mundo, pos, 2 + r.nextInt(2));
			case ARCO -> arco(mundo, r, pos);
			case RUINA -> ruina(mundo, r, pos);
		}
		return true;
	}

	private static void poner(WorldGenLevel mundo, BlockPos pos, BlockState estado) {
		BlockState ahi = mundo.getBlockState(pos);
		if (ahi.isAir() || ahi.canBeReplaced()) mundo.setBlock(pos, estado, 2);
	}

	/** Un ladrillo de pizarra, a veces rajado. */
	private static BlockState ladrillo(RandomSource r) {
		return (r.nextInt(3) == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS : Blocks.DEEPSLATE_BRICKS).defaultBlockState();
	}

	private static BlockState tronco(Direction.Axis eje) {
		return Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, eje);
	}

	/** Un árbol muerto: tronco oscuro de 4 a 7 y 2 o 3 ramas peladas que salen torcidas. */
	private static void arbol(WorldGenLevel mundo, RandomSource r, BlockPos pos) {
		int alto = 4 + r.nextInt(4);
		for (int y = 0; y < alto; y++) poner(mundo, pos.above(y), tronco(Direction.Axis.Y));
		int ramas = 2 + r.nextInt(2);
		for (int i = 0; i < ramas; i++) {
			Direction lado = Direction.Plane.HORIZONTAL.getRandomDirection(r);
			BlockPos rama = pos.above(alto - 1 - r.nextInt(3));
			int largo = 1 + r.nextInt(3);
			for (int k = 1; k <= largo; k++) {
				rama = rama.relative(lado);
				if (k > 1 && r.nextBoolean()) rama = rama.above();
				poner(mundo, rama, tronco(lado.getAxis()));
			}
		}
	}

	/** Una columna rota de ladrillos de pizarra (base tallada), a veces con pedazos caídos al lado. */
	private static void columna(WorldGenLevel mundo, RandomSource r, BlockPos pos, int alto, boolean conCaidos) {
		poner(mundo, pos, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
		for (int y = 1; y < alto; y++) poner(mundo, pos.above(y), ladrillo(r));
		if (r.nextBoolean()) poner(mundo, pos.above(alto), Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState());
		if (conCaidos && r.nextInt(3) > 0) {
			Direction lado = Direction.Plane.HORIZONTAL.getRandomDirection(r);
			BlockPos caido = pos.relative(lado, 2);
			for (int k = 0; k < 2 + r.nextInt(3); k++) {
				BlockPos p = caido.relative(lado, k);
				if (mundo.getBlockState(p.below()).isSolid()) poner(mundo, p, ladrillo(r));
			}
		}
	}

	/** Una tumba: lápida de piedra negra con tope tallado; a veces una vela de almas (un farol chiquito) adelante. */
	private static void tumba(WorldGenLevel mundo, RandomSource r, BlockPos pos) {
		poner(mundo, pos, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
		poner(mundo, pos.above(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
		Direction frente = Direction.Plane.HORIZONTAL.getRandomDirection(r);
		// La tierra removida de la tumba.
		for (int k = 1; k <= 2; k++) {
			BlockPos piso = pos.relative(frente, k).below();
			if (mundo.getBlockState(piso).isSolid()) mundo.setBlock(piso, Blocks.COARSE_DIRT.defaultBlockState(), 2);
		}
		if (r.nextInt(3) == 0) poner(mundo, pos.relative(frente.getClockWise()), Blocks.SOUL_LANTERN.defaultBlockState());
	}

	/** Un farol de almas colgado arriba de un poste de piedra negra. */
	private static void farol(WorldGenLevel mundo, BlockPos pos, int alto) {
		for (int y = 0; y < alto; y++) poner(mundo, pos.above(y), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
		poner(mundo, pos.above(alto), Blocks.SOUL_LANTERN.defaultBlockState());
	}

	/** Un arco en ruinas: dos columnas y el arco de arriba, con algún pedazo que falta. */
	private static void arco(WorldGenLevel mundo, RandomSource r, BlockPos pos) {
		Direction lado = r.nextBoolean() ? Direction.EAST : Direction.SOUTH;
		int ancho = 4, alto = 5;
		BlockPos otra = pos.relative(lado, ancho);
		if (!mundo.getBlockState(otra.below()).isSolid()) return;
		columna(mundo, r, pos, alto, false);
		columna(mundo, r, otra, alto - r.nextInt(3), false);
		for (int k = 0; k <= ancho; k++) {
			if (r.nextInt(4) == 0) continue;   // un pedazo caído
			poner(mundo, pos.relative(lado, k).above(alto), ladrillo(r));
		}
	}

	/** Las ruinas de una casa de 7×7: paredes rotas de distintas alturas, telarañas y un farol en el medio. */
	private static void ruina(WorldGenLevel mundo, RandomSource r, BlockPos pos) {
		int lado = 7;
		for (int x = 0; x < lado; x++) {
			for (int z = 0; z < lado; z++) {
				boolean borde = x == 0 || z == 0 || x == lado - 1 || z == lado - 1;
				BlockPos base = pos.offset(x - lado / 2, 0, z - lado / 2);
				// Que se apoye en el piso: busca el suelo cerca.
				int bajar = 0;
				while (bajar < 3 && !mundo.getBlockState(base.below()).isSolid()) {
					base = base.below();
					bajar++;
				}
				if (!borde) {
					if (r.nextInt(6) == 0) mundo.setBlock(base.below(), Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
					continue;
				}
				boolean puerta = (x == lado / 2 && z == 0);
				if (puerta) continue;
				int alto = r.nextInt(5);   // 0 = ya se cayó
				for (int y = 0; y < alto; y++) poner(mundo, base.above(y), ladrillo(r));
				if (alto > 0 && r.nextInt(5) == 0) poner(mundo, base.above(alto), Blocks.COBWEB.defaultBlockState());
			}
		}
		farol(mundo, pos, 1);
	}
}
