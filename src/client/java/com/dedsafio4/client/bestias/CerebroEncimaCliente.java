package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CerebroAmarilloEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Cuando tienes un Cerebro Amarillo en la cabeza y estás en primera persona (la cámara queda adentro del bicho y
 * no se lo ve), se ve el cerebro arriba de la pantalla con los tentáculos colgando sobre la cara, latiendo.
 */
public final class CerebroEncimaCliente {
	private CerebroEncimaCliente() {}

	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/misc/cerebro_encima.png");

	public static void registrar() {
		HudRenderCallback.EVENT.register((g, contador) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
			if (mc.player.getPassengers().stream().noneMatch(e -> e instanceof CerebroAmarilloEntity)) return;
			float t = (mc.player.tickCount + contador.getGameTimeDeltaPartialTick(false)) / 20f;
			// Aprieta despacio: la imagen se estira un poquito para abajo y vuelve.
			int estira = Math.round((Mth.sin(t * 2.2f) * 0.5f + 0.5f) * g.guiHeight() * 0.04f);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			g.blit(TEXTURA, 0, 0, g.guiWidth(), g.guiHeight() + estira, 0, 0, 640, 360, 640, 360);
			RenderSystem.disableBlend();
		});
	}
}
