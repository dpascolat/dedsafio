package com.dedsafio4.revivir;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

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
 * Todo con partículas; el jugador se mueve tick por tick (puede mirar para cualquier lado, pero no caminar).
 */
public final class Revivir {
	private Revivir() {}

	private static final int DURACION = 340;
	private static final Vector3f TURQUESA = color(0x12A59C), AURORA = color(0x3FF0A8), AURORA_2 = color(0x1FD38C),
			AZULEJO = color(0x1FC8B8), AMARILLO = color(0xFFD23A), ROJO = color(0xEC3013);

	private static Vector3f color(int rgb) {
		return new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
	}

	/** Una cinemática en curso: el jugador, el centro de la fogata, hacia dónde sale y en qué tick va. */
	private static final class Escena {
		final UUID jugador;
		final ServerLevel level;
		final Vec3 centro, adelante;
		final double pisoCaida;
		int t;

		Escena(ServerPlayer p) {
			jugador = p.getUUID();
			level = p.serverLevel();
			centro = p.position();
			float yaw = p.getYRot() * Mth.DEG_TO_RAD;
			adelante = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
			// Dónde está el piso donde cae (por si el terreno no es plano).
			Vec3 caida = centro.add(adelante.scale(9));
			BlockPos.MutableBlockPos pos = BlockPos.containing(caida.x, centro.y + 9, caida.z).mutable();
			while (pos.getY() > centro.y - 10 && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) pos.move(0, -1, 0);
			pisoCaida = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() ? centro.y : pos.getY() + 1;
		}
	}

