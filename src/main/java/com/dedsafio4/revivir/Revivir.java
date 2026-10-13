package com.dedsafio4.revivir;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * La cinemática de revivir: el salto desde la fogata.
 * "/revivir <x y z> <dirección>" (dirección: +x -x +y -y +z -z) elige dónde se hace: cuando alguien es desbaneado con
 * el Bloque de Baneados, la próxima vez que entra aparece en ese lugar, mirando hacia esa dirección, y hace la
 * animación (sale volando hacia allá; con +y o -y sube y cae en el mismo lugar, y al final mira arriba o abajo).
 * "/revivir quitar" saca el lugar (los desbaneados entran normal). El jugador está parado en el centro de la
 * fogata y sale volando hacia donde mira. Dura 17 s (340 ticks):
 *   0      Círculo 1 en el piso (se agranda lento de 3,6 a 14,6 bloques en 8 s)
 *   0–100  espera
 *   100    Círculo 2 y la onda roja; sale del centro agachado
 *   124    sube 3 s hasta 9 bloques
 *   184    va 3 s para adelante 9 bloques, flotando
 *   244    cae al piso (acostado), polvo turquesa al chocar
 *   276    se levanta; 308 queda parado; 340 termina
 * Todo el tiempo giran 4 aros parados (verde azulejo y amarillo) en el centro de la fogata.
 * Hasta que cae, el jugador de verdad está en modo espectador y el que hace todo es una copia con su skin (la dibuja
 * cada cliente, RevivirCliente). Cuando la copia toca el piso, el jugador vuelve a su modo de juego en ese lugar y él
 * mismo queda acostado y se levanta (el servidor lo mueve tick por tick). Los círculos, aros y ondas también los dibuja
 * cada cliente como formas lisas.
 */
public final class Revivir {
	private Revivir() {}

	public static final int DURACION = 340;

	/** Servidor → jugadores: empezó la cinemática de ese jugador, con el centro, hacia dónde sale y el piso donde cae. */
	public record Payload(UUID jugador, double x, double y, double z, float yaw, double pisoCaida, boolean vertical)
			implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "revivir"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of((buf, p) -> {
			UUIDUtil.STREAM_CODEC.encode(buf, p.jugador());
			buf.writeDouble(p.x());
			buf.writeDouble(p.y());
			buf.writeDouble(p.z());
			buf.writeFloat(p.yaw());
			buf.writeDouble(p.pisoCaida());
			buf.writeBoolean(p.vertical());
		}, buf -> new Payload(UUIDUtil.STREAM_CODEC.decode(buf), buf.readDouble(), buf.readDouble(), buf.readDouble(),
				buf.readFloat(), buf.readDouble(), buf.readBoolean()));

