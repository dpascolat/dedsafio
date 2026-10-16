package com.dedsafio4.dimension;

import com.mojang.brigadier.CommandDispatcher;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /quiu_reload eliminar: borra el terreno de Quiu y al entrar se genera de nuevo (como recién creado). Saca a todos
 * de Quiu, espera a que se descargue el terreno y anota el momento del reinicio. Después, los chunks de Quiu
 * guardados antes de ese momento se tiran al cargarlos (ChunkMapReinicioMixin) y se generan de nuevo, y los bichos
 * y cosas que había en ellos también (EntityStorageReinicioMixin).
 */
public final class ReinicioQuiu {
	private ReinicioQuiu() {}

	/** Si en 1 minuto no se descargó todo, se reinicia igual. */
	private static final int ESPERA_MAXIMA = 20 * 60;

	static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		long reinicio = -1;
		final LongOpenHashSet pendientes = new LongOpenHashSet();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.reinicio = tag.getLong("reinicio");
			for (long c : tag.getLongArray("pendientes")) d.pendientes.add(c);
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putLong("reinicio", reinicio);
			tag.put("pendientes", new LongArrayTag(new ArrayList<>(PENDIENTES)));
			return tag;
		}
	}

	// Copia de los datos que se puede leer desde los hilos que cargan chunks.
	private static MinecraftServer servidor;
	private static Datos datos;
	private static volatile long reinicio = -1;
	/** Chunks que se volvieron a generar y todavía tienen guardados los bichos viejos. */
	private static final Set<Long> PENDIENTES = ConcurrentHashMap.newKeySet();

	/** Mientras se espera a que se descargue Quiu: ticks esperando (-1 = no se está reiniciando). */
	private static int esperando = -1;
	private static CommandSourceStack quien;

	/** Carga los datos del mundo (en el hilo del servidor). */
	public static void asegurar(MinecraftServer server) {
		if (server == null || servidor == server) return;
		datos = server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_reinicio_quiu");
		reinicio = datos.reinicio;
		PENDIENTES.clear();
		PENDIENTES.addAll(datos.pendientes);
		esperando = -1;
		servidor = server;
	}

	private static boolean esQuiu(ServerLevel level) {
		return level.dimension() == Portales.DIMENSION_NUEVA;
	}

	/** ¿Este chunk de Quiu se guardó antes del reinicio? Entonces no se usa y se genera de nuevo. */
	public static boolean viejo(ServerLevel level, ChunkPos pos, CompoundTag tag) {
		long momento = reinicio;
		if (momento < 0 || !esQuiu(level) || tag.getLong("LastUpdate") >= momento) return false;
		PENDIENTES.add(pos.toLong());
		if (datos != null) datos.setDirty();
		return true;
	}

	/** ¿Los bichos guardados en este chunk son de antes del reinicio? Entonces no se cargan. */
	public static boolean sinEntidades(ServerLevel level, ChunkPos pos) {
		if (!esQuiu(level)) return false;
		asegurar(level.getServer());
		if (!PENDIENTES.remove(pos.toLong())) return false;
		datos.setDirty();
		return true;
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(ReinicioQuiu::tick);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("quiu_reload").requires(s -> s.hasPermission(2))
				.then(Commands.literal("eliminar").executes(c -> empezar(c.getSource()))));
	}

	private static int empezar(CommandSourceStack fuente) {
		MinecraftServer server = fuente.getServer();
		asegurar(server);
		if (esperando >= 0) {
			fuente.sendFailure(Component.literal("Quiu ya se está reiniciando."));
			return 0;
		}
		ServerLevel quiu = server.getLevel(Portales.DIMENSION_NUEVA);
		if (quiu == null) {
			fuente.sendFailure(Component.literal("No se encontró el mundo de Quiu."));
			return 0;
		}
		// Los chunks cargados a la fuerza (forceload) también tienen que descargarse.
		for (long c : new ArrayList<>(quiu.getForcedChunks())) {
			quiu.setChunkForced(ChunkPos.getX(c), ChunkPos.getZ(c), false);
		}
		sacarJugadores(server, quiu);
		esperando = 0;
		quien = fuente;
		fuente.sendSuccess(() -> Component.literal("Reiniciando Quiu: sacando a todos y descargando el terreno...")
				.withStyle(ChatFormatting.GOLD), true);
		return 1;
	}

	/** Los que están en Quiu vuelven al punto de aparición del mundo normal. */
	private static void sacarJugadores(MinecraftServer server, ServerLevel quiu) {
		ServerLevel mundo = server.overworld();
		BlockPos spawn = mundo.getSharedSpawnPos();
		for (ServerPlayer jugador : new ArrayList<>(quiu.players())) {
			int y = mundo.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
			jugador.teleportTo(mundo, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, jugador.getYRot(), jugador.getXRot());
			jugador.fallDistance = 0;
			jugador.sendSystemMessage(Component.literal("Quiu se está reiniciando: te sacamos de ahí.").withStyle(ChatFormatting.GOLD));
		}
	}

	private static void tick(MinecraftServer server) {
		if (esperando < 0 || servidor != server) return;
		ServerLevel quiu = server.getLevel(Portales.DIMENSION_NUEVA);
		if (quiu == null) {
			esperando = -1;
			return;
		}
		esperando++;
		if (!quiu.players().isEmpty()) sacarJugadores(server, quiu);
		// Hay que esperar también a que termine de guardar lo que se está descargando (si no, se guarda después
		// del reinicio y queda como nuevo).
		int cargados = quiu.getChunkSource().getLoadedChunksCount();
		boolean trabajando = cargados > 0 || quiu.getChunkSource().chunkMap.hasWork();
		if (trabajando && esperando < ESPERA_MAXIMA) return;
		// Todo lo que se guardó hasta ahora es de antes: desde el tick que viene, lo nuevo vale.
		reinicio = quiu.getGameTime() + 1;
		datos.reinicio = reinicio;
		datos.setDirty();
		esperando = -1;
		Component aviso = Component.literal("Quiu se reinició: cuando entres se genera terreno nuevo.").withStyle(ChatFormatting.GREEN);
		server.getPlayerList().broadcastSystemMessage(aviso, false);
		if (trabajando && quien != null) {
			quien.sendSystemMessage(Component.literal("Ojo: " + cargados + " partes de Quiu seguían cargadas y pueden quedar como antes.")
					.withStyle(ChatFormatting.YELLOW));
		}
		quien = null;
	}
}
