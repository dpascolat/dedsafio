package com.dedsafio4.client.mixin;

import com.dedsafio4.items.AvisoTotem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Los mensajes que empiezan con la marca de AvisoTotem (un hueco en la fuente "cara") llevan la cara del jugador en
 * ese hueco. El nombre del jugador viene guardado en el carácter de la marca.
 */
@Mixin(ChatComponent.class)
public abstract class ChatCaraMixin {
	@WrapOperation(method = "render", require = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
	private int dedsafio4$cara(GuiGraphics g, Font font, FormattedCharSequence texto, int x, int y, int color, Operation<Integer> original) {
		int r = original.call(g, font, texto, x, y, color);
		String[] nombre = {null};
		texto.accept((i, estilo, c) -> {
			if (c == AvisoTotem.MARCA.charAt(0) && AvisoTotem.FUENTE_CARA.equals(estilo.getFont())) nombre[0] = estilo.getInsertion();
			return false;   // sólo importa el primer carácter
		});
		if (nombre[0] == null) return r;
		float alfa = (color >>> 24) / 255f;
		var conexion = Minecraft.getInstance().getConnection();
		PlayerInfo info = conexion != null ? conexion.getPlayerInfo(nombre[0]) : null;
		g.setColor(1f, 1f, 1f, alfa);
		if (info != null) PlayerFaceRenderer.draw(g, info.getSkin(), x, y, 8);
		else PlayerFaceRenderer.draw(g, DefaultPlayerSkin.getDefaultTexture(), x, y, 8, true, false);
		g.setColor(1f, 1f, 1f, 1f);
		return r;
	}
}
