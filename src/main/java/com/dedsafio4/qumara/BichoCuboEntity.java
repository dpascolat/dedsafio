package com.dedsafio4.qumara;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Los bichos cubo que salen con el Enfriamiento de Qumara (7 bloques de alto, quietos, no atacan):
 * - Bicho no de la cabeza (naranja y morado): 40 de vida, solo lo lastiman las explosiones.
 * - Bicho de la cabeza (blanco y rojo): con click derecho se sube a tu cabeza; con otro click derecho lo
 *   tirás (cae a unos 2 bloques, no explota al caer). Si lo tenés 2 segundos en la cabeza, te explota
 *   encima; y a los 20 segundos de nacer explota, esté donde esté. Salen del Capullo.
 */
public class BichoCuboEntity extends PathfinderMob {
	/** 2 segundos en la cabeza: explota. Y a los 20 segundos de nacer explota igual, esté donde esté. */
	private static final int TICKS_EN_LA_CABEZA = 40, MECHA = 20 * 20;
	/** Al que tiene encima le saca esto: con armadura de diamante con Protección IV queda en medio corazón. */
	private static final float DANIO_JUGADOR = 9.4f;
	/** Al Bicho no de la cabeza, cada explosión le saca la mitad de la vida. */
	private static final float DANIO_BICHO = 20f;
	private static final double RADIO = 5;

	private final boolean deLaCabeza;
	private int enLaCabeza;
	private boolean lanzado;
	private int vuelo;
	private UUID lanzador;

	public BichoCuboEntity(EntityType<? extends PathfinderMob> tipo, Level level, boolean deLaCabeza) {
		super(tipo, level);
		this.deLaCabeza = deLaCabeza;
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	public boolean deLaCabeza() {
		return deLaCabeza;
	}

	// --- Daño: el de la cabeza no se lastima; el otro, solo con explosiones ---

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		if (fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(fuente, cantidad);
		if (deLaCabeza || !fuente.is(DamageTypeTags.IS_EXPLOSION)) return false;
		return super.hurt(fuente, cantidad);
	}

	// --- Agarrarlo y tirarlo ---

	@Override
	protected InteractionResult mobInteract(Player jugador, InteractionHand mano) {
		if (!deLaCabeza || isPassenger() || lanzado || !jugador.getPassengers().isEmpty()) return InteractionResult.PASS;
		if (!level().isClientSide) {
			startRiding(jugador, true);
			avisarPasajeros(jugador);
			enLaCabeza = 0;
			level().playSound(null, jugador.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 0.6f);
		}
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	/** Lo tira hacia donde mira el jugador que lo tiene en la cabeza: cerquita, a unos 2 bloques. */
	public void lanzar(ServerPlayer jugador) {
		if (getVehicle() != jugador) return;
		stopRiding();
		avisarPasajeros(jugador);
		Vec3 mira = jugador.getLookAngle();
		Vec3 plano = new Vec3(mira.x, 0, mira.z);
		plano = plano.lengthSqr() < 1e-4 ? Vec3.ZERO : plano.normalize();
		setPos(jugador.position().add(plano.scale(0.6)).add(0, 1.2, 0));
		setDeltaMovement(plano.scale(0.16).add(0, 0.25, 0));
		hasImpulse = true;
		lanzado = true;
		vuelo = 0;
		lanzador = jugador.getUUID();
		level().playSound(null, jugador.blockPosition(), SoundEvents.WITCH_THROW, SoundSource.PLAYERS, 1f, 0.6f);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel mundo) || !deLaCabeza) return;
		if (tickCount >= MECHA) {
			explotar(mundo, getVehicle() instanceof Player p ? p : null);
			return;
		}
		if (getVehicle() instanceof Player jugador) {
			if (++enLaCabeza % 10 == 0) mundo.playSound(null, blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 0.6f, 1.4f);
			mundo.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 2, 0.2, 0.2, 0.2, 0.01);
			if (enLaCabeza >= TICKS_EN_LA_CABEZA) explotar(mundo, jugador);
			return;
		}
		if (lanzado) {
			vuelo++;
			// No explota al caer: cuando toca el piso se lo puede volver a agarrar.
			if (vuelo > 3 && onGround()) lanzado = false;
		}
	}

	/** La explosión: no rompe bloques; lastima a los que están cerca (y al Bicho no de la cabeza). */
	private void explotar(ServerLevel mundo, Player encima) {
		Vec3 centro = encima != null ? encima.position().add(0, 1, 0) : position().add(0, 1, 0);
		mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centro.x, centro.y, centro.z, 1, 0, 0, 0, 0);
		mundo.playSound(null, centro.x, centro.y, centro.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3f, 0.9f);
		Entity culpable = lanzador == null ? this : mundo.getEntity(lanzador);
		DamageSource fuente = damageSources().explosion(this, culpable instanceof LivingEntity l ? l : this);
		stopRiding();
		if (encima instanceof ServerPlayer sp) avisarPasajeros(sp);
		for (LivingEntity quien : mundo.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIO + 4), e -> e != this && e.isAlive())) {
			double d = Math.max(0, distanciaA(quien, centro));
			if (d > RADIO) continue;
			if (quien instanceof BichoCuboEntity bicho && !bicho.deLaCabeza()) {
				bicho.hurt(fuente, DANIO_BICHO);
			} else if (quien instanceof Player p) {
				if (p.isCreative() || p.isSpectator()) continue;
				float parte = p == encima ? 1f : (float) (1 - d / RADIO);
				p.hurt(fuente, DANIO_JUGADOR * parte);
			} else if (!(quien instanceof QumaraEntity)) {
				quien.hurt(fuente, DANIO_JUGADOR * (float) (1 - d / RADIO));
			}
		}
		discard();
	}

	/** Al propio jugador Minecraft no le avisa qué lleva encima: se le manda (para que lo vea y lo pueda tirar). */
	public static void avisarPasajeros(Player jugador) {
		if (jugador instanceof ServerPlayer sp) {
			sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(sp));
		}
	}

	/** Distancia desde el punto a la caja de la entidad (0 si está adentro). */
	private static double distanciaA(LivingEntity e, Vec3 p) {
		var c = e.getBoundingBox();
		double dx = Math.max(Math.max(c.minX - p.x, 0), p.x - c.maxX);
		double dy = Math.max(Math.max(c.minY - p.y, 0), p.y - c.maxY);
		double dz = Math.max(Math.max(c.minZ - p.z, 0), p.z - c.maxZ);
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	// --- Quieto, no se empuja ni desaparece ---

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
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Lanzado", lanzado);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		lanzado = tag.getBoolean("Lanzado");
	}
}
