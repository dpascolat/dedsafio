package com.dedsafio4.nave;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rayo celeste de la nave: 5x5 píxeles de sección y 1 bloque de largo (el largo es solo visual;
 * el choque se calcula con la punta). Va recto, sin gravedad, y hace 1 de daño.
 */
public class RayoEntity extends Projectile {
	public static final float DANIO = 1f;
	/** Ticks hasta desaparecer si no choca con nada (3 segundos). */
	private static final int VIDA_MAXIMA = 60;
	private int vida;

	public RayoEntity(EntityType<? extends RayoEntity> tipo, Level level) {
		super(tipo, level);
		this.setNoGravity(true);
	}

	public RayoEntity(Level level, LivingEntity duenio, Vec3 posicion) {
		this(ModEntidades.RAYO, level);
		this.setOwner(duenio);
		this.setPos(posicion);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		if (++vida > VIDA_MAXIMA) {
			this.discard();
			return;
		}
		HitResult choque = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (choque.getType() != HitResult.Type.MISS) {
			this.onHit(choque);
			if (this.isRemoved()) return;
		}
		Vec3 movimiento = this.getDeltaMovement();
		this.setPos(this.getX() + movimiento.x, this.getY() + movimiento.y, this.getZ() + movimiento.z);
	}

	/** No les pega a otras naves (así no se matan entre ellas). */
	@Override
	protected boolean canHitEntity(Entity entidad) {
		return super.canHitEntity(entidad) && !(entidad instanceof NaveEntity);
	}

	@Override
	protected void onHitEntity(EntityHitResult resultado) {
		super.onHitEntity(resultado);
		if (this.level().isClientSide) return;
		LivingEntity duenio = this.getOwner() instanceof LivingEntity l ? l : null;
		Entity golpeado = resultado.getEntity();
		// Dispara más rápido que el medio segundo de invulnerabilidad tras un golpe: sin esto,
		// la mitad de los rayos no harían daño.
		golpeado.invulnerableTime = 0;
		golpeado.hurt(this.damageSources().mobProjectile(this, duenio), NaveEntity.danioExacto(this.level(), golpeado, DANIO));
		this.discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult resultado) {
		super.onHitBlock(resultado);
		if (!this.level().isClientSide) this.discard();
	}
}
