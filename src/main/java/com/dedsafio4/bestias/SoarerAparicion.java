package com.dedsafio4.bestias;

import com.dedsafio4.nave.ModEntidades;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;

/**
 * El Soarer aparece como los Phantoms: de noche y a cielo abierto, sobre el que lleva 3 días o más sin dormir (cuantos
 * más días, más seguido). Solo después de /mob soarer.
 */
public final class SoarerAparicion {
	private SoarerAparicion() {}

	/** 3 días de Minecraft. */
	private static final int SIN_DORMIR = 3 * 24000;
	/** Cada cuánto se prueba (1 minuto). */
	private static final int CADA = 1200;
	/** Cuántos puede haber cerca de un jugador. */
	private static final int MAXIMO = 3;

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CADA == 0) probar(server);
		});
	}

	public static void probar(MinecraftServer server) {
		if (!AnuncioMob.activo(server, ModEntidades.SOARER)) return;
		ServerLevel mundo = server.overworld();
		if (!mundo.isNight() || mundo.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
		RandomSource azar = mundo.getRandom();
		for (ServerPlayer jugador : mundo.players()) {
			if (jugador.isSpectator() || jugador.isCreative() || jugador.getY() < mundo.getSeaLevel() || !mundo.canSeeSky(jugador.blockPosition())) continue;
			int sinDormir = Mth.clamp(jugador.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
			if (azar.nextInt(sinDormir) < SIN_DORMIR) continue;
			if (mundo.getEntities(ModEntidades.SOARER, jugador.getBoundingBox().inflate(64), e -> true).size() >= MAXIMO) continue;
			BlockPos pos = jugador.blockPosition().offset(azar.nextInt(21) - 10, 20 + azar.nextInt(15), azar.nextInt(21) - 10);
			if (mundo.isEmptyBlock(pos)) ModEntidades.SOARER.spawn(mundo, pos, MobSpawnType.NATURAL);
		}
	}
}
