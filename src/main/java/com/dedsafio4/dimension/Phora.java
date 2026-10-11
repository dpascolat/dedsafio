package com.dedsafio4.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * El bioma Phora, en el Centro de Quiu (la parte más caliente): sin árboles, con agua verde y donde aparecen la Nave
 * y la Nave Veloz (worldgen/biome/phora.json). Ahí no se pueden romper bloques (en creativo sí); poner bloques y
 * agua, sí.
 */
public final class Phora {
	private Phora() {}

	public static final ResourceKey<Biome> BIOMA = ResourceKey.create(Registries.BIOME,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "phora"));

	public static boolean enPhora(Level mundo, BlockPos pos) {
		return mundo.getBiome(pos).is(BIOMA);
	}

	public static void registrar() {
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((mundo, jugador, pos, estado, entidad) -> {
			if (jugador.isCreative() || !enPhora(mundo, pos)) return true;
			jugador.displayClientMessage(Component.literal("En Phora no se pueden romper bloques.").withColor(0x5FD96A), true);
			return false;
		});
	}
}
