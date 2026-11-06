package com.dedsafio4.client.mixin;

import com.dedsafio4.client.CieloCliente;
import com.dedsafio4.client.PocionesCliente;
import com.dedsafio4.client.cielo2.CieloRojo2;
import com.dedsafio4.client.ruleta.EntregaCliente;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
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

	/** Luna Roja: justo después de elegir la textura de la luna, se tiñe de rojo (se deja la transparencia de la lluvia). */
	@Inject(method = "renderSky", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",
			ordinal = 1, shift = At.Shift.AFTER))
	private void dedsafio4$lunaRoja(CallbackInfo ci) {
		if (!com.dedsafio4.client.LunaRojaCliente.visible()) return;
		float[] c = RenderSystem.getShaderColor();
		RenderSystem.setShaderColor(com.dedsafio4.client.LunaRojaCliente.LUNA_R, com.dedsafio4.client.LunaRojaCliente.LUNA_G,
				com.dedsafio4.client.LunaRojaCliente.LUNA_B, c[3]);
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
		if (com.dedsafio4.client.EclipseCliente.activoAqui() || EntregaCliente.vacioCompleto()) ci.cancel();
	}

	/** La entrega de la Dedita y el ritual de la Fogata: el Vacío Cósmico tapa lo que se dibujó hasta acá (el terreno y el cielo). */
	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=entities"))
	private void dedsafio4$vacio(DeltaTracker tiempo, boolean contorno, Camera camara, GameRenderer juego, LightTexture luz, Matrix4f vista, Matrix4f proyeccion, CallbackInfo ci) {
		EntregaCliente.dibujarVacio(camara, vista, proyeccion, tiempo.getGameTimeDeltaPartialTick(false));
	}

	/** La entrega de la Dedita: el piso, la luz y las chispas. */
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V"))
	private void dedsafio4$entrega(DeltaTracker tiempo, boolean contorno, Camera camara, GameRenderer juego, LightTexture luz, Matrix4f vista, Matrix4f proyeccion, CallbackInfo ci) {
		EntregaCliente.dibujar(camara, vista, proyeccion, tiempo.getGameTimeDeltaPartialTick(false));
	}

	/** En el Vacío Cósmico no llueve. */
	@Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinLluvia(CallbackInfo ci) {
		if (EntregaCliente.vacioCompleto()) ci.cancel();
	}

	/** En el Vacío Cósmico se apaga la luz del terreno y no se ve el agua (se dibuja después del vacío). */
	@Inject(method = "renderSectionLayer", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$terrenoVacio(RenderType capa, double x, double y, double z, Matrix4f vista, Matrix4f proyeccion, CallbackInfo ci) {
		if (EntregaCliente.vacioCompleto() && (capa == RenderType.translucent() || capa == RenderType.tripwire())) {
			ci.cancel();
			return;
		}
		EntregaCliente.apagarTerreno();
	}

	@Inject(method = "renderSectionLayer", at = @At("RETURN"))
	private void dedsafio4$terrenoVacioFin(CallbackInfo ci) {
		EntregaCliente.encenderTerreno();
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
