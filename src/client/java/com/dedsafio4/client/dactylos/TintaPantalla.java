package com.dedsafio4.client.dactylos;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.dactylos.ModDactylos;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

/** Con el efecto Tinta (la tinta del Dáctylo) la pantalla se oscurece de violeta, con un fundido corto. */
public final class TintaPantalla {
	private TintaPantalla() {}

	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/tinta_dactylo.png");
	/** El fundido de entrada y de salida: 5 ticks (0,25 s). */
	private static final float FUNDIDO = 5f;

	public static void dibujar(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		MobEffectInstance efecto = mc.player.getEffect(ModDactylos.EFECTO_TINTA);
		if (efecto == null) return;
		int queda = efecto.getDuration();
		float desde = com.dedsafio4.dactylos.TintaDactyloEntity.DURACION - queda;
		float alfa = Math.max(0, Math.min(1, Math.min((desde + 1) / FUNDIDO, queda / FUNDIDO)));
		RenderSystem.enableBlend();
		g.setColor(1, 1, 1, alfa);
		g.blit(TEXTURA, 0, 0, g.guiWidth(), g.guiHeight(), 0, 0, 128, 128, 128, 128);
		g.setColor(1, 1, 1, 1);
		RenderSystem.disableBlend();
	}
}
