package com.dedsafio4.momentito;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /momentito 1 [largo ancho arriba abajo]  la escena del hacker (30 segundos) en el lugar donde se escribe el
 * comando, mirando hacia donde mira el que lo escribe. El campo de fuerza es un óvalo centrado en los pies del
 * héroe: si no se dicen las medidas, 200 de largo, 200 de ancho, 30 bloques para arriba y 180 para abajo. Llega la nave negra del hacker, tira una TNT hackeada, el héroe busca el
 * Cristal del Desierto y lo levanta, aparece el campo de fuerza violeta, la bomba explota contra el campo,
 * el hacker se enoja y se va volando al cielo. Todo es de mentira: no rompe nada ni lastima a nadie.
 * La escena la dibuja el cliente (MomentitoRenderer) y, a los que están cerca, les maneja la cámara con
 * tomas de cine (MomentitoCamara); acá solo vive 30 segundos y hace los sonidos.
 */
public class MomentitoEntity extends Entity {
	/**
	 * Primero, 8 segundos de cielo rojo que se esparce y la grieta que se abre (como /cielo rojo, sin Reviil);
	 * cuando la nave sale de la grieta empieza la cinemática (el resto de los 30 s de la escena).
	 */
	public static final float INTRO = 8f;
	/**
	 * La cinemática termina a los 24 s de la escena (antes de que el héroe baje los brazos): que guarde el escudo
	 * (el campo de fuerza se achica) se ve con la cámara de cada uno, y cuando el escudo desaparece (25,6 s)
	 * termina todo con el destello.
	 */
	public static final float FIN_CINEMATICA = 24f, FIN = 25.6f;
	public static final int DURACION = (int) ((INTRO + FIN - Escena.SALE_GRIETA) * 20);
	/** Cuántos ticks hay que restarle al tiempo total para tener el de la escena del diseño. */
	private static final int CORRIMIENTO = (int) ((INTRO - Escena.SALE_GRIETA) * 20);

