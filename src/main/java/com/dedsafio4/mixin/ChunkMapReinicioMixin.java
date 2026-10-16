package com.dedsafio4.mixin;

import com.dedsafio4.dimension.ReinicioQuiu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Después de /quiu_reload eliminar, los chunks de Quiu guardados antes no se usan: se generan de nuevo. */
@Mixin(ChunkMap.class)
public abstract class ChunkMapReinicioMixin {
	@Shadow
	@Final
	ServerLevel level;

	@Inject(method = "readChunk", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$reinicioQuiu(ChunkPos pos, CallbackInfoReturnable<CompletableFuture<Optional<CompoundTag>>> cir) {
		ServerLevel mundo = this.level;
		ReinicioQuiu.asegurar(mundo.getServer());
		cir.setReturnValue(cir.getReturnValue().thenApply(guardado -> guardado.filter(tag -> !ReinicioQuiu.viejo(mundo, pos, tag))));
	}
}
