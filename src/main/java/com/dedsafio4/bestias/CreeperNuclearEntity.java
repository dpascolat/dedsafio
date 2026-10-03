package com.dedsafio4.bestias;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * Creeper Nuclear (el diseño "Bomba andante"): un bloque explosivo amarillo con patas y botón rojo.
 * Se porta como un creeper (camina hacia el jugador, a menos de 3 bloques aprieta el botón y titila, y si
 * te alejas a más de 7 se calma), pero tarda 2 segundos y la explosión es grande: como la de un creeper
 * cargado (potencia 6). Aparece en la nieve. No suelta nada.
 */
public class CreeperNuclearEntity extends Creeper {
	public static final int POTENCIA = 6, MECHA = 40;

	public CreeperNuclearEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
		// El creeper guarda la potencia y la mecha en sus datos: se cargan los nuestros.
		CompoundTag datos = new CompoundTag();
		datos.putByte("ExplosionRadius", (byte) POTENCIA);
		datos.putShort("Fuse", (short) MECHA);
		super.readAdditionalSaveData(datos);
	}
}
