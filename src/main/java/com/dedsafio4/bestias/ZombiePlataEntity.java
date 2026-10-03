package com.dedsafio4.bestias;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * Zombie Plata: un zombie consumido por Qumara (con cristales que brillan). Muy rápido (×1,6) y fuerte
 * (×1,75 de daño), le cuesta más empujarlo, escala paredes como una araña y no se quema con el sol.
 */
public class ZombiePlataEntity extends Zombie {
	public ZombiePlataEntity(EntityType<? extends Zombie> tipo, Level level) {
		super(tipo, level);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Zombie.createAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.23 * 1.6)
				.add(Attributes.ATTACK_DAMAGE, 3.0 * 1.75)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	/** Busca caminos por las paredes, como la araña. */
	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WallClimberNavigation(this, level);
	}

	/** Si choca contra una pared, la sube (igual que la araña). */
	@Override
	public boolean onClimbable() {
		return horizontalCollision || super.onClimbable();
	}

	/** La Qumara lo protege del sol. */
	@Override
	protected boolean isSunSensitive() {
		return false;
	}

	/** Siempre adulto (el dibujo es de tamaño grande). */
	@Override
	public void setBaby(boolean bebe) {
		super.setBaby(false);
	}

	/** No se convierte en ahogado bajo el agua. */
	@Override
	protected boolean convertsInWater() {
		return false;
	}
}
