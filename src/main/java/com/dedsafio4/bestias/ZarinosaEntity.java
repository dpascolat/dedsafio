package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * Zarinosa Espigüeya: criatura pequeña pero astuta de las Cuevas del Centro de Quiu (no se la ve a más
 * de 8 bloques). A menos de 4 bloques se abalanza sobre tu cabeza y se queda prendida (va montada en el
 * jugador) drenándote la vida (0,8 corazones por segundo, sin importar la armadura) hasta matarte; con
 * lo que drena se cura. El que la tiene encima no la puede atacar: solo otro jugador lo puede liberar,
 * pegándole o con click derecho, y ella sale despedida. Si el jugador muere, se baja sola.
 */
public class ZarinosaEntity extends Monster {
	public static final int NORMAL = 0, SALTANDO = 1, PRENDIDA = 2, DESPEDIDA = 3;
	public static final int TIEMPO_SALTO = 15, TIEMPO_DESPEDIDA = 18;
	/** 2 de vida cada 25 ticks = 0,8 corazones por segundo. */
	private static final int CADA = 25;
	private static final float DRENA = 2;
	private static final Vector3f ROJO = new Vector3f(0xD0 / 255f, 0x20 / 255f, 0x2A / 255f);

	private static final EntityDataAccessor<Integer> ESTADO =
			SynchedEntityData.defineId(ZarinosaEntity.class, EntityDataSerializers.INT);

	/** Ticks desde que cambió de estado (cada lado lleva su cuenta). */
	public int ticksEstado;
	private int estadoAntes, espera;

