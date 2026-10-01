package com.dedsafio4.qumara;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Círculo Explosivo (botón 4 de Qumara): un círculo rojo en el piso de 6 bloques de radio que avisa
 * (parpadea cada vez más rápido y se va llenando desde el centro, con un pitido por segundo) y después
 * explota: a los que están adentro les saca vida. Queda la marca quemada unos segundos.
 *
 * Según el nivel (la vida de Qumara), con armadura completa de diamante con Protección IV:
 * nivel 1: 5 s, 2 corazones · nivel 2: 4 s, 4 corazones · nivel 3: 4 s, 6 corazones · nivel 4: 3 s, 10 corazones.
 */
public class CirculoEntity extends Entity {
	public static final float RADIO = 6;
	/** Segundos de aviso por nivel. */
	private static final float[] AVISO = {5, 4, 4, 3};
	/** Daño antes de la armadura: con diamante completo y Protección IV queda en 4, 8, 12 y 20 (2/4/6/10 corazones). */
	private static final float[] DANIO = {24.8f, 38.2f, 48.6f, 66.2f};
	/** Lo que queda la marca quemada después de explotar. */
	public static final int TICKS_QUEMADO = 4 * 20;

	/** Puesto por la Qumara de la IA (/boss 1 ai): también les pega a los admins. */
	public boolean contraAdmins;

	private static final EntityDataAccessor<Integer> NIVEL = SynchedEntityData.defineId(CirculoEntity.class, EntityDataSerializers.INT);

	public CirculoEntity(EntityType<? extends CirculoEntity> tipo, Level level) {
		super(tipo, level);
		noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(NIVEL, 1);
	}

	public void setNivel(int nivel) {
		entityData.set(NIVEL, Math.max(1, Math.min(4, nivel)));
	}

	public int nivel() {
		return entityData.get(NIVEL);
	}

	/** Ticks que dura el aviso (según el nivel). */
	public int ticksAviso() {
		return (int) (AVISO[nivel() - 1] * 20);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel mundo)) return;
		int aviso = ticksAviso();
		// Un pitido por segundo que falta (el último, más agudo).
		int falta = aviso - tickCount;
		if (falta > 0 && falta % 20 == 0) {
			mundo.playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.HOSTILE, 2f, falta <= 20 ? 1.5f : 1f);
		}
		if (tickCount == aviso) explotar(mundo);
		if (tickCount > aviso + TICKS_QUEMADO) discard();
	}

	private void explotar(ServerLevel mundo) {
		mundo.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6f, 0.7f);
		mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
		mundo.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 220, RADIO / 2.5, 1.5, RADIO / 2.5, 0.25);
		mundo.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 120, RADIO / 2.5, 1.5, RADIO / 2.5, 0.05);
		for (int i = 0; i < 6; i++) {
			mundo.sendParticles(ParticleTypes.EXPLOSION, getX() + (random.nextDouble() - 0.5) * RADIO * 1.4, getY() + 1,
					getZ() + (random.nextDouble() - 0.5) * RADIO * 1.4, 1, 0, 0, 0, 0);
		}
		DamageSource fuente = new DamageSource(mundo.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(ModQumara.DANIO_CIRCULO));
		AABB zona = new AABB(getX() - RADIO, getY() - 2, getZ() - RADIO, getX() + RADIO, getY() + 6, getZ() + RADIO);
		for (Player p : mundo.getEntitiesOfClass(Player.class, zona, p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
			if (Math.hypot(p.getX() - getX(), p.getZ() - getZ()) > RADIO) continue;
			if (p.getVehicle() instanceof QumaraEntity || (!contraAdmins && QumaraEntity.esAdmin.test(p))) continue;   // ni el que maneja a Qumara ni los admins (salvo con la IA)
			p.invulnerableTime = 0;   // si hay dos círculos encimados, pegan los dos
			p.hurt(fuente, DANIO[nivel() - 1]);
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return distancia < 128 * 128;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}
}
