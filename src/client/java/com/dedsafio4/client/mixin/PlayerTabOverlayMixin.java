package com.dedsafio4.client.mixin;

import com.dedsafio4.client.AlmaCliente;
import com.dedsafio4.client.HermandadesCliente;
import com.dedsafio4.hermandad.HermandadesJugadoresPayload;
import com.dedsafio4.items.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * La lista de jugadores (Tab): el nombre con el color de su Hermandad y, a la derecha (antes de la señal), el ícono
 * del Alma o del Sin Alma.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
	/** Espacios que se suman al nombre para dejar lugar al ícono (así la columna sale más ancha). */
	private static final String LUGAR_ICONO = "   ";

	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$nombre(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		Component nombre = cir.getReturnValue();
		HermandadesJugadoresPayload.Entrada hermandad = HermandadesCliente.de(info.getProfile().getId());
		var nuevo = hermandad != null
				? nombre.copy().withStyle(s -> s.withColor(TextColor.fromRgb(hermandad.color())))
				: nombre.copy();
		cir.setReturnValue(nuevo.append(LUGAR_ICONO));
	}

	/** Justo antes de la señal (que va en la punta derecha de la fila): el ícono del alma. */
	@Inject(method = "renderPingIcon", at = @At("HEAD"))
	private void dedsafio4$alma(GuiGraphics g, int ancho, int x, int y, PlayerInfo info, CallbackInfo ci) {
		boolean tiene = !AlmaCliente.SIN_ALMA.contains(info.getProfile().getId());
		ItemStack icono = new ItemStack(tiene ? ModItems.ALMA : ModItems.SIN_ALMA);
		g.pose().pushPose();
		g.pose().translate(x + ancho - 11 - 10, y, 0);
		g.pose().scale(0.5f, 0.5f, 1f);   // el ítem mide 16: queda de 8, como la fila
		g.renderItem(icono, 0, 0);
		g.pose().popPose();
	}
}
