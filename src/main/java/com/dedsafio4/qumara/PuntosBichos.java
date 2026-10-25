package com.dedsafio4.qumara;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.TreeMap;

/**
 * Lugares donde aparece solo el Bicho no de la cabeza (el que suelta 2 Pimpollos de Qumara al morir), y si los
 * Capullos están siempre abiertos. Se guarda con el mundo. Comandos (admins):
 *   /spawn_eggs <x y z> <número>   pone (o mueve) el lugar con ese número: ahí aparece uno ya, y después cada 10
 *                                  minutos aparece otro, salvo que todavía haya uno ahí;
 *   /spawn_eggs quitar <número>    saca ese lugar;   /spawn_eggs lista   los muestra.
 *   /capullos abiertos | normal    los Capullos siempre abiertos, o como siempre (se cierran en el Enfriamiento).
 */
public final class PuntosBichos {
	private PuntosBichos() {}

	/** Cada cuánto (10 minutos) y a cuántos bloques del lugar cuenta que "ya hay uno". */
	private static final int CADA = 10 * 60 * 20;
	private static final double CERCA = 8;

	private record Punto(ResourceKey<Level> mundo, BlockPos pos, long ultimo) {}

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		final Map<Integer, Punto> puntos = new TreeMap<>();
		boolean capullosAbiertos;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			CompoundTag p = tag.getCompound("puntos");
			for (String k : p.getAllKeys()) {
				CompoundTag t = p.getCompound(k);
				ResourceKey<Level> mundo = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("mundo")));
				d.puntos.put(Integer.parseInt(k), new Punto(mundo, BlockPos.of(t.getLong("pos")), t.getLong("ultimo")));
			}
			d.capullosAbiertos = tag.getBoolean("capullosAbiertos");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			CompoundTag p = new CompoundTag();
			puntos.forEach((n, punto) -> {
				CompoundTag t = new CompoundTag();
				t.putString("mundo", punto.mundo().location().toString());
				t.putLong("pos", punto.pos().asLong());
				t.putLong("ultimo", punto.ultimo());
				p.put(String.valueOf(n), t);
			});
			tag.put("puntos", p);
			tag.putBoolean("capullosAbiertos", capullosAbiertos);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_puntos_bichos");
	}

	/** ¿Están los Capullos siempre abiertos (/capullos abiertos)? */
	public static boolean capullosAbiertos(MinecraftServer server) {
		return datos(server).capullosAbiertos;
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 != 0) return;
			Datos d = datos(server);
			for (var e : d.puntos.entrySet()) {
				Punto p = e.getValue();
				ServerLevel mundo = server.getLevel(p.mundo());
				long ahora = server.overworld().getGameTime();
				if (mundo == null || ahora - p.ultimo() < CADA || !mundo.isLoaded(p.pos())) continue;
				d.puntos.put(e.getKey(), new Punto(p.mundo(), p.pos(), ahora));
				d.setDirty();
				aparecer(mundo, p.pos());
			}
		});
	}

	/** Un Bicho no de la cabeza en ese lugar, salvo que ya haya uno. */
	private static void aparecer(ServerLevel mundo, BlockPos pos) {
		if (!mundo.getEntitiesOfClass(BichoCuboEntity.class, new AABB(pos).inflate(CERCA), b -> !b.deLaCabeza() && b.isAlive()).isEmpty()) return;
		BichoCuboEntity bicho = ModQumara.BICHO_NO_CABEZA.create(mundo);
		if (bicho == null) return;
		bicho.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, mundo.random.nextFloat() * 360, 0);
		mundo.addFreshEntity(bicho);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("spawn_eggs").requires(s -> s.hasPermission(2))
				.then(Commands.literal("lista").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					if (d.puntos.isEmpty()) {
						c.getSource().sendSuccess(() -> Component.literal("No hay lugares puestos.").withStyle(ChatFormatting.GRAY), false);
						return 0;
					}
					d.puntos.forEach((n, p) -> c.getSource().sendSuccess(() -> Component.literal("#" + n + ": " + p.pos().getX() + " "
							+ p.pos().getY() + " " + p.pos().getZ() + " (" + p.mundo().location().getPath() + ")").withStyle(ChatFormatting.GOLD), false));
					return d.puntos.size();
				}))
				.then(Commands.literal("quitar").then(Commands.argument("numero", IntegerArgumentType.integer(1)).executes(c -> {
					int n = IntegerArgumentType.getInteger(c, "numero");
					Datos d = datos(c.getSource().getServer());
					if (d.puntos.remove(n) == null) {
						c.getSource().sendFailure(Component.literal("No hay un lugar #" + n + "."));
						return 0;
					}
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Listo: se sacó el lugar #" + n + ".").withStyle(ChatFormatting.GOLD), true);
					return 1;
				})))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("numero", IntegerArgumentType.integer(1)).executes(c -> {
							BlockPos pos = BlockPosArgument.getBlockPos(c, "pos");
							int n = IntegerArgumentType.getInteger(c, "numero");
							ServerLevel mundo = c.getSource().getLevel();
							Datos d = datos(c.getSource().getServer());
							d.puntos.put(n, new Punto(mundo.dimension(), pos.immutable(), c.getSource().getServer().overworld().getGameTime()));
							d.setDirty();
							aparecer(mundo, pos);
							c.getSource().sendSuccess(() -> Component.literal("Listo: en el lugar #" + n + " (" + pos.getX() + " " + pos.getY() + " "
									+ pos.getZ() + ") aparece un Bicho no de la cabeza cada 10 minutos (si no hay uno).").withStyle(ChatFormatting.GOLD), true);
							return 1;
						}))));
		dispatcher.register(Commands.literal("capullos").requires(s -> s.hasPermission(2))
				.then(Commands.literal("abiertos").executes(c -> capullos(c.getSource(), true)))
				.then(Commands.literal("normal").executes(c -> capullos(c.getSource(), false))));
	}

	private static int capullos(CommandSourceStack fuente, boolean abiertos) {
		Datos d = datos(fuente.getServer());
		d.capullosAbiertos = abiertos;
		d.setDirty();
		fuente.sendSuccess(() -> Component.literal(abiertos ? "Listo: los Capullos quedan siempre abiertos."
				: "Listo: los Capullos se cierran como siempre en el Enfriamiento de Qumara.").withStyle(ChatFormatting.GOLD), true);
		return 1;
	}
}
