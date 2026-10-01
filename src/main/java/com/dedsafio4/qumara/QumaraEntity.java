package com.dedsafio4.qumara;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Qumara (la Flor Mutante): un jefe de 40 bloques de alto, pegado al suelo, que maneja un admin montado
 * en su cara. Aparece como una rosa gigante cerrada. Botones:
 * 1. Nacer (una sola vez): la rosa se abre y sale Qumara.
 * 2. Giro: saca un brazo que da una vuelta entera (30 bloques de alcance); al que le pega le saca vida.
 * 4. Círculos Explosivos en todos los jugadores (el nivel depende de la vida).
 * 5. Atraer: durante 15 segundos todos los jugadores se acercan a la planta. Al terminar agarra al más
 *    cercano con el brazo, lo sube delante de la cara y le tira gas: el agarrado juega un minijuego
 *    (15 aciertos con el espacio y se salva; 5 fallos o 10 segundos sin pulsar y muere, salvo con tótem).
 *    Si se salva, lo tira lejos (el "Sacar" del diseño).
 * 6. Gas: una nube de Gas Morado en la posición de cada jugador (da el Veneno Primitivo).
 * Estar pegado a la planta saca 5 corazones (cada 3 segundos, y te empuja para atrás). Los admins y el que
 * la maneja no son afectados por nada de esto. Se la puede lastimar con proyectiles.
 * Enfriamiento (solo, al llegar la vida a cada pinchito de la barra: 75%, 50% y 25%): se debilita (se
 * marchita y se pone celeste), no puede atacar ni recibir daño; aparecen 5 Bichos no de la cabeza (a 30
 * bloques) y el Capullo, que se cierra y larga Bichos de la cabeza si le pegan. Cuando mueren los 5, se
 * recupera.
 * Todo el cuerpo recibe golpes. Al morir hace la animación de derrota y queda ahí (no desaparece).
 *
 * Los tiempos de cada animación se guardan como el tick del mundo en que empezaron, así el cliente
 * dibuja exactamente lo mismo que calcula el servidor.
 */
public class QumaraEntity extends PathfinderMob {
	public static final float TICKS_NACER = 6.2f * 20, TICKS_BARRIDO = 7f * 20, TICKS_GRITO = 3.2f * 20;
	/** El barrido se puede volver a usar a los 10 segundos de haberlo usado. */
	public static final int RECARGA_BARRIDO = 10 * 20;
	/** Alcance del brazo (desde el centro) y lo que saca al pegar. */
	public static final double ALCANCE = 30;
	private static final float DANIO_BARRIDO = 14f;
	/** Dónde van los ojos del que la maneja: delante de la cara. */
	private static final Vec3 OJOS_JINETE = new Vec3(0, 30, 8.5);

	private static final EntityDataAccessor<Boolean> NACIDA = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Long> T_NACER = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> T_BARRIDO = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> T_CIRCULOS = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> T_ATRAER = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> T_GAS = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	/** El agarre: a quién (id, -1 nadie), cuándo lo agarró y cuándo lo empezó a sacar, dónde estaba y dónde cae, y el minijuego. */
	/** /boss 1 ai: la maneja una IA (para practicar). */
	private static final EntityDataAccessor<Boolean> IA = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> AGARRADO = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> T_AGARRE = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> T_SACAR = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<org.joml.Vector3f> AGARRE_INICIO = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<org.joml.Vector3f> AGARRE_CAE = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Integer> ACIERTOS = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FALLOS = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> T_PULSO = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> T_GRITO = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Boolean> DEBIL = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DERROTADA = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Long> T_DERROTA = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> PINCHITOS_PASADOS = SynchedEntityData.defineId(QumaraEntity.class, EntityDataSerializers.INT);

	/** Del barrido en curso: a quiénes ya les pegó y dónde estaba el brazo el tick anterior. */
	private final Set<UUID> golpeados = new HashSet<>();
	private double brazoAntes = Double.NaN;

	/** Del lado del cliente: qué tan debilitada se ve (de 0 a 1, cambia de a poco). */
	public float debilVisto, debilVistoAntes;

	public QumaraEntity(EntityType<? extends PathfinderMob> tipo, Level level) {
		super(tipo, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 1000)
				.add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1)
				.add(Attributes.ARMOR, 6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(NACIDA, false);
		builder.define(T_NACER, Long.MIN_VALUE / 2);
		builder.define(T_BARRIDO, Long.MIN_VALUE / 2);
		builder.define(T_CIRCULOS, Long.MIN_VALUE / 2);
		builder.define(T_ATRAER, Long.MIN_VALUE / 2);
		builder.define(T_GAS, Long.MIN_VALUE / 2);
		builder.define(IA, false);
		builder.define(AGARRADO, -1);
		builder.define(T_AGARRE, Long.MIN_VALUE / 2);
		builder.define(T_SACAR, Long.MIN_VALUE / 2);
		builder.define(AGARRE_INICIO, new org.joml.Vector3f());
		builder.define(AGARRE_CAE, new org.joml.Vector3f());
		builder.define(ACIERTOS, 0);
		builder.define(FALLOS, 0);
		builder.define(T_PULSO, Long.MIN_VALUE / 2);
		builder.define(T_GRITO, Long.MIN_VALUE / 2);
		builder.define(DEBIL, false);
		builder.define(DERROTADA, false);
		builder.define(T_DERROTA, Long.MIN_VALUE / 2);
		builder.define(PINCHITOS_PASADOS, 0);
	}

	// --- Estado (lo lee también el dibujo) ---

	/** Ticks (con fracción) desde un momento guardado. */
	public float desde(EntityDataAccessor<Long> cual, float parcial) {
		return (float) (level().getGameTime() - entityData.get(cual)) + parcial;
	}

