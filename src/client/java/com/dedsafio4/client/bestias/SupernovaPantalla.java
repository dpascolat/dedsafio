package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.NucleoEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * La supernova del Núcleo vista desde adentro (como en el video de Patricio): mientras un Núcleo explota y el jugador
 * está dentro del radio, la pantalla se llena de una nube de colores (rosa, magenta, verde agua, azul) estirada en
 * rayas desde el centro, que gira y late, y dos haces de luz blanco-celeste que pasan por el medio y giran como un
 * faro. Al empezar hay un fogonazo blanco, que también ven (más suave) los que están cerca pero afuera: la "nuke".
 */
public final class SupernovaPantalla {
	private SupernovaPantalla() {}

	private static final ResourceLocation NUBE = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/misc/supernova_nube.png");
	private static final ResourceLocation HAZ = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/misc/supernova_haz.png");

	/** Cuánto se ve ahora (de 0 a 1), para que entre y salga suave. */
	private static float adentro, fogonazo;

	public static void registrar() {
		HudRenderCallback.EVENT.register((g, contador) -> dibujar(g, contador.getGameTimeDeltaPartialTick(false)));
	}

	private static void dibujar(GuiGraphics g, float parcial) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			adentro = fogonazo = 0;
			return;
		}
		// El Núcleo que está explotando más cerca: ¿estoy adentro de su radio? ¿cuánto hace que explotó?
		float quiero = 0, flash = 0;
		for (var e : mc.level.entitiesForRendering()) {
			if (!(e instanceof NucleoEntity n) || n.estado() != NucleoEntity.SUPERNOVA) continue;
			double d = mc.player.position().add(0, mc.player.getBbHeight() / 2, 0).distanceTo(n.position().add(0, NucleoEntity.ALTO / 2 * n.getScale(), 0));
			float t = n.tiempo() + parcial;
			if (d <= n.radio()) quiero = 1;
			// El fogonazo: fuerte al principio, y lo ven también los que están cerca (hasta 3 veces el radio).
			if (d <= n.radio() * 3) flash = Math.max(flash, Mth.clamp(1 - t / 30f, 0, 1) * (d <= n.radio() ? 1f : 0.6f));
		}
		adentro += (quiero - adentro) * (quiero > adentro ? 0.25f : 0.05f);
		fogonazo = Math.max(flash, fogonazo * 0.9f);
		if (adentro < 0.01f && fogonazo < 0.01f) return;

		int ancho = g.guiWidth(), alto = g.guiHeight();
		float cx = ancho / 2f, cy = alto / 2f, lado = (float) Math.hypot(ancho, alto);
		float tiempo = (mc.level.getGameTime() + parcial) / 20f;

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		if (adentro > 0.01f) {
			// La nube: dos capas que giran para lados distintos y laten (como si pasaran volando).
			RenderSystem.defaultBlendFunc();
			nube(g, cx, cy, lado * (1.15f + 0.08f * Mth.sin(tiempo * 2.1f)), tiempo * 12f, 0.8f * adentro);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			nube(g, cx, cy, lado * (1.4f + 0.12f * Mth.sin(tiempo * 3.3f + 1f)), -tiempo * 20f + 40f, 0.22f * adentro);
			// Los haces de luz: dos pares que giran a distinta velocidad, como un faro (un púlsar).
			haz(g, cx, cy, lado, tiempo * 95f + 25f * Mth.sin(tiempo * 1.7f), 46f, 0.95f * adentro);
			haz(g, cx, cy, lado, -tiempo * 60f + 70f, 30f, 0.45f * adentro);
		}
		if (fogonazo > 0.01f) {
			RenderSystem.defaultBlendFunc();
			g.fill(0, 0, ancho, alto, ((int) (fogonazo * 235) << 24) | 0xFFFFFF);
		}
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		g.setColor(1, 1, 1, 1);
	}

	private static void nube(GuiGraphics g, float cx, float cy, float lado, float grados, float alfa) {
		var p = g.pose();
		p.pushPose();
		p.translate(cx, cy, 0);
		p.mulPose(Axis.ZP.rotationDegrees(grados));
		g.setColor(1, 1, 1, alfa);
		int l = (int) lado;
		g.blit(NUBE, -l / 2, -l / 2, l, l, 0, 0, 512, 512, 512, 512);
		g.setColor(1, 1, 1, 1);
		p.popPose();
	}

	/** Un haz que pasa por el medio de la pantalla, girado. */
	private static void haz(GuiGraphics g, float cx, float cy, float largo, float grados, float ancho, float alfa) {
		var p = g.pose();
		p.pushPose();
		p.translate(cx, cy, 0);
		p.mulPose(Axis.ZP.rotationDegrees(grados));
		g.setColor(1, 1, 1, alfa);
		int a = (int) ancho, l = (int) largo;
		g.blit(HAZ, -a / 2, -l / 2, a, l, 0, 0, 32, 256, 32, 256);
		g.setColor(1, 1, 1, 1);
		p.popPose();
	}
}
