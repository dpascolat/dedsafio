package com.dedsafio4.bestias;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Creeper Nuclear (el diseño "Bomba andante"): un bloque explosivo amarillo con patas y botón rojo.
 * Se porta como un creeper (camina hacia el jugador, a menos de 3 bloques aprieta el botón y titila, y si
 * te alejas a más de 7 se calma), pero tarda 2 segundos y la explosión es grande: como la de un creeper
 * cargado (potencia 6). Aparece en la nieve y en el desierto. No suelta nada.
 * Además, al explotar, la onda mata a todos los jugadores a menos de 40 bloques que estén a la vista de la explosión;
 * el que está tapado (una pared, un techo, bajo tierra) se salva, y el que se cubre con un escudo también, pero el
 * escudo se rompe entero, tenga la durabilidad que tenga. También rompe en cadena el Hierro Oxidado a menos de 50
 * bloques. CreeperNuclearMixin la dispara justo al explotar.
 */
public class CreeperNuclearEntity extends Creeper {
	public static final int POTENCIA = 6, MECHA = 40;
	/** Hasta dónde llega la onda que mata. */
	public static final double RADIO_ONDA = 40;

	public CreeperNuclearEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
		// El creeper guarda la potencia y la mecha en sus datos: se cargan los nuestros.
		CompoundTag datos = new CompoundTag();
		datos.putByte("ExplosionRadius", (byte) POTENCIA);
		datos.putShort("Fuse", (short) MECHA);
		super.readAdditionalSaveData(datos);
	}

	/** La onda: a la vista y a menos de RADIO_ONDA bloques, te mata (o te rompe el escudo si te cubres). */
	public void onda(ServerLevel mundo) {
		Vec3 centro = this.position().add(0, this.getBbHeight() / 2, 0);
		// Los bloques de Hierro Oxidado cercanos se rompen en cadena.
		com.dedsafio4.bloques.HierroOxidado.explosion(mundo, centro);
		for (ServerPlayer jugador : mundo.players()) {
			if (!jugador.isAlive() || jugador.isSpectator() || jugador.isCreative()) continue;
			if (jugador.position().distanceTo(centro) > RADIO_ONDA) continue;
			// Tapado: ningún rayo desde la explosión llega a su cuerpo.
			if (Explosion.getSeenPercent(centro, jugador) <= 0) continue;
			if (jugador.isBlocking()) {
				// El escudo de la mano (no la copia que guarda el "usando"), y se rompe entero.
				ItemStack escudo = jugador.getItemInHand(jugador.getUsedItemHand());
				var mano = LivingEntity.getSlotForHand(jugador.getUsedItemHand());
				jugador.stopUsingItem();
				escudo.hurtAndBreak(escudo.getMaxDamage() + 1, jugador, mano);
				continue;
			}
			jugador.hurt(this.damageSources().explosion(this, this), 10000f);
		}
	}
}
