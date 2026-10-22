package com.dedsafio4.bestias;

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
 * /zombies_normales desactivar: los zombis de Minecraft dejan de aparecer solos (SpawnPlacementsMixin), así quedan los
 * zombis nuevos del mod. /zombies_normales activar los vuelve a dejar aparecer. Queda guardado en el mundo.
 */
public final class ZombisNormales {
	private ZombisNormales() {}

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean desactivados;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.desactivados = tag.getBoolean("desactivados");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("desactivados", desactivados);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_zombis_normales");
	}

	/** ¿Pueden aparecer solos los zombis de Minecraft? */
	public static boolean activos(MinecraftServer server) {
		return server == null || !datos(server).desactivados;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("zombies_normales").requires(s -> s.hasPermission(2))
				.executes(c -> {
					boolean a = activos(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal(a ? "Los zombis de Minecraft aparecen."
							: "Los zombis de Minecraft no aparecen.").withStyle(ChatFormatting.GOLD), false);
					return 1;
				})
				.then(Commands.literal("activar").executes(c -> cambiar(c.getSource(), true)))
				.then(Commands.literal("desactivar").executes(c -> cambiar(c.getSource(), false))));
	}

	private static int cambiar(CommandSourceStack fuente, boolean activos) {
		Datos d = datos(fuente.getServer());
		d.desactivados = !activos;
		d.setDirty();
		fuente.sendSuccess(() -> Component.literal(activos ? "Listo: los zombis de Minecraft vuelven a aparecer."
				: "Listo: los zombis de Minecraft ya no aparecen solos.").withStyle(ChatFormatting.GOLD), true);
		return 1;
	}
}
