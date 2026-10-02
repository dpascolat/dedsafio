package com.dedsafio4.neocompat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** La entrada del mod en NeoForge: todo lo demás lo arma Puente con el mismo código de la versión Fabric. */
@Mod("dedsafio4")
public final class Dedsafio4NeoForge {
	public Dedsafio4NeoForge(IEventBus busDelMod, Dist lado) {
		Puente.iniciar(busDelMod, lado);
	}
}
