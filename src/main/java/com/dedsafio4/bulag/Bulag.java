package com.dedsafio4.bulag;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bulag 2: el Cráneo Explosivo.
 * - /bulag 2 salvar <cantidad>  a todos los jugadores en supervivencia se les pone el cráneo con tentáculos
 *   en la cabeza y en la pantalla les aparecen cuentas. Son 3 niveles: el 1 (sumas y restas fáciles), el 2
 *   (sumas y restas más difíciles y alguna multiplicación o división) y el 3 (multiplicaciones y divisiones
 *   difíciles). En cada nivel, los primeros en resolver todas sus cuentas se sacan el cráneo. Entre los tres
 *   niveles quedan eliminados todos menos <cantidad>: la mitad en el 1 y un cuarto en el 2 y en el 3
 *   (30 jugadores y se salvan 10: 10, 5 y 5).
 * - /bulag 2.1|2.2|2.3 salvar <cantidad>  una sola ronda de ese nivel.
 * - Cuando se llenan los lugares, los que siguen con el cráneo esperan: /bulag 2 explotar les explota el
 *   cráneo (pasan a espectador) y, si quedan niveles, arranca el siguiente.
 * - Si alguien tarda más de 20 segundos en una cuenta, el cráneo le explota ahí mismo.
 * - /bulag 2 parar  corta todo sin eliminar a nadie.
 */
public final class Bulag {
	private Bulag() {}

	/** Cuentas que hay que resolver para salvarse, en cada nivel. */
	private static final int[] CUENTAS = {0, 5, 4, 3};
	/** Tiempo para cada cuenta: 20 segundos. */
	public static final int TIEMPO_CUENTA = 20 * 20;
	/** Cuenta regresiva antes de cada nivel (3 segundos) y pausa entre niveles. */
	private static final int PREVIA = 60, PAUSA = 100;
	/** Castigo por equivocarse: un segundo sin poder contestar. */
	private static final int CASTIGO = 20;

	private static final RandomSource AZAR = RandomSource.create();

	// ---------------------------------------------------------------- Estado del juego

	/** Los niveles que faltan jugar (1, 2 o 3), en orden. Vacío = no hay juego. */
	private static final List<Integer> niveles = new ArrayList<>();
	/** Cuántos se salvan al final de todo. */
	private static int meta;
	/** Los que siguen en juego (no eliminados). */
	private static final Set<UUID> vivos = new LinkedHashSet<>();

	/** Ronda actual. */
	private static int nivel, cupo, ticks, pausa;
	private static boolean enRonda, lugaresLlenos;
	private static final Set<UUID> conCraneo = new LinkedHashSet<>();
	private static final Set<UUID> salvados = new LinkedHashSet<>();
	private static final Map<UUID, Jugada> jugadas = new HashMap<>();

	private static final class Jugada {
		String problema = "";
		String respuesta = "";
		int aciertos;
		/** Tick del juego en que empezó esta cuenta, y hasta cuándo no puede contestar (por equivocarse). */
		int desde, bloqueado;
	}

	// ---------------------------------------------------------------- Red

