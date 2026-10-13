package com.dedsafio4.client.mixin;

import com.dedsafio4.client.CieloCliente;
import com.dedsafio4.client.PocionesCliente;
import com.dedsafio4.client.cielo2.CieloRojo2;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	/** Poción Negro Puro: mientras se dibuja el mundo, los bloques usan la textura negra. */
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void dedsafio4$negroPuroInicio(CallbackInfo ci) {
		CieloRojo2.antesDelMundo();   // la cabeza de Reviil, en su imagen aparte
		PocionesCliente.antesDelMundo();
	}

	@Shadow
	private boolean doesMobEffectBlockSky(Camera camara) {
		return false;
	}

	/** Cielo rojo 2: el shader del rojo, la grieta y Reviil van encima del cielo de Minecraft. */
	@Inject(method = "renderSky", at = @At("RETURN"))
	private void dedsafio4$cieloRojo2(Matrix4f matrizVista, Matrix4f proyeccion, float partialTick, Camera camara,
									  boolean niebla, Runnable prepararNiebla, CallbackInfo ci) {
		if (niebla) return;
		FogType fluido = camara.getFluidInCamera();
		if (fluido == FogType.LAVA || fluido == FogType.POWDER_SNOW || doesMobEffectBlockSky(camara)) return;
		CieloRojo2.dibujar(matrizVista);
		// Las animaciones de revivir y de la Dedita de la Misión: de noche con estrellas para los que están cerca.
		com.dedsafio4.client.CieloNocheCliente.dibujar(matrizVista);
	}

	/** Cielo rojo 2: mientras el rojo tapa el cielo, las nubes se esconden. */
	@Unique
	private boolean dedsafio4$nubesCambiadas;

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$nubesCielo2(CallbackInfo ci) {
		float alfa = CieloRojo2.nubes() * (1f - com.dedsafio4.client.CieloNocheCliente.noche());
		dedsafio4$nubesCambiadas = false;
		if (alfa <= 0.003f) {
			ci.cancel();
		} else if (alfa < 0.999f) {
			float[] c = RenderSystem.getShaderColor();
			RenderSystem.setShaderColor(c[0], c[1], c[2], c[3] * alfa);
			dedsafio4$nubesCambiadas = true;
		}
	}

	@Inject(method = "renderClouds", at = @At("RETURN"))
	private void dedsafio4$nubesCielo2Fin(CallbackInfo ci) {
		if (!dedsafio4$nubesCambiadas) return;
		dedsafio4$nubesCambiadas = false;
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void dedsafio4$negroPuroFin(CallbackInfo ci) {
		PocionesCliente.despuesDelMundo();
	}

	/** Eclipse: cielo negro con el sol eclipsado, en lugar del de Minecraft (va antes que el cielo rojo). */
	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$eclipse(Matrix4f matrizVista, Matrix4f proyeccion, float partialTick, Camera camara,
								   boolean niebla, Runnable prepararNiebla, CallbackInfo ci) {
		if (!com.dedsafio4.client.EclipseCliente.activoAqui()) return;
		com.dedsafio4.client.EclipseCliente.dibujarCielo(matrizVista, partialTick);
		ci.cancel();
	}

	/** Eclipse: sin nubes. */
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinNubes(CallbackInfo ci) {
		if (com.dedsafio4.client.EclipseCliente.activoAqui()) ci.cancel();
	}

	/** Con el cielo rojo ya completo, se dibuja en lugar del de Minecraft (sin sol, luna ni estrellas). */
	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$cieloRojo(Matrix4f matrizVista, Matrix4f proyeccion, float partialTick, Camera camara,
									 boolean niebla, Runnable prepararNiebla, CallbackInfo ci) {
		if (!CieloCliente.activoAqui() || CieloCliente.progresoCielo(partialTick) < 1f) return;
		CieloCliente.dibujar(matrizVista, partialTick);
		ci.cancel();
	}

	/** Mientras el rojo se esparce, se dibuja encima del cielo de Minecraft, que todavía se ve abajo. */
	@Inject(method = "renderSky", at = @At("RETURN"))
	private void dedsafio4$cieloRojoAvanzando(Matrix4f matrizVista, Matrix4f proyeccion, float partialTick, Camera camara,
											 boolean niebla, Runnable prepararNiebla, CallbackInfo ci) {
		if (!CieloCliente.activoAqui() || CieloCliente.progresoCielo(partialTick) >= 1f) return;
		CieloCliente.dibujar(matrizVista, partialTick);
	}
}
