package com.dedsafio4.neocompat.mixin;

import com.dedsafio4.neocompat.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import com.dedsafio4.neocompat.fabric.api.client.rendering.v1.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** DimensionRenderingRegistry de Fabric: cielo, nubes y clima propios de una dimensión. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererCieloMixin {
	@Shadow
	private ClientLevel level;

	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$cielo(Matrix4f vista, Matrix4f proyeccion, float parcial, Camera camara, boolean niebla, Runnable prepararNiebla, CallbackInfo ci) {
		if (level == null) return;
		var cielo = DimensionRenderingRegistry.CIELOS.get(level.dimension());
		if (cielo == null) return;
		cielo.render(new WorldRenderContext(camara, vista, proyeccion, Minecraft.getInstance().getTimer(), new PoseStack()));
		ci.cancel();
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$nubes(CallbackInfo ci) {
		if (level == null) return;
		var nubes = DimensionRenderingRegistry.NUBES.get(level.dimension());
		if (nubes == null) return;
		nubes.render(dedsafio4$contexto());
		ci.cancel();
	}

	@Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$clima(CallbackInfo ci) {
		if (level == null) return;
		var clima = DimensionRenderingRegistry.CLIMAS.get(level.dimension());
		if (clima == null) return;
		clima.render(dedsafio4$contexto());
		ci.cancel();
	}

	@org.spongepowered.asm.mixin.Unique
	private static WorldRenderContext dedsafio4$contexto() {
		Minecraft mc = Minecraft.getInstance();
		return new WorldRenderContext(mc.gameRenderer.getMainCamera(), new Matrix4f(), new Matrix4f(), mc.getTimer(), new PoseStack());
	}
}
