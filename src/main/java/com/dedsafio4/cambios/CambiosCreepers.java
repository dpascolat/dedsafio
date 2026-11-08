package com.dedsafio4.cambios;

import com.dedsafio4.mixin.CreeperAccessor;
import com.dedsafio4.nave.ModEntidades;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Cambios de dificultad que hacen aparecer bichos:
 * - totem_creepers 1: al saltar un tótem, 20% Creeper de Pasto o 10% Creeper Raíz de Azalea donde estás.
 * - perla_nuclear 1: donde cae una Perla del End, 3% de Creeper Nuclear.
 * - cerebro_nuclear 1: al morir un Cerebro (Amarillo o Rojo), 10% de Creeper Nuclear.
 * - cerebro_amarillo 1 / cerebro_rojo 1: romper una flor amarilla / roja en el Overworld hace salir ese Cerebro
 *   (el doble de daño está en CerebroAmarilloEntity).
 * - reviil_permanente 1: cuando muere un jugador, al lado de cada jugador vivo aparece un Creeper Eléctrico con 1 de vida.
 */
public final class CambiosCreepers {
	private CambiosCreepers() {}

	private static final float PASTO = 0.20f, AZALEA = 0.10f, PERLA = 0.03f, CEREBRO = 0.10f;

	/** Flores con amarillo: Diente de León, Girasol, Margarita, Anflorcha y Rubiácea. (Las Flores Silvestres no existen en la 1.21.1.) */
	private static final Set<Block> FLORES_AMARILLAS = Set.of(Blocks.DANDELION, Blocks.SUNFLOWER, Blocks.OXEYE_DAISY,
			Blocks.TORCHFLOWER, Blocks.AZURE_BLUET);
	/** Flores con rojo: Amapola, Tulipán Rojo, Rosal, Bayas Dulces, Hongo Rojo y Bloque de Hongo Rojo. */
	private static final Set<Block> FLORES_ROJAS = Set.of(Blocks.POPPY, Blocks.RED_TULIP, Blocks.ROSE_BUSH,
			Blocks.SWEET_BERRY_BUSH, Blocks.RED_MUSHROOM, Blocks.RED_MUSHROOM_BLOCK);

	public static void registrar() {
		PlayerBlockBreakEvents.AFTER.register((level, jugador, pos, estado, entidad) -> {
			if (level instanceof ServerLevel mundo && mundo.dimension() == Level.OVERWORLD) rompioFlor(mundo, pos, estado);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entidad, fuente) -> {
			if (entidad instanceof ServerPlayer muerto) murioJugador(muerto);
		});
	}

	private static boolean activo(ServerLevel mundo, String cambio) {
		return Cambios.nivel(mundo.getServer(), cambio) >= 1;
	}

	/** Lo llama AvisoTotem cada vez que a alguien le salta un tótem. */
	public static void saltoTotem(ServerPlayer jugador) {
		ServerLevel mundo = jugador.serverLevel();
		if (!activo(mundo, Cambios.TOTEM_CREEPERS)) return;
		float azar = mundo.getRandom().nextFloat();
		if (azar < PASTO) ModEntidades.CREEPER_PASTO.spawn(mundo, jugador.blockPosition(), MobSpawnType.EVENT);
		else if (azar < PASTO + AZALEA) ModEntidades.CREEPER_AZALEA.spawn(mundo, jugador.blockPosition(), MobSpawnType.EVENT);
	}

	/** Lo llama PerlaFallaMixin cuando una Perla del End cae. */
	public static void cayoPerla(ServerLevel mundo, Vec3 donde) {
		if (!activo(mundo, Cambios.PERLA_NUCLEAR) || mundo.getRandom().nextFloat() >= PERLA) return;
		ModEntidades.CREEPER_NUCLEAR.spawn(mundo, BlockPos.containing(donde), MobSpawnType.EVENT);
	}

	/** Lo llama CerebroAmarilloEntity (y el Rojo) al morir. */
	public static void murioCerebro(ServerLevel mundo, BlockPos donde) {
		if (!activo(mundo, Cambios.CEREBRO_NUCLEAR) || mundo.getRandom().nextFloat() >= CEREBRO) return;
		ModEntidades.CREEPER_NUCLEAR.spawn(mundo, donde, MobSpawnType.EVENT);
	}

	private static void rompioFlor(ServerLevel mundo, BlockPos pos, BlockState estado) {
		if (FLORES_AMARILLAS.contains(estado.getBlock()) && activo(mundo, Cambios.CEREBRO_AMARILLO)) {
			ModEntidades.CEREBRO_AMARILLO.spawn(mundo, pos, MobSpawnType.EVENT);
		} else if (FLORES_ROJAS.contains(estado.getBlock()) && activo(mundo, Cambios.CEREBRO_ROJO)) {
			ModEntidades.CEREBRO_ROJO.spawn(mundo, pos, MobSpawnType.EVENT);
		}
	}

	private static void murioJugador(ServerPlayer muerto) {
		if (!activo(muerto.serverLevel(), Cambios.REVIIL_PERMANENTE)) return;
		for (ServerPlayer vivo : muerto.server.getPlayerList().getPlayers()) {
			if (vivo == muerto || !vivo.isAlive() || vivo.isCreative() || vivo.isSpectator()) continue;
			Creeper creeper = EntityType.CREEPER.spawn(vivo.serverLevel(), vivo.blockPosition(), MobSpawnType.EVENT);
			if (creeper == null) continue;
			creeper.getEntityData().set(CreeperAccessor.dedsafio4$powered(), true);
			creeper.setHealth(1f);
		}
	}
}
