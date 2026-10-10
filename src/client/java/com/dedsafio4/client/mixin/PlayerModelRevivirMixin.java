package com.dedsafio4.client.mixin;

import com.dedsafio4.client.revivir.RevivirAnimacion;
import com.dedsafio4.client.revivir.RevivirCliente;
import com.dedsafio4.revivir.Revivir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * En /revivir cinematica, desde que cae la copia, el jugador de verdad hace la animación (acostado y levantándose).
 * Antes de eso el que se mueve es la copia con su skin (ver RevivirCliente).
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelRevivirMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void dedsafio4$revivir(LivingEntity entidad, float limbSwing, float limbSwingAmount, float edad,
								   float giroCabeza, float inclinacionCabeza, CallbackInfo ci) {
		if (!(entidad instanceof Player jugador)) return;
		float t = RevivirCliente.tiempo(jugador.getUUID(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
		if (t < Revivir.CAIDA) return;
		RevivirAnimacion.aplicar((PlayerModel<?>) (Object) this, t);
	}
}
