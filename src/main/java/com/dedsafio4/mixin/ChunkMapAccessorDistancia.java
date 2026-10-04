package com.dedsafio4.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** La distancia de chunks que Minecraft le manda a un jugador (para limitarla durante /momentito 1). */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessorDistancia {
	@Invoker("getPlayerViewDistance")
	int dedsafio4$distancia(ServerPlayer jugador);
}
