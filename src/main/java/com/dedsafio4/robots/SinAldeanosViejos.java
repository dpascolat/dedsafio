package com.dedsafio4.robots;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;

/**
 * En Dedsafío 4 no hay aldeanos normales ni aldeanos zombis: los reemplazan los Aldeanos Robot.
 * Cualquiera que aparezca (en una aldea, con un huevo, por un zombi que infecta) se borra.
 */
public final class SinAldeanosViejos {
	private SinAldeanosViejos() {}

	public static void registrar() {
		ServerEntityEvents.ENTITY_LOAD.register((entidad, level) -> {
			if (entidad instanceof Villager || entidad instanceof ZombieVillager) entidad.discard();
		});
	}
}
