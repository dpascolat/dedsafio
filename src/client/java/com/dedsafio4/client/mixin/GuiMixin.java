package com.dedsafio4.client.mixin;

import com.dedsafio4.client.PocionesCliente;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.player.Player;
import com.dedsafio4.pociones.ModPociones;

@Mixin(Gui.class)
public abstract class GuiMixin {
	/** Antes del HUD: el tinte de la poción Rojizo afecta al mundo pero no a la barra, corazones, etc. */
	@Inject(method = "render", at = @At("HEAD"))
	private void dedsafio4$rojizo(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		PocionesCliente.dibujarRojizo(graphics);
		com.dedsafio4.client.LinternaLuz.dibujarPantalla(graphics);
	}

	/** Los íconos de las pociones van abajo a la derecha (EfectosPantalla), no arriba. */
	@Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$efectosAbajo(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		com.dedsafio4.client.EfectosPantalla.dibujar(graphics);
		ci.cancel();
	}

	/** Poción Corazones Ocultos: no se dibuja la barra de vida. */
	@Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinCorazones(GuiGraphics graphics, Player jugador, int x, int y, int alto, int indice,
										float vidaMaxima, int vida, int vidaMostrada, int absorcion, boolean parpadeo,
										CallbackInfo ci) {
		if (jugador.hasEffect(ModPociones.EFECTO_CORAZONES_OCULTOS)) ci.cancel();
	}
}
