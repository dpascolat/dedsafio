package com.dedsafio4.bestias;

import com.dedsafio4.dimension.Limbo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Wraith: un espectro flotante con una guadaña. Aparece en la oscuridad del Overworld. Si te golpea con la guadaña te
 * arrastra al Limbo: a las coordenadas de /spawn_limbo, o a un lugar seguro cerca de donde estabas si no hay. Si ya
 * estás en el Limbo, el golpe solo lastima.
 */
public class WraithEntity extends Monster implements GeoEntity {
	private static final RawAnimation MUEVE = RawAnimation.begin().thenLoop("move"), QUIETO = RawAnimation.begin().thenLoop("idle"),
			GOLPE = RawAnimation.begin().thenPlay("attack");

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);

	public WraithEntity(EntityType<? extends WraithEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean doHurtTarget(Entity objetivo) {
		triggerAnim("ataque", "golpe");
		level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.6f);
		if (objetivo instanceof ServerPlayer jugador && !jugador.isCreative() && !jugador.isSpectator()
				&& !jugador.level().dimension().equals(Limbo.DIMENSION)) {
			// Te arrastra al Limbo (sin lastimarte).
			if (level() instanceof ServerLevel mundo) {
				mundo.sendParticles(ParticleTypes.SOUL, jugador.getX(), jugador.getY() + 1, jugador.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
				mundo.sendParticles(ParticleTypes.SCULK_SOUL, jugador.getX(), jugador.getY() + 1, jugador.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
			}
			Limbo.mandar(jugador);
			jugador.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
			return true;
		}
		return super.doHurtTarget(objetivo);
	}

	// Flota: no le pasa nada al caer.
	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {}

	@Override
	protected net.minecraft.sounds.SoundEvent getAmbientSound() {
		return SoundEvents.SOUL_ESCAPE.value();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "movimiento", 4,
				estado -> estado.setAndContinue(estado.isMoving() ? MUEVE : QUIETO)));
		controladores.add(new AnimationController<>(this, "ataque", 2, estado -> PlayState.STOP)
				.triggerableAnim("golpe", GOLPE));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
