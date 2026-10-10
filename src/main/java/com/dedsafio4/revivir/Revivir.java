package com.dedsafio4.revivir;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
 * /revivir cinematica [jugadores] (de prueba): el salto desde la fogata. El jugador está parado en el centro de la
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
	public record Payload(UUID jugador, double x, double y, double z, float yaw, double pisoCaida) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "revivir"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Payload::jugador, ByteBufCodecs.DOUBLE, Payload::x, ByteBufCodecs.DOUBLE, Payload::y,
				ByteBufCodecs.DOUBLE, Payload::z, ByteBufCodecs.FLOAT, Payload::yaw, ByteBufCodecs.DOUBLE, Payload::pisoCaida, Payload::new);

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
		/** El modo de juego que tenía (mientras vuela la copia está en espectador). */
		final net.minecraft.world.level.GameType modo;
		boolean volvio;
		int t;

		Escena(ServerPlayer p) {
			jugador = p.getUUID();
			level = p.serverLevel();
			modo = p.gameMode.getGameModeForPlayer();
			centro = p.position();
			yaw = p.getYRot();
			adelante = Revivir.adelante(yaw);
			// Dónde está el piso donde cae (por si el terreno no es plano).
			Vec3 caida = centro.add(adelante.scale(9));
			BlockPos.MutableBlockPos pos = BlockPos.containing(caida.x, centro.y + 9, caida.z).mutable();
			while (pos.getY() > centro.y - 10 && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) pos.move(0, -1, 0);
			pisoCaida = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() ? centro.y : pos.getY() + 1;
		}
	}

	private static final List<Escena> ESCENAS = new ArrayList<>();

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
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
		dispatcher.register(Commands.literal("revivir").requires(s -> s.hasPermission(2))
				.then(Commands.literal("cinematica")
						.executes(c -> empezar(c.getSource(), List.of(c.getSource().getPlayerOrException())))
						.then(Commands.argument("jugadores", EntityArgument.players())
								.executes(c -> empezar(c.getSource(), EntityArgument.getPlayers(c, "jugadores"))))));
	}

	private static int empezar(CommandSourceStack fuente, Collection<ServerPlayer> jugadores) {
		for (ServerPlayer p : jugadores) {
			for (Iterator<Escena> it = ESCENAS.iterator(); it.hasNext(); ) {
				Escena vieja = it.next();
				if (!vieja.jugador.equals(p.getUUID())) continue;
				terminar(vieja, p);
				it.remove();
			}
			Escena e = new Escena(p);
			ESCENAS.add(e);
			// El jugador mira como espectador; el que sale de la fogata es la copia con su skin.
			p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
			// Que no le pase nada mientras está en la fogata y cuando cae.
			p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURACION + 40, 0, false, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURACION + 40, 4, false, false, false));
			Payload payload = new Payload(p.getUUID(), e.centro.x, e.centro.y, e.centro.z, e.yaw, e.pisoCaida);
			for (ServerPlayer otro : e.level.players()) ServerPlayNetworking.send(otro, payload);
		}
		fuente.sendSuccess(() -> Component.literal("Cinemática de revivir (prueba): " + jugadores.size() + " jugador(es).")
				.withStyle(ChatFormatting.AQUA), true);
		return jugadores.size();
	}

	private static void terminar(Escena e, ServerPlayer p) {
		volver(e, p);
		p.setForcedPose(null);
		p.setNoGravity(false);
		p.fallDistance = 0;
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
			p.setForcedPose(pose(t));
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
