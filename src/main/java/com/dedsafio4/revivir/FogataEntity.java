package com.dedsafio4.revivir;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * La Fogata grande de revivir. Con clic derecho abre el menú con los baneados; al elegir uno empieza el ritual
 * (30,4 segundos, los efectos los dibuja FogataRitual en cada cliente). Cerca del segundo 7 empuja a todos para afuera.
 * Al terminar, el baneado queda desbaneado y, cuando entre, aparece a 9 bloques de la fogata.
 * La animación, el modelo, las texturas y los sonidos son de NavithLabs / AlieRequiem, usados con su permiso.
 */
public final class FogataEntity extends Entity {
	/** En ticks desde que empezó: empieza la gravedad baja, revive, avisa y termina. */
	private static final int LIVIANO = 164, REVIVE = 608, AVISA = 611, FIN = 663;
	private static final float ONDA_DESDE = 6.93f, ONDA_DURA = 0.8f;
	private static final int ENVION = 28;

	private static final EntityDataAccessor<Long> DESDE = SynchedEntityData.defineId(FogataEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<String> NOMBRE = SynchedEntityData.defineId(FogataEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> MUERTO = SynchedEntityData.defineId(FogataEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> TEXTURA = SynchedEntityData.defineId(FogataEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> FIRMA = SynchedEntityData.defineId(FogataEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> LUGAR_X = SynchedEntityData.defineId(FogataEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LUGAR_Z = SynchedEntityData.defineId(FogataEntity.class, EntityDataSerializers.FLOAT);

	/** En el cliente: avisa que esta fogata está en un ritual (lo pone FogataRitual). */
	public static Consumer<FogataEntity> enRitualCliente = f -> {};

	private static final Set<FogataEntity> ACTIVAS = new HashSet<>();

	private String quien = "";
	/** El lugar donde aparece el revivido. */
	private double lugarY;
	private float lugarYaw;
	private boolean revivio, aviso, reservada;
	private final Set<UUID> empujados = new HashSet<>();
	private final Map<UUID, double[]> envion = new HashMap<>();
	private AABB caja;
	private double cajaX = Double.NaN, cajaY, cajaZ;

	public FogataEntity(EntityType<? extends FogataEntity> tipo, Level mundo) {
		super(tipo, mundo);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		datos.define(DESDE, -1L);
		datos.define(NOMBRE, "");
		datos.define(MUERTO, "");
		datos.define(TEXTURA, "");
		datos.define(FIRMA, "");
		datos.define(LUGAR_X, 0f);
		datos.define(LUGAR_Z, 0f);
	}

	public long desde() {
		return entityData.get(DESDE);
	}

	public boolean reviviendo() {
		return desde() >= 0;
	}

	public String nombre() {
		return entityData.get(NOMBRE);
	}

	public String muerto() {
		return entityData.get(MUERTO);
	}

	public String textura() {
		return entityData.get(TEXTURA);
	}

	public String firma() {
		return entityData.get(FIRMA);
	}

	/** Dónde aparece el revivido, respecto de la fogata. */
	public float lugarX() {
		return entityData.get(LUGAR_X);
	}

	public float lugarZ() {
		return entityData.get(LUGAR_Z);
	}

	/** Ocupada: en un ritual, o a punto de empezar uno. */
	boolean ocupada() {
		return reviviendo() || reservada;
	}

	void reservar() {
		reservada = true;
	}

	void liberar() {
		reservada = false;
	}

	void empezar(UUID id, String nombre, String textura, String firma, double x, double y, double z, float yaw, String quien) {
		reservada = false;
		this.quien = quien;
		revivio = false;
		aviso = false;
		empujados.clear();
		envion.clear();
		lugarY = y;
		lugarYaw = yaw;
		entityData.set(LUGAR_X, (float) (x - getX()));
		entityData.set(LUGAR_Z, (float) (z - getZ()));
		entityData.set(MUERTO, id.toString());
		entityData.set(NOMBRE, nombre);
		entityData.set(TEXTURA, textura);
		entityData.set(FIRMA, firma);
		entityData.set(DESDE, level().getGameTime());
		ACTIVAS.add(this);
		if (level() instanceof ServerLevel mundo) avisarCielo(mundo, desde());
	}

	private void terminar() {
		entityData.set(DESDE, -1L);
		entityData.set(MUERTO, "");
		ACTIVAS.remove(this);
	}

	private void avisarCielo(ServerLevel mundo, long desde) {
		for (ServerPlayer jugador : mundo.players()) avisarCielo(jugador, desde);
	}

	private void avisarCielo(ServerPlayer jugador, long desde) {
		Fogata.mandarCielo(jugador, getStringUUID(), getX(), getY(), getZ(), desde);
	}

	/** Al entrar o cambiar de dimensión: si hay un ritual ahí, que también vea el cielo oscuro. */
	static void alEntrar(ServerPlayer jugador) {
		for (FogataEntity f : ACTIVAS) {
			if (!f.isRemoved() && f.reviviendo() && f.level() == jugador.level()) f.avisarCielo(jugador, f.desde());
		}
	}

	static void limpiar() {
		ACTIVAS.clear();
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (!tag.contains("revive_desde")) return;
		quien = tag.getString("revive_quien");
		lugarY = tag.getDouble("lugar_y");
		lugarYaw = tag.getFloat("lugar_yaw");
		revivio = tag.getBoolean("revive_volvio");
		aviso = tag.getBoolean("revive_avisado");
		entityData.set(LUGAR_X, tag.getFloat("lugar_x"));
		entityData.set(LUGAR_Z, tag.getFloat("lugar_z"));
		entityData.set(MUERTO, tag.getString("muerto"));
		entityData.set(NOMBRE, tag.getString("nombre"));
		entityData.set(TEXTURA, tag.getString("textura"));
		entityData.set(FIRMA, tag.getString("firma"));
		entityData.set(DESDE, tag.getLong("revive_desde"));
		ACTIVAS.add(this);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		if (!reviviendo()) return;
		tag.putLong("revive_desde", desde());
		tag.putString("revive_quien", quien);
		tag.putDouble("lugar_y", lugarY);
		tag.putFloat("lugar_yaw", lugarYaw);
		tag.putBoolean("revive_volvio", revivio);
		tag.putBoolean("revive_avisado", aviso);
		tag.putFloat("lugar_x", lugarX());
		tag.putFloat("lugar_z", lugarZ());
		tag.putString("muerto", muerto());
		tag.putString("nombre", nombre());
		tag.putString("textura", textura());
		tag.putString("firma", firma());
	}

	@Override
	public void tick() {
		long desde = desde();
		if (desde < 0) return;
		if (!(level() instanceof ServerLevel mundo)) {
			enRitualCliente.accept(this);
			return;
		}
		long t = mundo.getGameTime() - desde;
		float segundos = t / 20f;
		if (segundos >= ONDA_DESDE && segundos <= ONDA_DESDE + 0.9f) empujar(mundo, radioOnda(segundos));
		if (!envion.isEmpty()) seguirEnvion(mundo);
		// Se flota: la caída lenta dura hasta poco antes del final (como en el espacio).
		if (t >= LIVIANO && t < 469 && (t - LIVIANO) % 10 == 0) flotar(mundo, (int) (509 - t));
		if (t >= REVIVE && !revivio) revivir(mundo);
		if (t >= AVISA && !aviso) {
			aviso = true;
			Fogata.anunciar(mundo.getServer(), quien, nombre(), muerto());
		}
		if (t >= FIN || t < 0) terminar();
	}

	private void flotar(ServerLevel mundo, int ticks) {
		for (ServerPlayer jugador : mundo.players()) {
			if (jugador.isSpectator() || jugador.distanceToSqr(this) > 1024) continue;
			jugador.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0, false, false, false));
		}
	}

	/** Hasta dónde llegó la onda en ese segundo. */
	public static float radioOnda(float segundos) {
		float k = Mth.clamp((segundos - ONDA_DESDE) / ONDA_DURA, 0, 1);
		return 4 + 41 * (1 - (1 - k) * (1 - k));
	}

	private void empujar(ServerLevel mundo, float radio) {
		for (ServerPlayer jugador : mundo.players()) {
			if (jugador.isSpectator() || !empuja(jugador, radio)) continue;
			jugador.currentImpulseImpactPos = jugador.position();
			jugador.setIgnoreFallDamageFromCurrentImpulse(true);
		}
	}

	private boolean empuja(LivingEntity vivo, float radio) {
		if (!vivo.isAlive() || empujados.contains(vivo.getUUID())) return false;
		double x = vivo.getX() - getX(), z = vivo.getZ() - getZ(), y = vivo.getY() - getY();
		double distancia = Math.sqrt(x * x + z * z);
		if (distancia > radio || y < -4 || y > 40) return false;
		empujados.add(vivo.getUUID());
		if (distancia < 0.3) {
			float yaw = vivo.getYRot() * Mth.DEG_TO_RAD;
			x = Mth.sin(yaw);
			z = -Mth.cos(yaw);
			distancia = 1;
		}
		envion.put(vivo.getUUID(), new double[]{x / distancia, z / distancia, 0});
		return true;
	}

	private void seguirEnvion(ServerLevel mundo) {
		for (Iterator<Map.Entry<UUID, double[]>> it = envion.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, double[]> entrada = it.next();
			double[] e = entrada.getValue();
			if (!(mundo.getEntity(entrada.getKey()) instanceof LivingEntity vivo) || !vivo.isAlive() || vivo.isSpectator() || e[2] >= ENVION) {
				it.remove();
				continue;
			}
			double falta = 1 - e[2] / ENVION, fuerza = 0.6 + 2.9 * falta * falta;
			vivo.setDeltaMovement(e[0] * fuerza, -0.0784, e[1] * fuerza);
			vivo.hurtMarked = true;
			e[2]++;
		}
	}

	private void revivir(ServerLevel mundo) {
		revivio = true;
		double x = getX() + lugarX(), z = getZ() + lugarZ();
		Fogata.revivir(mundo, UUID.fromString(muerto()), nombre(), x, lugarY, z, lugarYaw);
		mundo.sendParticles(ParticleTypes.SOUL, x, lugarY + 1, z, 24, 0.3, 0.6, 0.3, 0.03);
	}

	/** Si la quitan en pleno ritual, igual revive. */
	@Override
	public void remove(RemovalReason razon) {
		if (razon.shouldDestroy() && level() instanceof ServerLevel mundo && reviviendo()) {
			if (!revivio) revivir(mundo);
			if (!aviso) Fogata.anunciar(mundo.getServer(), quien, nombre(), muerto());
			aviso = true;
			avisarCielo(mundo, -1);
		}
		ACTIVAS.remove(this);
		super.remove(razon);
	}

	/** Con clic derecho se abre el menú; se la mira como a un bloque. */
	@Override
	public InteractionResult interact(Player jugador, InteractionHand mano) {
		if (jugador instanceof ServerPlayer servidor) Fogata.abrir(servidor, this);
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean isAttackable() {
		return true;
	}

	/** Nada la lastima; en creativo, un golpe la saca. */
	@Override
	public boolean hurt(DamageSource fuente, float danio) {
		if (!level().isClientSide && fuente.getEntity() instanceof Player jugador && jugador.isCreative()) {
			discard();
			return true;
		}
		return false;
	}

	@Override
	public boolean ignoreExplosion(Explosion explosion) {
		return true;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		if (caja == null || getX() != cajaX || getY() != cajaY || getZ() != cajaZ) {
			cajaX = getX();
			cajaY = getY();
			cajaZ = getZ();
			caja = new AABB(cajaX - 3.65, cajaY - 0.25, cajaZ - 3.65, cajaX + 3.65, cajaY + 4.8, cajaZ + 3.65);
		}
		return caja;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return distancia < 9216;
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(Fogata.ITEM);
	}
}