	/** Servidor → cliente: la pantalla de cuentas (abierta = false la cierra). */
	public record PantallaPayload(boolean abierta, int nivel, String problema, int largo, String aviso, boolean error,
								  int aciertos, int cuentas, int salvados, int cupo, int tiempo) implements CustomPacketPayload {
		public static final Type<PantallaPayload> TYPE = new Type<>(id("bulag_pantalla"));
		public static final StreamCodec<ByteBuf, PantallaPayload> CODEC = StreamCodec.of(
				(buf, p) -> {
					ByteBufCodecs.BOOL.encode(buf, p.abierta());
					ByteBufCodecs.VAR_INT.encode(buf, p.nivel());
					ByteBufCodecs.STRING_UTF8.encode(buf, p.problema());
					ByteBufCodecs.VAR_INT.encode(buf, p.largo());
					ByteBufCodecs.STRING_UTF8.encode(buf, p.aviso());
					ByteBufCodecs.BOOL.encode(buf, p.error());
					ByteBufCodecs.VAR_INT.encode(buf, p.aciertos());
					ByteBufCodecs.VAR_INT.encode(buf, p.cuentas());
					ByteBufCodecs.VAR_INT.encode(buf, p.salvados());
					ByteBufCodecs.VAR_INT.encode(buf, p.cupo());
					ByteBufCodecs.VAR_INT.encode(buf, p.tiempo());
				},
				buf -> new PantallaPayload(ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
						ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
						ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.BOOL.decode(buf),
						ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
						ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
						ByteBufCodecs.VAR_INT.decode(buf)));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor → cliente: quiénes tienen el cráneo en la cabeza (para dibujarlo). */
	public record CraneosPayload(List<UUID> jugadores) implements CustomPacketPayload {
		public static final Type<CraneosPayload> TYPE = new Type<>(id("bulag_craneos"));
		public static final StreamCodec<ByteBuf, CraneosPayload> CODEC =
				UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).map(CraneosPayload::new, CraneosPayload::jugadores);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: lo que escribió el jugador como respuesta. */
	public record RespuestaPayload(String respuesta) implements CustomPacketPayload {
		public static final Type<RespuestaPayload> TYPE = new Type<>(id("bulag_respuesta"));
		public static final StreamCodec<ByteBuf, RespuestaPayload> CODEC =
				ByteBufCodecs.STRING_UTF8.map(RespuestaPayload::new, RespuestaPayload::respuesta);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(PantallaPayload.TYPE, PantallaPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CraneosPayload.TYPE, CraneosPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RespuestaPayload.TYPE, RespuestaPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RespuestaPayload.TYPE, (payload, context) ->
				context.server().execute(() -> responder(context.player(), payload.respuesta())));
		ServerTickEvents.END_SERVER_TICK.register(Bulag::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				ServerPlayNetworking.send(handler.player, new CraneosPayload(List.copyOf(conCraneo))));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> salio(server, handler.player.getUUID())));
	}

	// ---------------------------------------------------------------- Comandos

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("bulag").requires(s -> s.hasPermission(2))
				.then(Commands.literal("2")
						.then(salvar(List.of(1, 2, 3)))
						.then(Commands.literal("explotar").executes(ctx -> {
							int cuantos = explotar(ctx.getSource().getServer());
							ctx.getSource().sendSuccess(() -> Component.literal(cuantos == 0
									? "Nadie tiene el cráneo." : "Explotaron " + cuantos + " cráneos."), true);
							return cuantos;
						}))
						.then(Commands.literal("parar").executes(ctx -> {
							parar(ctx.getSource().getServer());
							ctx.getSource().sendSuccess(() -> Component.literal("Bulag 2 cortado."), true);
							return 1;
						})))
				.then(Commands.literal("2.1").then(salvar(List.of(1))))
				.then(Commands.literal("2.2").then(salvar(List.of(2))))
				.then(Commands.literal("2.3").then(salvar(List.of(3)))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> salvar(List<Integer> deNiveles) {
		return Commands.literal("salvar").then(Commands.argument("cantidad", IntegerArgumentType.integer(0))
				.executes(ctx -> {
					MinecraftServer server = ctx.getSource().getServer();
					int cantidad = IntegerArgumentType.getInteger(ctx, "cantidad");
					if (!niveles.isEmpty() || enRonda) {
						ctx.getSource().sendFailure(Component.literal("Ya hay un Bulag 2 en juego (/bulag 2 parar para cortarlo)."));
						return 0;
					}
					List<ServerPlayer> jugadores = server.getPlayerList().getPlayers().stream()
							.filter(j -> j.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && j.isAlive()).toList();
					if (jugadores.size() <= cantidad) {
						ctx.getSource().sendFailure(Component.literal("Hay " + jugadores.size()
								+ " jugadores en supervivencia: tienen que ser más que los que se salvan."));
						return 0;
					}
					vivos.clear();
					jugadores.forEach(j -> vivos.add(j.getUUID()));
					niveles.addAll(deNiveles);
					meta = cantidad;
					pausa = 1;
					ctx.getSource().sendSuccess(() -> Component.literal("Bulag 2: juegan " + jugadores.size()
							+ ", se salvan " + cantidad + "."), true);
					return 1;
				}));
	}

	// ---------------------------------------------------------------- Rondas

	private static void tick(MinecraftServer server) {
		if (pausa > 0 && --pausa == 0) empezarRonda(server);
		if (!enRonda) return;
		ticks++;
		if (ticks <= PREVIA) {
			if (ticks % 20 == 1) {
				int falta = 3 - (ticks - 1) / 20;
				for (UUID uuid : conCraneo) {
					ServerPlayer j = server.getPlayerList().getPlayer(uuid);
					if (j == null) continue;
					enviar(j, "", 0, "NIVEL " + nivel + "  ·  " + falta, false);
					j.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.6f + (3 - falta) * 0.2f);
				}
			}
			if (ticks == PREVIA) {
				for (UUID uuid : conCraneo) {
					ServerPlayer j = server.getPlayerList().getPlayer(uuid);
					if (j != null) nuevaCuenta(j);
				}
			}
			return;
		}
		if (lugaresLlenos) return;
		// Se le acabó el tiempo a alguien: le explota.
		for (UUID uuid : List.copyOf(conCraneo)) {
			Jugada jugada = jugadas.get(uuid);
			ServerPlayer j = server.getPlayerList().getPlayer(uuid);
			if (jugada == null || j == null) continue;
			int pasaron = ticks - jugada.desde;
			if (pasaron >= TIEMPO_CUENTA) {
				explotarJugador(server, j, "se quedó sin tiempo");
			} else if (pasaron >= TIEMPO_CUENTA - 100 && pasaron % 20 == 0) {
				j.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1f, 1.8f);
			}
		}
		revisarFinDeRonda(server);
	}

	private static void empezarRonda(MinecraftServer server) {
		// Los que siguen en juego (conectados y vivos).
		vivos.removeIf(uuid -> {
			ServerPlayer j = server.getPlayerList().getPlayer(uuid);
			return j == null || !j.isAlive() || j.isSpectator();
		});
		if (niveles.isEmpty() || vivos.size() <= meta) {
			terminarJuego(server);
			return;
		}
		nivel = niveles.remove(0);
		// Eliminados en este nivel: la mitad de los que faltan eliminar, y en el último todos.
		int faltanEliminar = vivos.size() - meta;
		int eliminar = niveles.isEmpty() ? faltanEliminar : (faltanEliminar + 1) / 2;
		cupo = vivos.size() - eliminar;
		ticks = 0;
		enRonda = true;
		lugaresLlenos = false;
		salvados.clear();
		jugadas.clear();
		conCraneo.clear();
		conCraneo.addAll(vivos);
		for (UUID uuid : conCraneo) {
			ServerPlayer j = server.getPlayerList().getPlayer(uuid);
			if (j == null) continue;
			jugadas.put(uuid, new Jugada());
			j.level().playSound(null, j.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1f, 0.5f);
			j.sendSystemMessage(Component.literal("¡Tienes el Cráneo Explosivo! Resuelve " + CUENTAS[nivel]
					+ " cuentas para sacártelo. Se salvan " + cupo + ".").withColor(0xFF4040));
		}
		sincronizarCraneos(server);
	}

	private static void nuevaCuenta(ServerPlayer j) {
		Jugada jugada = jugadas.get(j.getUUID());
		if (jugada == null) return;
		int[] cuenta = generar(nivel);
		char op = (char) cuenta[2];
		jugada.problema = cuenta[0] + " " + op + " " + cuenta[1];
		jugada.respuesta = Integer.toString(cuenta[3]);
		jugada.desde = ticks;
		enviar(j, jugada.problema, jugada.respuesta.length(), "", false);
	}

	private static void enviar(ServerPlayer j, String problema, int largo, String aviso, boolean error) {
		Jugada jugada = jugadas.get(j.getUUID());
		int aciertos = jugada == null ? 0 : jugada.aciertos;
		int tiempo = jugada == null || problema.isEmpty() ? 0 : Math.max(0, TIEMPO_CUENTA - (ticks - jugada.desde));
		ServerPlayNetworking.send(j, new PantallaPayload(true, nivel, problema, largo, aviso, error,
				aciertos, CUENTAS[nivel], salvados.size(), cupo, tiempo));
	}

	private static void cerrar(ServerPlayer j) {
		ServerPlayNetworking.send(j, new PantallaPayload(false, 0, "", 0, "", false, 0, 0, 0, 0, 0));
	}

	private static void responder(ServerPlayer j, String respuesta) {
		Jugada jugada = jugadas.get(j.getUUID());
		if (!enRonda || lugaresLlenos || ticks <= PREVIA || jugada == null || !conCraneo.contains(j.getUUID())) return;
		if (ticks < jugada.bloqueado || jugada.respuesta.isEmpty()) return;
		if (!respuesta.equals(jugada.respuesta)) {
			jugada.bloqueado = ticks + CASTIGO;
			j.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.MASTER, 1f, 1f);
			enviar(j, jugada.problema, jugada.respuesta.length(), "¡INCORRECTO!", true);
			return;
		}
		jugada.aciertos++;
		j.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1f, 1.2f);
		if (jugada.aciertos < CUENTAS[nivel]) {
			nuevaCuenta(j);
			return;
		}
		// ¡Se salvó!
		salvados.add(j.getUUID());
		conCraneo.remove(j.getUUID());
		jugadas.remove(j.getUUID());
		cerrar(j);
		j.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 1f);
		avisarTodos(j.server, Component.literal(j.getName().getString() + " se sacó el cráneo (" + salvados.size()
				+ "/" + cupo + ").").withColor(0x7CF08A));
		sincronizarCraneos(j.server);
		revisarFinDeRonda(j.server);
	}

	/** Se llenaron los lugares: los que siguen con el cráneo esperan el /bulag 2 explotar. */
	private static void revisarFinDeRonda(MinecraftServer server) {
		if (!enRonda) return;
		if (conCraneo.isEmpty()) {
			terminarRonda(server);
			return;
		}
		if (!lugaresLlenos && salvados.size() >= cupo) {
			lugaresLlenos = true;
			for (UUID uuid : conCraneo) {
				ServerPlayer j = server.getPlayerList().getPlayer(uuid);
				if (j == null) continue;
				enviar(j, "", 0, "SE ACABARON LOS LUGARES", true);
				j.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 0.6f, 1.4f);
			}
			avisarTodos(server, Component.literal("Se llenaron los lugares. " + conCraneo.size()
					+ " siguen con el cráneo...").withColor(0xFF4040));
		}
	}

	/** /bulag 2 explotar: a todos los que tienen el cráneo les explota. Devuelve cuántos. */
	private static int explotar(MinecraftServer server) {
		int cuantos = 0;
		for (UUID uuid : List.copyOf(conCraneo)) {
			ServerPlayer j = server.getPlayerList().getPlayer(uuid);
			if (j == null) {
				conCraneo.remove(uuid);
				continue;
			}
			explotarJugador(server, j, null);
			cuantos++;
		}
		if (enRonda) terminarRonda(server);
		return cuantos;
	}

	private static void explotarJugador(MinecraftServer server, ServerPlayer j, String motivo) {
		conCraneo.remove(j.getUUID());
		jugadas.remove(j.getUUID());
		vivos.remove(j.getUUID());
		cerrar(j);
		var mundo = j.serverLevel();
		mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, j.getX(), j.getEyeY(), j.getZ(), 1, 0, 0, 0, 0);
		mundo.sendParticles(ParticleTypes.LAVA, j.getX(), j.getEyeY(), j.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
		mundo.playSound(null, j.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2f, 0.9f);
		j.setGameMode(GameType.SPECTATOR);
		server.getPlayerList().broadcastSystemMessage(Component.literal("¡Al cráneo de " + j.getName().getString()
				+ " le explotó la cabeza!" + (motivo == null ? "" : " (" + motivo + ")")).withColor(0xFF4040), false);
		sincronizarCraneos(server);
	}

	private static void terminarRonda(MinecraftServer server) {
		enRonda = false;
		lugaresLlenos = false;
		for (UUID uuid : conCraneo) {
			ServerPlayer j = server.getPlayerList().getPlayer(uuid);
			if (j != null) cerrar(j);
		}
		conCraneo.clear();
		jugadas.clear();
		sincronizarCraneos(server);
		if (!niveles.isEmpty()) {
			pausa = PAUSA;
			avisarTodos(server, Component.literal("El nivel " + niveles.get(0) + " empieza en 5 segundos...").withColor(0xFFA23C));
		} else {
			terminarJuego(server);
		}
	}

	private static void terminarJuego(MinecraftServer server) {
		niveles.clear();
		enRonda = false;
		pausa = 0;
		List<String> nombres = new ArrayList<>();
		for (UUID uuid : vivos) {
			ServerPlayer j = server.getPlayerList().getPlayer(uuid);
			if (j != null) nombres.add(j.getName().getString());
		}
		if (!nombres.isEmpty()) {
			server.getPlayerList().broadcastSystemMessage(Component.literal("Bulag 2 terminado. Se salvaron: "
					+ String.join(", ", nombres)).withColor(0x7CF08A), false);
		}
		vivos.clear();
	}

	private static void parar(MinecraftServer server) {
		for (UUID uuid : conCraneo) {
			ServerPlayer j = server.getPlayerList().getPlayer(uuid);
			if (j != null) cerrar(j);
		}
		niveles.clear();
		vivos.clear();
		conCraneo.clear();
		jugadas.clear();
		salvados.clear();
		enRonda = false;
		lugaresLlenos = false;
		pausa = 0;
		sincronizarCraneos(server);
	}

	private static void salio(MinecraftServer server, UUID uuid) {
		vivos.remove(uuid);
		jugadas.remove(uuid);
		if (conCraneo.remove(uuid)) {
			sincronizarCraneos(server);
			revisarFinDeRonda(server);
		}
	}

	private static void avisarTodos(MinecraftServer server, Component mensaje) {
		server.getPlayerList().broadcastSystemMessage(mensaje, false);
	}

	private static void sincronizarCraneos(MinecraftServer server) {
		CraneosPayload payload = new CraneosPayload(List.copyOf(new HashSet<>(conCraneo)));
		for (ServerPlayer j : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(j, payload);
	}

	// ---------------------------------------------------------------- Cuentas

	/** {a, b, operación, resultado}. Las restas nunca dan negativo y las divisiones siempre son exactas. */
	static int[] generar(int nivel) {
		return switch (nivel) {
			case 1 -> AZAR.nextBoolean() ? suma(1, 20) : resta(1, 20);
			case 2 -> {
				int tipo = AZAR.nextInt(10);
				if (tipo == 0) yield multiplicacion(2, 9, 2, 9);
				if (tipo == 1) yield division(2, 9, 2, 9);
				yield tipo < 6 ? suma(10, 99) : resta(10, 99);
			}
			default -> AZAR.nextBoolean() ? multiplicacion(6, 15, 3, 12) : division(3, 12, 12, 125);
		};
	}

	private static int entre(int desde, int hasta) {
		return desde + AZAR.nextInt(hasta - desde + 1);
	}

	private static int[] suma(int desde, int hasta) {
		int a = entre(desde, hasta), b = entre(desde, hasta);
		return new int[]{a, b, '+', a + b};
	}

	private static int[] resta(int desde, int hasta) {
		int a = entre(desde, hasta), b = entre(desde, hasta);
		return new int[]{Math.max(a, b), Math.min(a, b), '-', Math.abs(a - b)};
	}

	private static int[] multiplicacion(int aDesde, int aHasta, int bDesde, int bHasta) {
		int a = entre(aDesde, aHasta), b = entre(bDesde, bHasta);
		return new int[]{a, b, 'x', a * b};
	}

	/** Dividendo ÷ divisor con resultado exacto. */
	private static int[] division(int divDesde, int divHasta, int resDesde, int resHasta) {
		int divisor = entre(divDesde, divHasta), resultado = entre(resDesde, resHasta);
		return new int[]{divisor * resultado, divisor, '÷', resultado};
	}
}
