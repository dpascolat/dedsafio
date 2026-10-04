package com.dedsafio4.client.mixin;

import com.dedsafio4.client.momentito.MomentitoCamara;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante /momentito 1 la niebla queda muy lejos (como en el diseño: de 900 a 7000 bloques). */
@Mixin(FogRenderer.class)
public abstract class MomentitoNieblaMixin {
	@Inject(method = "setupFog", at = @At("TAIL"))
	private static void dedsafio4$nieblaLejos(Camera camara, FogRenderer.FogMode modo, float distancia, boolean espesa,
											  float partialTick, CallbackInfo ci) {
		if (!MomentitoCamara.activa() || camara.getFluidInCamera() != FogType.NONE) return;
		RenderSystem.setShaderFogStart(900f);
		RenderSystem.setShaderFogEnd(7000f);
	}
}
