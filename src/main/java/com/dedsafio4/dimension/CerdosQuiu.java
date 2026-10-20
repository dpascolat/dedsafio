package com.dedsafio4.dimension;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Los cerdos en el Centro de Quiu: no aparecen solos (SpawnPlacementsMixin). Con /cerdos_quiu activar vuelven a
 * aparecer y con /cerdos_quiu desactivar dejan de aparecer otra vez. Queda guardado en el mundo.
 */
public final class CerdosQuiu {
	private CerdosQuiu() {}

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean activos;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.activos = tag.getBoolean("activos");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("activos", activos);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_cerdos_quiu");
	}

	/** ¿Pueden aparecer cerdos solos en el Centro de Quiu? */
	public static boolean activos(MinecraftServer server) {
		return server != null && datos(server).activos;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cerdos_quiu").requires(s -> s.hasPermission(2))
				.executes(c -> {
					boolean a = activos(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal(a ? "Los cerdos aparecen en el Centro de Quiu."
							: "Los cerdos no aparecen en el Centro de Quiu.").withStyle(ChatFormatting.GOLD), false);
					return 1;
				})
				.then(Commands.literal("activar").executes(c -> cambiar(c.getSource(), true)))
				.then(Commands.literal("desactivar").executes(c -> cambiar(c.getSource(), false))));
	}

	private static int cambiar(CommandSourceStack fuente, boolean activos) {
		Datos d = datos(fuente.getServer());
		d.activos = activos;
		d.setDirty();
		fuente.sendSuccess(() -> Component.literal(activos ? "Listo: los cerdos vuelven a aparecer en el Centro de Quiu."
				: "Listo: los cerdos ya no aparecen en el Centro de Quiu.").withStyle(ChatFormatting.GOLD), true);
		return 1;
	}
}
