package com.dedsafio4.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * La Dimensión de los Órganos: todo el piso es carne (rosa, roja y con venas) y no hay árboles ni plantas.
 * Por ahora se entra con "/admin organos" (y con el mismo comando se vuelve al Overworld).
 */
public final class Organos {
	private Organos() {}

	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "organos"));

	public static int viajar(ServerPlayer jugador) {
		ServerLevel desde = jugador.serverLevel();
		boolean volver = desde.dimension().equals(DIMENSION);
		ServerLevel destino = volver ? desde.getServer().overworld() : desde.getServer().getLevel(DIMENSION);
		if (destino == null) return 0;

		BlockPos llegada = Portales.lugarSeguro(destino, jugador.blockPosition());
		jugador.teleportTo(destino, llegada.getX() + 0.5, llegada.getY(), llegada.getZ() + 0.5,
				jugador.getYRot(), jugador.getXRot());
		destino.playSound(null, llegada, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1f, 0.8f);
		jugador.sendSystemMessage(Component.literal(volver
				? "Volviste al Overworld."
				: "Entraste a la Dimensión de los Órganos (para volver: /admin organos).").withColor(0xE0607A));
		return 1;
	}
}
