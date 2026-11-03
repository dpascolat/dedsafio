package com.dedsafio4.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Los golpes a los jugadores, de a uno. Con mods que mueven a los mobs en varios hilos a la vez (como "async" en el
 * servidor), dos mobs podían pegarle al mismo jugador en el mismo instante: los dos lo veían "por morir", así que se
 * gastaban dos tótems juntos o moría varias veces seguidas. Con el candado, el segundo golpe espera a que termine el
 * primero (y ya ve el tótem usado y la invulnerabilidad que deja el golpe).
 */
@Mixin(LivingEntity.class)
public abstract class JugadorGolpeDeAUnoMixin {
	private static final Object CANDADO = new Object();

	@WrapMethod(method = "hurt")
	private boolean dedsafio4$deAUno(DamageSource fuente, float danio, Operation<Boolean> original) {
		if (!((Object) this instanceof Player)) return original.call(fuente, danio);
		synchronized (CANDADO) {
			return original.call(fuente, danio);
		}
	}
}
