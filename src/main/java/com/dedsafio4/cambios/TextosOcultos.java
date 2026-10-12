package com.dedsafio4.cambios;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * /textocambio mob desactivar: los anuncios de /mob (Nuevo Mob) ya no aparecen, de ningún mob.
 * /textocambio cambio desactivar: los anuncios de /cambio y /dificultadcambio (Cambio de Dificultad) ya no aparecen.
 * Con "activar" vuelven a aparecer. Se guarda en el mundo.
 */
public final class TextosOcultos {
	private TextosOcultos() {}

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		final Set<String> ocultos = new HashSet<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (Tag t : tag.getList("ocultos", Tag.TAG_STRING)) d.ocultos.add(t.getAsString());
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag l = new ListTag();
			for (String s : ocultos) l.add(StringTag.valueOf(s));
			tag.put("ocultos", l);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_textos_ocultos");
	}

	/** ¿Están escondidos los anuncios de este tipo ("mob" o "cambio")? */
	public static boolean oculto(MinecraftServer server, String tipo) {
		return datos(server).ocultos.contains(tipo);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		var raiz = Commands.literal("textocambio").requires(s -> s.hasPermission(2));
		for (String tipo : new String[]{"mob", "cambio"}) {
			raiz.then(Commands.literal(tipo)
					.then(Commands.literal("desactivar").executes(c -> cambiar(c, tipo, true)))
					.then(Commands.literal("activar").executes(c -> cambiar(c, tipo, false))));
		}
		dispatcher.register(raiz);
	}

	private static int cambiar(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, String tipo, boolean esconder) {
		Datos d = datos(c.getSource().getServer());
		if (esconder) d.ocultos.add(tipo);
		else d.ocultos.remove(tipo);
		d.setDirty();
		String que = tipo.equals("mob") ? "Los textos de los mobs" : "Los textos de los cambios";
		c.getSource().sendSuccess(() -> Component.literal(que + (esconder ? " están desactivados (no van a aparecer)."
				: " están activados (van a aparecer).")).withStyle(ChatFormatting.AQUA), true);
		return 1;
	}
}
