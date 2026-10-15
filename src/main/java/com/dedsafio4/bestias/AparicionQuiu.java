package com.dedsafio4.bestias;

import com.dedsafio4.dimension.Portales;
import com.dedsafio4.nave.ModEntidades;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Hace aparecer al Plumosaurio y al Narval en Quiu cuando están habilitados con /mob. Minecraft solo no los pone:
 * Quiu tiene techo, y Minecraft busca el lugar para los animales arriba de todo (encima del techo) o a una altura
 * al azar, así que casi nunca les encontraba lugar. Esto busca el piso o el agua cerca de cada jugador.
 */
public final class AparicionQuiu {
	private AparicionQuiu() {}

	private static final ResourceKey<Biome> JUNGLA = ResourceKey.create(Registries.BIOME,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "jungla"));
	/** Cada cuánto prueba (10 segundos). */
	private static final int CADA = 200;
	/** Cuántos puede haber cerca de un jugador como mucho. */
	private static final int MAX_PLUMOSAURIOS = 2, MAX_NARVALES = 3;

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CADA != 0) return;
			ServerLevel quiu = server.getLevel(Portales.DIMENSION_NUEVA);
			if (quiu == null || quiu.players().isEmpty()) return;
			if (!quiu.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
			boolean plumosaurio = AnuncioMob.activo(server, ModEntidades.WALKER);
			boolean narval = AnuncioMob.activo(server, ModEntidades.NARVAL);
			if (!plumosaurio && !narval) return;
			for (ServerPlayer jugador : quiu.players()) {
				if (jugador.isSpectator()) continue;
				if (plumosaurio) plumosaurio(quiu, jugador);
				if (narval) narval(quiu, jugador);
			}
		});
	}

	private static int cuantos(ServerLevel level, EntityType<?> tipo, BlockPos centro, double radio) {
		return level.getEntities(tipo, new AABB(centro).inflate(radio), e -> true).size();
	}

	/** En el pasto de la jungla, a 24–64 bloques del jugador, donde entre entero (es enorme). */
	private static void plumosaurio(ServerLevel level, ServerPlayer jugador) {
		RandomSource azar = level.getRandom();
		BlockPos centro = jugador.blockPosition();
		if (cuantos(level, ModEntidades.WALKER, centro, 96) >= MAX_PLUMOSAURIOS || azar.nextInt(2) != 0) return;
		for (int intento = 0; intento < 8; intento++) {
			double angulo = azar.nextDouble() * Math.PI * 2, distancia = 24 + azar.nextDouble() * 40;
			int x = centro.getX() + (int) (Math.cos(angulo) * distancia);
			int z = centro.getZ() + (int) (Math.sin(angulo) * distancia);
			for (int y = centro.getY() + 24; y >= centro.getY() - 32; y--) {
				BlockPos suelo = new BlockPos(x, y, z);
				if (!level.isLoaded(suelo)) break;
				if (!level.getBlockState(suelo).is(BlockTags.ANIMALS_SPAWNABLE_ON)) continue;
				BlockPos pos = suelo.above();
				if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) continue;
				if (!level.getBiome(pos).is(JUNGLA)) break;
				if (!level.noCollision(ModEntidades.WALKER.getSpawnAABB(x + 0.5, pos.getY(), z + 0.5))) break;
				if (ModEntidades.WALKER.spawn(level, pos, MobSpawnType.NATURAL) != null) return;
				break;
			}
		}
	}

	/** En el agua de Quiu (dos bloques de hondo como mínimo), a 12–40 bloques del jugador. */
	private static void narval(ServerLevel level, ServerPlayer jugador) {
		RandomSource azar = level.getRandom();
		BlockPos centro = jugador.blockPosition();
		if (cuantos(level, ModEntidades.NARVAL, centro, 64) >= MAX_NARVALES || azar.nextInt(2) != 0) return;
		for (int intento = 0; intento < 12; intento++) {
			double angulo = azar.nextDouble() * Math.PI * 2, distancia = 12 + azar.nextDouble() * 28;
			int x = centro.getX() + (int) (Math.cos(angulo) * distancia);
			int z = centro.getZ() + (int) (Math.sin(angulo) * distancia);
			for (int y = centro.getY() + 16; y >= centro.getY() - 40; y--) {
				BlockPos pos = new BlockPos(x, y, z);
				if (!level.isLoaded(pos)) break;
				// Agua de verdad (el Agua Rara también es agua por dentro, pero es otro bloque).
				if (!level.getBlockState(pos).is(Blocks.WATER) || !level.getBlockState(pos.below()).is(Blocks.WATER)) continue;
				if (ModEntidades.NARVAL.spawn(level, pos.below(), MobSpawnType.NATURAL) != null) return;
				break;
			}
		}
	}
}
