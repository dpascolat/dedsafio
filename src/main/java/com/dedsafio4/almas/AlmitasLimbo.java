package com.dedsafio4.almas;

import com.dedsafio4.bestias.AlmitaEntity;
import com.dedsafio4.dimension.Limbo;
import com.dedsafio4.nave.ModEntidades;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Las Almitas del Limbo: "/almitas <x y z>" elige el bloque del Limbo donde aparecen. Desde ahí, por cada jugador
 * conectado aparece una Almita con su nombre, que no desaparece (no se la puede lastimar y vuelve a su lugar si se
 * aleja). No aparecen las de los jugadores con la marca "rolplayer" (/tag) ni las de los op. Con el Recolector de Almas
 * lleno, clic derecho en tu propia Almita: recuperas tu alma para revivir. "/almitas quitar" saca el lugar y las Almitas.
 */
public final class AlmitasLimbo {
	private AlmitasLimbo() {}

	public static final String MARCA_ROLPLAYER = "rolplayer";

	static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		BlockPos lugar;
		/** De quiénes ya hay una Almita en el Limbo. */
		final Set<UUID> conAlmita = new HashSet<>();
		/** Los que ya recuperaron su alma con el Recolector: sólo se puede una vez, y ya no tienen Almita. */
		final Set<UUID> recuperaron = new HashSet<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("lugar")) d.lugar = NbtUtils.readBlockPos(tag, "lugar").orElse(null);
			for (Tag t : tag.getList("conAlmita", Tag.TAG_INT_ARRAY)) d.conAlmita.add(NbtUtils.loadUUID(t));
			for (Tag t : tag.getList("recuperaron", Tag.TAG_INT_ARRAY)) d.recuperaron.add(NbtUtils.loadUUID(t));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			if (lugar != null) tag.put("lugar", NbtUtils.writeBlockPos(lugar));
			ListTag l = new ListTag();
			for (UUID u : conAlmita) l.add(NbtUtils.createUUID(u));
			tag.put("conAlmita", l);
			ListTag r = new ListTag();
			for (UUID u : recuperaron) r.add(NbtUtils.createUUID(u));
			tag.put("recuperaron", r);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_almitas_limbo");
	}

	/** ¿A este jugador le toca Almita? (no a los op ni a los que tienen la marca rolplayer) */
	public static boolean leToca(ServerPlayer jugador) {
		return !jugador.getTags().contains(MARCA_ROLPLAYER) && !jugador.server.getPlayerList().isOp(jugador.getGameProfile());
	}

	/** ¿Ya recuperó su alma una vez? (no puede de nuevo) */
	public static boolean yaRecupero(ServerPlayer jugador) {
		return datos(jugador.server).recuperaron.contains(jugador.getUUID());
	}

	/** Recuperó su alma: no puede volver a hacerlo y su Almita no vuelve a aparecer. */
	public static void recupero(ServerPlayer jugador) {
		Datos d = datos(jugador.server);
		if (d.recuperaron.add(jugador.getUUID())) d.setDirty();
	}

	/** La Almita de ese jugador se fue para siempre (la mataron con /kill, por ejemplo): vuelve a aparecer. */
	public static void olvidar(MinecraftServer server, UUID dueno) {
		Datos d = datos(server);
		if (d.conAlmita.remove(dueno)) d.setDirty();
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 != 0) return;
			Datos d = datos(server);
			ServerLevel limbo = server.getLevel(Limbo.DIMENSION);
			if (d.lugar == null || limbo == null) return;
			// Si alguien pasó a ser op o se puso la marca rolplayer, su Almita se va.
			java.util.List<AlmitaEntity> almitas = new java.util.ArrayList<>();
			for (var e : limbo.getAllEntities()) if (e instanceof AlmitaEntity a && a.dueno() != null) almitas.add(a);
			for (AlmitaEntity a : almitas) {
				ServerPlayer duenio = server.getPlayerList().getPlayer(a.dueno());
				if (duenio != null && !leToca(duenio)) {
					a.discard();
					olvidar(server, a.dueno());
				}
			}
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				if (d.conAlmita.contains(jugador.getUUID()) || d.recuperaron.contains(jugador.getUUID()) || !leToca(jugador)) continue;
				limbo.getChunk(d.lugar);   // que esté cargado para poder ponerla
				AlmitaEntity almita = ModEntidades.ALMITA.spawn(limbo, d.lugar, MobSpawnType.COMMAND);
				if (almita == null) continue;
				almita.serDe(jugador, d.lugar);
				d.conAlmita.add(jugador.getUUID());
				d.setDirty();
			}
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("almitas").requires(s -> s.hasPermission(2))
				.then(Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.lugar = null;
					d.conAlmita.clear();
					d.setDirty();
					ServerLevel limbo = c.getSource().getServer().getLevel(Limbo.DIMENSION);
					int sacadas = 0;
					if (limbo != null) {
						java.util.List<AlmitaEntity> almitas = new java.util.ArrayList<>();
						for (var e : limbo.getAllEntities()) if (e instanceof AlmitaEntity a && a.dueno() != null) almitas.add(a);
						for (AlmitaEntity a : almitas) a.discard();
						sacadas = almitas.size();
					}
					int n = sacadas;
					c.getSource().sendSuccess(() -> Component.literal("Ya no aparecen Almitas en el Limbo (se sacaron " + n + ").")
							.withStyle(ChatFormatting.AQUA), true);
					return 1;
				}))
				.then(Commands.argument("lugar", BlockPosArgument.blockPos()).executes(c -> {
					BlockPos pos = BlockPosArgument.getBlockPos(c, "lugar");
					Datos d = datos(c.getSource().getServer());
					d.lugar = pos.immutable();
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Las Almitas aparecen en el Limbo en " + pos.getX() + " " + pos.getY() + " "
							+ pos.getZ() + ": una por cada jugador conectado (menos los op y los de /tag rolplayer).").withStyle(ChatFormatting.AQUA), true);
					return 1;
				})));
	}
}
