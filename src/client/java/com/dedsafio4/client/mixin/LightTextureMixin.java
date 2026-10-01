package com.dedsafio4.client.mixin;

import com.dedsafio4.client.EclipseCliente;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
	@Shadow @Final private NativeImage lightPixels;

	/**
	 * Justo antes de subir la tabla de luces: en el Eclipse nada ilumina; y con la Linterna prendida en la
	 * oscuridad se ve iluminado (afuera del haz lo oscurece LinternaLuz.dibujarPantalla).
	 */
	@Inject(method = "updateLightTexture", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
	private void dedsafio4$eclipse(float parcial, CallbackInfo ci) {
		if (EclipseCliente.activoAqui()) EclipseCliente.oscurecer(lightPixels);
		if (com.dedsafio4.client.LinternaLuz.alumbrando()) com.dedsafio4.client.LinternaLuz.iluminar(lightPixels);
	}
}
