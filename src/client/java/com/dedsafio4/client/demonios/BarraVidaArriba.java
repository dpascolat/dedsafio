package com.dedsafio4.client.demonios;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * La barra de vida arriba del bicho (la usan los Colosos Prisioneros y el Farol), con el dibujo que pasó Patricio:
 * marco de piedra oscura con una cresta rosa arriba (textures/gui/barra_vida_marco.png) y relleno violeta con
 * medias lunas claras (barra_vida_relleno.png). Siempre mira a la cámara y brilla en la oscuridad. Arriba, la vida
 * en números ("120 / 150"). Lo que se acaba de perder se ve un rato más claro y baja despacio.
 */
public final class BarraVidaArriba {
	private BarraVidaArriba() {}

	private static final ResourceLocation MARCO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/barra_vida_marco.png");
	private static final ResourceLocation RELLENO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/barra_vida_relleno.png");
	/** Medidas de la textura del marco (en píxeles) y dónde va el relleno adentro. */
	private static final float TEX_ANCHO = 160, TEX_ALTO = 20, HUECO_X0 = 3, HUECO_X1 = 157, HUECO_Y0 = 9, HUECO_Y1 = 16;

	/** La vida "de antes" de cada bicho (lo que se acaba de perder) y el tick en que se miró. */
	private static final java.util.Map<LivingEntity, float[]> ANTES = new java.util.WeakHashMap<>();

	/** No se dibuja más lejos que esto (en bloques). */
	private static final double LEJOS = 48;

	public static void dibujar(LivingEntity mob, EntityRenderDispatcher camara, PoseStack pose, MultiBufferSource buffers, int luz) {
		if (mob.isInvisible() || mob.isDeadOrDying() || camara.distanceToSqr(mob) > LEJOS * LEJOS) return;
		float vida = Mth.clamp(mob.getHealth() / mob.getMaxHealth(), 0, 1);
		float[] guardado = ANTES.computeIfAbsent(mob, e -> new float[]{vida, mob.tickCount});
		float pasaron = mob.tickCount - guardado[1];
		guardado[1] = mob.tickCount;
		guardado[0] = Math.max(vida, guardado[0] - Math.max(0, pasaron) * 0.015f);
		float antes = guardado[0];

		float ancho = Mth.clamp(mob.getBbWidth() * 1.1f, 1.6f, 3.4f);
		float px = ancho / TEX_ANCHO, alto = TEX_ALTO * px;

		pose.pushPose();
		pose.translate(0, mob.getBbHeight() * 1.12f + 0.4f, 0);   // un poco más arriba de la cabeza
		pose.mulPose(camara.cameraOrientation());
		Matrix4f m = pose.last().pose();
		float x0 = -ancho / 2;
		// El hueco del relleno, en el mundo (y crece para arriba; las filas de la textura, para abajo).
		float hx0 = x0 + HUECO_X0 * px, hx1 = x0 + HUECO_X1 * px, hy0 = alto - HUECO_Y1 * px, hy1 = alto - HUECO_Y0 * px;
		float hAncho = hx1 - hx0;

		VertexConsumer relleno = buffers.getBuffer(RenderType.entityCutoutNoCull(RELLENO));
		cuadro(relleno, m, hx0, hy0, hx1, hy1, 0.001f, 0, 1, 0xFF2A1032);                                   // vacío (oscuro)
		if (vida > 0) cuadro(relleno, m, hx0, hy0, hx0 + hAncho * vida, hy1, 0.003f, 0, vida, 0xFFFFFFFF);  // el violeta
		if (antes > vida) {   // lo que se acaba de perder: rosa claro
			VertexConsumer c = buffers.getBuffer(RenderType.debugQuads());
			float a0 = hx0 + hAncho * vida, a1 = hx0 + hAncho * antes;
			c.addVertex(m, a0, hy0, 0.002f).setColor(248, 184, 224, 255);
			c.addVertex(m, a1, hy0, 0.002f).setColor(248, 184, 224, 255);
			c.addVertex(m, a1, hy1, 0.002f).setColor(248, 184, 224, 255);
			c.addVertex(m, a0, hy1, 0.002f).setColor(248, 184, 224, 255);
		}
		cuadro(buffers.getBuffer(RenderType.entityCutoutNoCull(MARCO)), m, x0, 0, x0 + ancho, alto, 0.004f, 0, 1, 0xFFFFFFFF);

		// La vida en números, arriba de la cresta.
		Font letra = Minecraft.getInstance().font;
		String texto = (int) Math.ceil(mob.getHealth()) + " / " + (int) Math.ceil(mob.getMaxHealth());
		pose.translate(0, alto + 0.04f, 0.006f);
		pose.scale(0.027f, -0.027f, 0.027f);   // como los nombres de Minecraft
		float w = letra.width(texto);
		letra.drawInBatch(texto, -w / 2, -9, 0xFFF8C8E8, true, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		pose.popPose();
	}

	/** Un rectángulo con textura, de u0 a u1 (de 0 a 1, toda la altura de la textura), a luz plena. */
	private static void cuadro(VertexConsumer v, Matrix4f m, float x0, float y0, float x1, float y1, float z, float u0, float u1, int argb) {
		int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
		punto(v, m, x0, y0, z, u0, 1, r, g, b, a);
		punto(v, m, x1, y0, z, u1, 1, r, g, b, a);
		punto(v, m, x1, y1, z, u1, 0, r, g, b, a);
		punto(v, m, x0, y1, z, u0, 0, r, g, b, a);
	}

	private static void punto(VertexConsumer v, Matrix4f m, float x, float y, float z, float u, float tv, int r, int g, int b, int a) {
		v.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, tv).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
	}
}
