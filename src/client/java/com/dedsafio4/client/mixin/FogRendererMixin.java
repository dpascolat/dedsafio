package com.dedsafio4.client.mixin;

import com.dedsafio4.client.CieloCliente;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	/**
	 * Eclipse o Linterna en la oscuridad: la niebla está cerca, así la luz de la linterna llega hasta unos
	 * 24 bloques y lo de más lejos queda a oscuras.
	 */
	@Inject(method = "setupFog", at = @At("TAIL"))
	private static void dedsafio4$nieblaOscura(Camera camara, FogRenderer.FogMode modo, float distancia, boolean espesa,
											   float partialTick, CallbackInfo ci) {
		if (camara.getFluidInCamera() != FogType.NONE) return;
		float oscuridad = com.dedsafio4.client.EclipseCliente.activoAqui() ? com.dedsafio4.client.EclipseCliente.oscuro(partialTick) : 0f;
		if (oscuridad <= 0f) return;
		float fin = Mth.lerp(oscuridad, distancia, 24f);
		RenderSystem.setShaderFogStart(Mth.lerp(oscuridad, RenderSystem.getShaderFogStart(), 2f));
		RenderSystem.setShaderFogEnd(Math.min(RenderSystem.getShaderFogEnd(), fin));
	}

	@Shadow private static float fogRed;
	@Shadow private static float fogGreen;
	@Shadow private static float fogBlue;

	/** Con /cielo rojo, la niebla del horizonte también es roja (bajo el agua o la lava queda normal). */
	@Inject(method = "setupColor", at = @At("TAIL"))
	private static void dedsafio4$nieblaRoja(Camera camara, float partialTick, ClientLevel level, int distancia,
											 float oscuridadJefe, CallbackInfo ci) {
		if (camara.getFluidInCamera() != FogType.NONE) return;
		// Eclipse, o la Linterna en la oscuridad: la niebla es negra (lo lejano se pierde en lo oscuro).
		if (com.dedsafio4.client.EclipseCliente.activoAqui()) {
			float p = com.dedsafio4.client.EclipseCliente.oscuro(partialTick);   // mientras empieza, de a poco
			fogRed = Mth.lerp(p, fogRed, com.dedsafio4.client.EclipseCliente.NIEBLA_R);
			fogGreen = Mth.lerp(p, fogGreen, com.dedsafio4.client.EclipseCliente.NIEBLA_G);
			fogBlue = Mth.lerp(p, fogBlue, com.dedsafio4.client.EclipseCliente.NIEBLA_B);
			RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0f);
			return;
		}
		if (!CieloCliente.activoAqui()) return;
		float p = CieloCliente.progresoCielo(partialTick);   // mientras se esparce, cambia de a poco
		fogRed = Mth.lerp(p, fogRed, CieloCliente.NIEBLA_R);
		fogGreen = Mth.lerp(p, fogGreen, CieloCliente.NIEBLA_G);
		fogBlue = Mth.lerp(p, fogBlue, CieloCliente.NIEBLA_B);
		RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0f);
	}
}