	public static final EntityType<MomentitoEntity> TIPO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "momentito"),
			EntityType.Builder.<MomentitoEntity>of(MomentitoEntity::new, MobCategory.MISC).sized(0.5f, 0.5f)
					.clientTrackingRange(16).updateInterval(20).build("momentito"));

	/** Tick del mundo en que empezó (así todos los que la ven van sincronizados). */
	private static final EntityDataAccessor<Integer> INICIO = SynchedEntityData.defineId(MomentitoEntity.class, EntityDataSerializers.INT);
	/** Medidas del campo de fuerza, en bloques. */
	private static final EntityDataAccessor<Float> LARGO = SynchedEntityData.defineId(MomentitoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ANCHO = SynchedEntityData.defineId(MomentitoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ARRIBA = SynchedEntityData.defineId(MomentitoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ABAJO = SynchedEntityData.defineId(MomentitoEntity.class, EntityDataSerializers.FLOAT);
	public static final float[] CAMPO_POR_DEFECTO = {200, 200, 30, 180};
	/** Qué momentito es: 1 = el hacker, 2 = la escalera. */
	private static final EntityDataAccessor<Integer> ESCENA = SynchedEntityData.defineId(MomentitoEntity.class, EntityDataSerializers.INT);

	public MomentitoEntity(EntityType<? extends MomentitoEntity> tipo, Level level) {
		super(tipo, level);
		noPhysics = true;
	}

	public static void registrar() {}

	// ---------------------------------------------------------------- Chunks donde mira la cámara

	/** Hasta cuántos chunks alrededor de la cámara se cargan y se mandan. */
	public static final int CHUNKS = 8;
	/** Los jugadores que están viendo la escena → el chunk donde está su cámara. */
	private static final Map<UUID, ChunkPos> CAMARAS = new ConcurrentHashMap<>();
	/** Cada segundo se pone un ticket nuevo (dura 3 s): así los chunks siguen cargados mientras la cámara esté ahí. */
	private static final TicketType<Integer> TICKET = TicketType.create("dedsafio4_momentito", Integer::compare, 60);

	/** El chunk donde está la cámara de este jugador, o null si no está viendo una escena (lo usa ChunkMapMomentitoMixin). */
	public static ChunkPos chunkCamara(ServerPlayer jugador) {
		return CAMARAS.get(jugador.getUUID());
	}

	/** Cuántos chunks se le mandan a este jugador alrededor de la cámara (como mucho 8). */
	public static int distanciaCamara(ServerPlayer jugador, int normal) {
		return CAMARAS.containsKey(jugador.getUUID()) ? Math.min(normal, CHUNKS) : normal;
	}

	private ChunkPos ultimoChunk;

	/**
	 * Mientras dura la escena, a los jugadores cercanos se les mandan los chunks de donde está la cámara (y se
	 * cargan en el servidor), así las tomas de lejos no se ven vacías. Al terminar, vuelven los de su lugar.
	 */
	private void chunksDeLaCamara(ServerLevel mundo, float t) {
		float[] toma = escena() == 2 ? Escena2.toma(t) : Escena.toma(t, campo());
		double[] p = Escena.alMundo(getX(), getY(), getZ(), getYRot(), toma[0], toma[1], toma[2]);
		ChunkPos chunk = new ChunkPos(Mth.floor(p[0]) >> 4, Mth.floor(p[2]) >> 4);
		if (chunk.equals(ultimoChunk) && tickCount % 20 != 0) return;
		ultimoChunk = chunk;
		mundo.getChunkSource().addRegionTicket(TICKET, chunk, CHUNKS, tickCount / 20);
		for (ServerPlayer jugador : mundo.players()) {
			if (jugador.distanceToSqr(this) > 300 * 300) continue;
			ChunkPos antes = CAMARAS.put(jugador.getUUID(), chunk);
			if (!chunk.equals(antes)) ((com.dedsafio4.mixin.ChunkMapAccessor) mundo.getChunkSource().chunkMap).dedsafio4$actualizarChunks(jugador);
		}
	}

	/** Se terminó la escena: cada uno vuelve a recibir los chunks de donde está parado. */
	private void soltarCamaras(ServerLevel mundo) {
		for (ServerPlayer jugador : mundo.players()) {
			if (CAMARAS.remove(jugador.getUUID()) != null) {
				((com.dedsafio4.mixin.ChunkMapAccessor) mundo.getChunkSource().chunkMap).dedsafio4$actualizarChunks(jugador);
			}
		}
	}

	@Override
	public void remove(RemovalReason razon) {
		if (level() instanceof ServerLevel mundo) soltarCamaras(mundo);
		super.remove(razon);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("momentito").requires(s -> s.hasPermission(2))
				.then(Commands.argument("escena", IntegerArgumentType.integer(1))
						.executes(ctx -> empezar(ctx, CAMPO_POR_DEFECTO))
						.then(Commands.argument("largo", IntegerArgumentType.integer(2, 1500))
								.then(Commands.argument("ancho", IntegerArgumentType.integer(2, 1500))
										.then(Commands.argument("arriba", IntegerArgumentType.integer(1, 750))
												.then(Commands.argument("abajo", IntegerArgumentType.integer(0, 750))
														.executes(ctx -> empezar(ctx, new float[]{
																IntegerArgumentType.getInteger(ctx, "largo"), IntegerArgumentType.getInteger(ctx, "ancho"),
																IntegerArgumentType.getInteger(ctx, "arriba"), IntegerArgumentType.getInteger(ctx, "abajo")}))))))));
	}

	private static int empezar(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, float[] campo) {
		int escena = IntegerArgumentType.getInteger(ctx, "escena");
		if (escena == 2) return empezarEscalera(ctx);
		if (escena != 1) {
			ctx.getSource().sendFailure(Component.literal("Todavía no existe el momentito " + escena + "."));
			return 0;
		}
		ServerLevel mundo = ctx.getSource().getLevel();
		Vec3 pos = ctx.getSource().getPosition();
		MomentitoEntity m = TIPO.create(mundo);
		if (m == null) return 0;
		m.moveTo(pos.x, pos.y, pos.z, ctx.getSource().getRotation().y, 0);
		m.entityData.set(INICIO, (int) mundo.getGameTime());
		m.entityData.set(LARGO, campo[0]);
		m.entityData.set(ANCHO, campo[1]);
		m.entityData.set(ARRIBA, campo[2]);
		m.entityData.set(ABAJO, campo[3]);
		mundo.addFreshEntity(m);
		ctx.getSource().sendSuccess(() -> Component.literal("Momentito 1: la escena del hacker (campo de "
				+ Math.round(campo[0]) + " de largo × " + Math.round(campo[1]) + " de ancho, " + Math.round(campo[2])
				+ " para arriba y " + Math.round(campo[3]) + " para abajo)."), true);
		return 1;
	}

	/**
	 * /momentito 2: la escalera (60 s). La escalera la construye el jugador: 13 de ancho, 59 escalones (uno por
	 * bloque, como una escalera de escalones de piedra) y arriba un descanso plano de 10 bloques. Se escribe
	 * parado en el piso, en el medio, pegado al primer escalón y mirando hacia arriba de la escalera: la escena
	 * se acomoda a los bloques (centro del bloque donde está parado y la dirección más cercana).
	 */
	private static int empezarEscalera(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
		ServerLevel mundo = ctx.getSource().getLevel();
		Vec3 pos = ctx.getSource().getPosition();
		float giro = Math.round(ctx.getSource().getRotation().y / 90f) * 90f;
		double adelanteX = -Math.sin(giro * Mth.DEG_TO_RAD), adelanteZ = Math.cos(giro * Mth.DEG_TO_RAD);
		MomentitoEntity m = TIPO.create(mundo);
		if (m == null) return 0;
		// El primer escalón empieza justo en el borde del bloque de adelante. El giro guardado va dado vuelta
		// (+180) para que el render (que gira 180 - giro) deje la escalera subiendo hacia adelante.
		m.moveTo(Mth.floor(pos.x) + 0.5 + adelanteX * 0.5, Mth.floor(pos.y + 0.01), Mth.floor(pos.z) + 0.5 + adelanteZ * 0.5, giro + 180, 0);
		m.entityData.set(INICIO, (int) mundo.getGameTime());
		m.entityData.set(ESCENA, 2);
		mundo.addFreshEntity(m);
		ctx.getSource().sendSuccess(() -> Component.literal("Momentito 2: la escalera."), true);
		return 1;
	}

	public int escena() {
		return entityData.get(ESCENA);
	}

	/** Cuántos ticks dura todo. */
	public int duracion() {
		return escena() == 2 ? (int) (Escena2.T * 20) : DURACION;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		datos.define(ESCENA, 1);
		datos.define(INICIO, 0);
		datos.define(LARGO, CAMPO_POR_DEFECTO[0]);
		datos.define(ANCHO, CAMPO_POR_DEFECTO[1]);
		datos.define(ARRIBA, CAMPO_POR_DEFECTO[2]);
		datos.define(ABAJO, CAMPO_POR_DEFECTO[3]);
	}

	/** Las medidas del campo de fuerza: {largo, ancho, arriba, abajo}. */
	public float[] campo() {
		return new float[]{entityData.get(LARGO), entityData.get(ANCHO), entityData.get(ARRIBA), entityData.get(ABAJO)};
	}

	/** La escena es enorme: se dibuja desde cualquier distancia. */
	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return true;
	}

	/** Segundos desde que empezó la escena. */
	/**
	 * Segundos de la escena del diseño (la nave sale de la grieta a los 2,5): durante la introducción del
	 * cielo rojo da menos de 2,5.
	 */
	public float tiempoEscena(float parcial) {
		if (escena() == 2) return tiempo(parcial);
		return tiempo(parcial) - INTRO + Escena.SALE_GRIETA;
	}

	/** ¿Ya empezó la cinemática (la nave salió de la grieta)? La escalera es toda cinemática. */
	public boolean enCinematica(float parcial) {
		float t = tiempoEscena(parcial);
		if (escena() == 2) return t >= 0 && t < Escena2.T;
		return t >= Escena.SALE_GRIETA && t < FIN_CINEMATICA;
	}

	public float tiempo(float parcial) {
		return (level().getGameTime() - entityData.get(INICIO) + parcial) / 20f;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) return;
		int t = (int) (level().getGameTime() - entityData.get(INICIO));
		if (t >= duracion()) {
			discard();
			return;
		}
		if (level() instanceof ServerLevel mundo) {
			if (enCinematica(0)) chunksDeLaCamara(mundo, tiempoEscena(0));
			else if (escena() == 1 && tiempoEscena(0) >= FIN_CINEMATICA) soltarCamaras(mundo);   // cada uno vuelve a su lugar
		}
		if (escena() == 2) {
			sonidosEscalera(t);
			return;
		}
		switch (t - CORRIMIENTO) {
			case 50 -> sonar(SoundEvents.BEACON_AMBIENT, 2f, 0.5f);   // la nave sale de la grieta
			case 90 -> sonar(SoundEvents.BEACON_AMBIENT, 2f, 0.6f);
			case 132 -> sonar(SoundEvents.STONE_BUTTON_CLICK_ON, 1.5f, 1.2f);
			case 148 -> sonar(SoundEvents.TNT_PRIMED, 2f, 1f);
			case 150 -> sonar(SoundEvents.WITCH_CELEBRATE, 1.5f, 0.7f);
			case 212 -> sonar(SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 1f);
			case 224 -> sonar(SoundEvents.BEACON_ACTIVATE, 2f, 1.2f);
			case 240 -> {
				sonar(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.8f);
				sonar(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.6f);
			}
			case 290, 310 -> sonar(SoundEvents.VILLAGER_NO, 2f, 0.6f);
			case 340 -> sonar(SoundEvents.FIREWORK_ROCKET_LAUNCH, 4f, 0.5f);
			case 490 -> sonar(SoundEvents.BEACON_DEACTIVATE, 2f, 1f);
			default -> {
			}
		}
	}

	private void sonar(SoundEvent sonido, float volumen, float tono) {
		level().playSound(null, getX(), getY() + 3, getZ(), sonido, SoundSource.AMBIENT, volumen, tono);
	}

	/** Los pasos del héroe en la piedra y el amuleto (los sonidos salen de donde está el héroe). */
	private void sonidosEscalera(int t) {
		float s = t / 20f;
		org.joml.Vector3f h = Escena2.pose(s).heroe;
		double[] p = Escena.alMundo(getX(), getY(), getZ(), getYRot(), h.x, h.y, h.z);
		float paso = Math.abs(h.z - Escena2.pose(s - 0.2f).heroe.z) + Math.abs(h.x - Escena2.pose(s - 0.2f).heroe.x);
		if (t % 7 == 0 && paso > 0.15f) sonarEn(p, SoundEvents.STONE_STEP, 0.6f, 1f);
		switch (t) {
			case 532, 1010 -> sonarEn(p, SoundEvents.STONE_STEP, 1f, 0.7f);   // se arrodilla / se agacha
			case 932 -> sonarEn(p, SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 0.8f);   // ve el esqueleto
			case 1098 -> sonarEn(p, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.5f, 1.2f);   // agarra el amuleto
			case 1124 -> sonarEn(p, SoundEvents.BEACON_ACTIVATE, 2f, 1.3f);
			case 1150 -> sonarEn(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1f);   // el cristal brilla
			default -> {
			}
		}
	}

	private void sonarEn(double[] p, SoundEvent sonido, float volumen, float tono) {
		level().playSound(null, p[0], p[1] + 1, p[2], sonido, SoundSource.AMBIENT, volumen, tono);
	}

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag datos) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag datos) {
	}
}
