package com.dedsafio4.bossplanta;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * La Planta Mordedora (jefe, nombre provisorio): una planta negra y rosa con mandíbula, plantada en el medio de un
 * campo redondo (/campo_boss &lt;radio&gt;). No camina: ataca sola a los que están dentro del campo, alternando:
 * <ol>
 * <li><b>Luz:</b> baila y en el medio del campo, arriba de ella, sale una luz. Al que la mira le hace daño
 *     (hay que darse vuelta o taparse detrás de algo).</li>
 * <li><b>Mordida:</b> los bloques a su alrededor se ponen rojos. Al terminar el aviso agarra al jugador más cercano
 *     que siga en lo rojo, lo sube a la boca y lo muerde 4 veces: cada mordida saca un cuarto de la vida y las cuatro
 *     lo matan, sin importar la armadura (y sin gastarla).</li>
 * </ol>
 * Vida: 500 por persona (con "/boss 2 gente N" se multiplica por N, como Qumara).
 * Los tiempos de cada ataque se guardan como el tick del mundo en que empezó, así el cliente dibuja lo mismo.
 */
public class PlantaBossEntity extends PathfinderMob {
	public static final double VIDA_BASE = 500;
	/** Cuánto mide el modelo: 1 píxel del modelo = ESCALA bloques (queda de unos 5 bloques de alto). */
	public static final float ESCALA = 5f / 15.2f;

	public static final int QUIETO = 0, LUZ = 1, MORDIDA = 2;
	/** Luz: a los AVISO_LUZ ticks se prende y dura hasta FIN_LUZ. Altura de la luz sobre los pies de la planta. */
	public static final int AVISO_LUZ = 30, FIN_LUZ = 130, DURA_LUZ = 140;
	public static final double ALTO_LUZ = 7.5;
	private static final float DANIO_LUZ = 4f;
	/** Mordida: lo rojo dura MARCA ticks; después muerde cada ENTRE_MORDIDAS (4 mordidas). */
	public static final int MARCA = 60, ENTRE_MORDIDAS = 20, MORDIDAS = 4, FIN_SIN_NADIE = MARCA + 20,
			FIN_MORDIDA = MARCA + ENTRE_MORDIDAS * MORDIDAS + 15;
	/** Hasta dónde llega lo rojo (radio en bloques, desde el centro de la planta). */
	public static final int ZONA = 6;
	/** Entre un ataque y el siguiente. */
	private static final int DESCANSO = 70;