		/** Hacia dónde sale volando (con +y o -y no se mueve para los costados). */
		public Vec3 adelante() {
			return vertical ? Vec3.ZERO : Revivir.adelante(yaw);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Hacia adelante según el yaw de Minecraft. */
	public static Vec3 adelante(float yaw) {
		float r = yaw * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
	}

	/** Cuándo la copia toca el piso y vuelve el jugador de verdad. */
	public static final int CAIDA = 258;

	/** La pose del jugador de verdad (desde que cae; null = la normal). La usan el servidor y el cliente del propio jugador. */
	public static Pose pose(int t) {
		if (t >= 258 && t < 276) return Pose.SLEEPING;    // acostado boca arriba en el piso
		if (t >= 276 && t < 292) return Pose.CROUCHING;   // sentado → de pie
		return null;
	}

	private static double easeOut(double k) {
		return 1 - (1 - k) * (1 - k);
	}

	private static double easeInOut(double k) {
		return k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
	}

	/** Dónde están los pies (de la copia o del jugador) en el momento t. La usan el servidor y los clientes. */
	public static Vec3 posicion(double t, Vec3 centro, Vec3 adelante, double pisoCaida) {
		double avance = 0, altura = 0;
		if (t < 100) {
			altura = 0;
		} else if (t < 124) {
			altura = 1.7 * ((t - 100) / 24.0);
		} else if (t < 184) {
			altura = 1.7 + (8.3 - 1.7) * easeOut((t - 124) / 60.0);
		} else if (t < 244) {
			double k = (t - 184) / 60.0;
			avance = 9 * easeInOut(k);
			altura = 8.3 + 0.3 * Math.sin(Math.PI * k);
		} else if (t < 258) {
			double k = (t - 244) / 14.0;
			avance = 9;
			altura = 8.3 + (pisoCaida - centro.y - 8.3) * k * k;
		} else {
			avance = 9;
			altura = pisoCaida - centro.y;
			if (t < 262) altura += 0.25 * Math.sin(Math.PI * (t - 258) / 4.0);   // el rebote
		}
		return centro.add(adelante.scale(avance)).add(0, altura, 0);
	}

	/** Una cinemática en curso: el jugador, el centro de la fogata, hacia dónde sale y en qué tick va. */
	private static final class Escena {
		final UUID jugador;
		final ServerLevel level;
		final Vec3 centro, adelante;
		final float yaw;
		final double pisoCaida;
		/** Con +y o -y: sube y cae en el mismo lugar; al final mira arriba (-90) o abajo (90). */
		final boolean vertical;
		final float mirada;
		/** El modo de juego que tenía (mientras vuela la copia está en espectador). */
		final net.minecraft.world.level.GameType modo;
		boolean volvio;
		int t;

		Escena(ServerPlayer p, boolean vertical, float mirada) {
			jugador = p.getUUID();
			level = p.serverLevel();
			modo = p.gameMode.getGameModeForPlayer();
			centro = p.position();
			yaw = p.getYRot();
			this.vertical = vertical;
			this.mirada = mirada;
			adelante = vertical ? Vec3.ZERO : Revivir.adelante(yaw);
			// Dónde está el piso donde cae (por si el terreno no es plano).
			Vec3 caida = centro.add(adelante.scale(9));
			BlockPos.MutableBlockPos pos = BlockPos.containing(caida.x, centro.y + 9, caida.z).mutable();
			while (pos.getY() > centro.y - 10 && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) pos.move(0, -1, 0);
			pisoCaida = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() ? centro.y : pos.getY() + 1;
		}
	}

	private static final List<Escena> ESCENAS = new ArrayList<>();

	/** Las direcciones de /revivir: el yaw de Minecraft y hacia dónde mira al final (0 = derecho). */
	private static final java.util.Map<String, float[]> DIRECCIONES = java.util.Map.of(
			"+x", new float[]{-90, 0}, "-x", new float[]{90, 0}, "+z", new float[]{0, 0}, "-z", new float[]{180, 0},
			"+y", new float[]{0, -90}, "-y", new float[]{0, 90});

	/** El lugar de /revivir y los desbaneados que todavía no entraron (guardado en el mundo). */
	private static final class Datos extends net.minecraft.world.level.saveddata.SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		BlockPos lugar;
		String dimension = "minecraft:overworld", direccion = "+z";
		final java.util.Set<String> pendientes = new java.util.HashSet<>();

		private static Datos leer(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("lugar")) d.lugar = BlockPos.of(tag.getLong("lugar"));
			if (tag.contains("dimension")) d.dimension = tag.getString("dimension");
			if (tag.contains("direccion")) d.direccion = tag.getString("direccion");
			for (net.minecraft.nbt.Tag t : tag.getList("pendientes", net.minecraft.nbt.Tag.TAG_STRING)) d.pendientes.add(t.getAsString());
			return d;
		}

