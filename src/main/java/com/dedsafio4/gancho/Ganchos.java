package com.dedsafio4.gancho;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cómo funciona el Gancho de Huesos (como el látigo de Indiana Jones): se tira la caña de pescar y, si la boya pasa
 * cerca de un gancho, se queda enganchada en él. Mientras está enganchada se ve una línea de partículas que muestra
 * hacia dónde vas a ir (en línea recta, no dónde vas a caer). Con click derecho (recoger la caña) el gancho te empuja
 * hacia adelante: pasas por debajo del gancho y sigues de largo, un poco para arriba, más o menos la cantidad de
 * bloques del gancho. /ganchito <distancia> da un gancho con ese impulso.
 */
public final class Ganchos {
	private Ganchos() {}

	private static final int DISTANCIA_MAXIMA = 100;
	private static final DustParticleOptions LINEA = new DustParticleOptions(new Vector3f(0.96f, 0.94f, 0.86f), 1.0f);
	private static final DustParticleOptions PUNTA = new DustParticleOptions(new Vector3f(0.69f, 0.25f, 0.18f), 1.4f);

	/** Jugador → el gancho donde quedó su boya. */
	private record Enganche(BlockPos pos, Vec3 punto, int distancia) {}

	private static final Map<UUID, Enganche> ENGANCHES = new HashMap<>();

	/** Velocidad del empujón (bloques por tick) y cuánto sube por cada bloque que avanza. */
	private static final double VELOCIDAD = 1.0, SUBIDA = 0.15;

	/** Jugador que el gancho está empujando: hacia dónde, desde dónde salió, cuánto tiene que avanzar y hasta cuándo. */
	private record Vuelo(Vec3 dir, Vec3 desde, double tramo, int ticks) {}

	private static final Map<UUID, Vuelo> VUELOS = new HashMap<>();

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(Ganchos::tick);
		// Recoger la caña enganchada a un gancho: te lanza (y la caña se recoge como siempre).
		UseItemCallback.EVENT.register((jugador, mundo, mano) -> {
			if (jugador instanceof ServerPlayer j && j.getItemInHand(mano).is(Items.FISHING_ROD) && j.fishing != null) {
				Enganche e = ENGANCHES.remove(j.getUUID());
				if (e != null) lanzar(j, e);
			}
			return InteractionResultHolder.pass(jugador.getItemInHand(mano));
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ganchito").requires(s -> s.hasPermission(2))
				.then(Commands.argument("distancia", IntegerArgumentType.integer(1, DISTANCIA_MAXIMA))
						.executes(c -> {
							ServerPlayer jugador = c.getSource().getPlayerOrException();
							int d = IntegerArgumentType.getInteger(c, "distancia");
							if (!jugador.getInventory().add(GanchoItem.con(d))) jugador.drop(GanchoItem.con(d), false);
							c.getSource().sendSuccess(() -> Component.literal("Te di un Gancho de Huesos con impulso de " + d + " bloques.")
									.withStyle(ChatFormatting.GREEN), false);
							return 1;
						})));
	}

