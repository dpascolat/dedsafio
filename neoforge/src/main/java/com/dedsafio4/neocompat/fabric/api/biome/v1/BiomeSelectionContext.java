package com.dedsafio4.neocompat.fabric.api.biome.v1;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class BiomeSelectionContext {
	private final Holder<Biome> bioma;

	public BiomeSelectionContext(Holder<Biome> bioma) {
		this.bioma = bioma;
	}

	public ResourceKey<Biome> getBiomeKey() {
		return bioma.unwrapKey().orElse(null);
	}

	public Biome getBiome() {
		return bioma.value();
	}

	public boolean hasTag(TagKey<Biome> etiqueta) {
		return bioma.is(etiqueta);
	}

	public Holder<Biome> holder() {
		return bioma;
	}
}
