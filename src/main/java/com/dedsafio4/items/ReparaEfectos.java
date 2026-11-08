package com.dedsafio4.items;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Arregla los efectos que quedaron "sueltos" (sin su lugar en el registro). En el servidor algún mod
 * (customworlds, al devolver los efectos de un mundo) los rearma así, y después el juego no los puede guardar:
 * al cambiar de dimensión salta un error y el viaje se cancela (por ejemplo, el Wraith no te puede llevar al Limbo).
 * Se cambian por el mismo efecto bien registrado, con el mismo tiempo y nivel.
 */
public final class ReparaEfectos {
	private ReparaEfectos() {}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			for (ServerPlayer p : server.getPlayerList().getPlayers()) reparar(p);
		});
	}

	public static void reparar(LivingEntity quien) {
		List<MobEffectInstance> sueltos = null;
		for (MobEffectInstance e : quien.getActiveEffects()) {
			if (e.getEffect() instanceof Holder.Reference<MobEffect>) continue;
			if (sueltos == null) sueltos = new ArrayList<>();
			sueltos.add(e);
		}
		if (sueltos == null) return;
		for (MobEffectInstance e : sueltos) {
			Holder<MobEffect> bueno = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e.getEffect().value());
			quien.getActiveEffectsMap().remove(e.getEffect());
			if (bueno instanceof Holder.Reference<MobEffect>) {
				quien.getActiveEffectsMap().put(bueno, new MobEffectInstance(bueno, e.getDuration(), e.getAmplifier(),
						e.isAmbient(), e.isVisible(), e.showIcon()));
			}
		}
	}
}