	private static final EntityDataAccessor<Integer> ATAQUE = SynchedEntityData.defineId(PlantaBossEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> T_ATAQUE = SynchedEntityData.defineId(PlantaBossEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> AGARRADO = SynchedEntityData.defineId(PlantaBossEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> RADIO = SynchedEntityData.defineId(PlantaBossEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Vector3f> CENTRO = SynchedEntityData.defineId(PlantaBossEntity.class, EntityDataSerializers.VECTOR3);

	private final ServerBossEvent barra = new ServerBossEvent(Component.translatable("entity.dedsafio4.planta_boss"),
			BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_10);
	private int espera = DESCANSO;
	private boolean tocaLuz = true;
	private int mordidasDadas;

	public PlantaBossEntity(EntityType<? extends PathfinderMob> tipo, Level level) {
		super(tipo, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, VIDA_BASE)
				.add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1)
				.add(Attributes.ARMOR, 6)
				.add(Attributes.FOLLOW_RANGE, 64);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(ATAQUE, QUIETO);
		datos.define(T_ATAQUE, Long.MIN_VALUE / 2);
		datos.define(AGARRADO, -1);
		datos.define(RADIO, 20f);
		datos.define(CENTRO, new Vector3f());
	}

	// --- Lo que lee el cliente ---

	public int ataque() { return entityData.get(ATAQUE); }

	/** Ticks desde que empezó el ataque actual (con la parte del cuadro). */
	public float tiempoAtaque(float parcial) {
		return level().getGameTime() - entityData.get(T_ATAQUE) + parcial;
	}

	public int agarradoId() { return entityData.get(AGARRADO); }

	public float radio() { return entityData.get(RADIO); }

	public Vec3 centro() {
		Vector3f c = entityData.get(CENTRO);
		return new Vec3(c.x, c.y, c.z);
	}

	/** Arma el campo: centro donde está ahora y el radio dado. */
	public void campo(double radio) {
		entityData.set(RADIO, (float) radio);
		entityData.set(CENTRO, new Vector3f((float) getX(), (float) getY(), (float) getZ()));
	}

	// --- Comportamiento ---

	@Override
	public void tick() {
		super.tick();
		setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
		if (!(level() instanceof ServerLevel mundo) || !isAlive()) return;
		if (centro().lengthSqr() == 0) campo(20);   // puesta con /summon: campo de 20 bloques alrededor

		List<ServerPlayer> enCampo = jugadoresEnCampo(mundo);
		actualizarBarra(mundo);
		if (tickCount % 10 == 0) borde(mundo);

		ServerPlayer mira = null;
		Entity agarrado = agarradoId() >= 0 ? mundo.getEntity(agarradoId()) : null;
		if (agarrado instanceof ServerPlayer p) mira = p;
		else mira = masCercano(enCampo, 1e9);
		if (mira != null) girarHacia(mira.position());

		switch (ataque()) {
			case LUZ -> tickLuz(mundo, enCampo);
			case MORDIDA -> tickMordida(mundo, enCampo);
			default -> {
				if (enCampo.isEmpty()) {
					espera = Math.max(espera, 40);
					return;
				}
				if (--espera > 0) return;
				empezar(mundo, tocaLuz ? LUZ : MORDIDA, enCampo);
				tocaLuz = !tocaLuz;
			}
		}
	}

	private void empezar(ServerLevel mundo, int ataque, List<ServerPlayer> enCampo) {
		entityData.set(ATAQUE, ataque);
		entityData.set(T_ATAQUE, mundo.getGameTime());
		entityData.set(AGARRADO, -1);
		mordidasDadas = 0;
		Component aviso = ataque == LUZ
				? Component.literal("¡No mires la luz!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
				: Component.literal("¡Sal de la zona roja!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		for (ServerPlayer p : enCampo) p.displayClientMessage(aviso, true);
		mundo.playSound(null, getX(), getY() + 3, getZ(), ataque == LUZ ? SoundEvents.EVOKER_PREPARE_SUMMON : SoundEvents.RAVAGER_ROAR,
				SoundSource.HOSTILE, 3f, ataque == LUZ ? 1.4f : 0.8f);
	}

	private void terminar() {
		entityData.set(ATAQUE, QUIETO);
		entityData.set(AGARRADO, -1);
		espera = DESCANSO;
	}

	/** Dónde está la luz (en el medio del campo, arriba de la planta). */
	public Vec3 luz() {
		return position().add(0, ALTO_LUZ, 0);
	}

	private void tickLuz(ServerLevel mundo, List<ServerPlayer> enCampo) {
		int t = (int) tiempoAtaque(0);
		if (t == AVISO_LUZ) mundo.playSound(null, getX(), getY() + ALTO_LUZ, getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4f, 1.6f);
		if (t >= AVISO_LUZ && t < FIN_LUZ) {
			Vec3 luz = luz();
			if (t % 4 == 0) mundo.sendParticles(ParticleTypes.END_ROD, luz.x, luz.y, luz.z, 6, 0.6, 0.6, 0.6, 0.15);
			if ((t - AVISO_LUZ) % 10 == 0) {
				for (ServerPlayer p : enCampo) {
					if (!estaMirando(mundo, p, luz)) continue;
					p.hurt(fuente(mundo, ModPlantaBoss.DANIO_LUZ), DANIO_LUZ);
				}
			}
		}
		if (t >= DURA_LUZ) terminar();
	}

	/** Si el jugador está mirando la luz (más o menos de frente) y no hay bloques en el medio. */
	private boolean estaMirando(ServerLevel mundo, ServerPlayer p, Vec3 luz) {
		Vec3 ojos = p.getEyePosition();
		Vec3 hacia = luz.subtract(ojos).normalize();
		if (p.getViewVector(1f).dot(hacia) < 0.8) return false;
		return mundo.clip(new ClipContext(ojos, luz, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
	}

	private void tickMordida(ServerLevel mundo, List<ServerPlayer> enCampo) {
		int t = (int) tiempoAtaque(0);
		if (t < MARCA) {
			if (t % 20 == 0) {
				for (ServerPlayer p : enCampo) {
					if (enZona(p)) p.displayClientMessage(Component.literal("¡Sal de la zona roja!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
				}
			}
			return;
		}
		if (t == MARCA) {
			List<ServerPlayer> enRojo = new ArrayList<>();
			for (ServerPlayer p : enCampo) if (enZona(p)) enRojo.add(p);
			ServerPlayer presa = masCercano(enRojo, ZONA + 2);
			mundo.playSound(null, getX(), getY() + 3, getZ(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 3f, 0.6f);
			if (presa != null) {
				entityData.set(AGARRADO, presa.getId());
				presa.stopRiding();
				presa.startRiding(this, true);
				presa.displayClientMessage(Component.literal("¡Te agarró!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
			}
			return;
		}
		Entity e = agarradoId() >= 0 ? mundo.getEntity(agarradoId()) : null;
		if (!(e instanceof ServerPlayer presa) || !presa.isAlive()) {
			if (agarradoId() >= 0 || t >= FIN_SIN_NADIE) {
				soltar(mundo);
				terminar();
			}
			return;
		}
		// Si se bajó (shift), lo vuelve a agarrar.
		if (presa.getVehicle() != this) presa.startRiding(this, true);
		presa.setDeltaMovement(Vec3.ZERO);
		presa.fallDistance = 0;
		if ((t - MARCA) % ENTRE_MORDIDAS == 0 && mordidasDadas < MORDIDAS) {
			mordidasDadas++;
			Vec3 boca = presa.position().add(0, 1, 0);
			mundo.sendParticles(new DustParticleOptions(new Vector3f(0.6f, 0f, 0.1f), 2f), boca.x, boca.y, boca.z, 25, 0.4, 0.5, 0.4, 0);
			mundo.playSound(null, presa.getX(), presa.getY(), presa.getZ(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 2.5f, 0.7f);
			// Cada mordida, un cuarto de la vida; la cuarta mata. No mira la armadura (ni la gasta).
			float danio = mordidasDadas >= MORDIDAS ? Float.MAX_VALUE : presa.getMaxHealth() * 0.25f;
			presa.invulnerableTime = 0;
			presa.hurt(fuente(mundo, ModPlantaBoss.DANIO_MORDIDA), danio);
			presa.displayClientMessage(Component.literal("Mordida " + mordidasDadas + " de " + MORDIDAS).withStyle(ChatFormatting.DARK_RED), true);
		}
		if (t >= FIN_MORDIDA) {
			soltar(mundo);
			terminar();
		}
	}

	private void soltar(ServerLevel mundo) {
		Entity e = agarradoId() >= 0 ? mundo.getEntity(agarradoId()) : null;
		if (e != null && e.getVehicle() == this) e.stopRiding();
		entityData.set(AGARRADO, -1);
	}

	/** Si el jugador está parado en lo rojo (alrededor de la planta). */
	private boolean enZona(ServerPlayer p) {
		double dx = p.getX() - getX(), dz = p.getZ() - getZ();
		return dx * dx + dz * dz <= (ZONA + 0.5) * (ZONA + 0.5) && Math.abs(p.getY() - getY()) < 4;
	}

	private DamageSource fuente(ServerLevel mundo, net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> tipo) {
		return new DamageSource(mundo.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
				.getHolderOrThrow(tipo), this);
	}

	/** El agarrado va en la boca (delante de la cara, a la altura de la mandíbula). */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity pasajero, EntityDimensions tamano, float escala) {
		return new Vec3(0, 2.9, 1.5).yRot(-yBodyRot * Mth.DEG_TO_RAD);
	}

	@Override
	protected boolean canAddPassenger(Entity pasajero) {
		return getPassengers().isEmpty();
	}

	/** Los jugadores (en supervivencia o aventura) que están dentro del campo redondo. */
	private List<ServerPlayer> jugadoresEnCampo(ServerLevel mundo) {
		Vec3 c = centro();
		double r = radio();
		List<ServerPlayer> lista = new ArrayList<>();
		for (ServerPlayer p : mundo.players()) {
			if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
			double dx = p.getX() - c.x, dz = p.getZ() - c.z;
			if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - c.y) < 24) lista.add(p);
		}
		return lista;
	}

	private ServerPlayer masCercano(List<ServerPlayer> lista, double maximo) {
		ServerPlayer mejor = null;
		double d = maximo * maximo;
		for (ServerPlayer p : lista) {
			double dd = p.distanceToSqr(this);
			if (dd < d) {
				d = dd;
				mejor = p;
			}
		}
		return mejor;
	}

	private void girarHacia(Vec3 donde) {
		float objetivo = (float) (Mth.atan2(donde.z - getZ(), donde.x - getX()) * Mth.RAD_TO_DEG) - 90f;
		float giro = Mth.approachDegrees(getYRot(), objetivo, 5f);
		setYRot(giro);
		yBodyRot = yHeadRot = giro;
	}

	/** El borde del campo, marcado con partículas rosas: sólo lo ven los /op (los demás no saben dónde empieza). */
	private void borde(ServerLevel mundo) {
		List<ServerPlayer> ops = new ArrayList<>();
		for (ServerPlayer p : mundo.players()) if (p.hasPermissions(2) && p.distanceToSqr(this) < 160 * 160) ops.add(p);
		if (ops.isEmpty()) return;
		Vec3 c = centro();
		double r = radio();
		int n = Mth.clamp((int) (r * 4), 24, 240);
		DustParticleOptions polvo = new DustParticleOptions(new Vector3f(0.95f, 0.25f, 0.55f), 1.5f);
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			for (ServerPlayer op : ops) mundo.sendParticles(op, polvo, true, c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0);
		}
	}

	private void actualizarBarra(ServerLevel mundo) {
		barra.setProgress(getHealth() / getMaxHealth());
		Vec3 c = centro();
		double r = radio() + 24;
		for (ServerPlayer p : mundo.players()) {
			double dx = p.getX() - c.x, dz = p.getZ() - c.z;
			boolean cerca = dx * dx + dz * dz <= r * r && Math.abs(p.getY() - c.y) < 48;
			if (cerca) barra.addPlayer(p);
			else barra.removePlayer(p);
		}
		for (ServerPlayer p : new ArrayList<>(barra.getPlayers())) if (p.level() != mundo || p.isRemoved()) barra.removePlayer(p);
	}

	@Override
	public void remove(RemovalReason razon) {
		barra.removeAllPlayers();
		super.remove(razon);
	}

	@Override
	public void die(DamageSource fuente) {
		if (level() instanceof ServerLevel mundo) soltar(mundo);
		barra.removeAllPlayers();
		super.die(fuente);
	}

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		if (fuente.is(DamageTypeTags.IS_FALL) || fuente.is(DamageTypeTags.IS_DROWNING)
				|| fuente.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return false;
		// El que tiene agarrado también le puede pegar.
		return super.hurt(fuente, cantidad);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity otro) {
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	/** El campo y lo rojo se ven aunque la planta no esté en pantalla. */
	@Override
	public AABB getBoundingBoxForCulling() {
		return getBoundingBox().inflate(ZONA + 2, 6, ZONA + 2);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		Vec3 c = centro();
		tag.putDouble("campo_x", c.x);
		tag.putDouble("campo_y", c.y);
		tag.putDouble("campo_z", c.z);
		tag.putFloat("campo_radio", radio());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("campo_radio")) {
			entityData.set(RADIO, tag.getFloat("campo_radio"));
			entityData.set(CENTRO, new Vector3f((float) tag.getDouble("campo_x"), (float) tag.getDouble("campo_y"), (float) tag.getDouble("campo_z")));
		}
	}
}
