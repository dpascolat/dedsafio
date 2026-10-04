package com.dedsafio4.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Para volver a calcular qué chunks se le mandan a un jugador (lo usa /momentito 1). */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
	@Invoker("updateChunkTracking")
	void dedsafio4$actualizarChunks(ServerPlayer jugador);
}
