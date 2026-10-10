package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.nave.ModEntidades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Los fantasmas del Limbo (del Dedsafío 3). Vuelan y persiguen a los jugadores:
 * - Fantasma Amarillo: si te golpea, te mata al instante (ni el tótem te salva) y desaparece.
 * - Fantasma Blanco: golpea a gran velocidad y te da Lentitud.
 * - Fantasma Rojo: golpea a gran velocidad.
 * - Fantasma Negro: cada golpe te quita un corazón para siempre (como el agua del Limbo; vuelve con /limbo devolver).
 */
public class FantasmaEntity extends Monster implements GeoEntity {
	public static final SoundEvent SONIDO_AMBIENTE = sonido("fantasma_ambiente"), SONIDO_GOLPE = sonido("fantasma_golpe");

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);

	public FantasmaEntity(EntityType<? extends FantasmaEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.xpReward = 8;
		this.moveControl = new FlyingMoveControl(this, 10, true);
		setNoGravity(true);
	}

	private static SoundEvent sonido(String nombre) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	private boolean amarillo() {
		return getType() == ModEntidades.FANTASMA_AMARILLO;
	}

	private boolean blanco() {
		return getType() == ModEntidades.FANTASMA_BLANCO;
	}

	private boolean negro() {
		return getType() == ModEntidades.FANTASMA_NEGRO;
	}

	private boolean rojo() {
		return getType() == ModEntidades.FANTASMA_ROJO;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 16.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected PathNavigation createNavigation(Level mundo) {
		FlyingPathNavigation navegacion = new FlyingPathNavigation(this, mundo);
		navegacion.setCanOpenDoors(false);
		navegacion.setCanFloat(true);
		navegacion.setCanPassDoors(true);
		return navegacion;
	}

	@Override
	protected void registerGoals() {
		// El Blanco y el Rojo pegan mucho más seguido (cada 6 ticks); el Amarillo y el Negro, normal (cada 20).
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.6, true) {
			@Override
			protected int getAttackInterval() {
				return amarillo() || negro() ? 20 : 6;
			}
		});
		goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean doHurtTarget(Entity objetivo) {
		triggerAnim("ataque", "golpe");
		level().playSound(null, getX(), getY(), getZ(), SONIDO_GOLPE, SoundSource.HOSTILE, 1f, 1f);
		if (amarillo() && objetivo instanceof LivingEntity vivo) {
			// Te mata al instante (como /kill: no lo para la armadura ni el tótem) y el fantasma desaparece.
			vivo.hurt(damageSources().genericKill(), Float.MAX_VALUE);
			if (level() instanceof ServerLevel mundo) {
				mundo.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.9, getZ(), 25, 0.4, 0.6, 0.4, 0.05);
				mundo.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.9, getZ(), 15, 0.3, 0.5, 0.3, 0.02);
			}
			discard();
			return true;
		}
		boolean pego = super.doHurtTarget(objetivo);
		if (pego && negro() && objetivo instanceof net.minecraft.server.level.ServerPlayer jugador && !jugador.isCreative()) {
			com.dedsafio4.dimension.Limbo.quitarCorazon(jugador, "El Fantasma Negro te quitó un corazón para siempre.");
		}
		if (pego && blanco() && objetivo instanceof LivingEntity vivo) {
			vivo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
		}
		return pego;
	}

	// Vuela: no se cae ni le pasa nada al caer.
	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {}

	@Override
	protected SoundEvent getAmbientSound() {
		return SONIDO_AMBIENTE;
	}

	// --- Animaciones (las del Dedsafío 3) ---

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		String mueve = rojo() ? "fly" : "walk", golpe = rojo() ? "hit" : "attack";
		RawAnimation moviendo = RawAnimation.begin().thenLoop(mueve), quieto = RawAnimation.begin().thenLoop("idle");
		controladores.add(new AnimationController<>(this, "movimiento", 4,
				estado -> estado.setAndContinue(estado.isMoving() ? moviendo : quieto)));
		controladores.add(new AnimationController<>(this, "ataque", 2, estado -> software.bernie.geckolib.animation.PlayState.STOP)
				.triggerableAnim("golpe", RawAnimation.begin().thenPlay(golpe)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
