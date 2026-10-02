package com.dedsafio4.neocompat.fabric.api.biome.v1;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;
import java.util.function.Predicate;

public final class BiomeSelectors {
	private BiomeSelectors() {}

	@SafeVarargs
	public static Predicate<BiomeSelectionContext> includeByKey(ResourceKey<Biome>... biomas) {
		Set<ResourceKey<Biome>> conjunto = Set.of(biomas);
		return c -> conjunto.contains(c.getBiomeKey());
	}

	public static Predicate<BiomeSelectionContext> foundInOverworld() {
		return c -> c.hasTag(BiomeTags.IS_OVERWORLD);
	}

	public static Predicate<BiomeSelectionContext> foundInTheNether() {
		return c -> c.hasTag(BiomeTags.IS_NETHER);
	}

	public static Predicate<BiomeSelectionContext> foundInTheEnd() {
		return c -> c.hasTag(BiomeTags.IS_END);
	}
}
