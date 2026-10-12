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
	/** Cuántos bloques arriba del lugar de llegada queda el círculo de vuelta (debajo del techo de roca). */
	private static final int ALTURA_CIRCULO = 20;

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

		// Al Centro de Quiu: si se eligió un lugar con /quiu, se llega ahí.
		BlockPos fijo = destino.dimension().equals(DIMENSION_NUEVA) ? datos(desde.getServer()).lugar : null;
		BlockPos llegada = fijo != null ? fijo : lugarSeguro(destino, jugador.blockPosition());
		// El círculo de vuelta sólo cuando se cae donde sea (en el lugar de /quiu, la vuelta la arma el que lo eligió).
		if (destino.dimension().equals(DIMENSION_NUEVA) && fijo == null) asegurarCirculo(destino, llegada);
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
		BlockPos centro = llegada.above(ALTURA_CIRCULO);
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

	// ---------------------------------------------------------------- /quiu

	/** El lugar de llegada al Centro de Quiu elegido con /quiu (null = donde caiga, como siempre). */
	private static final class Datos extends net.minecraft.world.level.saveddata.SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		BlockPos lugar;

		private static Datos leer(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("x")) d.lugar = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
			return d;
		}

		@Override
		public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			if (lugar != null) {
				tag.putInt("x", lugar.getX());
				tag.putInt("y", lugar.getY());
				tag.putInt("z", lugar.getZ());
			}
			return tag;
		}
	}

	private static Datos datos(net.minecraft.server.MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_quiu_llegada");
	}

	/**
	 * /quiu <x y z>   al entrar al Agua Rara desde el Overworld se llega a ese lugar del Centro de Quiu
	 * /quiu quitar    se vuelve a llegar como siempre (en el mismo X/Z, sobre el piso)
	 * /quiu           muestra el lugar elegido
	 */
	public static void registrarComandos(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
		dispatcher.register(net.minecraft.commands.Commands.literal("quiu").requires(s -> s.hasPermission(2))
				.executes(c -> {
					BlockPos l = datos(c.getSource().getServer()).lugar;
					c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(l == null
							? "No hay lugar elegido: al entrar al Agua Rara se llega sobre el piso, en el mismo lugar."
							: "Al entrar al Agua Rara se llega a " + l.getX() + " " + l.getY() + " " + l.getZ() + " del Centro de Quiu.")
							.withStyle(net.minecraft.ChatFormatting.AQUA), false);
					return 1;
				})
				.then(net.minecraft.commands.Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.lugar = null;
					d.setDirty();
					c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
							"Listo: al entrar al Agua Rara se llega como siempre.").withStyle(net.minecraft.ChatFormatting.AQUA), true);
					return 1;
				}))
				.then(net.minecraft.commands.Commands.argument("lugar", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
						.executes(c -> {
							BlockPos l = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "lugar");
							Datos d = datos(c.getSource().getServer());
							d.lugar = l.immutable();
							d.setDirty();
							c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Listo: al entrar al Agua Rara se llega a "
									+ l.getX() + " " + l.getY() + " " + l.getZ() + " del Centro de Quiu.").withStyle(net.minecraft.ChatFormatting.AQUA), true);
							return 1;
						})));
	}

	/** Busca el piso en el mismo X/Z, para no aparecer dentro de la tierra ni colgado en el aire. */
	public static BlockPos lugarSeguro(ServerLevel destino, BlockPos desde) {
		BlockPos columna = new BlockPos(desde.getX(), 0, desde.getZ());
		// El Centro de Quiu tiene techo: lo más alto es la roca de arriba, así que se baja hasta el primer piso de verdad.
		if (destino.dimension().equals(DIMENSION_NUEVA)) {
			for (int y = destino.getMaxBuildHeight() - 2; y > destino.getMinBuildHeight() + 1; y--) {
				BlockPos pos = columna.atY(y);
				var abajo = destino.getBlockState(pos.below());
				if (destino.getBlockState(pos).isAir() && destino.getBlockState(pos.above()).isAir()
						&& abajo.blocksMotion() && !abajo.is(ModBloques.ROCA_TECHO) && !abajo.is(ModBloques.TIERRA_TECHO)) return pos;
			}
			return columna.atY(100);
		}
		BlockPos arriba = destino.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, columna);
		return arriba.getY() <= destino.getMinBuildHeight() + 1 ? columna.atY(100) : arriba;
	}
}
