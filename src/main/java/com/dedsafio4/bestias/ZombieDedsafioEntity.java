package com.dedsafio4.bestias;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * Los zombis nuevos (con el modelo de mandíbula y dedos, y ropa al azar): tres tipos que aparecen en todo el Overworld.
 *  - Común: al pegarte te da Lentitud.
 *  - Del Desierto: al pegarte te da Hambre II, y no se quema con el sol.
 *  - De Nieve (aparece menos): al pegarte te bloquea un casillero de la barra por un rato (SlotsBloqueados); después
 *    tiene que esperar para volver a hacerlo (los tiempos se cambian con /zombienieve).
 * No hay bebés ni se convierten en ahogados. Daño, vida y velocidad se cambian como los demás con /dañomob, /vidamob y /velocidad.
 */
public class ZombieDedsafioEntity extends Zombie {
	public enum Tipo { COMUN, DESIERTO, NIEVE }

	private final Tipo tipo;
	/** Cuándo (tick del mundo) puede volver a bloquear un casillero el de nieve. */
	private long bloqueaDesde;

	public ZombieDedsafioEntity(EntityType<? extends Zombie> entidad, Level level, Tipo tipo) {
		super(entidad, level);
		this.tipo = tipo;
	}

	public Tipo tipo() {
		return tipo;
	}

	@Override
	public boolean doHurtTarget(Entity objetivo) {
		boolean pego = super.doHurtTarget(objetivo);
		if (!pego || !(objetivo instanceof LivingEntity vivo) || !(level() instanceof ServerLevel mundo)) return pego;
		switch (tipo) {
			case COMUN -> vivo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0), this);
			case DESIERTO -> vivo.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1), this);
			case NIEVE -> {
				if (vivo instanceof ServerPlayer jugador && mundo.getGameTime() >= bloqueaDesde) {
					SlotsBloqueados.bloquear(jugador);
					bloqueaDesde = mundo.getGameTime() + SlotsBloqueados.espera(mundo.getServer());
				}
			}
		}
		return pego;
	}

	@Override
	protected boolean isSunSensitive() {
		return tipo != Tipo.DESIERTO;
	}

	@Override
	protected boolean convertsInWater() {
		return false;
	}

	@Override
	public void setBaby(boolean bebe) {
		// Sin bebés: el modelo es de adulto.
	}

	@Override
	public boolean isBaby() {
		return false;
	}
}
