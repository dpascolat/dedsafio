package com.dedsafio4.dimension;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * La Dimensión de los Órganos: todo el piso es carne (rosa, roja y con venas), con pasto rosa y árboles de gelatina.
 * Por ahora se entra con "/admin organos" (y con el mismo comando se vuelve al Overworld).
 * El agua de esta dimensión es rosa y lastima: medio corazón por segundo mientras estés adentro. No hay nubes.
 */
public final class Organos {
	private Organos() {}

	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "organos"));

	public static final ResourceKey<DamageType> DANIO_AGUA = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "agua_organos"));
	/** Cada cuánto lastima el agua (en ticks) y cuánto (1 = medio corazón). */
	private static final int CADA = 20;
	private static final float DANIO = 1f;

	public static void registrar() {
		ServerTickEvents.END_WORLD_TICK.register(mundo -> {
			if (!mundo.dimension().equals(DIMENSION) || mundo.getGameTime() % CADA != 0) return;
			DamageSource fuente = new DamageSource(mundo.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
					.getHolderOrThrow(DANIO_AGUA));
			for (ServerPlayer jugador : mundo.players()) {
				if (jugador.isAlive() && jugador.isInWater()) jugador.hurt(fuente, DANIO);
			}
		});
	}

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
