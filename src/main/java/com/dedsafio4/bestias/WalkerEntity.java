package com.dedsafio4.bestias;

import com.dedsafio4.bloques.ModBloques;
import com.dedsafio4.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Plumosaurio (el id sigue siendo "walker"): el coloso herbívoro más grande del núcleo de Quiu.
 * Es pacífico, pero si te chocás con él o lo golpeás te hace MUCHO daño. Si le das una Fruta
 * Solaria la procesa al instante y puede dejar un montón (el bloque de CACA) del que se saca
 * Excremento con un pico. Suelta una Manzana de Ámbar.
 * Usa el modelo y las animaciones que pasó el usuario (necesita GeckoLib).
 */
public class WalkerEntity extends PathfinderMob implements GeoEntity {
	private static final RawAnimation QUIETO = RawAnimation.begin().thenLoop("misc.idle");
	private static final RawAnimation CAMINAR = RawAnimation.begin().thenLoop("move.walk");
	private static final RawAnimation CORRER = RawAnimation.begin().thenLoop("move.run");
	private static final RawAnimation PISOTON = RawAnimation.begin().thenPlay("stomping");

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);
	/** Probabilidad de que suelte el montón de excremento al comer una Fruta Solaria. */
	public static final float PROBABILIDAD_EXCREMENTO = 0.5f;
	/** Lo que saca de vida chocarlo o golpearlo. */
	public static final float DANIO = 16f;
	/** Ticks entre un golpe y otro al mismo jugador que se le queda pegado. */
	private static final int ESPERA_CHOQUE = 20;

	/** Ticks que dura el pisotón en pantalla (se manda al cliente). */
	private static final EntityDataAccessor<Integer> PISANDO =
			SynchedEntityData.defineId(WalkerEntity.class, EntityDataSerializers.INT);
	/** Cuándo se lastimó a cada jugador por última vez al chocarlo. */
	private final Map<UUID, Long> ultimoChoque = new HashMap<>();

	public WalkerEntity(EntityType<? extends WalkerEntity> tipo, Level level) {
		super(tipo, level);
		this.xpReward = 30;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 150.0)
				.add(Attributes.MOVEMENT_SPEED, 0.2)
				.add(Attributes.ATTACK_DAMAGE, DANIO)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 28.0)
				.add(Attributes.STEP_HEIGHT, 2.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder constructor) {
		super.defineSynchedData(constructor);
		constructor.define(PISANDO, 0);
	}

	/** Pacífico: pasea y mira, no persigue a nadie. */
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	/** Le pega a alguien con todo y lo tira lejos. */
	private void aplastar(LivingEntity victima) {
		this.entityData.set(PISANDO, 40);
		if (victima.hurt(this.damageSources().mobAttack(this), DANIO)) {
			Vec3 lejos = victima.position().subtract(this.position()).multiply(1, 0, 1);
			if (lejos.lengthSqr() < 1.0E-4) lejos = new Vec3(1, 0, 0);
			lejos = lejos.normalize().scale(1.6);
			victima.push(lejos.x, 0.5, lejos.z);
			victima.hurtMarked = true;
			this.playSound(SoundEvents.RAVAGER_ATTACK, 1.5f, 0.8f);
		}
	}

	/** Si lo golpean de cerca, devuelve el golpe al instante. */
	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		boolean dolio = super.hurt(fuente, cantidad);
		if (dolio && !this.level().isClientSide && this.isAlive() && fuente.getEntity() instanceof LivingEntity quien
				&& fuente.getDirectEntity() == quien && quien.distanceToSqr(this) < 8 * 8) {
			aplastar(quien);
			if (quien instanceof Player) ultimoChoque.put(quien.getUUID(), this.level().getGameTime());
		}
		return dolio;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		int pisando = this.entityData.get(PISANDO);
		if (pisando > 0 && !this.level().isClientSide) this.entityData.set(PISANDO, pisando - 1);
		if (this.level().isClientSide || !this.isAlive()) return;
		// Chocarlo duele: a cualquier jugador que lo toque (con un segundo de espera entre golpes).
		long ahora = this.level().getGameTime();
		for (Player jugador : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.15))) {
			if (jugador.isSpectator() || jugador.isCreative()) continue;
			Long antes = ultimoChoque.get(jugador.getUUID());
			if (antes != null && ahora - antes < ESPERA_CHOQUE) continue;
			ultimoChoque.put(jugador.getUUID(), ahora);
			aplastar(jugador);
		}
		if (ahora % 200 == 0) ultimoChoque.values().removeIf(t -> ahora - t > 200);
	}

	/** Con una Fruta Solaria la procesa al instante y, a veces, deja el montón de excremento atrás. */
	@Override
	protected InteractionResult mobInteract(Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		if (!pila.is(ModItems.FRUTA_SOLARIA)) return super.mobInteract(jugador, mano);
		if (this.level() instanceof ServerLevel mundo) {
			pila.consume(1, jugador);
			this.playSound(SoundEvents.GENERIC_EAT, 1.5f, 0.6f);
			mundo.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.FRUTA_SOLARIA)),
					this.getX(), this.getY() + this.getBbHeight() * 0.8, this.getZ(), 20, 0.6, 0.4, 0.6, 0.1);
			if (this.random.nextFloat() < PROBABILIDAD_EXCREMENTO) soltarExcremento(mundo);
		}
		return InteractionResult.sidedSuccess(this.level().isClientSide);
	}

	private void soltarExcremento(ServerLevel mundo) {
		// Atrás del Plumosaurio, a la altura del piso.
		Vec3 atras = Vec3.directionFromRotation(0, this.yBodyRot).scale(-(this.getBbWidth() / 2 + 0.8));
		BlockPos pos = BlockPos.containing(this.getX() + atras.x, this.getY() + 0.5, this.getZ() + atras.z);
		BlockState caca = ModBloques.CACA.defaultBlockState();
		if (mundo.getBlockState(pos).canBeReplaced()) {
			mundo.setBlock(pos, caca, 3);
		} else {
			this.spawnAtLocation(new ItemStack(ModBloques.CACA_ITEM));
		}
		this.playSound(SoundEvents.MUD_PLACE, 1.5f, 0.7f);
	}

	/** Siempre suelta una Manzana de Ámbar. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel mundo, DamageSource fuente, boolean muertePorJugador) {
		super.dropCustomDeathLoot(mundo, fuente, muertePorJugador);
		this.spawnAtLocation(new ItemStack(ModItems.MANZANA_DE_AMBAR));
	}

	/** Es enorme y único: no desaparece al alejarse, como los animales. */
	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	/** Aparece en el piso de pasto de la jungla, con luz. */
	public static boolean puedeAparecer(EntityType<WalkerEntity> tipo, LevelAccessor mundo, MobSpawnType razon,
										BlockPos pos, RandomSource azar) {
		return mundo.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && mundo.getRawBrightness(pos, 0) > 8;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.RAVAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RAVAGER_DEATH;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "principal", 5, this::animar));
	}

	private PlayState animar(AnimationState<WalkerEntity> estado) {
		if (this.entityData.get(PISANDO) > 0) return estado.setAndContinue(PISOTON);
		if (!estado.isMoving()) return estado.setAndContinue(QUIETO);
		boolean rapido = this.getDeltaMovement().horizontalDistanceSqr() > 0.02;
		return estado.setAndContinue(rapido ? CORRER : CAMINAR);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
