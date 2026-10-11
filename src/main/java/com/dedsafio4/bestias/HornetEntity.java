package com.dedsafio4.bestias;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Hornet (de Hollow Knight: Silksong; modelo, textura y animaciones del addon que pasó Patricio). Sobre todo para
 * /cambiarmob: el jugador se ve como ella, camina como ella y, al pegar, hace el golpe con la aguja. Como mob camina
 * por ahí y, si le pegas, te devuelve el golpe.
 */
public class HornetEntity extends PathfinderMob implements GeoEntity {
	private static final RawAnimation QUIETA = RawAnimation.begin().thenLoop("animation.hornet.idle");
	private static final RawAnimation CAMINAR = RawAnimation.begin().thenLoop("animation.hornet.walk");
	private static final RawAnimation GOLPE = RawAnimation.begin().thenPlay("animation.hornet.attack");
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	/** Para empezar el golpe una sola vez por cada vez que pega (lo usa el cliente). */
	private boolean pegando;

	public HornetEntity(EntityType<? extends HornetEntity> tipo, Level mundo) {
		super(tipo, mundo);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 5.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "movimiento", 4,
				estado -> estado.setAndContinue(estado.isMoving() ? CAMINAR : QUIETA)));
		// El golpe con la aguja cada vez que pega (también cuando el que pega es un jugador transformado).
		controladores.add(new AnimationController<>(this, "ataque", 1, estado -> {
			var control = estado.getController();
			if (swinging && !pegando) {
				pegando = true;
				control.forceAnimationReset();
				return estado.setAndContinue(GOLPE);
			}
			if (!swinging) pegando = false;
			return control.getCurrentAnimation() != null && !control.hasAnimationFinished() ? PlayState.CONTINUE : PlayState.STOP;
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