	private static final List<Escena> ESCENAS = new ArrayList<>();

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Iterator<Escena> it = ESCENAS.iterator(); it.hasNext(); ) {
				Escena e = it.next();
				ServerPlayer p = server.getPlayerList().getPlayer(e.jugador);
				if (p == null || !p.isAlive() || p.serverLevel() != e.level || e.t >= DURACION) {
					if (p != null) terminar(p);
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
			ESCENAS.removeIf(e -> e.jugador.equals(p.getUUID()));
			ESCENAS.add(new Escena(p));
			// Que no le pase nada mientras está en la fogata y cuando cae.
			p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURACION + 40, 0, false, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURACION + 40, 4, false, false, false));
		}
		fuente.sendSuccess(() -> Component.literal("Cinemática de revivir (prueba): " + jugadores.size() + " jugador(es).")
				.withStyle(ChatFormatting.AQUA), true);
		return jugadores.size();
	}

	private static void terminar(ServerPlayer p) {
		p.setForcedPose(null);
		p.setShiftKeyDown(false);
		p.setNoGravity(false);
		p.fallDistance = 0;
	}

	// --- Cada tick ---

	private static double easeOut(double k) {
		return 1 - (1 - k) * (1 - k);
	}

	private static double easeInOut(double k) {
		return k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
	}

	private static void tick(Escena e, ServerPlayer p) {
		int t = e.t;
		ServerLevel level = e.level;
		RandomSource r = level.random;

		// --- El jugador ---
		double avance = 0, altura = 0;
		Pose pose = null;
		boolean agachado = false;
		if (t < 100) {
			altura = 0;
		} else if (t < 124) {
			altura = 1.7 * ((t - 100) / 24.0);
			agachado = true;
		} else if (t < 184) {
			altura = 1.7 + (8.3 - 1.7) * easeOut((t - 124) / 60.0);
		} else if (t < 244) {
			double k = (t - 184) / 60.0;
			avance = 9 * easeInOut(k);
			altura = 8.3 + 0.3 * Math.sin(Math.PI * k);
		} else if (t < 258) {
			double k = (t - 244) / 14.0;
			avance = 9;
			altura = 8.3 + (e.pisoCaida - e.centro.y - 8.3) * k * k;
		} else {
			avance = 9;
			altura = e.pisoCaida - e.centro.y;
			if (t < 276) {
				pose = Pose.SWIMMING;   // acostado
				if (t < 262) altura += 0.25 * Math.sin(Math.PI * (t - 258) / 4.0);   // el rebote
			} else if (t < 308) {
				if (t < 292) pose = Pose.CROUCHING;   // sentado → de pie
			}
		}
		p.setForcedPose(pose);
		p.setShiftKeyDown(agachado);
		p.setNoGravity(true);
		p.fallDistance = 0;
		Vec3 pos = e.centro.add(e.adelante.scale(avance)).add(0, altura, 0);
		// Solo la posición: la cámara queda libre.
		p.connection.teleport(pos.x, pos.y, pos.z, 0, 0, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
		p.setDeltaMovement(Vec3.ZERO);

		// --- Los 4 aros parados que giran en el centro de la fogata ---
		double[] radios = {1.8, 2.1, 2.4, 2.7}, velocidades = {0.8, 1.15, 1.5, 1.85};
		for (int i = 0; i < 4; i++) {
			double giro = i * Math.PI / 4 + (i % 2 == 0 ? 1 : -1) * velocidades[i] * t / 20.0;
			DustParticleOptions polvo = new DustParticleOptions(i % 2 == 0 ? AZULEJO : AMARILLO, 0.7f);
			int puntos = 26;
			for (int j = 0; j < puntos; j++) {
				double fi = 2 * Math.PI * j / puntos;
				double h = radios[i] * Math.cos(fi);
				level.sendParticles(polvo, e.centro.x + h * Math.cos(giro), e.centro.y + 1.8 + radios[i] * Math.sin(fi),
						e.centro.z + h * Math.sin(giro), 1, 0, 0, 0, 0);
			}
		}

		// --- Los círculos del piso (cada 2 ticks) ---
		if (t % 2 == 0) {
			for (int inicio : new int[]{0, 100}) {
				if (t < inicio || t >= inicio + 160) continue;
				double prog = (t - inicio) / 160.0;
				double radio = 3.6 + 11 * prog, alfa = Math.min(1, prog * 6) * (1 - prog);
				// El borde turquesa.
				int puntos = (int) (radio * 6 * (0.3 + 0.7 * alfa));
				DustParticleOptions borde = new DustParticleOptions(TURQUESA, 1.6f);
				for (int j = 0; j < puntos; j++) {
					double a = 2 * Math.PI * j / puntos + r.nextDouble() * 0.05;
					level.sendParticles(borde, e.centro.x + radio * Math.cos(a), e.centro.y + 0.1, e.centro.z + radio * Math.sin(a), 1, 0, 0, 0, 0);
				}
				// El relleno tipo aurora (verde, que pulsa).
				double pulso = 1 + 0.1 * Math.sin(2 * Math.PI * 0.35 * t / 20.0);
				int relleno = (int) (radio * radio * 0.35 * alfa * pulso);
				for (int j = 0; j < relleno; j++) {
					double a = r.nextDouble() * 2 * Math.PI, d = radio * (0.36 + 0.64 * Math.sqrt(r.nextDouble()));
					Vector3f c = new Vector3f(AURORA).lerp(AURORA_2, (float) (0.5 + 0.5 * Math.sin(t * 0.1 + a)));
					level.sendParticles(new DustParticleOptions(c, 1.0f), e.centro.x + d * Math.cos(a), e.centro.y + 0.08,
							e.centro.z + d * Math.sin(a), 1, 0, 0, 0, 0);
				}
			}
		}

		// --- La onda roja cuando sale (0,85 s) ---
		if (t >= 100 && t < 117) {
			double w = (t - 100) / 17.0, radio = 1 + 9 * w;
			int puntos = (int) (radio * 8 * (1 - w) + 6);
			DustParticleOptions rojo = new DustParticleOptions(ROJO, 2f);
			for (int j = 0; j < puntos; j++) {
				double a = 2 * Math.PI * j / puntos;
				level.sendParticles(rojo, e.centro.x + radio * Math.cos(a), e.centro.y + 3.05, e.centro.z + radio * Math.sin(a), 1, 0, 0, 0, 0);
			}
		}
		if (t == 100) {
			level.playSound(null, e.centro.x, e.centro.y, e.centro.z, SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.2f, 0.7f);
			level.sendParticles(ParticleTypes.EXPLOSION, e.centro.x, e.centro.y + 3, e.centro.z, 1, 0, 0, 0, 0);
		}

		// --- El polvo al caer (0,9 s) ---
		if (t >= 258 && t < 276) {
			double d = (t - 258) / 18.0, radio = 0.6 + 3 * d;
			Vec3 caida = e.centro.add(e.adelante.scale(9));
			int puntos = (int) (radio * 8 * (1 - d) + 4);
			DustParticleOptions polvo = new DustParticleOptions(TURQUESA, 1.4f);
			for (int j = 0; j < puntos; j++) {
				double a = 2 * Math.PI * j / puntos;
				level.sendParticles(polvo, caida.x + radio * Math.cos(a), e.pisoCaida + 0.1, caida.z + radio * Math.sin(a), 1, 0, 0, 0, 0);
			}
			if (t == 258) {
				BlockState piso = level.getBlockState(BlockPos.containing(caida.x, e.pisoCaida - 1, caida.z));
				if (!piso.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, piso), caida.x, e.pisoCaida + 0.1, caida.z,
						40, 0.8, 0.1, 0.8, 0.15);
				level.playSound(null, caida.x, e.pisoCaida, caida.z, SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1f, 0.8f);
			}
		}
	}
}
