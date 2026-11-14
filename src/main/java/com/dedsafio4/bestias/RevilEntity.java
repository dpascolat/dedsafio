package com.dedsafio4.bestias;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Revil: un caballero de armadura dorada (modelo de modelo_minecraft.glb, pasado a bloquecitos). No hace nada: es
 * solo para verlo. No se mueve, no ataca, no lo empujan y no desaparece solo.
 */
public class RevilEntity extends PathfinderMob {
	public RevilEntity(EntityType<? extends RevilEntity> tipo, Level mundo) {
		super(tipo, mundo);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}
}
