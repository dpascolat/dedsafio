package com.dedsafio4.bestias;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * /tiempito &lt;segundos&gt;: cuánto tarda en explotar el Núcleo después de matarlo (lo que tienen para escapar de su
 * radio). /tiempito dice cuánto está puesto y /tiempito normal lo vuelve a 10 segundos. Queda guardado en el mundo.
 */
public final class Tiempito {
	private Tiempito() {}

	private static final int NARANJA = 0xFF7A3D;

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		int segundos = NucleoEntity.ESCAPE / 20;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("segundos")) d.segundos = tag.getInt("segundos");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putInt("segundos", segundos);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_tiempito");
	}

	/** La cuenta para escapar, en ticks. */
	public static int ticks(MinecraftServer server) {
		return datos(server).segundos * 20;
	}

	private static int poner(CommandSourceStack fuente, int segundos) {
		Datos d = datos(fuente.getServer());
		d.segundos = segundos;
		d.setDirty();
		fuente.sendSuccess(() -> Component.literal("Listo: el Núcleo explota " + segundos + " segundos después de que lo maten.")
				.withColor(NARANJA), true);
		return segundos;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tiempito").requires(s -> s.hasPermission(2))
				.executes(c -> {
					int s = datos(c.getSource().getServer()).segundos;
					c.getSource().sendSuccess(() -> Component.literal("El Núcleo explota " + s + " segundos después de que lo maten.")
							.withColor(NARANJA), false);
					return s;
				})
				.then(Commands.literal("normal").executes(c -> poner(c.getSource(), NucleoEntity.ESCAPE / 20)))
				.then(Commands.argument("segundos", IntegerArgumentType.integer(1, 3600))
						.executes(c -> poner(c.getSource(), IntegerArgumentType.getInteger(c, "segundos")))));
	}
}
