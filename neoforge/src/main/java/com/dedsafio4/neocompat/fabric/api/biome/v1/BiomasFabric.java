package com.dedsafio4.neocompat.fabric.api.biome.v1;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** Modificador de biomas de NeoForge que aplica las apariciones anotadas con BiomeModifications.addSpawn. */
public record BiomasFabric() implements BiomeModifier {
	public static final MapCodec<BiomasFabric> CODEC = MapCodec.unit(BiomasFabric::new);

	@Override
	public void modify(Holder<Biome> bioma, Phase fase, ModifiableBiomeInfo.BiomeInfo.Builder constructor) {
		if (fase != Phase.ADD) return;
		BiomeSelectionContext contexto = new BiomeSelectionContext(bioma);
		for (BiomeModifications.Aparicion a : BiomeModifications.APARICIONES) {
			if (a.donde().test(contexto)) {
				constructor.getMobSpawnSettings().addSpawn(a.categoria(), new MobSpawnSettings.SpawnerData(a.tipo(), a.peso(), a.minimo(), a.maximo()));
			}
		}
	}

	@Override
	public MapCodec<? extends BiomeModifier> codec() {
		return CODEC;
	}
}
