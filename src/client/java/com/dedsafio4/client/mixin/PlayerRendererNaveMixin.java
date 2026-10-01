package com.dedsafio4.client.mixin;

import com.dedsafio4.despegue.NaveViajeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * En la nave parada (la punta para arriba) los jugadores van como astronautas: acostados boca arriba, con la cara y
 * las rodillas mirando al cielo. Se gira el cuerpo 90° hacia atrás y se corre para que quede centrado en la cabina.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererNaveMixin {
	@Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
			at = @At("TAIL"))
	private void dedsafio4$acostadoEnLaNave(AbstractClientPlayer jugador, PoseStack pose, float balanceo, float giroCuerpo,
											float partialTick, float escala, CallbackInfo ci) {
		if (!(jugador.getVehicle() instanceof NaveViajeEntity)) return;
		pose.translate(0f, 0f, -0.9f);
		pose.mulPose(Axis.XP.rotationDegrees(90f));
	}
}
