package com.dedsafio4.bestias;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Nautilus Óseo Grande: el caparazón en espiral de hueso con los tentáculos de carne colgando, flotando en la Dimensión
 * de los Órganos. Igual que el pequeño, se enoja si te acercas, llama a los nautilus de al lado, no se lo puede empujar
 * y al pegar da Levitación; pero no muerde: cada 7 segundos toma carrera y te embiste, y entre una embestida y otra
 * se queda flotando a unos bloques, mirándote. El golpe es el ATTACK_DAMAGE (se cambia con /dañomob, la vida con
 * /vidamob y la velocidad con /velocidad, como los demás mobs).
 */
public class NautilusOseoGrandeEntity extends NautilusOseoEntity {
	/** Cada cuánto embiste (7 segundos), cuánto dura la carrera como mucho y qué tan rápido va. */
	private static final int CADA = 7 * 20, CARRERA = 25;
	private static final double RAPIDEZ = 1.1;

	/** Si está embistiendo (para el dibujo: se tira para adelante con los tentáculos estirados). */
	private static final EntityDataAccessor<Boolean> EMBISTIENDO =
			SynchedEntityData.defineId(NautilusOseoGrandeEntity.class, EntityDataSerializers.BOOLEAN);

	public NautilusOseoGrandeEntity(EntityType<? extends NautilusOseoGrandeEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.xpReward = 15;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.5)
				.add(Attributes.ATTACK_DAMAGE, 8.0)
				.add(Attributes.ATTACK_KNOCKBACK, 1.5)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(EMBISTIENDO, false);
	}

	public boolean embistiendo() {
		return entityData.get(EMBISTIENDO);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new Embestida());
		goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				jugador -> jugador.distanceToSqr(this) < 10 * 10));
	}

	/** Flota a unos bloques del objetivo y cada 7 segundos se le tira encima en línea recta. */
	private class Embestida extends Goal {
		private int espera = 40, carrera;
		private Vec3 direccion = Vec3.ZERO;

		Embestida() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity objetivo = getTarget();
			return objetivo != null && objetivo.isAlive();
		}

		@Override
		public void stop() {
			terminar();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		private void terminar() {
			carrera = 0;
			entityData.set(EMBISTIENDO, false);
		}

		@Override
		public void tick() {
			LivingEntity objetivo = getTarget();
			if (objetivo == null) return;
			Vec3 pecho = objetivo.position().add(0, objetivo.getBbHeight() * 0.5, 0);
			Vec3 centro = position().add(0, getBbHeight() * 0.5, 0);
			if (carrera > 0) {
				// La carrera: derecho a donde estaba el jugador al arrancar.
				carrera--;
				setDeltaMovement(direccion.scale(RAPIDEZ));
				getLookControl().setLookAt(objetivo);
				if (getBoundingBox().inflate(0.4).intersects(objetivo.getBoundingBox())) {
					if (doHurtTarget(objetivo)) objetivo.push(direccion.x * 1.2, 0.4, direccion.z * 1.2);
					terminar();
					setDeltaMovement(direccion.scale(-0.3));
				} else if (carrera == 0 || horizontalCollision || verticalCollision) {
					terminar();
				}
				return;
			}
			getLookControl().setLookAt(objetivo, 30f, 30f);
			// Esperando: flota a unos 6 bloques, un poco más arriba que el jugador.
			Vec3 desde = centro.subtract(pecho);
			Vec3 lado = desde.horizontalDistanceSqr() < 0.01 ? new Vec3(1, 0, 0) : new Vec3(desde.x, 0, desde.z).normalize();
			Vec3 lugar = pecho.add(lado.scale(6)).add(0, 1.5, 0);
			getMoveControl().setWantedPosition(lugar.x, lugar.y, lugar.z, 1.0);
			if (espera > 0) espera--;
			if (espera == 0 && centro.distanceToSqr(pecho) < 20 * 20 && getSensing().hasLineOfSight(objetivo)) {
				direccion = pecho.subtract(centro).normalize();
				carrera = CARRERA;
				espera = CADA;
				entityData.set(EMBISTIENDO, true);
				playSound(SoundEvents.PHANTOM_SWOOP, 2f, 0.6f);
			}
		}
	}
}