		@Override
		public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			if (lugar != null) tag.putLong("lugar", lugar.asLong());
			tag.putString("dimension", dimension);
			tag.putString("direccion", direccion);
			net.minecraft.nbt.ListTag lista = new net.minecraft.nbt.ListTag();
			for (String s : pendientes) lista.add(net.minecraft.nbt.StringTag.valueOf(s));
			tag.put("pendientes", lista);
			return tag;
		}
	}

	private static Datos datos(net.minecraft.server.MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_revivir");
	}

	/** Los que acaban de entrar y van a hacer la animación: el jugador y en qué tick del servidor empieza. */
	private static final java.util.Map<UUID, Integer> POR_EMPEZAR = new java.util.HashMap<>();

	/** Lo llama el Bloque de Baneados al desbanear: si hay lugar de /revivir, hará la animación al entrar. */
	public static void alDesbanear(net.minecraft.server.MinecraftServer server, UUID jugador) {
		Datos d = datos(server);
		if (d.lugar == null) return;
		d.pendientes.add(jugador.toString());
		d.setDirty();
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		// El desbaneado entra: un momento después (que termine de cargar) aparece en el lugar y hace la animación.
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			Datos d = datos(server);
			if (d.lugar == null || !d.pendientes.remove(handler.player.getUUID().toString())) return;
			d.setDirty();
			POR_EMPEZAR.put(handler.player.getUUID(), server.getTickCount() + 40);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Iterator<java.util.Map.Entry<UUID, Integer>> it = POR_EMPEZAR.entrySet().iterator(); it.hasNext(); ) {
				java.util.Map.Entry<UUID, Integer> entrada = it.next();
				if (server.getTickCount() < entrada.getValue()) continue;
				it.remove();
				ServerPlayer p = server.getPlayerList().getPlayer(entrada.getKey());
				if (p != null && p.isAlive()) enLugar(server, p);
			}
			for (Iterator<Escena> it = ESCENAS.iterator(); it.hasNext(); ) {
				Escena e = it.next();
				ServerPlayer p = server.getPlayerList().getPlayer(e.jugador);
				if (p == null || !p.isAlive() || p.serverLevel() != e.level || e.t >= DURACION) {
					if (p != null) terminar(e, p);
					it.remove();
					continue;
				}
				tick(e, p);
				e.t++;
			}
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		// Los admins, y los que tienen permiso con /opop (sin ser OP).
		dispatcher.register(Commands.literal("revivir").requires(com.dedsafio4.catalogo.Catalogo::puedeEditar)
				.executes(c -> {
					Datos d = datos(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal(d.lugar == null ? "No hay lugar de revivir: los desbaneados entran normal."
							: "Lugar de revivir: " + d.lugar.getX() + " " + d.lugar.getY() + " " + d.lugar.getZ() + " mirando hacia "
									+ d.direccion + ".").withStyle(ChatFormatting.AQUA), false);
					return 1;
				})
				.then(Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.lugar = null;
					d.pendientes.clear();
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Lugar de revivir quitado.").withStyle(ChatFormatting.AQUA), true);
					return 1;
				}))
				.then(Commands.argument("lugar", BlockPosArgument.blockPos())
						.then(Commands.argument("direccion", StringArgumentType.word())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("+x", "-x", "+y", "-y", "+z", "-z"), b))
								.executes(c -> {
									String direccion = StringArgumentType.getString(c, "direccion").toLowerCase(java.util.Locale.ROOT);
									if (!DIRECCIONES.containsKey(direccion)) {
										c.getSource().sendFailure(Component.literal("La dirección tiene que ser +x, -x, +y, -y, +z o -z."));
										return 0;
									}
									BlockPos lugar = BlockPosArgument.getBlockPos(c, "lugar");
									Datos d = datos(c.getSource().getServer());
									d.lugar = lugar;
									d.dimension = c.getSource().getLevel().dimension().location().toString();
									d.direccion = direccion;
									d.setDirty();
									c.getSource().sendSuccess(() -> Component.literal("Lugar de revivir: " + lugar.getX() + " " + lugar.getY() + " "
											+ lugar.getZ() + " mirando hacia " + direccion + ". Los desbaneados harán la animación ahí al entrar.")
											.withStyle(ChatFormatting.AQUA), true);
									return 1;
								}))));
	}

	/** Lo lleva al lugar de /revivir, mirando hacia esa dirección, y empieza la animación. */
	private static void enLugar(net.minecraft.server.MinecraftServer server, ServerPlayer p) {
		Datos d = datos(server);
		if (d.lugar == null) return;
		ServerLevel mundo = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
				ResourceLocation.parse(d.dimension)));
		if (mundo == null) mundo = server.overworld();
		float[] dir = DIRECCIONES.getOrDefault(d.direccion, DIRECCIONES.get("+z"));
		p.teleportTo(mundo, d.lugar.getX() + 0.5, d.lugar.getY(), d.lugar.getZ() + 0.5, dir[0], 0);
		empezar(List.of(p), dir[1] != 0, dir[1]);
	}

	private static void empezar(Collection<ServerPlayer> jugadores, boolean vertical, float mirada) {
		for (ServerPlayer p : jugadores) {
			for (Iterator<Escena> it = ESCENAS.iterator(); it.hasNext(); ) {
				Escena vieja = it.next();
				if (!vieja.jugador.equals(p.getUUID())) continue;
				terminar(vieja, p);
				it.remove();
			}
			Escena e = new Escena(p, vertical, mirada);
			ESCENAS.add(e);
			// El jugador mira como espectador; el que sale de la fogata es la copia con su skin.
			p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
			// Que no le pase nada mientras está en la fogata y cuando cae.
			p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURACION + 40, 0, false, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURACION + 40, 4, false, false, false));
			Payload payload = new Payload(p.getUUID(), e.centro.x, e.centro.y, e.centro.z, e.yaw, e.pisoCaida, e.vertical);
			for (ServerPlayer otro : e.level.players()) ServerPlayNetworking.send(otro, payload);
			// Mientras dura, el cielo se pone de noche con estrellas para los que están a menos de 30 bloques.
			com.dedsafio4.cielo.CieloNoche.mostrar(e.level, e.centro, 30f, DURACION);
		}
	}

	private static void terminar(Escena e, ServerPlayer p) {
		volver(e, p);
		forzarPose(p, null);
		p.setNoGravity(false);
		p.fallDistance = 0;
		// Con +y o -y: al final mira arriba o abajo.
		if (e.vertical) p.connection.teleport(p.getX(), p.getY(), p.getZ(), e.yaw, e.mirada);
	}

	/**
	 * setForcedPose solo existe en NeoForge: ahí deja la pose fija. En Fabric no está, así que se usa setPose
	 * (con null no hace nada y Minecraft le vuelve a poner la pose normal solo).
	 */
	public static void forzarPose(net.minecraft.world.entity.player.Player p, Pose pose) {
		try {
			net.minecraft.world.entity.player.Player.class.getMethod("setForcedPose", Pose.class).invoke(p, pose);
		} catch (ReflectiveOperationException ex) {
			if (pose != null) p.setPose(pose);
		}
	}

	/** El jugador vuelve a su modo de juego, donde cayó la copia. */
	private static void volver(Escena e, ServerPlayer p) {
		if (e.volvio) return;
		e.volvio = true;
		Vec3 pos = posicion(CAIDA, e.centro, e.adelante, e.pisoCaida);
		p.setGameMode(e.modo);
		p.connection.teleport(pos.x, pos.y, pos.z, 0, 0, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
	}

	// --- Cada tick ---

	private static void tick(Escena e, ServerPlayer p) {
		int t = e.t;
		ServerLevel level = e.level;

		// --- El jugador: espectador hasta que cae la copia; después, él mismo acostado y levantándose ---
		if (t >= CAIDA) {
			volver(e, p);
			forzarPose(p, pose(t));
			p.setNoGravity(true);
			p.fallDistance = 0;
			Vec3 pos = posicion(t, e.centro, e.adelante, e.pisoCaida);
			// Solo la posición: la cámara queda libre.
			p.connection.teleport(pos.x, pos.y, pos.z, 0, 0, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
			p.setDeltaMovement(Vec3.ZERO);
		}

		// --- Sonidos y la tierra que salta al caer (los círculos, aros y ondas los dibuja el cliente) ---
		if (t == 100) {
			level.playSound(null, e.centro.x, e.centro.y, e.centro.z, SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.2f, 0.7f);
		}
		if (t == 258) {
			Vec3 caida = e.centro.add(e.adelante.scale(9));
			BlockState piso = level.getBlockState(BlockPos.containing(caida.x, e.pisoCaida - 1, caida.z));
			if (!piso.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, piso), caida.x, e.pisoCaida + 0.1, caida.z,
					40, 0.8, 0.1, 0.8, 0.15);
			level.playSound(null, caida.x, e.pisoCaida, caida.z, SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1f, 0.8f);
		}
	}
}
