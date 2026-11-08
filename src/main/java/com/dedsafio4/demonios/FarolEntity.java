package com.dedsafio4.demonios;

import net.minecraft.server.level.ServerPlayer;
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
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * El Farol: un farol oscuro que camina con 4 patas. Pelea cuerpo a cuerpo y, cuando te pega, además del daño te
 * abre el puzzle de la cámara (ver PuzzleCamara): tienes que girar la palanca 4 veces para seguir.
 * Con "/cambio farol 1": cuando está cerca de un jugador se infla como un Creeper y explota (y se muere), y al
 * morir (de cualquier forma) les abre el puzzle a los jugadores que están cerca.
 */
public class FarolEntity extends Monster implements GeoEntity {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	/** Con el cambio farol: a qué distancia empieza a inflarse, cuánto tarda (1,5 s, como un Creeper) y qué tan fuerte explota. */
	private static final double CERCA = 3, PUZZLE_RADIO = 6;
	private static final int MECHA = 30;
	private static final float EXPLOSION = 3f;
	private int inflandose;

	public FarolEntity(EntityType<? extends Monster> tipo, Level level) {
		super(tipo, level);
		xpReward = 10;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 40)
				.add(Attributes.ATTACK_DAMAGE, 6)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.ARMOR, 4)
				.add(Attributes.FOLLOW_RANGE, 32);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	private boolean cambioActivo() {
		return level() instanceof net.minecraft.server.level.ServerLevel mundo
				&& com.dedsafio4.cambios.Cambios.nivel(mundo.getServer(), com.dedsafio4.cambios.Cambios.FAROL) >= 1;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof net.minecraft.server.level.ServerLevel mundo) || !isAlive() || !cambioActivo()) return;
		boolean cerca = getTarget() instanceof Player p && p.isAlive() && distanceTo(p) <= CERCA;
		if (cerca || inflandose > 0) {
			if (inflandose == 0) playSound(net.minecraft.sounds.SoundEvents.CREEPER_PRIMED, 1f, 0.5f);
			// Se aleja el jugador (más del doble): se desinfla, como un Creeper.
			if (!cerca && !(getTarget() instanceof Player p2 && distanceTo(p2) <= CERCA * 2)) {
				inflandose = Math.max(0, inflandose - 2);
				return;
			}
			inflandose++;
			getNavigation().stop();
			if (inflandose % 4 == 0) mundo.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, getX(), getY(0.6), getZ(), 6, 0.3, 0.4, 0.3, 0.02);
			if (inflandose >= MECHA) {
				mundo.explode(this, getX(), getY(), getZ(), EXPLOSION, Level.ExplosionInteraction.MOB);
				kill();
			}
		}
	}

	/** Con el cambio farol, al morir abre el puzzle a los jugadores que están cerca. */
	@Override
	public void die(net.minecraft.world.damagesource.DamageSource fuente) {
		super.die(fuente);
		if (!cambioActivo()) return;
		for (ServerPlayer jugador : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(PUZZLE_RADIO))) {
			if (jugador.isAlive() && !jugador.isCreative() && !jugador.isSpectator()) PuzzleCamara.abrir(jugador);
		}
	}

	@Override
	public boolean doHurtTarget(Entity objetivo) {
		boolean pego = super.doHurtTarget(objetivo);
		if (pego && objetivo instanceof ServerPlayer jugador && jugador.isAlive()) PuzzleCamara.abrir(jugador);
		return pego;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