	public ZarinosaEntity(EntityType<? extends ZarinosaEntity> tipo, Level level) {
		super(tipo, level);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 8.0)
				.add(Attributes.MOVEMENT_SPEED, 0.36)
				.add(Attributes.ATTACK_DAMAGE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	/** Aparece en las cuevas: donde no llega el cielo, a oscuras y sobre piso firme. */
	public static boolean puedeAparecerEnCueva(EntityType<ZarinosaEntity> tipo, LevelAccessor mundo, MobSpawnType razon,
											   BlockPos pos, RandomSource azar) {
		if (razon == MobSpawnType.SPAWNER) return true;
		return mundo.getBrightness(LightLayer.SKY, pos) == 0 && mundo.getBrightness(LightLayer.BLOCK, pos) < 8
				&& mundo.getBlockState(pos.below()).isSolidRender(mundo, pos.below());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(ESTADO, NORMAL);
	}

	public int estado() {
		return entityData.get(ESTADO);
	}

	private void estado(int nuevo) {
		entityData.set(ESTADO, nuevo);
		ticksEstado = 0;
		estadoAntes = nuevo;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new AbalanzarseGoal(this));
		goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.1, 16) {
			@Override
			public boolean canUse() {
				return estado() == NORMAL && super.canUse();
			}
		});
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0) {
			@Override
			public boolean canUse() {
				return estado() == NORMAL && super.canUse();
			}
		});
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			if (estado() != estadoAntes) {
				estadoAntes = estado();
				ticksEstado = 0;
			}
			ticksEstado++;
			return;
		}
		ticksEstado++;
		if (espera > 0) espera--;
		switch (estado()) {
			case SALTANDO -> {
				LivingEntity objetivo = getTarget();
				if (objetivo instanceof Player jugador && !jugador.isVehicle() && cerca(jugador)) {
					if (startRiding(jugador, true)) {
						estado(PRENDIDA);
						playSound(SoundEvents.SLIME_ATTACK, 1f, 1.4f);
					}
				} else if (ticksEstado > 25 || (ticksEstado > 4 && onGround())) {
					estado(NORMAL);
					espera = 40;
				}
			}
			case PRENDIDA -> {
				if (!(getVehicle() instanceof Player jugador) || !jugador.isAlive()) {
					soltarse(null);
					return;
				}
				setYRot(jugador.getYHeadRot());
				yBodyRot = yHeadRot = jugador.getYHeadRot();
				if (ticksEstado % CADA == 0 && jugador.hurt(damageSources().indirectMagic(this, this), DRENA)) {
					heal(DRENA);
					playSound(SoundEvents.HONEY_DRINK, 0.8f, 0.6f);
				}
				if (ticksEstado % 4 == 0 && level() instanceof ServerLevel mundo) {
					mundo.sendParticles(new DustParticleOptions(ROJO, 1f), jugador.getX(), jugador.getEyeY() - 0.1, jugador.getZ(),
							2, 0.3, 0.2, 0.3, 0);
				}
			}
			case DESPEDIDA -> {
				if (ticksEstado >= TIEMPO_DESPEDIDA) estado(NORMAL);
			}
			default -> {
			}
		}
	}

	/** Cerca de la parte de arriba de la cabeza del jugador. */
	private boolean cerca(Player jugador) {
		Vec3 cabeza = new Vec3(jugador.getX(), jugador.getY() + jugador.getBbHeight(), jugador.getZ());
		return position().distanceToSqr(cabeza) < 1.6 * 1.6;
	}

	/** La sacan de la cabeza (o el jugador murió): sale despedida hacia atrás dando una vuelta. */
	private void soltarse(Player liberador) {
		Entity vehiculo = getVehicle();
		stopRiding();
		estado(DESPEDIDA);
		espera = 100;
		Vec3 hacia = liberador != null ? position().subtract(liberador.position())
				: vehiculo != null ? Vec3.directionFromRotation(0, vehiculo.getYRot()).scale(-1) : Vec3.ZERO;
		hacia = new Vec3(hacia.x, 0, hacia.z);
		if (hacia.lengthSqr() > 1e-4) hacia = hacia.normalize();
		setDeltaMovement(hacia.x * 0.6, 0.5, hacia.z * 0.6);
		hurtMarked = true;
		playSound(SoundEvents.SLIME_JUMP, 1f, 1.5f);
	}

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		Entity quien = fuente.getEntity();
		if (estado() == PRENDIDA && getVehicle() != null) {
			// El que la tiene encima no puede sacársela; otro jugador sí.
			if (quien == getVehicle()) return false;
			if (quien instanceof Player otro && !level().isClientSide) soltarse(otro);
		}
		return super.hurt(fuente, cantidad);
	}

	@Override
	protected InteractionResult mobInteract(Player jugador, InteractionHand mano) {
		if (estado() == PRENDIDA && getVehicle() != null && getVehicle() != jugador) {
			if (!level().isClientSide) soltarse(jugador);
			return InteractionResult.sidedSuccess(level().isClientSide);
		}
		return super.mobInteract(jugador, mano);
	}

	@Override
	public boolean canBeCollidedWith() {
		return estado() != PRENDIDA && super.canBeCollidedWith();
	}

	@Override
	public void readAdditionalSaveData(CompoundTag datos) {
		super.readAdditionalSaveData(datos);
		estado(NORMAL);
	}

	/** A menos de 4 bloques salta en arco hacia la cabeza del jugador. */
	static class AbalanzarseGoal extends Goal {
		private final ZarinosaEntity zarinosa;

		AbalanzarseGoal(ZarinosaEntity zarinosa) {
			this.zarinosa = zarinosa;
			setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity objetivo = zarinosa.getTarget();
			return zarinosa.estado() == NORMAL && zarinosa.espera == 0 && zarinosa.onGround()
					&& objetivo instanceof Player jugador && !jugador.isCreative() && !jugador.isSpectator()
					&& !jugador.isVehicle() && zarinosa.distanceTo(jugador) < 4 && zarinosa.hasLineOfSight(jugador);
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			LivingEntity objetivo = zarinosa.getTarget();
			Vec3 cabeza = new Vec3(objetivo.getX(), objetivo.getY() + objetivo.getBbHeight(), objetivo.getZ());
			Vec3 hacia = cabeza.subtract(zarinosa.position());
			double plano = Math.sqrt(hacia.x * hacia.x + hacia.z * hacia.z);
			// Salto en arco: llega arriba de la cabeza en unos 12 ticks.
			zarinosa.setDeltaMovement(hacia.x / 12 * 1.3, Mth.clamp(0.42 + hacia.y * 0.12, 0.42, 0.9), hacia.z / 12 * 1.3);
			zarinosa.getNavigation().stop();
			zarinosa.getLookControl().setLookAt(objetivo);
			zarinosa.estado(SALTANDO);
			zarinosa.playSound(SoundEvents.SPIDER_AMBIENT, 1f, 1.8f);
			if (plano < 0.5) zarinosa.setDeltaMovement(0, 0.7, 0);
		}
	}
}
