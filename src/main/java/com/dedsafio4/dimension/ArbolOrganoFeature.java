package com.dedsafio4.dimension;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bloques.ModBloques;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Árbol de la Dimensión de los Órganos: un tronco hueco y retorcido de Gelatina Roja que se abre arriba como una
 * copa, con una corona de Gelatina Rosa. Uno de cada tres es el árbol original (structure/arbol_organo.nbt), girado
 * o espejado al azar; los demás se arman con la misma forma pero distintos (alto, inclinación, ancho de la copa).
 */
public class ArbolOrganoFeature extends Feature<NoneFeatureConfiguration> {
	private static final ResourceLocation MOLDE = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "arbol_organo");
	/** Dónde está la base del tronco dentro del molde (para girarlo sobre ese punto). */
	private static final BlockPos BASE_MOLDE = new BlockPos(6, 0, 4);

	public ArbolOrganoFeature() {
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
		if (!level.getBlockState(base).isAir()) return false;
		if (base.getY() + 20 > level.getMaxBuildHeight()) return false;

		if (azar.nextInt(3) == 0 && ponerMolde(level, base, azar)) return true;
		crecer(level, base, azar);
		return true;
	}

	/** El árbol tal como lo armó Patricio, girado o espejado al azar. */
	private static boolean ponerMolde(WorldGenLevel level, BlockPos base, RandomSource azar) {
		StructureTemplate molde = level.getLevel().getServer().getStructureManager().get(MOLDE).orElse(null);
		if (molde == null) return false;
		StructurePlaceSettings ajustes = new StructurePlaceSettings()
				.setRotation(Rotation.getRandom(azar))
				.setMirror(azar.nextBoolean() ? Mirror.NONE : Mirror.FRONT_BACK)
				.setRotationPivot(BASE_MOLDE)
				.setRandom(azar);
		return molde.placeInWorld(level, base.subtract(BASE_MOLDE), base, ajustes, azar, 2);
	}

	/** Un árbol nuevo con la misma forma: base llena, tronco finito que se va torciendo, copa que se abre. */
	private static void crecer(WorldGenLevel level, BlockPos base, RandomSource azar) {
		BlockState roja = ModBloques.GELATINA_ROJA.defaultBlockState();
		BlockState rosa = ModBloques.GELATINA_ROSA.defaultBlockState();
		double[] centro = {0, 0};
		double angulo = azar.nextDouble() * 2 * Math.PI;   // hacia dónde se inclina
		int tronco = 7 + azar.nextInt(5);
		int copa = 3 + azar.nextInt(2);
		double radioCopa = 2.4 + azar.nextDouble() * 1.3;

		int y = 0;
		anillo(level, base, y++, centro, 1.3 + azar.nextDouble() * 0.5, roja, 0, true, azar);
		for (int i = 0; i < tronco; i++) {
			if (azar.nextFloat() < 0.45f) {
				centro[0] += Math.cos(angulo) * 0.7;
				centro[1] += Math.sin(angulo) * 0.7;
				angulo += (azar.nextDouble() - 0.5) * 1.2;
			}
			double radio = azar.nextFloat() < 0.25f ? 0 : 0.8 + azar.nextDouble() * 0.5;
			if (radio < 0.5) poner(level, base.offset((int) Math.round(centro[0]), y, (int) Math.round(centro[1])), roja);
			else anillo(level, base, y, centro, radio, roja, 0.12, false, azar);
			y++;
		}
		for (int i = 0; i < copa; i++) {
			anillo(level, base, y++, centro, 1.4 + (radioCopa - 1.4) * (i + 1) / copa, roja, 0.08, false, azar);
		}
		anillo(level, base, y, centro, radioCopa, rosa, 0.1, false, azar);
		anillo(level, base, y++, centro, radioCopa - 1, roja, 0.6, false, azar);
		anillo(level, base, y++, centro, radioCopa - 0.8, rosa, 0.3, true, azar);
		anillo(level, base, y, centro, radioCopa - 1.6, rosa, 0.55, true, azar);
	}

	/** Un aro (o un disco, si {@code lleno}) alrededor del centro; {@code huecos} es la probabilidad de saltear un bloque. */
	private static void anillo(WorldGenLevel level, BlockPos base, int y, double[] centro, double radio,
							   BlockState bloque, double huecos, boolean lleno, RandomSource azar) {
		int ox = (int) Math.round(centro[0]), oz = (int) Math.round(centro[1]);
		double fx = centro[0] - ox, fz = centro[1] - oz;
		int r = (int) Math.ceil(radio + 1);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.hypot(dx - fx, dz - fz);
				boolean adentro = lleno ? d <= radio + 0.5 : Math.abs(d - radio) <= 0.55;
				if (adentro && azar.nextFloat() >= huecos) poner(level, base.offset(ox + dx, y, oz + dz), bloque);
			}
		}
	}

	private static void poner(WorldGenLevel level, BlockPos pos, BlockState bloque) {
		if (level.getBlockState(pos).canBeReplaced()) level.setBlock(pos, bloque, 2);
	}
}
