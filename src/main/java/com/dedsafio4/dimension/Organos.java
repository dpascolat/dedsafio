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
 * Gleba son islas de carne flotando en el aire, con el mar abajo de todo, y llega hasta 2.000 bloques hacia cada lado
 * del centro: más allá no se puede ir. Al entrar, se llega arriba de la isla más cercana.
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

	/** Hasta dónde llega Gleba, hacia cada lado del centro. */
	public static final int BORDE = 2000;

	/** Una posición dentro del borde de Gleba. */
	private static double dentro(double v) {
		return Math.max(-BORDE + 0.5, Math.min(BORDE - 0.5, v));
	}

	public static void registrar() {
		// El borde: el que se pasa de los 2.000 bloques vuelve adentro.
		ServerTickEvents.END_WORLD_TICK.register(mundo -> {
			if (!mundo.dimension().equals(DIMENSION)) return;
			for (ServerPlayer jugador : mundo.players()) {
				double x = jugador.getX(), z = jugador.getZ();
				if (Math.abs(x) <= BORDE && Math.abs(z) <= BORDE) continue;
				if (jugador.isPassenger()) jugador.stopRiding();
				jugador.teleportTo(dentro(x), jugador.getY(), dentro(z));
				jugador.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
				jugador.displayClientMessage(Component.literal("Llegaste al borde de Gleba.").withColor(0xE0607A), true);
			}
		});
		ServerTickEvents.END_WORLD_TICK.register(mundo -> {
			if (!mundo.dimension().equals(DIMENSION) || mundo.getGameTime() % CADA != 0) return;
			DamageSource fuente = new DamageSource(mundo.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
					.getHolderOrThrow(DANIO_AGUA));
			for (ServerPlayer jugador : mundo.players()) {
				if (jugador.isAlive() && jugador.isInWater()) jugador.hurt(fuente, DANIO);
			}
		});
	}

	/** Arriba de la isla flotante más cercana (buscando en vueltas cada vez más grandes); si no hay, donde se pueda. */
	private static BlockPos islaCercana(ServerLevel mundo, BlockPos desde) {
		for (int radio = 0; radio <= 400; radio += 16) {
			for (int dx = -radio; dx <= radio; dx += 16) {
				for (int dz = -radio; dz <= radio; dz += 16) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radio) continue;
					BlockPos col = BlockPos.containing(dentro(desde.getX() + dx), 0, dentro(desde.getZ() + dz));
					BlockPos arriba = mundo.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
					// Una isla: piso firme bien arriba del mar (no el agua).
					if (arriba.getY() > mundo.getSeaLevel() + 10 && mundo.getFluidState(arriba.below()).isEmpty()) return arriba;
				}
			}
		}
		return Portales.lugarSeguro(mundo, desde);
	}

	public static int viajar(ServerPlayer jugador) {
		ServerLevel desde = jugador.serverLevel();
		boolean volver = desde.dimension().equals(DIMENSION);
		ServerLevel destino = volver ? desde.getServer().overworld() : desde.getServer().getLevel(DIMENSION);
		if (destino == null) return 0;

		BlockPos desdeAca = jugador.blockPosition();
		// Al entrar a Gleba, siempre dentro de su borde.
		if (!volver) desdeAca = BlockPos.containing(dentro(desdeAca.getX()), desdeAca.getY(), dentro(desdeAca.getZ()));
		BlockPos llegada = volver ? Portales.lugarSeguro(destino, desdeAca) : islaCercana(destino, desdeAca);
		jugador.teleportTo(destino, llegada.getX() + 0.5, llegada.getY(), llegada.getZ() + 0.5,
				jugador.getYRot(), jugador.getXRot());
		destino.playSound(null, llegada, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1f, 0.8f);
		jugador.sendSystemMessage(Component.literal(volver
				? "Volviste al Overworld."
				: "Entraste a la Dimensión de los Órganos (para volver: /admin organos).").withColor(0xE0607A));
		return 1;
	}
}