	private static void tick(MinecraftServer server) {
		empujar(server);
		ENGANCHES.keySet().removeIf(id -> {
			ServerPlayer j = server.getPlayerList().getPlayer(id);
			return j == null || j.fishing == null;
		});
		for (ServerPlayer j : server.getPlayerList().getPlayers()) {
			FishingHook boya = j.fishing;
			if (boya == null) continue;
			Enganche e = ENGANCHES.get(j.getUUID());
			if (e == null) {
				e = buscarGancho(j.serverLevel(), boya);
				if (e == null) continue;
				ENGANCHES.put(j.getUUID(), e);
				j.serverLevel().playSound(null, e.punto().x, e.punto().y, e.punto().z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
			}
			// Si rompieron el gancho, se suelta.
			if (!(j.serverLevel().getBlockEntity(e.pos()) instanceof GanchoBlockEntity)) {
				ENGANCHES.remove(j.getUUID());
				boya.setNoGravity(false);
				continue;
			}
			// La boya queda colgada del gancho.
			boya.setNoGravity(true);
			boya.setDeltaMovement(Vec3.ZERO);
			boya.setPos(e.punto());
			if (server.getTickCount() % 3 == 0) mostrarLinea(j, e);
		}
	}

	/** El gancho que está cerca de la boya (a menos de un bloque y medio de la curva del gancho), si hay. */
	private static Enganche buscarGancho(ServerLevel mundo, FishingHook boya) {
		BlockPos centro = boya.blockPosition();
		Enganche mejor = null;
		double mejorDist = 1.5 * 1.5;
		for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-2, -2, -2), centro.offset(2, 2, 2))) {
			if (!(mundo.getBlockEntity(pos) instanceof GanchoBlockEntity gancho)) continue;
			// La curva del gancho cuelga hasta casi un bloque más abajo.
			Vec3 punto = Vec3.atBottomCenterOf(pos);
			double d = punto.distanceToSqr(boya.position());
			if (d < mejorDist) {
				mejorDist = d;
				mejor = new Enganche(pos.immutable(), punto, gancho.distancia());
			}
		}
		return mejor;
	}

	/** Hacia dónde te empuja: hacia adelante (de ti hacia el gancho, sin contar la altura) y un poco para arriba. */
	private static Vec3 direccion(ServerPlayer j, Enganche e) {
		Vec3 d = e.punto().subtract(j.position()).multiply(1, 0, 1);
		if (d.lengthSqr() < 0.25) d = j.getLookAngle().multiply(1, 0, 1);   // justo debajo: hacia donde mira
		if (d.lengthSqr() < 1.0E-4) d = new Vec3(0, 0, 1);
		return d.normalize().add(0, SUBIDA, 0);
	}

	/** Una línea recta de partículas (sólo para ese jugador) desde él hacia donde lo va a llevar el gancho. */
	private static void mostrarLinea(ServerPlayer j, Enganche e) {
		Vec3 dir = direccion(j, e), desde = j.position().add(0, 0.5, 0);
		ServerLevel mundo = j.serverLevel();
		for (double k = 1; k <= e.distancia(); k += 0.5) {
			Vec3 p = desde.add(dir.scale(k));
			mundo.sendParticles(j, k + 0.5 > e.distancia() ? PUNTA : LINEA, true, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	/**
	 * Empieza el empujón: lo lleva parejo hacia adelante hasta que avanzó la distancia del gancho (menos 1 bloque, que
	 * recorre solo al soltarlo). Si se traba contra algo, lo suelta igual al rato.
	 */
	private static void lanzar(ServerPlayer j, Enganche e) {
		double tramo = Math.max(0.5, e.distancia() - 1);
		VUELOS.put(j.getUUID(), new Vuelo(direccion(j, e), j.position(), tramo, (int) (tramo / VELOCIDAD) * 2 + 10));
		j.serverLevel().playSound(null, j.getX(), j.getY(), j.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f, 0.7f);
	}

	/** Cada tick, a los que van colgados: los sigue empujando; al terminar, los suelta con un poco de impulso. */
	private static void empujar(MinecraftServer server) {
		for (var it = VUELOS.entrySet().iterator(); it.hasNext(); ) {
			var v = it.next();
			ServerPlayer j = server.getPlayerList().getPlayer(v.getKey());
			if (j == null || !j.isAlive()) {
				it.remove();
				continue;
			}
			Vuelo vuelo = v.getValue();
			int quedan = vuelo.ticks() - 1;
			boolean sigue = quedan > 0 && j.position().subtract(vuelo.desde()).horizontalDistance() < vuelo.tramo();
			j.setDeltaMovement(sigue ? vuelo.dir().scale(VELOCIDAD) : vuelo.dir().multiply(0.4, 0, 0.4).add(0, 0.2, 0));
			j.hurtMarked = true;
			j.fallDistance = 0;
			if (sigue) v.setValue(new Vuelo(vuelo.dir(), vuelo.desde(), vuelo.tramo(), quedan));
			else it.remove();
		}
	}
}
