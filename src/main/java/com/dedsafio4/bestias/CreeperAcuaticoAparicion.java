package com.dedsafio4.bestias;

import com.dedsafio4.nave.ModEntidades;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * El Creeper Acuático aparece en el agua del Overworld cerca de los jugadores, de día o de noche y con cualquier luz: no
 * usa la aparición de Minecraft, que reparte los monstruos entre todos los de la zona y casi nunca lo elegía. Solo
 * después de /mob creeper_acuatico.
 */
public final class CreeperAcuaticoAparicion {
	private CreeperAcuaticoAparicion() {}

	/** Cada cuánto se prueba (20 segundos). */
	private static final int CADA = 400;
	/** Cuántos lugares se miran por jugador. */
	private static final int INTENTOS = 8;
	/** Cuántos puede haber cerca de un jugador. */
	private static final int MAXIMO = 4, ALCANCE_MAXIMO = 64;
	private static final int DISTANCIA_MINIMA = 24, DISTANCIA_MAXIMA = 48;

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CADA == 0) probar(server);
		});
	}

	public static void probar(MinecraftServer server) {
		if (!AnuncioMob.activo(server, ModEntidades.CREEPER_ACUATICO)) return;
		ServerLevel mundo = server.overworld();
		if (mundo.getDifficulty() == Difficulty.PEACEFUL) return;
		for (ServerPlayer jugador : mundo.players()) {
			if (jugador.isSpectator()) continue;
			if (mundo.getEntities(ModEntidades.CREEPER_ACUATICO, jugador.getBoundingBox().inflate(ALCANCE_MAXIMO), e -> true).size() >= MAXIMO) continue;
			for (int i = 0; i < INTENTOS; i++) {
				BlockPos pos = lugar(mundo, jugador);
				if (pos == null) continue;
				ModEntidades.CREEPER_ACUATICO.spawn(mundo, pos, MobSpawnType.NATURAL);
				break;
			}
		}
	}

	/** Un lugar con agua para sus dos bloques de alto, a un costado del jugador; null si donde cayó no hay. */
	private static BlockPos lugar(ServerLevel mundo, ServerPlayer jugador) {
		RandomSource azar = mundo.getRandom();
		double angulo = azar.nextDouble() * 2 * Math.PI;
		int distancia = DISTANCIA_MINIMA + azar.nextInt(DISTANCIA_MAXIMA - DISTANCIA_MINIMA + 1);
		int x = jugador.getBlockX() + (int) (Math.cos(angulo) * distancia);
		int z = jugador.getBlockZ() + (int) (Math.sin(angulo) * distancia);
		if (!mundo.isPositionEntityTicking(new BlockPos(x, jugador.getBlockY(), z))) return null;
		int fondo = mundo.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
		int profundidad = mundo.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - fondo;
		if (profundidad < 2) return null;
		BlockPos pos = new BlockPos(x, fondo + azar.nextInt(profundidad - 1), z);
		// Más arriba o más abajo que esto ya no contarían para el MAXIMO.
		if (Math.abs(pos.getY() - jugador.getBlockY()) > ALCANCE_MAXIMO) return null;
		return mundo.getFluidState(pos).is(FluidTags.WATER) && mundo.getFluidState(pos.above()).is(FluidTags.WATER) ? pos : null;
	}
}
