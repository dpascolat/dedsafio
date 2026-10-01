package com.dedsafio4.qumara;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * El Capullo (planta capullo morada, 14 bloques de alto): de donde salen los Bichos de la cabeza.
 * Siempre está abierto; mientras Qumara está en Enfriamiento se cierra, y si le pegás sale un Bicho de
 * la cabeza. No se lo puede romper.
 */
public class CapulloEntity extends PathfinderMob {
	/** Cada cuánto puede largar un bicho (medio segundo). */
	private static final int ESPERA = 10;

	private static final EntityDataAccessor<Boolean> ABIERTO = SynchedEntityData.defineId(CapulloEntity.class, EntityDataSerializers.BOOLEAN);

	private int espera;
	/** Del lado del cliente: qué tan abierto se ve (0 cerrado, 1 abierto); tarda 2,6 s. */
	public float apertura = -1, aperturaAntes = -1;

	public CapulloEntity(EntityType<? extends PathfinderMob> tipo, Level level) {
		super(tipo, level);
		setPersistenceRequired();
	}

	/** Si aparece con Qumara en Enfriamiento, ya nace cerrado. */
	public void setAbierto(boolean valor) {
		entityData.set(ABIERTO, valor);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ABIERTO, true);
	}

	public boolean abierto() {
		return entityData.get(ABIERTO);
	}

	@Override
	public void tick() {
		super.tick();
		setDeltaMovement(0, getDeltaMovement().y, 0);
		if (level().isClientSide) {
			if (apertura < 0) apertura = abierto() ? 1 : 0;   // al aparecer, ya como está (sin animación)
			aperturaAntes = apertura;
			apertura = Math.max(0, Math.min(1, apertura + (abierto() ? 1 : -1) / (2.6f * 20)));
			return;
		}
		if (espera > 0) espera--;
		// Cerrado mientras alguna Qumara cerca está en Enfriamiento.
		if (tickCount % 10 == 0) {
			boolean enfriando = !level().getEntitiesOfClass(QumaraEntity.class, getBoundingBox().inflate(120),
					q -> q.debil() && !q.derrotada()).isEmpty();
			if (abierto() == enfriando) {
				entityData.set(ABIERTO, !enfriando);
				level().playSound(null, getX(), getY() + 7, getZ(), enfriando ? SoundEvents.CHORUS_FLOWER_DEATH : SoundEvents.CHORUS_FLOWER_GROW,
						SoundSource.HOSTILE, 3f, 0.6f);
			}
		}
	}

	/** No se rompe; cerrado, cada golpe de un jugador hace salir un Bicho de la cabeza. */
	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		if (fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(fuente, cantidad);
		if (!(level() instanceof ServerLevel mundo) || abierto() || espera > 0) return false;
		if (!(fuente.getDirectEntity() instanceof Player)) return false;   // solo golpes (no explosiones ni flechas)
		espera = ESPERA;
		largarBicho(mundo, fuente.getEntity());
		return false;
	}

	/** Un Bicho de la cabeza: sale por arriba del capullo, tirado para arriba, y cae del lado de quien le pegó. */
	private void largarBicho(ServerLevel mundo, Entity quien) {
		double a = Math.atan2(quien.getZ() - getZ(), quien.getX() - getX()) + (random.nextDouble() - 0.5) * 0.8;
		double x = getX() + Math.cos(a) * 2, y = getY() + 13, z = getZ() + Math.sin(a) * 2;
		BichoCuboEntity bicho = ModQumara.BICHO_CABEZA.create(mundo);
		if (bicho == null) return;
		bicho.moveTo(x, y, z, random.nextFloat() * 360, 0);
		bicho.setDeltaMovement(Math.cos(a) * 0.4, 1.0, Math.sin(a) * 0.4);
		bicho.hasImpulse = true;
		mundo.addFreshEntity(bicho);
		mundo.sendParticles(ParticleTypes.POOF, x, y + 0.5, z, 20, 0.5, 0.5, 0.5, 0.05);
		mundo.playSound(null, x, y, z, SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 2f, 0.6f);
	}

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
		tag.putBoolean("Abierto", abierto());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(ABIERTO, !tag.contains("Abierto") || tag.getBoolean("Abierto"));
	}
}