	public boolean nacida() { return entityData.get(NACIDA); }
	public boolean debil() { return entityData.get(DEBIL); }
	public boolean derrotada() { return entityData.get(DERROTADA); }
	public float tNacer(float parcial) { return desde(T_NACER, parcial); }
	public float tBarrido(float parcial) { return desde(T_BARRIDO, parcial); }
	public float tGrito(float parcial) { return desde(T_GRITO, parcial); }
	public float tDerrota(float parcial) { return desde(T_DERROTA, parcial); }

	/** ¿Está en medio de la transformación (rosa → Qumara)? */
	public boolean naciendo() {
		return nacida() && tNacer(0) < TICKS_NACER;
	}

	/** ¿Todavía es la rosa cerrada? */
	public boolean esRosa() {
		return !nacida();
	}

	public boolean barriendo() {
		return tBarrido(0) < TICKS_BARRIDO;
	}

	/** Cuánto falta para poder usar el barrido otra vez, en ticks (0 = ya se puede). */
	public int recargaBarrido() {
		return Math.max(0, RECARGA_BARRIDO - (int) tBarrido(0));
	}

	// --- Botones (los manda el que la maneja) ---

	public void accion(ServerPlayer jugador, int boton) {
		if (jugador != getControllingPassenger()) return;   // el agarrado no la maneja
		switch (boton) {
			case 1 -> nacer(jugador);
			case 2 -> barrer(jugador);
			case 4 -> circulos(jugador);
			case 5 -> atraer(jugador);
			case 6 -> gasATodos(jugador);
			default -> {}
		}
	}

	private void avisar(@Nullable ServerPlayer jugador, String texto) {
		if (jugador == null) return;   // la IA no necesita avisos
		jugador.displayClientMessage(Component.literal(texto).withColor(0xF0418F), true);
	}

	private void nacer(ServerPlayer jugador) {
		if (derrotada()) return;
		if (nacida()) {
			avisar(jugador, "Qumara ya nació.");
			return;
		}
		entityData.set(NACIDA, true);
		entityData.set(T_NACER, level().getGameTime());
		level().playSound(null, getX(), getY() + 20, getZ(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.HOSTILE, 6f, 0.5f);
	}

	private void barrer(ServerPlayer jugador) {
		if (!nacida() || naciendo() || derrotada()) return;
		if (agarrando()) {
			avisar(jugador, "Tiene el brazo ocupado agarrando a alguien.");
			return;
		}
		if (debil()) {
			avisar(jugador, "Está debilitada: no puede atacar.");
			return;
		}
		if (barriendo() || recargaBarrido() > 0) {
			avisar(jugador, "Giro: faltan " + (int) Math.ceil(recargaBarrido() / 20f) + " s.");
			return;
		}
		entityData.set(T_BARRIDO, level().getGameTime());
		golpeados.clear();
		brazoAntes = Double.NaN;
		gritar();
	}

	/**
	 * Los pinchitos de la barra de vida: al 75%, 50% y 25% de la vida entra sola en Enfriamiento
	 * (una vez en cada uno). No se puede activar con un botón.
	 */
	public static final float[] PINCHITOS = {0.75f, 0.5f, 0.25f};
	/** Cuántos Bichos no de la cabeza salen en cada Enfriamiento (hay que matarlos a todos para que se recupere). */
	public static final int BICHOS_NO = 5;

	/** Cuántos pinchitos ya pasó (0 a 3). */
	public int pinchitos() {
		return entityData.get(PINCHITOS_PASADOS);
	}

	/** Si la vida llegó al próximo pinchito: la deja justo ahí y entra en Enfriamiento. Devuelve true si entró. */
	private boolean quizasEnfriar() {
		int pasados = pinchitos();
		if (pasados >= PINCHITOS.length || debil() || derrotada()) return false;
		float umbral = PINCHITOS[pasados] * getMaxHealth();
		if (getHealth() > umbral) return false;
		setHealth(umbral);
		entityData.set(PINCHITOS_PASADOS, pasados + 1);
		entityData.set(DEBIL, true);
		entityData.set(T_BARRIDO, Long.MIN_VALUE / 2);   // si estaba barriendo, el brazo se esconde
		if (level() instanceof ServerLevel mundo) sacarBichos(mundo);
		level().playSound(null, getX(), getY() + 20, getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 5f, 0.5f);
		return true;
	}

	/**
	 * 5 Bichos no de la cabeza a 30 bloques, repartidos alrededor, y un Capullo para cada uno, a 20 bloques
	 * de él (hacia afuera). Los Capullos de antes se van. Del Capullo salen los Bichos de la cabeza cuando le pegan.
	 */
	private void sacarBichos(ServerLevel mundo) {
		double a0 = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < BICHOS_NO; i++) {
			double a = a0 + i * Math.PI * 2 / BICHOS_NO;
			poner(mundo, ModQumara.BICHO_NO_CABEZA, getX() + Math.cos(a) * 30, getZ() + Math.sin(a) * 30, (float) Math.toDegrees(a) + 90);
		}
		mundo.getEntitiesOfClass(CapulloEntity.class, getBoundingBox().inflate(120)).forEach(Entity::discard);
		for (int i = 0; i < BICHOS_NO; i++) {
			double a = a0 + i * Math.PI * 2 / BICHOS_NO;
			poner(mundo, ModQumara.CAPULLO, getX() + Math.cos(a) * 50, getZ() + Math.sin(a) * 50, random.nextFloat() * 360);
		}
		mundo.playSound(null, getX(), getY() + 5, getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 4f, 0.7f);
	}

