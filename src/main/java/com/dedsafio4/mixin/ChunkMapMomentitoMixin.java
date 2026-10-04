package com.dedsafio4.mixin;

import com.dedsafio4.momentito.MomentitoEntity;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Durante /momentito 1, a los que ven la escena se les mandan los chunks de alrededor de la cámara (hasta 8)
 * en vez de los de donde están parados.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMomentitoMixin {
	@Redirect(method = "updateChunkTracking", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
	private ChunkPos dedsafio4$centroCamara(ServerPlayer jugador) {
		ChunkPos camara = MomentitoEntity.chunkCamara(jugador);
		return camara != null ? camara : jugador.chunkPosition();
	}

	@Redirect(method = "updateChunkTracking", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ChunkMap;getPlayerViewDistance(Lnet/minecraft/server/level/ServerPlayer;)I"))
	private int dedsafio4$distanciaCamara(ChunkMap mapa, ServerPlayer jugador) {
		return MomentitoEntity.distanciaCamara(jugador, ((ChunkMapAccessorDistancia) mapa).dedsafio4$distancia(jugador));
	}
}
