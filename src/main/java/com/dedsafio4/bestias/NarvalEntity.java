package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * El Narval: habita los acuíferos del Centro de Quiu. Si ve a un intruso cerca (a 12 bloques), se echa
 * un poco para atrás y lo embiste a gran velocidad: no lastima, pero lo empuja lejos ("juega con él").
 * Después espera unos 3 segundos y a veces vuelve a embestir. Nada como un delfín y fuera del agua se ahoga.
 */
public class NarvalEntity extends WaterAnimal {
	/** Evento para que el cliente haga la animación de embestir. */
	private static final byte EVENTO_EMBESTIDA = 61;
	/** Lo que dura la embestida (1,6 segundos), como en el diseño. */
	public static final int TIEMPO_EMBESTIDA = 32;
	private static final double DISTANCIA = 12;

	/** En el cliente: ticks desde que empezó a embestir (-1 = no está embistiendo). */
	public int embestida = -1;

	public NarvalEntity(EntityType<? extends NarvalEntity> tipo, Level level) {
		super(tipo, level);
		moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02f, 0.1f, true);
		lookControl = new SmoothSwimmingLookControl(this, 10);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return WaterAnimal.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 1.2)
				.add(Attributes.FOLLOW_RANGE, DISTANCIA + 4);
	}

	/** Aparece en el agua de las cuevas (donde no llega el cielo). */
	public static boolean puedeAparecer(EntityType<NarvalEntity> tipo, ServerLevelAccessor mundo, MobSpawnType razon,
										BlockPos pos, RandomSource azar) {
		if (razon == MobSpawnType.SPAWNER) return true;
		return mundo.getFluidState(pos).is(FluidTags.WATER) && mundo.getFluidState(pos.above()).is(FluidTags.WATER)
				&& mundo.getBrightness(LightLayer.SKY, pos) == 0;
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new EmbestirGoal(this));
		goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 10));
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
	}

	/** Nada como el delfín. */
	@Override
	public void travel(Vec3 direccion) {
		if (isControlledByLocalInstance() && isInWater()) {
			moveRelative(getSpeed(), direccion);
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.9));
			if (embestida < 0) setDeltaMovement(getDeltaMovement().add(0, -0.005, 0));
		} else {
			super.travel(direccion);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide && embestida >= 0 && ++embestida >= TIEMPO_EMBESTIDA) embestida = -1;
	}

	@Override
	public void handleEntityEvent(byte evento) {
		if (evento == EVENTO_EMBESTIDA) embestida = 0;
		else super.handleEntityEvent(evento);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isInWater() ? SoundEvents.DOLPHIN_AMBIENT_WATER : SoundEvents.DOLPHIN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.DOLPHIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.DOLPHIN_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.7f;
	}

	/**
	 * La embestida: 0–6 ticks se echa atrás mirando al intruso; 6–14 sale disparado hacia él (si lo toca, lo
	 * empuja fuerte sin lastimarlo); después vuelve a nadar. Entre embestidas espera 3 segundos y a veces
	 * (la mitad de las veces) no repite.
	 */
	static class EmbestirGoal extends Goal {
		private final NarvalEntity narval;
		private Player objetivo;
		private int tiempo, espera;
		private boolean choco;

		EmbestirGoal(NarvalEntity narval) {
			this.narval = narval;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (espera > 0) {
				espera--;
				return false;
			}
			if (!narval.isInWater()) return false;
			Player cerca = narval.level().getNearestPlayer(narval, DISTANCIA);
			if (cerca == null || cerca.isCreative() || cerca.isSpectator() || !cerca.isInWater() || !narval.hasLineOfSight(cerca)) {
				return false;
			}
			// No siempre: a veces se queda mirando un rato más.
			if (narval.getRandom().nextFloat() < 0.5f) {
				espera = 20;
				return false;
			}
			objetivo = cerca;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return tiempo < TIEMPO_EMBESTIDA && objetivo != null && objetivo.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			tiempo = 0;
			choco = false;
			narval.getNavigation().stop();
			narval.level().broadcastEntityEvent(narval, EVENTO_EMBESTIDA);
		}

		@Override
		public void stop() {
			objetivo = null;
			espera = 60;
		}

		@Override
		public void tick() {
			tiempo++;
			narval.getLookControl().setLookAt(objetivo, 30f, 30f);
			Vec3 hacia = objetivo.getEyePosition().subtract(narval.position()).normalize();
			float giro = (float) (Mth.atan2(hacia.z, hacia.x) * Mth.RAD_TO_DEG) - 90f;
			narval.setYRot(giro);
			narval.yBodyRot = giro;
			if (tiempo <= 6) {
				narval.setDeltaMovement(hacia.scale(-0.08));
			} else if (tiempo <= 14) {
				narval.setDeltaMovement(hacia.scale(1.1));
				if (tiempo == 7) narval.playSound(SoundEvents.DOLPHIN_PLAY, 1.5f, 0.8f);
				if (!choco && narval.getBoundingBox().inflate(0.8).intersects(objetivo.getBoundingBox())) {
					choco = true;
					objetivo.push(hacia.x * 2.2, 0.5 + hacia.y * 0.5, hacia.z * 2.2);
					if (objetivo instanceof ServerPlayer jugador) jugador.hurtMarked = true;
					narval.playSound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1f, 1f);
					narval.setDeltaMovement(hacia.scale(-0.2));
					tiempo = 15;
				}
			}
		}
	}
}
