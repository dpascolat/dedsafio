package com.dedsafio4.client.mixin;

import com.dedsafio4.client.fogata.FogataRitual;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante el ritual de la Fogata también se oscurece la niebla, para que el horizonte acompañe al cielo. */
@Mixin(FogRenderer.class)
abstract class NieblaFogataMixin {
	@Shadow
	private static float fogRed;
	@Shadow
	private static float fogGreen;
	@Shadow
	private static float fogBlue;

	@Inject(method = "setupColor", at = @At("TAIL"))
	private static void dedsafio4$niebla(Camera camara, float parcial, ClientLevel mundo, int distancia, float oscuridad, CallbackInfo ci) {
		if (camara.getFluidInCamera() != FogType.NONE) return;
		Vector4f antes = new Vector4f(fogRed, fogGreen, fogBlue, 1);
		Vector4f despues = FogataRitual.a(antes, parcial);
		if (despues == antes) return;
		fogRed = despues.x;
		fogGreen = despues.y;
		fogBlue = despues.z;
		RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0);
	}
}
