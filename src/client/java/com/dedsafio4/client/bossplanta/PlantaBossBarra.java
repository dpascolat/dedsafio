package com.dedsafio4.client.bossplanta;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bossplanta.PlantaBossEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * La barra del jefe (la imagen que pasó Patricio, sin el fondo), arriba en el medio de la pantalla mientras estás
 * cerca del campo de la Planta Mordedora: el retrato a la izquierda (textures/gui/jefe_retrato.png), la barra de
 * piedra con la cresta rosa y el relleno violeta (las mismas texturas de la barra de los mobs) y el nombre abajo.
 * Lo que se acaba de perder se ve un rato en rosa claro.
 */
public final class PlantaBossBarra {
	private PlantaBossBarra() {}

	private static final ResourceLocation RETRATO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/jefe_retrato.png");
	private static final ResourceLocation MARCO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/barra_vida_marco.png");
	private static final ResourceLocation RELLENO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/barra_vida_relleno.png");
	private static final int RETRATO_W = 149, RETRATO_H = 186, MARCO_W = 160, MARCO_H = 20, RELLENO_W = 154, RELLENO_H = 7;
	/** Tamaños en la pantalla: el retrato a la mitad y la barra al 140% (así quedan en la misma proporción que en la
	 * imagen). La barra empieza BARRA_X y BARRA_Y píxeles (de la pantalla) desde la esquina del retrato, como en la imagen:
	 * la cabeza del bicho queda encima del comienzo de la barra. */
	private static final float ESC_RETRATO = 0.5f, ESC_BARRA = 1.4f;
	private static final int BARRA_X = 60, BARRA_Y = 28;

	/** Lo que se acaba de perder (de 0 a 1) y el tick en que se miró. */
	private static float antes = -1;
	private static int ultimoId = -1;
	private static long ultimoTick;

	public static void dibujar(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.options.hideGui) return;
		PlantaBossEntity jefe = null;
		double mejor = Double.MAX_VALUE;
		for (PlantaBossEntity e : mc.level.getEntitiesOfClass(PlantaBossEntity.class, mc.player.getBoundingBox().inflate(160))) {
			if (!e.isAlive()) continue;
			Vec3 c = e.centro();
			double r = e.radio() + 24, dx = mc.player.getX() - c.x, dz = mc.player.getZ() - c.z;
			if (dx * dx + dz * dz > r * r || Math.abs(mc.player.getY() - c.y) > 48) continue;
			double d = e.distanceToSqr(mc.player);
			if (d < mejor) {
				mejor = d;
				jefe = e;
			}
		}
		if (jefe == null) {
			ultimoId = -1;
			return;
		}
		float vida = Mth.clamp(jefe.getHealth() / jefe.getMaxHealth(), 0, 1);
		long tick = mc.level.getGameTime();
		if (jefe.getId() != ultimoId) antes = vida;
		else antes = Math.max(vida, antes - Math.max(0, tick - ultimoTick) * 0.008f);
		ultimoId = jefe.getId();
		ultimoTick = tick;

		int rw = Math.round(RETRATO_W * ESC_RETRATO), rh = Math.round(RETRATO_H * ESC_RETRATO);
		int bw = Math.round(MARCO_W * ESC_BARRA), bh = Math.round(MARCO_H * ESC_BARRA);
		int total = Math.max(rw, BARRA_X + bw);
		int x0 = (g.guiWidth() - total) / 2, y0 = 2;
		int bx = x0 + BARRA_X, by = y0 + BARRA_Y;

		RenderSystem.enableBlend();
		// La barra: primero el relleno (vacío, lo perdido, el violeta), y encima el marco.
		g.pose().pushPose();
		g.pose().translate(bx, by, 0);
		g.pose().scale(ESC_BARRA, ESC_BARRA, 1);
		int hx = 3, hy = 9;
		g.fill(hx, hy, hx + RELLENO_W, hy + RELLENO_H, 0xFF2A1032);
		int lleno = Math.round(RELLENO_W * vida), perdido = Math.round(RELLENO_W * antes);
		if (perdido > lleno) g.fill(hx + lleno, hy, hx + perdido, hy + RELLENO_H, 0xFFF8B8E0);
		if (lleno > 0) g.blit(RELLENO, hx, hy, 0, 0, lleno, RELLENO_H, RELLENO_W, RELLENO_H);
		g.blit(MARCO, 0, 0, 0, 0, MARCO_W, MARCO_H, MARCO_W, MARCO_H);
		g.pose().popPose();

		// El retrato, encima del comienzo de la barra.
		g.pose().pushPose();
		g.pose().translate(x0, y0, 0);
		g.pose().scale(ESC_RETRATO, ESC_RETRATO, 1);
		g.blit(RETRATO, 0, 0, 0, 0, RETRATO_W, RETRATO_H, RETRATO_W, RETRATO_H);
		g.pose().popPose();

		// El nombre, abajo de la barra, en magenta.
		var nombre = jefe.getDisplayName();
		g.drawString(mc.font, nombre, bx + bw / 2 - mc.font.width(nombre) / 2, by + bh + 3, 0xFFD23BAE, true);
		RenderSystem.disableBlend();
	}
}
