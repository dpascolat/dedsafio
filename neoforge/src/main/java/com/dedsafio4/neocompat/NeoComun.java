package com.dedsafio4.neocompat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Reemplazos de cosas de Minecraft que Fabric deja usar directo y NeoForge no (se cambian al copiar el código). */
public final class NeoComun {
	private NeoComun() {}

	static final List<Consumer<RegisterSpawnPlacementsEvent>> COLOCACIONES = new ArrayList<>();

	/** En lugar de SpawnPlacements.register (en NeoForge es privado: va en RegisterSpawnPlacementsEvent). */
	public static <T extends Mob> void colocacion(EntityType<T> tipo, SpawnPlacementType donde, Heightmap.Types altura,
			SpawnPlacements.SpawnPredicate<T> regla) {
		COLOCACIONES.add(e -> e.register(tipo, donde, altura, regla, RegisterSpawnPlacementsEvent.Operation.REPLACE));
	}
}
