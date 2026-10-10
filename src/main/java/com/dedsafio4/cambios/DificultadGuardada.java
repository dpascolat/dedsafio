package com.dedsafio4.cambios;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Guarda la dificultad (/difficulty) en el mundo. En un servidor, Minecraft la vuelve a poner como dice
 * server.properties cada vez que arranca; con esto, al arrancar se pone la última que se eligió.
 */
public final class DificultadGuardada {
	private DificultadGuardada() {}

	private static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		int dificultad = -1;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.dificultad = tag.contains("dificultad") ? tag.getInt("dificultad") : -1;
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putInt("dificultad", dificultad);
			return tag;
		}
	}

	/** El servidor en el que ya se puso la dificultad guardada (para hacerlo una sola vez al arrancar). */
	private static MinecraftServer aplicado;

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Datos d = server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_dificultad");
			int actual = server.getWorldData().getDifficulty().getId();
			if (aplicado != server) {
				aplicado = server;
				if (d.dificultad >= 0 && d.dificultad != actual) server.setDifficulty(Difficulty.byId(d.dificultad), true);
				return;
			}
			// Alguien la cambió: se anota para la próxima vez.
			if (d.dificultad != actual) {
				d.dificultad = actual;
				d.setDirty();
			}
		});
	}
}
