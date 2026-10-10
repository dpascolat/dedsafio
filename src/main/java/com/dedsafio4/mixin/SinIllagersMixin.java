package com.dedsafio4.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Los Saqueadores (pillager), los Evocadores y los Devastadores (ravager) no aparecen solos: ni de forma natural, ni
 * en patrullas, ni en los asaltos, ni en los puestos de avanzada o las mansiones. Con huevo, /summon o un spawner sí.
 * Se los saca antes de que entren al mundo (Minecraft no agrega una entidad que ya está quitada).
 */
@Mixin(Mob.class)
public abstract class SinIllagersMixin {
	@Inject(method = "finalizeSpawn", at = @At("HEAD"))
	private void dedsafio4$sinIllagers(ServerLevelAccessor level, DifficultyInstance dificultad, MobSpawnType razon,
									   SpawnGroupData datos, CallbackInfoReturnable<SpawnGroupData> cir) {
		Mob mob = (Mob) (Object) this;
		EntityType<?> tipo = mob.getType();
		if (tipo != EntityType.PILLAGER && tipo != EntityType.EVOKER && tipo != EntityType.RAVAGER) return;
		switch (razon) {
			case NATURAL, CHUNK_GENERATION, STRUCTURE, PATROL, EVENT, REINFORCEMENT, JOCKEY -> mob.discard();
			default -> {}
		}
	}
}
