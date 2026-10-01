package com.dedsafio4.client.mixin;

import com.dedsafio4.client.CieloCliente;
import com.dedsafio4.client.PocionesCliente;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	/** Poción Negro Puro: mientras se dibuja el mundo, los bloques usan la textura negra. */
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void dedsafio4$negroPuroInicio(CallbackInfo ci) {
		PocionesCliente.antesDelMundo();
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
