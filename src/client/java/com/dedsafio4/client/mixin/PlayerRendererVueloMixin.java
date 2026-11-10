package com.dedsafio4.client.mixin;

import com.dedsafio4.client.VueloTraje;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Volando con el Traje Espacial: el cuerpo se inclina hacia adelante según la velocidad (ver VueloTraje). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererVueloMixin {
	@Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
			at = @At("TAIL"))
	private void dedsafio4$inclinarVolando(AbstractClientPlayer jugador, PoseStack pose, float balanceo, float giroCuerpo,
										   float partialTick, float escala, CallbackInfo ci) {
		if (!VueloTraje.volando(jugador)) return;
		float f = VueloTraje.avance(jugador);
		// Gira alrededor de la cintura, no de los pies.
		pose.translate(0f, 0.9f, 0f);
		pose.mulPose(Axis.XP.rotationDegrees(-f * VueloTraje.INCLINACION_MAXIMA));
		pose.translate(0f, -0.9f, 0f);
	}
}
