package com.dedsafio4.client.mixin;

import com.dedsafio4.client.VueloTraje;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Volando con el Traje Espacial: brazos, piernas y cabeza con la animación de volar (ver VueloTraje). */
@Mixin(PlayerModel.class)
public abstract class PlayerModelVueloMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void dedsafio4$animarVuelo(LivingEntity entidad, float limbSwing, float limbSwingAmount, float edad,
									   float giroCabeza, float inclinacionCabeza, CallbackInfo ci) {
		if (!(entidad instanceof Player jugador) || !VueloTraje.volando(jugador)) return;
		VueloTraje.posar((PlayerModel<?>) (Object) this, jugador, edad, inclinacionCabeza);
	}
}
