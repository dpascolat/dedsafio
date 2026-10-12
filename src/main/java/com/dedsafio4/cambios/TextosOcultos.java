package com.dedsafio4.cambios;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
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
import java.util.stream.Stream;

/**
 * /textocambio <cambio o mob> desactivar: el anuncio que sale en el chat (y arriba a la izquierda) al usar /cambio,
 * /dificultadcambio o /mob con eso ya no aparece; "activar" lo vuelve a mostrar. Se guarda en el mundo.
 * El cambio se escribe con su nombre interno (creepers_electricos, arboles...) y el mob como en /mob (walker...).
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

	/** ¿El anuncio de este cambio (su id) o de este mob (su nombre en /mob) está escondido? */
	public static boolean oculto(MinecraftServer server, String nombre) {
		return datos(server).ocultos.contains(nombre);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		var nombre = Commands.argument("nombre", StringArgumentType.word())
				.suggests((c, b) -> SharedSuggestionProvider.suggest(Stream.concat(
						Cambios.TODOS.stream().map(Cambios.Cambio::id),
						com.dedsafio4.bestias.AnuncioMob.LUGARES.keySet().stream()), b));
		dispatcher.register(Commands.literal("textocambio").requires(s -> s.hasPermission(2))
				.then(nombre
						.then(Commands.literal("desactivar").executes(c -> cambiar(c, true)))
						.then(Commands.literal("activar").executes(c -> cambiar(c, false)))));
	}

	/** desactivar = el texto no aparece; activar = vuelve a aparecer. */
	private static int cambiar(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, boolean esconder) {
		String nombre = StringArgumentType.getString(c, "nombre").toLowerCase(java.util.Locale.ROOT);
		boolean existe = Cambios.TODOS.stream().anyMatch(x -> x.id().equals(nombre))
				|| com.dedsafio4.bestias.AnuncioMob.LUGARES.containsKey(nombre);
		if (!existe) {
			c.getSource().sendFailure(Component.literal("No existe el cambio o mob \"" + nombre + "\"."));
			return 0;
		}
		Datos d = datos(c.getSource().getServer());
		if (esconder) d.ocultos.add(nombre);
		else d.ocultos.remove(nombre);
		d.setDirty();
		c.getSource().sendSuccess(() -> Component.literal(esconder
				? "El texto de " + nombre + " está desactivado (no va a aparecer)."
				: "El texto de " + nombre + " está activado (va a aparecer).").withStyle(ChatFormatting.AQUA), true);
		return 1;
	}
}
