package com.dedsafio4.client.mixin;

import com.dedsafio4.client.cielo2.CieloRojo2;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cielo rojo 2: la cámara tiembla mientras Reviil llega. */
@Mixin(GameRenderer.class)
public abstract class GameRendererTemblorMixin {
	@Inject(method = "bobHurt", at = @At("HEAD"))
	private void dedsafio4$temblor(PoseStack pose, float partialTick, CallbackInfo ci) {
		CieloRojo2.temblor(pose, partialTick);
	}
}
