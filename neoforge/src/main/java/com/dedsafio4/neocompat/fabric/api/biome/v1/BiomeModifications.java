package com.dedsafio4.neocompat.fabric.api.biome.v1;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Anota dónde aparece cada bicho; lo aplica el modificador de biomas de Puente (BiomasFabric). */
public final class BiomeModifications {
	private BiomeModifications() {}

	public record Aparicion(Predicate<BiomeSelectionContext> donde, MobCategory categoria, EntityType<?> tipo, int peso, int minimo, int maximo) {}

	public static final List<Aparicion> APARICIONES = new ArrayList<>();

	public static void addSpawn(Predicate<BiomeSelectionContext> donde, MobCategory categoria, EntityType<?> tipo, int peso, int minimo, int maximo) {
		APARICIONES.add(new Aparicion(donde, categoria, tipo, peso, minimo, maximo));
	}
}
