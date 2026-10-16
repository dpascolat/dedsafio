package com.dedsafio4.mixin;

import com.dedsafio4.dimension.ReinicioQuiu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.entity.ChunkEntities;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Los bichos y cosas guardados en un chunk de Quiu de antes de /quiu_reload eliminar no se cargan. */
@Mixin(EntityStorage.class)
public abstract class EntityStorageReinicioMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "loadEntities", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$reinicioQuiu(ChunkPos pos, CallbackInfoReturnable<CompletableFuture<ChunkEntities<Entity>>> cir) {
		if (ReinicioQuiu.sinEntidades(this.level, pos)) {
			cir.setReturnValue(CompletableFuture.completedFuture(new ChunkEntities<>(pos, List.of())));
		}
	}
}