	private static void poner(ServerLevel mundo, EntityType<?> tipo, double x, double z, float giro) {
		int y = mundo.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
		Entity e = tipo.create(mundo);
		if (e == null) return;
		if (e instanceof CapulloEntity capullo) capullo.setAbierto(false);   // aparece durante el Enfriamiento
		e.moveTo(x, y, z, giro, 0);
		mundo.addFreshEntity(e);
		mundo.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, x, y + 3, z, 30, 1.5, 2, 1.5, 0.05);
	}

	/** Cuando se mueren los Bichos no de la cabeza, se recupera del Enfriamiento. */
	private void quizasRecuperarse(ServerLevel mundo) {
		if (!debil() || tickCount % 10 != 0) return;
		boolean quedan = !mundo.getEntitiesOfClass(BichoCuboEntity.class, getBoundingBox().inflate(120), b -> !b.deLaCabeza() && b.isAlive()).isEmpty();
		if (quedan) return;
		entityData.set(DEBIL, false);
		mundo.playSound(null, getX(), getY() + 20, getZ(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.HOSTILE, 5f, 0.8f);
		gritar();
	}

	/** Cada cuánto se pueden usar los Círculos (8 segundos desde que se usaron). */
	public static final int RECARGA_CIRCULOS = 8 * 20;

	/** Los admins (permiso 2, los que usan comandos): a ellos no les hace nada. Se puede cambiar para las pruebas. */
	public static java.util.function.Predicate<Player> esAdmin = p -> p.hasPermissions(2);

	/**
	 * ¿A este jugador le afectan los ataques? A todos menos a los admins, al que la maneja, y a los de creativo
	 * o espectador. Con la IA (para practicar) los admins también.
	 */
	public boolean esBlanco(Player p) {
		return p.isAlive() && !p.isSpectator() && !p.isCreative() && (ia() || !esAdmin.test(p)) && !hasPassenger(p) && p.level() == level();
	}

	// --- IA (/boss 1 ai) ---

	public boolean ia() { return entityData.get(IA); }
	public void setIa(boolean valor) { entityData.set(IA, valor); }

	/** Hasta dónde "ve" jugadores la IA, y la pausa entre ataque y ataque. */
	private static final double IA_VISTA = 80;
	private int iaEspera = 40;

	/**
	 * La IA: nace cuando hay alguien cerca, gira hacia el jugador más cercano y, cada 2,5 a 5 segundos, usa un
	 * ataque de los que tenga listos: el Giro (si alguien está al alcance del brazo), los Círculos, o el
	 * Atraer (que termina agarrando al más cercano). No hace nada mientras alguien la maneja.
	 */
	private void tickIa(ServerLevel mundo) {
		if (!ia() || getControllingPassenger() != null || derrotada()) return;
		ServerPlayer blanco = null;
		double d2 = IA_VISTA * IA_VISTA;
		for (ServerPlayer p : mundo.players()) {
			if (!esBlanco(p)) continue;
			double d = Mth.square(p.getX() - getX()) + Mth.square(p.getZ() - getZ());
			if (d < d2) {
				d2 = d;
				blanco = p;
			}
		}
		if (blanco == null) return;
		if (!nacida()) {
			nacer(null);
			return;
		}
		if (naciendo()) return;
		// Mira hacia el más cercano (no mientras gira el brazo ni mientras tiene a alguien agarrado).
		if (!barriendo() && !agarrando() && !debil()) {
			float objetivo = (float) (Mth.atan2(blanco.getZ() - getZ(), blanco.getX() - getX()) * Mth.RAD_TO_DEG) - 90;
			setYRot(Mth.approachDegrees(getYRot(), objetivo, 3));
			yRotO = yBodyRot = yHeadRot = getYRot();
		}
		if (iaEspera > 0) {
			iaEspera--;
			return;
		}
		if (debil() || agarrando() || barriendo()) return;
		double d = Math.sqrt(d2);
		java.util.List<Runnable> ataques = new java.util.ArrayList<>();
		if (recargaBarrido() == 0 && d <= ALCANCE + 2) {
			for (int i = 0; i < 3; i++) ataques.add(() -> barrer(null));
		}
		if (recargaCirculos() == 0) {
			for (int i = 0; i < 2; i++) ataques.add(() -> circulos(null));
		}
		if (recargaGas() == 0) {
			for (int i = 0; i < 2; i++) ataques.add(() -> gasATodos(null));
		}
		if (recargaAtraer() == 0 && !atrayendo()) {
			for (int i = 0; i < (d > ALCANCE ? 4 : 2); i++) ataques.add(() -> atraer(null));
		}
		if (ataques.isEmpty()) return;
		ataques.get(random.nextInt(ataques.size())).run();
		iaEspera = 50 + random.nextInt(51);
	}

	// --- Botón 5: Atraer ---

	/** Lo que dura el Atraer, y cada cuánto se puede usar (desde que empezó). */
	public static final int TICKS_ATRAER = 15 * 20, RECARGA_ATRAER = 30 * 20;
	/** A qué velocidad los trae (bloques por tick: 10 bloques por segundo, el doble de correr). */
	public static final double VELOCIDAD_ATRAER = 0.5;

	public boolean atrayendo() {
		return level().getGameTime() - entityData.get(T_ATRAER) < TICKS_ATRAER && !debil() && !derrotada();
	}

	public int recargaAtraer() {
		return (int) Math.max(0, RECARGA_ATRAER - (level().getGameTime() - entityData.get(T_ATRAER)));
	}

	private void atraer(ServerPlayer jugador) {
		if (!nacida() || naciendo() || derrotada()) return;
		if (debil()) {
			avisar(jugador, "Está debilitada: no puede atacar.");
			return;
		}
		if (recargaAtraer() > 0) {
			avisar(jugador, "Atraer: faltan " + (int) Math.ceil(recargaAtraer() / 20f) + " s.");
			return;
		}
		if (!(level() instanceof ServerLevel mundo)) return;
		atraerYa(mundo);
		avisar(jugador, "Atraer: 15 s");
	}

	/** Empieza el Atraer (sin controles: lo usan el botón 5 y las pruebas). */
	public void atraerYa(ServerLevel mundo) {
		entityData.set(T_ATRAER, mundo.getGameTime());
		entityData.set(T_GRITO, mundo.getGameTime());
		mundo.playSound(null, getX(), getY() + 25, getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 8f, 0.5f);
		Component texto = Component.literal("¡Qumara te está atrayendo!").withColor(0xF0418F);
		for (ServerPlayer p : mundo.players()) if (esBlanco(p)) p.displayClientMessage(texto, true);
	}

	/**
	 * Cada tick del Atraer: las partículas en los que trae. El movimiento lo hace el cliente de cada uno
	 * (AtraerCliente), así va igual de rápido caminando o saltando.
	 */
	private void atraerJugadores(ServerLevel mundo) {
		for (ServerPlayer p : mundo.players()) {
			if (!esBlanco(p)) continue;
			double dx = getX() - p.getX(), dz = getZ() - p.getZ(), d = Math.sqrt(dx * dx + dz * dz);
			if (d < RADIO_CERCA - 1) continue;
			if (tickCount % 4 == 0) mundo.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.5, 0.3, 0.3);
		}
	}

	// --- Botón 6: Gas a todos ---

	/** Cada cuánto se puede usar, y cuánto dura cada nube. */
	public static final int RECARGA_GAS = 20 * 20, TICKS_NUBE_GAS = 6 * 20;

	public int recargaGas() {
		return (int) Math.max(0, RECARGA_GAS - (level().getGameTime() - entityData.get(T_GAS)));
	}

	/** Una nube de Gas Morado en la posición de cada jugador (menos admins y el que la maneja, salvo con la IA). */
	private void gasATodos(@Nullable ServerPlayer jugador) {
		if (!nacida() || naciendo() || derrotada()) return;
		if (debil()) {
			avisar(jugador, "Está debilitada: no puede atacar.");
			return;
		}
		if (recargaGas() > 0) {
			avisar(jugador, "Gas: faltan " + (int) Math.ceil(recargaGas() / 20f) + " s.");
			return;
		}
		if (!(level() instanceof ServerLevel mundo)) return;
		entityData.set(T_GAS, mundo.getGameTime());
		entityData.set(T_GRITO, mundo.getGameTime());   // abre la boca
		int nubes = 0;
		for (ServerPlayer p : mundo.players()) {
			if (!esBlanco(p) || p.getVehicle() == this) continue;
			double y = p.onGround() ? p.getY() : piso(mundo, p.getX(), p.getY(), p.getZ());
			mundo.addFreshEntity(GasMoradoEntity.nube(mundo, new Vec3(p.getX(), y, p.getZ()), TICKS_NUBE_GAS, 1.6f));
			mundo.playSound(null, p.getX(), y, p.getZ(), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.HOSTILE, 1.5f, 0.5f);
			nubes++;
		}
		mundo.playSound(null, getX(), getY() + 25, getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 6f, 1.5f);
		avisar(jugador, "Gas a " + nubes + (nubes == 1 ? " jugador" : " jugadores"));
	}

	// --- Agarrar (al terminar el Atraer) ---

	public static final int ACIERTOS_PARA_SALVARSE = 15, FALLOS_PARA_MORIR = 5, SEGUNDOS_SIN_PULSAR = 10;
	/** Hasta dónde llega el brazo para agarrar (desde el centro). */
	private static final double ALCANCE_AGARRE = 30;

	public int agarradoId() { return entityData.get(AGARRADO); }
	public boolean agarrando() { return entityData.get(AGARRADO) >= 0; }
	public int aciertos() { return entityData.get(ACIERTOS); }
	public int fallos() { return entityData.get(FALLOS); }
	public long inicioAgarre() { return entityData.get(T_AGARRE); }

	/** Segundos desde que lo agarró. */
	public float tAgarre(float parcial) {
		return (level().getGameTime() - entityData.get(T_AGARRE) + parcial) / 20f;
	}

	/** Segundos desde que lo empezó a sacar (-1 si todavía lo sostiene). */
	public float tSacar(float parcial) {
		long t = entityData.get(T_SACAR);
		return t < entityData.get(T_AGARRE) ? -1 : (level().getGameTime() - t + parcial) / 20f;
	}

	/** El tiempo del diseño (gT) del agarre. */
	public float tiempoAgarre(float parcial) {
		return QumaraAgarre.tiempo(tAgarre(parcial), tSacar(parcial));
	}

	/** Cuánto le queda para pulsar antes de que lo mate (segundos). */
	public float tiempoParaPulsar(float parcial) {
		long desde = Math.max(entityData.get(T_PULSO), entityData.get(T_AGARRE) + (long) (QumaraAgarre.MINIJUEGO * 20));
		return SEGUNDOS_SIN_PULSAR - (level().getGameTime() - desde + parcial) / 20f;
	}

	public boolean minijuegoActivo() {
		return agarrando() && tSacar(0) < 0 && tAgarre(0) >= QumaraAgarre.MINIJUEGO;
	}

	private QumaraAgarre.Pose alcanceCache;
	private long alcanceDe = Long.MIN_VALUE;

	/** La pose del brazo para llegar a donde estaba el agarrado (se calcula una vez por agarre). */
	public QumaraAgarre.Pose poseAlcance() {
		long t = entityData.get(T_AGARRE);
		if (alcanceCache == null || alcanceDe != t) {
			alcanceCache = QumaraAgarre.alcance(vec(entityData.get(AGARRE_INICIO)));
			alcanceDe = t;
		}
		return alcanceCache;
	}

	private static Vec3 vec(org.joml.Vector3f v) {
		return new Vec3(v.x, v.y, v.z);
	}

	/** Del mundo al cuerpo de Qumara, y al revés. */
	private Vec3 alCuerpo(Vec3 mundo) {
		return mundo.subtract(position()).yRot(getYRot() * Mth.DEG_TO_RAD);
	}

	private Vec3 alMundo(Vec3 cuerpo) {
		return cuerpo.yRot(-getYRot() * Mth.DEG_TO_RAD).add(position());
	}

	/** Dónde van los pies del agarrado ahora (en el mundo). */
	public Vec3 piesAgarrado(float parcial) {
		return alMundo(QumaraAgarre.pies(tiempoAgarre(parcial), poseAlcance(), vec(entityData.get(AGARRE_INICIO)), vec(entityData.get(AGARRE_CAE))));
	}

	/** La boca (en el mundo), de donde sale el gas. */
	public Vec3 boca() {
		return alMundo(new Vec3(0, 24, 9.3));
	}

	/** Al terminar el Atraer: agarra al jugador más cercano (si el brazo llega). */
	private void agarrarAlMasCercano(ServerLevel mundo) {
		ServerPlayer mejor = null;
		double d = ALCANCE_AGARRE * ALCANCE_AGARRE;
		for (ServerPlayer p : mundo.players()) {
			if (!esBlanco(p) || Math.abs(p.getY() - getY()) > 20) continue;
			double dx = p.getX() - getX(), dz = p.getZ() - getZ();
			if (dx * dx + dz * dz < d) {
				d = dx * dx + dz * dz;
				mejor = p;
			}
		}
		if (mejor != null) agarrar(mejor);
	}

	public void agarrar(ServerPlayer p) {
		if (!(level() instanceof ServerLevel mundo) || agarrando()) return;
		Vec3 inicio = alCuerpo(p.position());
		entityData.set(AGARRE_INICIO, new org.joml.Vector3f((float) inicio.x, (float) inicio.y, (float) inicio.z));
		entityData.set(T_AGARRE, mundo.getGameTime());
		entityData.set(T_SACAR, Long.MIN_VALUE / 2);
		entityData.set(ACIERTOS, 0);
		entityData.set(FALLOS, 0);
		entityData.set(T_PULSO, Long.MIN_VALUE / 2);
		entityData.set(AGARRADO, p.getId());
		p.startRiding(this, true);
		mundo.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 3f, 0.6f);
	}

	/** El minijuego: el agarrado pulsó el espacio (acertó o no). */
	public void pulso(ServerPlayer p, boolean acierto) {
		if (!minijuegoActivo() || p.getId() != agarradoId()) return;
		entityData.set(acierto ? ACIERTOS : FALLOS, entityData.get(acierto ? ACIERTOS : FALLOS) + 1);
		entityData.set(T_PULSO, level().getGameTime());
		level().playSound(null, p.getX(), p.getY(), p.getZ(), acierto ? SoundEvents.NOTE_BLOCK_PLING.value() : SoundEvents.NOTE_BLOCK_BASS.value(),
				SoundSource.PLAYERS, 1f, acierto ? 1.2f + aciertos() * 0.05f : 0.6f);
	}

	private void tickAgarre(ServerLevel mundo) {
		Entity e = mundo.getEntity(agarradoId());
		ServerPlayer p = e instanceof ServerPlayer sp && sp.isAlive() && !sp.isRemoved() ? sp : null;
		float tS = tSacar(0);
		if (tS < 0) {
			// Lo sostiene.
			if (p == null || debil() || derrotada()) {
				soltar(p);
				return;
			}
			if (p.getVehicle() != this) p.startRiding(this, true);
			float tA = tAgarre(0);
			// (un poco después de montarlo, así no lo tapa el "Shift para bajarse" de Minecraft)
			if (mundo.getGameTime() - entityData.get(T_AGARRE) == 3) {
				p.displayClientMessage(Component.literal("¡Qumara te agarró! Pulsá el espacio cuando el cosito esté en el recuadro").withColor(0xF0418F), true);
			}
			if (tA >= 3.3f && tickCount % 2 == 0) soplarGas(mundo, p);
			if (tA >= QumaraAgarre.MINIJUEGO) {
				if (aciertos() >= ACIERTOS_PARA_SALVARSE) {
					p.displayClientMessage(Component.literal("¡Te salvaste!").withColor(0x7CFC6A), true);
					sacar(mundo, p);
				} else if (fallos() >= FALLOS_PARA_MORIR || tiempoParaPulsar(0) <= 0) {
					matar(mundo, p);
				}
			}
			return;
		}
		// Lo saca: toma impulso, lo tira, cae y queda la nube de gas.
		Vec3 cae = alMundo(vec(entityData.get(AGARRE_CAE)));
		float g = QumaraAgarre.SOSTIENE + tS;
		if (p != null && p.getVehicle() == this && g >= QumaraAgarre.SUELTA) {
			p.stopRiding();
			p.teleportTo(cae.x, cae.y, cae.z);
			p.setDeltaMovement(Vec3.ZERO);
			p.fallDistance = 0;
			p.hurtMarked = true;
			// Queda tirado dentro de una nube de gas (hasta los 11,2 s del diseño).
			mundo.addFreshEntity(GasMoradoEntity.nube(mundo, cae, (int) ((11.2f - QumaraAgarre.SUELTA) * 20), 1.6f));
		}
		if (g >= QumaraAgarre.FIN) entityData.set(AGARRADO, -1);
	}

	/** 10 segundos sin pulsar o 5 fallos: lo mata (salvo que tenga un tótem: entonces lo saca). */
	private void matar(ServerLevel mundo, ServerPlayer p) {
		DamageSource fuente = new DamageSource(mundo.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
				.getHolderOrThrow(ModQumara.DANIO_AGARRE), this);
		p.invulnerableTime = 0;
		p.hurt(fuente, 1_000_000f);
		if (p.isAlive()) {
			sacar(mundo, p);   // el tótem lo salvó
		} else {
			if (p.getVehicle() == this) p.stopRiding();
			sacar(mundo, null);
		}
	}

	/** Empieza el "Sacar": el brazo toma impulso y lo tira lejos. */
	private void sacar(ServerLevel mundo, @Nullable ServerPlayer p) {
		Vec3 cae = alMundo(QumaraAgarre.ATERRIZA);
		int y = mundo.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, Mth.floor(cae.x), Mth.floor(cae.z));
		entityData.set(AGARRE_CAE, new org.joml.Vector3f((float) QumaraAgarre.ATERRIZA.x, (float) (y - getY()), (float) QumaraAgarre.ATERRIZA.z));
		entityData.set(T_SACAR, mundo.getGameTime());
		mundo.playSound(null, getX(), getY() + 20, getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 4f, 0.5f);
	}

	/** Lo suelta sin tirarlo (se debilitó, la derrotaron o el jugador ya no está). */
	private void soltar(@Nullable ServerPlayer p) {
		if (p != null && p.getVehicle() == this) {
			p.stopRiding();
			p.fallDistance = 0;
		}
		entityData.set(AGARRADO, -1);
	}

	/**
	 * Un soplo de Gas Morado desde la boca hacia el agarrado (como el aliento del diseño: sale rápido,
	 * se va frenando y agrandando). Al que toca le da el Veneno Primitivo.
	 */
	private void soplarGas(ServerLevel mundo, ServerPlayer p) {
		Vec3 desde = boca();
		Vec3 dir = p.position().add(0, 1, 0).subtract(desde).normalize();
		Vec3 vel = dir.scale((7 + random.nextDouble() * 3) / 20)
				.add((random.nextDouble() - 0.5) * 3 / 20, (random.nextDouble() - 0.5) * 2 / 20, (random.nextDouble() - 0.5) * 3 / 20);
		mundo.addFreshEntity(GasMoradoEntity.soplo(mundo, desde, vel, 24 + random.nextInt(17), 0.6f + random.nextFloat() * 0.5f));
	}

	// --- Estar pegado a la planta ---

	/** Desde el centro: el cuerpo (12 de radio) y 2 bloques más. */
	public static final double RADIO_CERCA = 14;
	/** Con diamante completo y Protección IV queda en 10 (5 corazones). */
	private static final float DANIO_CERCA = 43.6f;
	private static final int RECARGA_CERCA = 3 * 20;
	private final java.util.Map<UUID, Long> ultimoGolpeCerca = new java.util.HashMap<>();

	private void lastimarAlQueEstaCerca(ServerLevel mundo) {
		long ahora = mundo.getGameTime();
		DamageSource fuente = null;
		for (ServerPlayer p : mundo.players()) {
			if (!esBlanco(p) || p.getY() < getY() - 3 || p.getY() > getY() + 40) continue;
			double dx = p.getX() - getX(), dz = p.getZ() - getZ(), d = Math.sqrt(dx * dx + dz * dz);
			if (d > RADIO_CERCA) continue;
			Long antes = ultimoGolpeCerca.get(p.getUUID());
			if (antes != null && ahora - antes < RECARGA_CERCA) continue;
			ultimoGolpeCerca.put(p.getUUID(), ahora);
			if (fuente == null) {
				fuente = new DamageSource(mundo.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
						.getHolderOrThrow(ModQumara.DANIO_PLANTA), this);
			}
			p.invulnerableTime = 0;
			p.hurt(fuente, DANIO_CERCA);
			// Lo empuja para afuera.
			if (d > 0.01) p.setDeltaMovement(dx / d * 1.6, 0.5, dz / d * 1.6);
			p.hurtMarked = true;
		}
	}

	/** El nivel de los Círculos según la vida: más de 75% nivel 1, más de 50% nivel 2, más de 25% nivel 3, si no nivel 4. */
	public int nivelCirculos() {
		float vida = getHealth() / getMaxHealth();
		return vida > 0.75f ? 1 : vida > 0.5f ? 2 : vida > 0.25f ? 3 : 4;
	}

	public int recargaCirculos() {
		return (int) Math.max(0, RECARGA_CIRCULOS - (level().getGameTime() - entityData.get(T_CIRCULOS)));
	}

	/**
	 * Botón 4: Círculos Explosivos en la posición de cada jugador (menos admins y el que la maneja), más otros al lado,
	 * en fila, según el nivel: nivel 1 → 1 círculo, nivel 2 → 2, nivel 3 → 3, nivel 4 → 4.
	 */
	private void circulos(ServerPlayer jugador) {
		if (!nacida() || naciendo() || derrotada()) return;
		if (debil()) {
			avisar(jugador, "Está debilitada: no puede atacar.");
			return;
		}
		if (recargaCirculos() > 0) {
			avisar(jugador, "Círculos: faltan " + (int) Math.ceil(recargaCirculos() / 20f) + " s.");
			return;
		}
		if (!(level() instanceof ServerLevel mundo)) return;
		entityData.set(T_CIRCULOS, mundo.getGameTime());
		int nivel = nivelCirculos();
		int puestos = 0;
		for (ServerPlayer p : mundo.players()) {
			if (!esBlanco(p)) continue;
			puestos += ponerCirculos(mundo, p.getX(), p.onGround() ? p.getY() : piso(mundo, p.getX(), p.getY(), p.getZ()), p.getZ(),
					nivel, random.nextDouble() * Math.PI * 2, ia());
		}
		avisar(jugador, "Círculos nivel " + nivel + (puestos == 0 ? " (no hay jugadores)" : ""));
		entityData.set(T_GRITO, mundo.getGameTime());   // la animación del grito
		mundo.playSound(null, getX(), getY() + 25, getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 8f, 1.2f);
	}

	/**
	 * Pone los círculos de ese nivel pegados, girados según el ángulo: uno en (x, y, z); en nivel 2 otro al
	 * lado; en nivel 3 y 4 van formando un cuadrado de 2×2 (al 3 le falta una esquina). Devuelve cuántos puso.
	 */
	public static int ponerCirculos(ServerLevel mundo, double x, double y, double z, int nivel, double angulo) {
		return ponerCirculos(mundo, x, y, z, nivel, angulo, false);
	}

	/** contraAdmins: con la IA, los círculos también les pegan a los admins. */
	public static int ponerCirculos(ServerLevel mundo, double x, double y, double z, int nivel, double angulo, boolean contraAdmins) {
		double paso = CirculoEntity.RADIO * 2;
		int[][] lugar = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
		double cos = Math.cos(angulo), sin = Math.sin(angulo);
		int puestos = 0;
		for (int i = 0; i < nivel; i++) {
			double a = lugar[i][0] * paso, b = lugar[i][1] * paso;
			double cx = x + cos * a - sin * b, cz = z + sin * a + cos * b;
			CirculoEntity c = ModQumara.CIRCULO.create(mundo);
			if (c == null) continue;
			c.setNivel(nivel);
			c.contraAdmins = contraAdmins;
			c.moveTo(cx, i == 0 ? y : piso(mundo, cx, y, cz), cz, 0, 0);
			mundo.addFreshEntity(c);
			puestos++;
		}
		return puestos;
	}

	/** El piso cerca de esa altura (buscando un poco arriba y abajo). */
	private static double piso(ServerLevel mundo, double x, double y, double z) {
		net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
		int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
		for (int dy = 6; dy >= -12; dy--) {
			pos.set(bx, (int) Math.floor(y) + dy, bz);
			if (!mundo.getBlockState(pos).getCollisionShape(mundo, pos).isEmpty() && mundo.getBlockState(pos.above()).getCollisionShape(mundo, pos.above()).isEmpty()) {
				return pos.getY() + 1;
			}
		}
		return y;
	}

	/** El grito: animación, sonido fuerte y "¡GRIIIAAAAH!" a los que están cerca. */
	private void gritar() {
		entityData.set(T_GRITO, level().getGameTime());
		level().playSound(null, getX(), getY() + 25, getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 8f, 1.4f);
		level().playSound(null, getX(), getY() + 25, getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6f, 1.6f);
		if (level() instanceof ServerLevel mundo) {
			Component texto = Component.literal("¡GRIIIAAAAH!").withColor(0x8A4AD8);
			for (ServerPlayer p : mundo.players()) if (p.distanceToSqr(this) < 96 * 96) p.displayClientMessage(texto, true);
		}
	}

	// --- Cada tick ---

	/**
	 * Las Qumaras cargadas, para que la encuentren las flechas y los golpes (ver LevelEntitiesMixin). Una lista
	 * para el servidor y otra para el cliente: en un mundo de un jugador las dos Qumaras tienen el mismo
	 * número y Minecraft las considera iguales.
	 */
	private static final Set<QumaraEntity> CARGADAS_SERVIDOR = java.util.concurrent.ConcurrentHashMap.newKeySet(),
			CARGADAS_CLIENTE = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private static Set<QumaraEntity> cargadas(Level level) {
		return level.isClientSide ? CARGADAS_CLIENTE : CARGADAS_SERVIDOR;
	}

	public static void agregarGrandes(Level level, @Nullable Entity excepto, AABB zona, java.util.function.Predicate<? super Entity> filtro, java.util.List<Entity> lista) {
		Set<QumaraEntity> cargadas = cargadas(level);
		if (cargadas.isEmpty()) return;
		for (QumaraEntity q : cargadas) {
			if (q.isRemoved()) {
				cargadas.remove(q);   // descargada o muerta
				continue;
			}
			if (q.level() != level || q == excepto || lista.contains(q)) continue;
			if (q.getBoundingBox().intersects(zona) && filtro.test(q)) lista.add(q);
		}
	}

	@Override
	public void remove(RemovalReason razon) {
		super.remove(razon);
		cargadas(level()).remove(this);
	}

	@Override
	public void tick() {
		super.tick();
		cargadas(level()).add(this);
		setDeltaMovement(0, getDeltaMovement().y, 0);
		if (level().isClientSide) {
			debilVistoAntes = debilVisto;
			debilVisto += ((debil() ? 1 : 0) - debilVisto) * (1.8f / 20f);   // como en el diseño: 1,8 por segundo
			return;
		}
		// A los 4,8 s de la transformación grita.
		if (nacida()) {
			long n = level().getGameTime() - entityData.get(T_NACER);
			if (n == (long) (4.8f * 20)) gritar();
		}
		if (barriendo() && !debil() && !derrotada()) golpearConElBrazo();
		if (level() instanceof ServerLevel mundo && agarrando()) tickAgarre(mundo);
		if (level() instanceof ServerLevel mundo) tickIa(mundo);
		if (level() instanceof ServerLevel mundo && nacida() && !naciendo() && !derrotada()) {
			// Al terminar el Atraer, agarra al más cercano.
			if (!debil() && !agarrando() && mundo.getGameTime() - entityData.get(T_ATRAER) == TICKS_ATRAER) agarrarAlMasCercano(mundo);
			if (atrayendo()) atraerJugadores(mundo);
			lastimarAlQueEstaCerca(mundo);
		}
		if (level() instanceof ServerLevel mundo && !derrotada()) quizasRecuperarse(mundo);
	}

	/** Hacia dónde apunta el brazo (en el cuerpo), en radianes, como en el diseño. */
	public static double yawBrazo(float tSeg) {
		double a0 = -Math.PI / 2;
		return a0 + 0.6 * suave((tSeg - 1.4f) / 0.6f) - (2 * Math.PI + 0.6) * suave((tSeg - 2f) / 3.6f);
	}

	public static float suave(float x) {
		return x < 0 ? 0 : x > 1 ? 1 : x * x * (3 - 2 * x);
	}

	/**
	 * Durante la vuelta (de 2 a 5,6 s), le pega a quien esté dentro del alcance cuando el brazo pasa por
	 * su dirección. Una vez por barrido a cada uno.
	 */
	private void golpearConElBrazo() {
		float tSeg = tBarrido(0) / 20f;
		double ahora = yawBrazo(tSeg);
		double antes = brazoAntes;
		brazoAntes = ahora;
		if (tSeg < 2f || tSeg > 5.6f || Double.isNaN(antes)) return;
		// Para pasar del mundo al cuerpo se deshace el giro del cuerpo (el dibujo gira el cuerpo en -yaw).
		double cuerpo = getYRot() * Mth.DEG_TO_RAD;
		AABB zona = getBoundingBox().inflate(ALCANCE, 4, ALCANCE);
		for (LivingEntity quien : level().getEntitiesOfClass(LivingEntity.class, zona, e -> e != this && e.isAlive() && !hasPassenger(e))) {
			if (golpeados.contains(quien.getUUID())) continue;
			if (quien instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
			Vec3 d = quien.position().subtract(position());
			double dist = Math.hypot(d.x, d.z);
			if (dist > ALCANCE + 1 || quien.getY() > getY() + 12 || quien.getY() < getY() - 4) continue;
			// Ángulo del jugador medido igual que el brazo: local x = (cos a, -sin a) en (x, z) del cuerpo.
			Vec3 local = d.yRot((float) cuerpo);
			double ang = Math.atan2(-local.z, local.x);
			if (!pasoPor(antes, ahora, ang)) continue;
			golpeados.add(quien.getUUID());
			quien.hurt(damageSources().mobAttack(this), DANIO_BARRIDO);
			Vec3 afuera = new Vec3(d.x, 0, d.z).normalize();
			quien.push(afuera.x * 1.6, 0.6, afuera.z * 1.6);
			quien.hurtMarked = true;
		}
	}

	/** ¿El brazo pasó por el ángulo "a" yendo de "desde" a "hasta" (gira hacia valores más chicos)? */
	private static boolean pasoPor(double desde, double hasta, double a) {
		double lo = Math.min(desde, hasta), hi = Math.max(desde, hasta);
		for (int k = -2; k <= 2; k++) {
			double x = a + k * 2 * Math.PI;
			if (x >= lo - 0.05 && x <= hi + 0.05) return true;
		}
		return false;
	}

	// --- Golpes y derrota ---

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		if (derrotada() && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		if (!nacida() && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;   // la rosa no se puede lastimar
		if (debil() && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;   // en Enfriamiento tampoco
		boolean pego = super.hurt(fuente, cantidad);
		if (pego && isAlive()) quizasEnfriar();
		return pego;
	}

	/** En vez de morir y desaparecer, queda derrotada (con /kill sí desaparece). */
	@Override
	public void die(DamageSource fuente) {
		if (fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || derrotada()) {
			super.die(fuente);
			return;
		}
		// Un golpe que la mataría sin haber pasado todos los pinchitos: queda en el pinchito y se enfría.
		if (pinchitos() < PINCHITOS.length) {
			setHealth(0.01f);
			quizasEnfriar();
			return;
		}
		setHealth(1f);
		entityData.set(DERROTADA, true);
		entityData.set(DEBIL, false);
		entityData.set(T_DERROTA, level().getGameTime());
		entityData.set(T_BARRIDO, Long.MIN_VALUE / 2);
		ejectPassengers();
		level().playSound(null, getX(), getY() + 25, getZ(), SoundEvents.RAVAGER_DEATH, SoundSource.HOSTILE, 8f, 0.7f);
		level().playSound(null, getX(), getY() + 25, getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6f, 1.2f);
		if (level() instanceof ServerLevel mundo) {
			mundo.getServer().getPlayerList().broadcastSystemMessage(Component.literal("¡Qumara fue derrotada!").withColor(0xF0418F), false);
		}
	}

	// --- El que la maneja ---

	@Override
	protected InteractionResult mobInteract(Player jugador, InteractionHand mano) {
		if (derrotada() || getControllingPassenger() != null || jugador.getId() == agarradoId() || !(jugador.hasPermissions(2) || jugador.isCreative())
				|| com.dedsafio4.AdminComandos.bloqueado(jugador)) return InteractionResult.PASS;
		if (!level().isClientSide) jugador.startRiding(this);
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	/** La maneja el que está montado (no el que tiene agarrado). */
	@Nullable
	@Override
	public LivingEntity getControllingPassenger() {
		for (Entity e : getPassengers()) if (e instanceof Player jugador && e.getId() != agarradoId()) return jugador;
		return null;
	}

	/** No se mueve (está pegada al suelo); gira hacia donde mira el que la maneja. */
	@Override
	protected void tickRidden(Player jugador, Vec3 movimiento) {
		super.tickRidden(jugador, movimiento);
		if (!naciendo() && !esRosa()) setRot(jugador.getYRot(), 0);
		yRotO = yBodyRot = yHeadRot = getYRot();
	}

	@Override
	protected Vec3 getRiddenInput(Player jugador, Vec3 movimiento) {
		return Vec3.ZERO;
	}

	@Override
	protected float getRiddenSpeed(Player jugador) {
		return 0;
	}

	/** El que la maneja ve desde su cara. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity pasajero, EntityDimensions dimensiones, float escala) {
		// El agarrado va en la pinza del brazo.
		if (pasajero.getId() == agarradoId()) return piesAgarrado(0).subtract(position()).add(pasajero.getVehicleAttachmentPoint(this));
		double bajar = pasajero.getEyeHeight() - pasajero.getVehicleAttachmentPoint(this).y;
		return OJOS_JINETE.subtract(0, bajar, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity pasajero) {
		return position().add(new Vec3(0, 0, 18).yRot(-getYRot() * Mth.DEG_TO_RAD));
	}

	// --- Jefe: no se empuja, no desaparece ---

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity otro) {}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return distancia < 256 * 256;
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Nacida", nacida());
		tag.putBoolean("Debil", debil());
		tag.putBoolean("Derrotada", derrotada());
		tag.putInt("Pinchitos", pinchitos());
		tag.putBoolean("IA", ia());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(NACIDA, tag.getBoolean("Nacida"));
		entityData.set(DEBIL, tag.getBoolean("Debil"));
		entityData.set(DERROTADA, tag.getBoolean("Derrotada"));
		entityData.set(PINCHITOS_PASADOS, tag.getInt("Pinchitos"));
		entityData.set(IA, tag.getBoolean("IA"));
	}
}
