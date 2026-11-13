package com.dedsafio4.portal;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/**
 * A dónde lleva el Portal del Espacio: /spawn_del_espacio2 &lt;x y z&gt; lo marca (en la dimensión donde está el que lo
 * escribe). /spawn_del_espacio2 ver dice dónde es y /spawn_del_espacio2 quitar lo saca (el portal no lleva a ningún lado).
 */
public final class DestinoPortal {
	private DestinoPortal() {}

	private static final int VIOLETA = 0xB58CFF;

	public record Destino(ResourceKey<Level> mundo, double x, double y, double z) {}

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean hay;
		String dimension = "";
		double x, y, z;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.hay = tag.getBoolean("hay");
			d.dimension = tag.getString("dimension");
			d.x = tag.getDouble("x");
			d.y = tag.getDouble("y");
			d.z = tag.getDouble("z");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("hay", hay);
			tag.putString("dimension", dimension);
			tag.putDouble("x", x);
			tag.putDouble("y", y);
			tag.putDouble("z", z);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_destino_portal");
	}

	/** El destino, o null si no hay. */
	public static Destino destino(MinecraftServer server) {
		Datos d = datos(server);
		if (!d.hay) return null;
		ResourceLocation id = ResourceLocation.tryParse(d.dimension);
		if (id == null) return null;
		return new Destino(ResourceKey.create(Registries.DIMENSION, id), d.x, d.y, d.z);
	}

	private static String nombre(String dimension) {
		if (dimension.equals(com.dedsafio4.despegue.ModDespegue.ESPACIO.location().toString())) return "el Espacio";
		if (dimension.equals(com.dedsafio4.dimension.EspacioInterdimensional.DIMENSION.location().toString())) return "el Espacio Interdimensional";
		if (dimension.equals("minecraft:overworld")) return "el mundo normal";
		return dimension;
	}

	private static String numero(double v) {
		return v == Math.floor(v) ? String.valueOf((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("spawn_del_espacio2").requires(s -> s.hasPermission(2))
				.then(Commands.literal("ver").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal(d.hay
							? "El Portal del Espacio lleva a " + numero(d.x) + " " + numero(d.y) + " " + numero(d.z) + " en " + nombre(d.dimension) + "."
							: "El Portal del Espacio no tiene destino.").withColor(VIOLETA), false);
					return d.hay ? 1 : 0;
				}))
				.then(Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.hay = false;
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Listo: el Portal del Espacio ya no lleva a ningún lado.").withColor(VIOLETA), true);
					return 1;
				}))
				.then(Commands.argument("lugar", Vec3Argument.vec3()).executes(c -> {
					Vec3 lugar = Vec3Argument.getVec3(c, "lugar");
					Datos d = datos(c.getSource().getServer());
					d.hay = true;
					d.dimension = c.getSource().getLevel().dimension().location().toString();
					d.x = lugar.x;
					d.y = lugar.y;
					d.z = lugar.z;
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Listo: el que entre al Portal del Espacio aparece en " + numero(d.x) + " "
							+ numero(d.y) + " " + numero(d.z) + " en " + nombre(d.dimension) + ".").withColor(VIOLETA), true);
					return 1;
				})));
	}
}
