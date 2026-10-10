package com.dedsafio4.bestias;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Cerebro Amarillo: una medusa con campana de cerebro y 18 tentáculos, de la Dimensión de los Órganos. Camina y
 * trepa paredes con los tentáculos; cuando ve a un jugador salta hacia él y se le sube a la cabeza. Pegado ahí lo
 * deja ciego y le saca medio corazón cada medio segundo, sin importar la armadura ni los encantamientos, y se cura
 * con lo que le saca. El que lo tiene encima no le puede hacer nada: otro jugador lo tiene que matar.
 */
public class CerebroAmarilloEntity extends Monster {
	/** Cada cuántos ticks saca medio corazón (10 ticks = medio segundo). */
	private static final int CADA = 10;
	private int drenando;

	public CerebroAmarilloEntity(EntityType<? extends CerebroAmarilloEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected PathNavigation createNavigation(Level mundo) {
		return new WallClimberNavigation(this, mundo);
	}

	/** Trepa paredes con los tentáculos, como las arañas. */
	@Override
	public boolean onClimbable() {
		return horizontalCollision && !isPassenger();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.45f));
		goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** ¿Se puede subir a este jugador? (No a los de creativo, ni si ya tiene otro cerebro encima.) */
	private static boolean sirve(Player jugador) {
		return jugador.isAlive() && !jugador.isCreative() && !jugador.isSpectator()
				&& jugador.getPassengers().stream().noneMatch(e -> e instanceof CerebroAmarilloEntity);
	}

	/** En lugar de pegar, se sube a la cabeza. */
	@Override
	public boolean doHurtTarget(Entity objetivo) {
		if (objetivo instanceof Player jugador && sirve(jugador) && !isPassenger()) {
			subirse(jugador);
			return true;
		}
		return false;
	}

	private void subirse(Player jugador) {
		if (startRiding(jugador, true)) {
			drenando = 0;
			playSound(SoundEvents.SLIME_ATTACK, 1f, 0.6f);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) return;
		if (getVehicle() instanceof Player jugador) {
			if (!jugador.isAlive() || jugador.isCreative() || jugador.isSpectator()) {
				stopRiding();
				return;
			}
			// Se metió con él en un lugar de 2 bloques con techo: el cerebro choca contra el techo y queda aplastado.
			if (aplastado()) {
				playSound(SoundEvents.SLIME_DEATH, 1.2f, 0.5f);
				if (level() instanceof net.minecraft.server.level.ServerLevel mundo) {
					mundo.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,
							new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL)), getX(), getY() + 0.4, getZ(), 25, 0.3, 0.2, 0.3, 0.1);
				}
				stopRiding();
				kill();
				return;
			}
			jugador.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
			if (++drenando % CADA == 0) drenar(jugador);
		} else if (getTarget() instanceof Player jugador && sirve(jugador)
				&& getBoundingBox().inflate(0.3).intersects(jugador.getBoundingBox())) {
			// Lo tocó (por ejemplo, después del salto): se sube.
			subirse(jugador);
		}
	}

	/** Medio corazón, pase lo que pase (armadura, Protección...); se cura con lo que saca. */
	private void drenar(Player jugador) {
		float vida = jugador.getHealth();
		DamageSource fuente = damageSources().mobAttack(this);
		if (vida > 1f) {
			jugador.setHealth(vida - 1f);
			level().broadcastDamageEvent(jugador, fuente);
		} else {
			jugador.hurt(fuente, Float.MAX_VALUE);
		}
		heal(1f);
		playSound(SoundEvents.HONEY_DRINK, 0.6f, 0.6f);
	}

	@Override
	public void rideTick() {
		super.rideTick();
		// Mira para donde mira la cabeza que tiene agarrada.
		if (getVehicle() instanceof Player jugador) {
			setYRot(jugador.getYHeadRot());
			yBodyRot = jugador.getYHeadRot();
			yHeadRot = jugador.getYHeadRot();
		}
	}

	/**
	 * El que lo tiene encima no le puede pegar. Sí lo mata: otro jugador, una flecha (también la suya) que le cae desde
	 * arriba, o meterse en un lugar con techo bajo (ver aplastado()).
	 */
	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		Entity vehiculo = getVehicle();
		if (vehiculo != null && (fuente.getEntity() == vehiculo || fuente.getDirectEntity() == vehiculo)) {
			boolean flechaQueCae = fuente.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow flecha
					&& flecha.getDeltaMovement().y < 0;
			if (!flechaQueCae) return false;
		}
		return super.hurt(fuente, cantidad);
	}

	/** ¿Arriba de la cabeza del jugador hay techo justo donde está el cerebro? (2 bloques de alto con techo) */
	private boolean aplastado() {
		net.minecraft.world.phys.AABB arriba = new net.minecraft.world.phys.AABB(getX() - 0.25, getY() + 0.2, getZ() - 0.25,
				getX() + 0.25, getY() + getBbHeight() - 0.1, getZ() + 0.25);
		return !level().noCollision(arriba);
	}

	@Override
	public boolean isInvulnerableTo(DamageSource fuente) {
		if (isPassenger() && (fuente.is(DamageTypes.IN_WALL) || fuente.is(DamageTypes.FALL) || fuente.is(DamageTypes.CRAMMING))) return true;
		return super.isInvulnerableTo(fuente);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SLIME_SQUISH_SMALL;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.SLIME_HURT_SMALL;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SLIME_DEATH_SMALL;
	}
}
