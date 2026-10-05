package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Flashbang (del Dedsafío 3): un bicho flaquito que aparece en la oscuridad y explota apenas toca a un jugador.
 * A los que están cerca (3 bloques) les saca 4 corazones justos, aunque tengan armadura de netherite con
 * Protección IV, y a todos los que están a menos de 12 bloques les deja la pantalla blanca 3 segundos, con un
 * zumbido en los oídos al final. La explosión no rompe bloques.
 */
public class FlashbangEntity extends Monster implements GeoEntity {
	public static final SoundEvent SONIDO_EXPLOSION = sonido("flashbang"), SONIDO_ZUMBIDO = sonido("flashbang_zumbido");
	private static final RawAnimation CAMINA = RawAnimation.begin().thenLoop("walk"), QUIETO = RawAnimation.begin().thenLoop("idle");

	/** A cuánto de un jugador explota, a cuánto lastima y a cuánto encandila (bloques). */
	private static final double TOCA = 1.0, DANIO = 3.0, ENCANDILA = 12.0;
	/** 4 corazones. */
	private static final float CUANTO = 8f;

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);

	public FlashbangEntity(EntityType<? extends FlashbangEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.xpReward = 5;
	}

	private static SoundEvent sonido(String nombre) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Servidor → jugador: la pantalla blanca (la dibuja FlashbangCliente). */
	public record DestelloPayload() implements CustomPacketPayload {
		public static final Type<DestelloPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "flashbang"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DestelloPayload> CODEC = StreamCodec.unit(new DestelloPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(DestelloPayload.TYPE, DestelloPayload.CODEC);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 6.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	private static boolean sirve(Player jugador) {
		return jugador.isAlive() && !jugador.isCreative() && !jugador.isSpectator();
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide || !isAlive()) return;
		for (Player jugador : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(TOCA))) {
			if (sirve(jugador)) {
				explotar();
				return;
			}
		}
	}

	/** Por si el ataque llega antes que el toque. */
	@Override
	public boolean doHurtTarget(Entity objetivo) {
		if (objetivo instanceof Player jugador && sirve(jugador)) explotar();
		return true;
	}

	private void explotar() {
		if (!(level() instanceof ServerLevel mundo)) return;
		mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.8, getZ(), 1, 0, 0, 0, 0);
		mundo.sendParticles(ParticleTypes.FLASH, getX(), getY() + 0.8, getZ(), 3, 0.2, 0.2, 0.2, 0);
		mundo.playSound(null, getX(), getY(), getZ(), SONIDO_EXPLOSION, SoundSource.HOSTILE, 2f, 1f);
		mundo.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 1.3f);
		DamageSource fuente = damageSources().explosion(this, this);
		for (Player jugador : mundo.getEntitiesOfClass(Player.class, getBoundingBox().inflate(ENCANDILA))) {
			if (!sirve(jugador)) continue;
			if (jugador.distanceTo(this) <= DANIO) {
				// 4 corazones justos, sin importar armadura ni encantamientos.
				float vida = jugador.getHealth();
				if (vida > CUANTO) {
					jugador.setHealth(vida - CUANTO);
					mundo.broadcastDamageEvent(jugador, fuente);
				} else {
					jugador.hurt(fuente, Float.MAX_VALUE);
				}
			}
			if (jugador instanceof ServerPlayer sp) ServerPlayNetworking.send(sp, new DestelloPayload());
		}
		discard();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "movimiento", 4,
				estado -> estado.setAndContinue(estado.isMoving() ? CAMINA : QUIETO)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.CREEPER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.CREEPER_DEATH;
	}
}
