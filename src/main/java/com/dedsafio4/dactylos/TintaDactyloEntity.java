package com.dedsafio4.dactylos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** La tinta que escupe el Dáctylo: al pegarle a alguien le tapa la visión 3 segundos. */
public class TintaDactyloEntity extends ThrowableProjectile {
	/** Lo que dura la visión tapada (3 segundos). */
	public static final int DURACION = 60;

	public TintaDactyloEntity(EntityType<? extends TintaDactyloEntity> tipo, Level level) {
		super(tipo, level);
	}

	public TintaDactyloEntity(Level level, LivingEntity quien) {
		super(ModDactylos.TINTA_ENTIDAD, quien, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	@Override
	protected double getDefaultGravity() {
		return 0.01;
	}

	@Override
	protected boolean canHitEntity(Entity otro) {
		return super.canHitEntity(otro) && !(otro instanceof DactyloBebeEntity);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide && tickCount % 2 == 0) {
			level().addParticle(ParticleTypes.SQUID_INK, getX(), getY(), getZ(), 0, 0, 0);
		}
		if (!level().isClientSide && tickCount > 100) discard();
	}

	@Override
	protected void onHitEntity(EntityHitResult golpe) {
		super.onHitEntity(golpe);
		if (!level().isClientSide && golpe.getEntity() instanceof LivingEntity quien) {
			quien.addEffect(new MobEffectInstance(ModDactylos.EFECTO_TINTA, DURACION, 0, false, false, true), getOwner());
		}
	}

	@Override
	protected void onHit(HitResult golpe) {
		super.onHit(golpe);
		if (level() instanceof ServerLevel mundo) {
			mundo.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(), getZ(), 14, 0.2, 0.2, 0.2, 0.08);
			mundo.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 1f, 0.8f);
			discard();
		}
	}
}
