package com.dedsafio4.mixin;

import com.dedsafio4.cambios.Cambios;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin {
	/**
	 * Aparición natural del Warden (ver Dedsafio4#onInitialize): solo con "wardens 1",
	 * sobre suelo firme y con las mismas reglas de oscuridad que un zombie.
	 */
	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void dedsafio4$wardenNatural(EntityType<?> tipo, ServerLevelAccessor level, MobSpawnType razon,
												BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (tipo != EntityType.WARDEN || razon != MobSpawnType.NATURAL) return;
		cir.setReturnValue(Cambios.nivel(level.getLevel().getServer(), Cambios.WARDENS) >= 1
				&& SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, pos, tipo)
				&& Monster.checkMonsterSpawnRules(EntityType.WARDEN, level, razon, pos, random));
	}

	/**
	 * Los mobs del mod que aparecen solos (los de AnuncioMob.LUGARES) solo aparecen si se habilitaron con /mob.
	 */
	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void dedsafio4$mobsHabilitados(EntityType<?> tipo, ServerLevelAccessor level, MobSpawnType razon,
												  BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (razon != MobSpawnType.NATURAL && razon != MobSpawnType.CHUNK_GENERATION) return;
		if (!com.dedsafio4.bestias.AnuncioMob.controla(tipo)) return;
		if (!com.dedsafio4.bestias.AnuncioMob.activo(level.getLevel().getServer(), tipo)) cir.setReturnValue(false);
	}

	/**
	 * Zombis en el Nether (ver Dedsafio4#onInitialize): solo con "zombi 3", sin importar la luz
	 * (con suelo firme y fuera de pacífico). Sin el cambio, en el Nether no aparecen, como siempre.
	 */
	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void dedsafio4$zombiNether(EntityType<?> tipo, ServerLevelAccessor level, MobSpawnType razon,
											  BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (tipo != EntityType.ZOMBIE || razon != MobSpawnType.NATURAL) return;
		var mundo = level.getLevel();
		if (mundo.dimension() != net.minecraft.world.level.Level.NETHER) return;
		cir.setReturnValue(Cambios.nivel(mundo.getServer(), Cambios.ZOMBI) >= 3
				&& level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
				&& SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, pos, tipo)
				&& net.minecraft.world.entity.Mob.checkMobSpawnRules(EntityType.ZOMBIE, level, razon, pos, random));
	}

	/**
	 * Bogged en todo el Overworld (ver Dedsafio4#onInitialize): fuera de los pantanos solo con
	 * "esqueleto_bogged 1" y de noche (con las reglas de oscuridad de siempre). En los pantanos, como siempre.
	 */
	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void dedsafio4$boggedNoche(EntityType<?> tipo, ServerLevelAccessor level, MobSpawnType razon,
											  BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (tipo != EntityType.BOGGED || razon != MobSpawnType.NATURAL) return;
		var mundo = level.getLevel();
		if (mundo.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
		var bioma = level.getBiome(pos);
		if (bioma.is(net.minecraft.world.level.biome.Biomes.SWAMP) || bioma.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP)) return;
		if (Cambios.nivel(mundo.getServer(), Cambios.ESQUELETO_BOGGED) < 1 || !mundo.isNight()) cir.setReturnValue(false);
	}

	/**
	 * Stray en todo el Overworld (ver Dedsafio4#onInitialize): fuera de las llanuras nevadas y los picos
	 * de hielo solo con "esqueleto_stray 1" y de noche. En la nieve, como siempre.
	 */
	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void dedsafio4$strayNoche(EntityType<?> tipo, ServerLevelAccessor level, MobSpawnType razon,
											 BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (tipo != EntityType.STRAY || razon != MobSpawnType.NATURAL) return;
		var mundo = level.getLevel();
		if (mundo.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
		var bioma = level.getBiome(pos);
		if (bioma.is(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS) || bioma.is(net.minecraft.world.level.biome.Biomes.ICE_SPIKES)) return;
		if (Cambios.nivel(mundo.getServer(), Cambios.ESQUELETO_STRAY) < 1 || !mundo.isNight()) cir.setReturnValue(false);
	}

	/**
	 * Aparición natural del Blaze en todo el Nether (ver Dedsafio4#onInitialize): con "blaze 1" en
	 * cualquier lado; sin el cambio, sólo dentro de las fortalezas, como siempre.
	 */
	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void dedsafio4$blazeNatural(EntityType<?> tipo, ServerLevelAccessor level, MobSpawnType razon,
											   BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (tipo != EntityType.BLAZE || razon != MobSpawnType.NATURAL) return;
		var mundo = level.getLevel();
		if (Cambios.nivel(mundo.getServer(), Cambios.BLAZE) >= 1) return;
		var fortaleza = mundo.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
				.get(net.minecraft.world.level.levelgen.structure.BuiltinStructures.FORTRESS);
		boolean enFortaleza = fortaleza != null
				&& mundo.structureManager().getStructureWithPieceAt(pos, fortaleza).isValid();
		if (!enFortaleza) cir.setReturnValue(false);
	}
}
