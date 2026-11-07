package com.dedsafio4.client.demonios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * Una barra de vida arriba del bicho (la usan los Colosos Prisioneros y el Farol): siempre mira a la cámara, roja
 * sobre fondo oscuro, con la vida en números arriba ("120 / 150"). La parte que se acaba de perder se ve un rato
 * en amarillo y baja despacio.
 */
public final class BarraVidaArriba {
	private BarraVidaArriba() {}

	/** La vida "de antes" de cada bicho (lo amarillo) y el tick en que se miró. */
	private static final java.util.Map<LivingEntity, float[]> ANTES = new java.util.WeakHashMap<>();

	/** No se dibuja más lejos que esto (en bloques). */
	private static final double LEJOS = 48;

	public static void dibujar(LivingEntity mob, EntityRenderDispatcher camara, PoseStack pose, MultiBufferSource buffers, int luz) {
		if (mob.isInvisible() || mob.isDeadOrDying() || camara.distanceToSqr(mob) > LEJOS * LEJOS) return;
		float vida = Mth.clamp(mob.getHealth() / mob.getMaxHealth(), 0, 1);
		// Lo amarillo: la vida de hace un ratito, que va bajando hasta la de ahora.
		float[] guardado = ANTES.computeIfAbsent(mob, e -> new float[]{vida, mob.tickCount});
		float pasaron = mob.tickCount - guardado[1];
		guardado[1] = mob.tickCount;
		guardado[0] = Math.max(vida, guardado[0] - Math.max(0, pasaron) * 0.015f);
		float antes = guardado[0];
		float ancho = Mth.clamp(mob.getBbWidth() * 0.9f, 1.1f, 2.8f), alto = 0.18f;

		pose.pushPose();
		pose.translate(0, mob.getBbHeight() + 0.45f, 0);
		pose.mulPose(camara.cameraOrientation());
		Matrix4f m = pose.last().pose();
		VertexConsumer v = buffers.getBuffer(RenderType.debugQuads());
		float x0 = -ancho / 2, x1 = ancho / 2;
		cuadro(v, m, x0 - 0.03f, -0.03f, x1 + 0.03f, alto + 0.03f, 0.001f, 0xE0000000);   // borde
		cuadro(v, m, x0, 0, x1, alto, 0.002f, 0xFF3A1016);                                  // fondo
		if (antes > vida) cuadro(v, m, x0 + ancho * vida, 0, x0 + ancho * antes, alto, 0.003f, 0xFFF2C744);
		if (vida > 0) {
			cuadro(v, m, x0, 0, x0 + ancho * vida, alto, 0.004f, 0xFFD8263A);
			cuadro(v, m, x0, alto * 0.55f, x0 + ancho * vida, alto, 0.005f, 0xFFF0566A);    // brillo de arriba
		}

		// La vida en números, arriba de la barra.
		Font letra = Minecraft.getInstance().font;
		String texto = (int) Math.ceil(mob.getHealth()) + " / " + (int) Math.ceil(mob.getMaxHealth());
		pose.translate(0, alto + 0.05f, 0.006f);
		pose.scale(0.027f, -0.027f, 0.027f);   // como los nombres de Minecraft
		float w = letra.width(texto);
		letra.drawInBatch(texto, -w / 2, -9, 0xFFFFFFFF, true, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, luz);
		pose.popPose();
	}

	/** Un rectángulo plano (mirando a la cámara). z lo separa un poquito para que no se pisen las capas. */
	private static void cuadro(VertexConsumer v, Matrix4f m, float x0, float y0, float x1, float y1, float z, int argb) {
		int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
		v.addVertex(m, x0, y0, z).setColor(r, g, b, a);
		v.addVertex(m, x1, y0, z).setColor(r, g, b, a);
		v.addVertex(m, x1, y1, z).setColor(r, g, b, a);
		v.addVertex(m, x0, y1, z).setColor(r, g, b, a);
	}
}
