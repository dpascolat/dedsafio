package com.dedsafio4.client;

import com.dedsafio4.pociones.ModPociones;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;
import java.util.List;

/**
 * Los efectos de pociones en la pantalla: abajo a la derecha (en vez de arriba, donde está el minimapa),
 * con los mismos íconos que Minecraft. Arriba de la fila de los buenos va la de los malos.
 * No se muestran (ni acá ni en el inventario) el Rojizo, el Negro Puro ni los del minimapa.
 */
public final class EfectosPantalla {
	private EfectosPantalla() {}

	private static final ResourceLocation FONDO = ResourceLocation.withDefaultNamespace("hud/effect_background");
	private static final ResourceLocation FONDO_AMBIENTE = ResourceLocation.withDefaultNamespace("hud/effect_background_ambient");

	/** ¿Se muestra este efecto? */
	public static boolean mostrar(MobEffectInstance e) {
		if (!e.showIcon()) return false;
		if (e.getEffect().is(ModPociones.EFECTO_ROJIZO) || e.getEffect().is(ModPociones.EFECTO_NEGRO_PURO)) return false;
		return e.getEffect().unwrapKey().map(k -> !k.location().getNamespace().equals("xaerominimap")).orElse(true);
	}

	public static Collection<MobEffectInstance> visibles(Collection<MobEffectInstance> todos) {
		return todos.stream().filter(EfectosPantalla::mostrar).toList();
	}

	public static void dibujar(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) return;
		List<MobEffectInstance> lista = mc.player.getActiveEffects().stream().filter(EfectosPantalla::mostrar)
				.sorted(java.util.Comparator.reverseOrder()).toList();
		if (lista.isEmpty()) return;
		RenderSystem.enableBlend();
		int buenos = 0, malos = 0;
		for (MobEffectInstance e : lista) {
			boolean bueno = e.getEffect().value().isBeneficial();
			int x = g.guiWidth() - 1 - 25 * (bueno ? ++buenos : ++malos);
			int y = g.guiHeight() - 25 - (bueno ? 0 : 26);
			g.blitSprite(e.isAmbient() ? FONDO_AMBIENTE : FONDO, x, y, 24, 24);
			// Titila cuando le quedan menos de 10 segundos (como en Minecraft).
			float alfa = 1;
			if (!e.isAmbient() && e.endsWithin(200)) {
				int d = e.getDuration(), m = 10 - d / 20;
				alfa = Mth.clamp(d / 10f / 5f * 0.5f, 0, 0.5f) + Mth.cos(d * Mth.PI / 5f) * Mth.clamp(m / 10f * 0.25f, 0, 0.25f);
			}
			g.setColor(1, 1, 1, alfa);
			g.blit(x + 3, y + 3, 0, 18, 18, mc.getMobEffectTextures().get(e.getEffect()));
			g.setColor(1, 1, 1, 1);
		}
		RenderSystem.disableBlend();
	}
}
