package com.dedsafio4.bossplanta;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * El daño de los ataques de la Planta Mordedora (queda guardado en el mundo):
 * <ul>
 * <li>/boss 2 daño luz &lt;daño&gt;: lo que saca la Luz cada medio segundo al que la mira (normal: 4 = 2 corazones).</li>
 * <li>/boss 2 daño mordida &lt;daño&gt;: lo que saca cada una de las 4 mordidas. Normal: un cuarto de la vida cada una
 *     y la cuarta mata; con un número, las 4 sacan ese daño (y la cuarta ya no mata sola).</li>
 * <li>/boss 2 daño luz normal | mordida normal: vuelve a ser como era. Sin número dice cuánto está.</li>
 * </ul>
 * El daño va en medios corazones, como en Minecraft (2 = 1 corazón). Las mordidas no miran la armadura.
 */
public final class DanioJefe {
	private DanioJefe() {}

	public static final float LUZ_NORMAL = 4f;

	public static final class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		/** Negativo = normal. */
		float luz = -1, mordida = -1;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("luz")) d.luz = tag.getFloat("luz");
			if (tag.contains("mordida")) d.mordida = tag.getFloat("mordida");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putFloat("luz", luz);
			tag.putFloat("mordida", mordida);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer servidor) {
		return servidor.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_danio_jefe");
	}

	/** Lo que saca la Luz cada vez. */
	public static float luz(MinecraftServer servidor) {
		float v = datos(servidor).luz;
		return v < 0 ? LUZ_NORMAL : v;
	}

	/** Lo que saca cada mordida, o -1 si es la normal (un cuarto de la vida, y la cuarta mata). */
	public static float mordida(MinecraftServer servidor) {
		return datos(servidor).mordida;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		for (String palabra : new String[]{"daño", "dano"}) {
			LiteralArgumentBuilder<CommandSourceStack> danio = Commands.literal(palabra);
			for (String ataque : new String[]{"luz", "mordida"}) {
				danio.then(Commands.literal(ataque)
						.executes(c -> decir(c.getSource(), ataque, false))
						.then(Commands.literal("normal").executes(c -> cambiar(c.getSource(), ataque, -1)))
						.then(Commands.argument("dano", FloatArgumentType.floatArg(0, 10000))
								.executes(c -> cambiar(c.getSource(), ataque, FloatArgumentType.getFloat(c, "dano")))));
			}
			dispatcher.register(Commands.literal("boss").requires(s -> s.hasPermission(2)).then(Commands.literal("2").then(danio)));
		}
	}

	private static int cambiar(CommandSourceStack fuente, String ataque, float valor) {
		Datos d = datos(fuente.getServer());
		if (ataque.equals("luz")) d.luz = valor;
		else d.mordida = valor;
		d.setDirty();
		return decir(fuente, ataque, true);
	}

	private static int decir(CommandSourceStack fuente, String ataque, boolean atodos) {
		String texto;
		if (ataque.equals("luz")) {
			float v = luz(fuente.getServer());
			texto = "La Luz de la Planta Mordedora saca " + corazones(v) + " cada medio segundo al que la mira.";
		} else {
			float v = mordida(fuente.getServer());
			texto = v < 0 ? "Las mordidas de la Planta Mordedora son las normales: un cuarto de la vida cada una, y la cuarta mata."
					: "Cada mordida de la Planta Mordedora saca " + corazones(v) + " (son 4, sin mirar la armadura).";
		}
		fuente.sendSuccess(() -> Component.literal(texto).withStyle(ChatFormatting.LIGHT_PURPLE), atodos);
		return 1;
	}

	private static String corazones(float v) {
		float c = v / 2;
		String n = c == Math.floor(c) ? String.valueOf((int) c) : String.valueOf(c);
		return n + (c == 1 ? " corazón" : " corazones");
	}
}
