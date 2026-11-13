package com.dedsafio4.brazalete;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * El spawn del Espacio: un rectángulo (de arriba a abajo, todas las alturas) que se marca con
 * /spawn_de_espacio &lt;desde&gt; &lt;hasta&gt; en la dimensión donde está el que lo escribe. Adentro hay aire: aunque no tengas
 * el Traje Espacial puesto, no te ahogas (ver RespiracionEspacio).
 * /spawn_de_espacio ver dice dónde está y /spawn_de_espacio quitar lo saca.
 */
public final class ZonaEspacio {
	private ZonaEspacio() {}

	private static final int VIOLETA = 0xB58CFF;

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean hay;
		String dimension = "";
		int minX, minZ, maxX, maxZ;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.hay = tag.getBoolean("hay");
			d.dimension = tag.getString("dimension");
			d.minX = tag.getInt("minX");
			d.minZ = tag.getInt("minZ");
			d.maxX = tag.getInt("maxX");
			d.maxZ = tag.getInt("maxZ");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("hay", hay);
			tag.putString("dimension", dimension);
			tag.putInt("minX", minX);
			tag.putInt("minZ", minZ);
			tag.putInt("maxX", maxX);
			tag.putInt("maxZ", maxZ);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_spawn_espacio");
	}

	/** ¿Está este jugador adentro del spawn del Espacio? */
	public static boolean dentro(ServerPlayer jugador) {
		Datos d = datos(jugador.server);
		if (!d.hay || !jugador.level().dimension().location().toString().equals(d.dimension)) return false;
		double x = jugador.getX(), z = jugador.getZ();
		return x >= d.minX && x <= d.maxX + 1 && z >= d.minZ && z <= d.maxZ + 1;
	}

	private static String nombre(String dimension) {
		ResourceKey<Level> clave = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension));
		if (clave.equals(com.dedsafio4.despegue.ModDespegue.ESPACIO)) return "el Espacio";
		if (clave.equals(com.dedsafio4.dimension.EspacioInterdimensional.DIMENSION)) return "el Espacio Interdimensional";
		return dimension;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("spawn_de_espacio").requires(s -> s.hasPermission(2))
				.then(Commands.literal("ver").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal(d.hay
							? "El spawn del Espacio va de " + d.minX + " " + d.minZ + " a " + d.maxX + " " + d.maxZ + " en " + nombre(d.dimension)
							+ ": ahí adentro no te ahogas sin traje."
							: "No hay spawn del Espacio marcado.").withColor(VIOLETA), false);
					return d.hay ? 1 : 0;
				}))
				.then(Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.hay = false;
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Listo: ya no hay spawn del Espacio (sin traje te ahogas en todos lados).")
							.withColor(VIOLETA), true);
					return 1;
				}))
				.then(Commands.argument("desde", BlockPosArgument.blockPos())
						.then(Commands.argument("hasta", BlockPosArgument.blockPos()).executes(c -> {
							BlockPos a = BlockPosArgument.getBlockPos(c, "desde"), b = BlockPosArgument.getBlockPos(c, "hasta");
							Datos d = datos(c.getSource().getServer());
							d.hay = true;
							d.dimension = c.getSource().getLevel().dimension().location().toString();
							d.minX = Math.min(a.getX(), b.getX());
							d.maxX = Math.max(a.getX(), b.getX());
							d.minZ = Math.min(a.getZ(), b.getZ());
							d.maxZ = Math.max(a.getZ(), b.getZ());
							d.setDirty();
							String donde = nombre(d.dimension);
							c.getSource().sendSuccess(() -> Component.literal("Listo: el spawn del Espacio va de " + d.minX + " " + d.minZ
									+ " a " + d.maxX + " " + d.maxZ + " en " + donde + ". Ahí adentro no te ahogas sin traje.")
									.withColor(VIOLETA), true);
							if (!c.getSource().getLevel().dimension().equals(com.dedsafio4.despegue.ModDespegue.ESPACIO)
									&& !c.getSource().getLevel().dimension().equals(com.dedsafio4.dimension.EspacioInterdimensional.DIMENSION)) {
								c.getSource().sendSuccess(() -> Component.literal("Ojo: no estás en el Espacio. El spawn quedó en esta dimensión, "
										+ "donde igual se respira normal.").withStyle(ChatFormatting.GOLD), false);
							}
							return 1;
						}))));
	}
}
