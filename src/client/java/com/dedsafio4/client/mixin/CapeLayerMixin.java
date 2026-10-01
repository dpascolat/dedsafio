package com.dedsafio4.client.mixin;

import com.dedsafio4.client.HermandadesCliente;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.item.BannerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
	/** Si el jugador lleva el estandarte de su Hermandad como capa, su capa original no se dibuja. */
	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
			at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinCapaOriginal(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador,
										   float a, float b, float c, float d, float e, float f, CallbackInfo ci) {
		if (HermandadesCliente.estandarteDe(jugador.getUUID()).getItem() instanceof BannerItem) ci.cancel();
	}
}
