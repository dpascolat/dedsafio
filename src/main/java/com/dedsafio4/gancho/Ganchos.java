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
 * cerca de un gancho (a 2 bloques o menos en cualquier momento de su vuelo), se queda enganchada en él. Mientras está
 * enganchada te mueves como quieras (la cuerda no te tira) y se ve una línea de partículas (sólo
 * la ves tú) que muestra hacia dónde vas a ir, en línea recta: hacia el lado que apuntas, y mirando para abajo te
 * lanza para arriba (abajo del todo: derecho para arriba; al frente: en diagonal; arriba del todo: derecho adelante). Con click derecho (recoger la caña)
 * el gancho te lanza en esa línea, siempre con la misma fuerza (saltes o no), la cantidad de bloques del gancho.
 * /ganchito <distancia> da un gancho con ese impulso.
 */
public final class Ganchos {
	private Ganchos() {}

	private static final int DISTANCIA_MAXIMA = 100;
	private static final DustParticleOptions LINEA = new DustParticleOptions(new Vector3f(0.96f, 0.94f, 0.86f), 1.0f);
	private static final DustParticleOptions PUNTA = new DustParticleOptions(new Vector3f(0.69f, 0.25f, 0.18f), 1.4f);

	/** Jugador → el gancho donde quedó su boya. */
	private record Enganche(BlockPos pos, Vec3 punto, int distancia) {}

	private static final Map<UUID, Enganche> ENGANCHES = new HashMap<>();

	/** Velocidad del lanzamiento (bloques por tick). */
	private static final double VELOCIDAD = 1.0;

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
			if (server.getTickCount() % 2 == 0) mostrarLinea(j, e);
		}
	}

	/** Hasta qué distancia de la curva del gancho tiene que pasar la boya para engancharse. */
	private static final double ATRAPA = 2.0;

	/**
	 * El gancho por el que pasó cerca la boya en este tick (se mira todo el tramo que recorrió, así no se lo saltea
	 * aunque vaya rápido), si hay.
	 */
	private static Enganche buscarGancho(ServerLevel mundo, FishingHook boya) {
		Vec3 desde = new Vec3(boya.xo, boya.yo, boya.zo), hasta = boya.position();
		BlockPos a = BlockPos.containing(desde), b = BlockPos.containing(hasta);
		BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()) - 3, Math.min(a.getY(), b.getY()) - 3, Math.min(a.getZ(), b.getZ()) - 3);
		BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()) + 3, Math.max(a.getY(), b.getY()) + 3, Math.max(a.getZ(), b.getZ()) + 3);
		Enganche mejor = null;
		double mejorDist = ATRAPA * ATRAPA;
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			if (!(mundo.getBlockEntity(pos) instanceof GanchoBlockEntity gancho)) continue;
			// La curva del gancho cuelga hasta casi un bloque más abajo.
			Vec3 punto = Vec3.atBottomCenterOf(pos);
			double d = distanciaAlTramo(punto, desde, hasta);
			if (d < mejorDist) {
				mejorDist = d;
				mejor = new Enganche(pos.immutable(), punto, gancho.distancia());
			}
		}
		return mejor;
	}

	/** La distancia (al cuadrado) del punto p al tramo de a a b. */
	private static double distanciaAlTramo(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double largo = ab.lengthSqr();
		double k = largo < 1.0E-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / largo));
		return a.add(ab.scale(k)).distanceToSqr(p);
	}

	/**
	 * Hacia dónde te lanza: hacia el lado que mira (izquierda/derecha), y la subida al revés de la mirada: mirando
	 * abajo del todo va derecho para arriba, mirando al frente sube en diagonal (45°) y mirando arriba del todo va
	 * derecho hacia adelante.
	 */
	private static Vec3 direccion(ServerPlayer j) {
		double subida = Math.toRadians((j.getXRot() + 90) / 2);   // getXRot: -90 arriba, 90 abajo
		double yaw = Math.toRadians(j.getYRot());
		return new Vec3(-Math.sin(yaw) * Math.cos(subida), Math.sin(subida), Math.cos(yaw) * Math.cos(subida));
	}

	/** Una línea recta de partículas (sólo para ese jugador) desde él hacia donde lo va a llevar el gancho. */
	private static void mostrarLinea(ServerPlayer j, Enganche e) {
		Vec3 dir = direccion(j), desde = j.position().add(0, 1.0, 0);
		ServerLevel mundo = j.serverLevel();
		for (double k = 1; k <= e.distancia(); k += 0.5) {
			Vec3 p = desde.add(dir.scale(k));
			mundo.sendParticles(j, k + 0.5 > e.distancia() ? PUNTA : LINEA, true, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	/**
	 * Empieza el lanzamiento: lo lleva parejo en línea recta hacia donde apunta hasta que avanzó la distancia del gancho
	 * (menos 1 bloque, que recorre solo al soltarlo). Si se traba contra algo, lo suelta igual al rato.
	 */
	private static void lanzar(ServerPlayer j, Enganche e) {
		double tramo = Math.max(0.5, e.distancia() - 1);
		VUELOS.put(j.getUUID(), new Vuelo(direccion(j), j.position(), tramo, (int) (tramo / VELOCIDAD) * 2 + 10));
		j.serverLevel().playSound(null, j.getX(), j.getY(), j.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f, 0.7f);
	}

	/**
	 * Cada tick, a los que van volando: los sigue empujando (cuánto avanzaron se mide sobre la línea, así saltar no
	 * cambia nada); al terminar, los suelta siguiendo la misma dirección, sin saltito.
	 */
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
			boolean sigue = quedan > 0 && j.position().subtract(vuelo.desde()).dot(vuelo.dir()) < vuelo.tramo();
			j.setDeltaMovement(vuelo.dir().scale(sigue ? VELOCIDAD : 0.5));
			j.hurtMarked = true;
			j.fallDistance = 0;
			if (sigue) v.setValue(new Vuelo(vuelo.dir(), vuelo.desde(), vuelo.tramo(), quedan));
			else it.remove();
		}
	}
}
