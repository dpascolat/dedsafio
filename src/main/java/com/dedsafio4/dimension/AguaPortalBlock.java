package com.dedsafio4.dimension;

import net.minecraft.world.level.block.Block;

/**
 * El Agua Rara: si la tocás te lleva a la otra dimensión. El viaje lo maneja {@link Portales},
 * que revisa a los jugadores todos los ticks (así funciona aunque te quedes quieto adentro).
 */
public class AguaPortalBlock extends Block {
	public AguaPortalBlock(Properties propiedades) {
		super(propiedades);
	}
}
