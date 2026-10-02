package com.dedsafio4.bestias;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/** Creeper Amarillo: igual que el creeper de siempre (mismo modelo y misma explosión), pero amarillo clarito. */
public class CreeperAmarilloEntity extends Creeper {
	public CreeperAmarilloEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
	}
}
