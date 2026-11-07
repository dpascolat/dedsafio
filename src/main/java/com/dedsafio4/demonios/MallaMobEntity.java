package com.dedsafio4.demonios;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Los mobs nuevos que todavía no tienen función (los tres Demonios y la Criatura Oscura): se quedan quietos donde
 * los ponen y miran a los jugadores. Los Demonios flotan (sin gravedad, se mecen arriba y abajo al dibujarlos).
 * Su tamaño se puede cambiar con /boss tamano (o con el atributo minecraft:generic.scale).
 */
public class MallaMobEntity extends PathfinderMob {
	private final boolean flota;

	public MallaMobEntity(EntityType<? extends PathfinderMob> tipo, Level level, boolean flota) {
		super(tipo, level);
		this.flota = flota;
		setNoGravity(flota);
		setPersistenceRequired();
	}

	public boolean flota() {
		return flota;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40)
				.add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 16f));
		goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, net.minecraft.world.damagesource.DamageSource fuente) {
		return !flota && super.causeFallDamage(distancia, multiplicador, fuente);
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}
}
