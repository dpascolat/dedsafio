package com.dedsafio4.despegue;

import com.dedsafio4.dimension.Organos;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

/**
 * El lugar de aterrizaje de Gleba (la Dimensión de los Órganos): un rectángulo que se marca con
 * /spawn_gleba <desde> <hasta>. Las Naves Biplaza que llegan a Gleba aterrizan adentro, ordenadas en lugares de 2 × 2
 * con un bloque libre entre una y otra; y ahí adentro también se puede poner una nave guardada (sin soporte).
 * /spawn_gleba quitar lo saca (las naves vuelven a aterrizar donde caigan) y /spawn_gleba ver dice dónde está.
 */
public final class ZonaGleba {
	private ZonaGleba() {}

	/** Cada lugar mide 2 de ancho (la nave) y deja 1 libre: de centro a centro, 3. */
	private static final int PASO = 3;

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean hay;
		int minX, minZ, maxX, maxZ;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.hay = tag.getBoolean("hay");
			d.minX = tag.getInt("minX");
			d.minZ = tag.getInt("minZ");
			d.maxX = tag.getInt("maxX");
			d.maxZ = tag.getInt("maxZ");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("hay", hay);
			tag.putInt("minX", minX);
			tag.putInt("minZ", minZ);
			tag.putInt("maxX", maxX);
			tag.putInt("maxZ", maxZ);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_zona_gleba");
	}

	/** ¿Está ese punto (x, z) de Gleba adentro del lugar de aterrizaje? */
	public static boolean dentro(Level level, double x, double z) {
		if (!(level instanceof ServerLevel mundo) || !mundo.dimension().equals(Organos.DIMENSION)) return false;
		Datos d = datos(mundo.getServer());
		return d.hay && x >= d.minX && x <= d.maxX + 1 && z >= d.minZ && z <= d.maxZ + 1;
	}

	/** ¿Hay otra nave a menos de un bloque de distancia de una nave puesta en (x, z)? */
	public static boolean naveCerca(Level level, double x, double z, Entity excepto) {
		AABB zona = new AABB(x - 2.9, level.getMinBuildHeight(), z - 2.9, x + 2.9, level.getMaxBuildHeight(), z + 2.9);
		return !level.getEntitiesOfClass(NaveViajeEntity.class, zona, n -> n != excepto && n.isAlive()).isEmpty();
	}

	/** El primer lugar libre del aterrizaje (centro de la nave, y = el piso), o null si no hay zona o está llena. */
	public static BlockPos lugarLibre(ServerLevel gleba, Entity excepto) {
		Datos d = datos(gleba.getServer());
		if (!d.hay) return null;
		for (int z = d.minZ + 1; z <= d.maxZ; z += PASO) {
			for (int x = d.minX + 1; x <= d.maxX; x += PASO) {
				// Libre: ninguna nave (alineada o no) a menos de un bloque de distancia.
				if (naveCerca(gleba, x, z, excepto)) continue;
				gleba.getChunk(x >> 4, z >> 4);   // que esté cargado para saber la altura del piso
				int y = gleba.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				return new BlockPos(x, y, z);
			}
		}
		return null;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("spawn_gleba").requires(s -> s.hasPermission(2))
				.then(Commands.literal("ver").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal(d.hay
							? "El aterrizaje de Gleba va de " + d.minX + " " + d.minZ + " a " + d.maxX + " " + d.maxZ + "."
							: "No hay lugar de aterrizaje en Gleba (las naves bajan donde caen).").withStyle(ChatFormatting.LIGHT_PURPLE), false);
					return d.hay ? 1 : 0;
				}))
				.then(Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.hay = false;
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Listo: ya no hay lugar de aterrizaje en Gleba.")
							.withStyle(ChatFormatting.LIGHT_PURPLE), true);
					return 1;
				}))
				.then(Commands.argument("desde", BlockPosArgument.blockPos())
						.then(Commands.argument("hasta", BlockPosArgument.blockPos()).executes(c -> {
							BlockPos a = BlockPosArgument.getBlockPos(c, "desde"), b = BlockPosArgument.getBlockPos(c, "hasta");
							Datos d = datos(c.getSource().getServer());
							d.hay = true;
							d.minX = Math.min(a.getX(), b.getX());
							d.maxX = Math.max(a.getX(), b.getX());
							d.minZ = Math.min(a.getZ(), b.getZ());
							d.maxZ = Math.max(a.getZ(), b.getZ());
							d.setDirty();
							int lugares = Math.max(0, (d.maxX - d.minX - 1) / PASO + 1) * Math.max(0, (d.maxZ - d.minZ - 1) / PASO + 1);
							c.getSource().sendSuccess(() -> Component.literal("Listo: las naves aterrizan en Gleba de " + d.minX + " " + d.minZ
									+ " a " + d.maxX + " " + d.maxZ + " (entran unas " + lugares + ", con un bloque entre cada una).")
									.withStyle(ChatFormatting.LIGHT_PURPLE), true);
							return 1;
						}))));
	}
}
