package com.dedsafio4.dimension;

import com.dedsafio4.bloques.ModBloques;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * El viaje entre mundos: tocar el Agua Rara te lleva a la otra dimensión. Desde el Overworld
 * vas a la dimensión nueva, y desde ahí (por ejemplo en el círculo del cielo) volvés al Overworld.
 */
public final class Portales {
	private Portales() {}

	public static final ResourceKey<Level> DIMENSION_NUEVA = ResourceKey.create(Registries.DIMENSION,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "dimension_nueva"));

	/** Para no rebotar de un mundo al otro: 5 segundos. */
	private static final int ESPERA = 100;
	/** A qué altura queda el círculo de vuelta. */
	private static final int ALTURA_CIRCULO = 200;

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				if (jugador.isOnPortalCooldown() || !jugador.isAlive()) continue;
				if (!tocaAgua(jugador)) continue;
				viajar(jugador);
			}
		});
	}

	private static boolean tocaAgua(ServerPlayer jugador) {
		AABB caja = jugador.getBoundingBox().inflate(0.02);
		for (BlockPos pos : BlockPos.betweenClosed(
				BlockPos.containing(caja.minX, caja.minY, caja.minZ),
				BlockPos.containing(caja.maxX, caja.maxY, caja.maxZ))) {
			if (jugador.level().getBlockState(pos).is(ModBloques.AGUA_PORTAL)) return true;
		}
		return false;
	}

	private static void viajar(ServerPlayer jugador) {
		ServerLevel desde = jugador.serverLevel();
		ServerLevel destino = desde.dimension().equals(DIMENSION_NUEVA)
				? desde.getServer().overworld()
				: desde.getServer().getLevel(DIMENSION_NUEVA);
		if (destino == null) return;

		BlockPos llegada = lugarSeguro(destino, jugador.blockPosition());
		if (destino.dimension().equals(DIMENSION_NUEVA)) asegurarCirculo(destino, llegada);
		jugador.setPortalCooldown(ESPERA);
		jugador.teleportTo(destino, llegada.getX() + 0.5, llegada.getY(), llegada.getZ() + 0.5,
				jugador.getYRot(), jugador.getXRot());
		jugador.setPortalCooldown(ESPERA);
		destino.playSound(null, llegada, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.7f);
	}

	/**
	 * El círculo del cielo: 20 bloques de ancho de Agua Rara, flotando alto. Se arma la primera vez
	 * que alguien llega a esa zona, así siempre hay por dónde volver. Sólo se mira el lugar del
	 * círculo (no toda la zona) para no trabar el viaje cargando trozos de mundo de más.
	 */
	private static void asegurarCirculo(ServerLevel mundo, BlockPos llegada) {
		BlockPos centro = new BlockPos(llegada.getX(), ALTURA_CIRCULO, llegada.getZ());
		if (mundo.getBlockState(centro).is(ModBloques.AGUA_PORTAL)) return;   // ya está
		int radio = 10;
		for (int dx = -radio; dx <= radio; dx++) {
			for (int dz = -radio; dz <= radio; dz++) {
				if (dx * dx + dz * dz > radio * radio) continue;
				BlockPos pos = centro.offset(dx, 0, dz);
				if (mundo.getBlockState(pos).isAir()) {
					mundo.setBlock(pos, ModBloques.AGUA_PORTAL.defaultBlockState(), 2);
				}
			}
		}
	}

	/** Busca el piso en el mismo X/Z, para no aparecer dentro de la tierra ni colgado en el aire. */
	private static BlockPos lugarSeguro(ServerLevel destino, BlockPos desde) {
		BlockPos columna = new BlockPos(desde.getX(), 0, desde.getZ());
		BlockPos arriba = destino.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, columna);
		return arriba.getY() <= destino.getMinBuildHeight() + 1 ? columna.atY(100) : arriba;
	}
}
