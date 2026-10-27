package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Parásito Volador: un bichito rojo con alas que vive adentro de las Verrugas Claras y Oscuras de Gleba. Cuando un
 * jugador rompe una (en supervivencia), a veces sale (ver salirDeVerruga) y lo ataca; es muy rápido. Al morir suelta 2
 * Verrugas Oscuras. Daño, vida y velocidad se cambian con /dañomob, /vidamob y /velocidad.
 */
public class ParasitoVoladorEntity extends Monster {
	/** De cada cuántas verrugas rotas sale uno (1 de cada 3). */
	private static final int UNO_DE_CADA = 3;

	public ParasitoVoladorEntity(EntityType<? extends ParasitoVoladorEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
		this.xpReward = 4;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 8.0)
				.add(Attributes.MOVEMENT_SPEED, 0.45)
				.add(Attributes.FLYING_SPEED, 1.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	/** Al romper una verruga: a veces sale un parásito de adentro, enojado con el que la rompió. */
	public static void salirDeVerruga(ServerLevel mundo, BlockPos pos, Player jugador) {
		if (jugador.getAbilities().instabuild || mundo.getRandom().nextInt(UNO_DE_CADA) != 0) return;
		var tipo = com.dedsafio4.nave.ModEntidades.PARASITO_VOLADOR;
		if (!AnuncioMob.activo(mundo.getServer(), tipo)) return;
		ParasitoVoladorEntity parasito = tipo.create(mundo);
		if (parasito == null) return;
		parasito.moveTo(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, mundo.getRandom().nextFloat() * 360, 0);
		parasito.finalizeSpawn(mundo, mundo.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null);
		parasito.setTarget(jugador);
		mundo.addFreshEntity(parasito);
		mundo.playSound(null, pos, SoundEvents.SILVERFISH_AMBIENT, parasito.getSoundSource(), 1f, 1.4f);
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
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4, true));
		goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SILVERFISH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SILVERFISH_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.3f;
	}
}
