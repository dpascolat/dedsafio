package com.dedsafio4.demonios;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Los fantasmas encadenados (el mismo demonio de cubos en tres colores y dos tamaños). Todos pelean cuerpo a cuerpo:
 * <ul>
 * <li><b>Rosa:</b> si estás cerca te da Hambre nivel 100 por unos segundos.</li>
 * <li><b>Morado:</b> si te golpea, no puedes saltar por unos segundos (efecto Sin Salto). El chico vuela.</li>
 * <li><b>Rojo:</b> si te golpea, te da Lentitud IV.</li>
 * </ul>
 * Los chicos son los Reclusos Espectrales; los grandes, los Colosos Prisioneros: más vida y daño, y no se los puede
 * empujar (el Morado grande además es muy lento y tiene mucha más vida).
 */
public class EspectroEntity extends Monster implements GeoEntity {
	public enum Color { ROSA, MORADO, ROJO }

	/** Hasta dónde llega el Hambre del Rosa (en bloques) y cuánto dura cada efecto (en ticks). */
	private static final double CERCA_HAMBRE = 4;
	private static final int DURA_HAMBRE = 5 * 20, DURA_SIN_SALTO = 5 * 20, DURA_LENTITUD = 5 * 20;

	private final Color color;
	private final boolean grande;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public EspectroEntity(EntityType<? extends Monster> tipo, Level level, Color color, boolean grande) {
		super(tipo, level);
		this.color = color;
		this.grande = grande;
		if (vuela()) {
			moveControl = new FlyingMoveControl(this, 20, true);
			setNoGravity(true);
		}
		xpReward = grande ? 25 : 8;
	}

	public Color color() {
		return color;
	}

	public boolean grande() {
		return grande;
	}

	/** Sólo el Recluso Espectral Morado (el chico) vuela; los demás caminan flotando un poco sobre el piso. */
	public boolean vuela() {
		return color == Color.MORADO && !grande;
	}

	public static AttributeSupplier.Builder atributosRecluso(boolean vuela) {
		AttributeSupplier.Builder b = Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30)
				.add(Attributes.ATTACK_DAMAGE, 5)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.FOLLOW_RANGE, 32);
		if (vuela) b.add(Attributes.FLYING_SPEED, 0.75);
		return b;
	}

	public static AttributeSupplier.Builder atributosColoso(boolean muyLento) {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, muyLento ? 300 : 150)
				.add(Attributes.ATTACK_DAMAGE, 12)
				.add(Attributes.MOVEMENT_SPEED, muyLento ? 0.14 : 0.22)
				.add(Attributes.ARMOR, 6)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1)
				.add(Attributes.FOLLOW_RANGE, 32);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		// (Se llama antes de que el constructor guarde el color: se mira el tipo.)
		if (getType() == ModDemonios.RECLUSO_MORADO) {
			FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
			nav.setCanOpenDoors(false);
			nav.setCanFloat(true);
			nav.setCanPassDoors(true);
			return nav;
		}
		return super.createNavigation(level);
	}

	@Override
	protected void registerGoals() {
		// (También se llama antes de guardar el tamaño: se mira el tipo.)
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, ModDemonios.esColoso(getType()) ? 1.0 : 1.15, true));
		if (getType() == ModDemonios.RECLUSO_MORADO) goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		else goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean doHurtTarget(Entity objetivo) {
		DanoMob.aplicar(this);   // el daño de /danomob
		boolean pego = super.doHurtTarget(objetivo);
		if (pego && objetivo instanceof LivingEntity vivo) {
			switch (color) {
				case MORADO -> vivo.addEffect(new MobEffectInstance(ModDemonios.SIN_SALTO, DURA_SIN_SALTO, 0), this);
				case ROJO -> vivo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURA_LENTITUD, 3), this);
				default -> {
				}
			}
		}
		return pego;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// El Rosa: a los que están cerca, Hambre nivel 100 por unos segundos.
		if (color == Color.ROSA && tickCount % 20 == 0 && level() instanceof ServerLevel mundo) {
			double r = CERCA_HAMBRE * getScale() * (grande ? 1.5 : 1);
			for (Player p : mundo.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r), p -> !p.isCreative() && !p.isSpectator())) {
				p.addEffect(new MobEffectInstance(MobEffects.HUNGER, DURA_HAMBRE, 99), this);
			}
		}
	}

	/** Los Colosos no se pueden empujar. */
	@Override
	public boolean isPushable() {
		return !grande && super.isPushable();
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {
		if (!vuela()) super.checkFallDamage(y, enElPiso, estado, pos);
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, net.minecraft.world.damagesource.DamageSource fuente) {
		return false;   // son fantasmas
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
