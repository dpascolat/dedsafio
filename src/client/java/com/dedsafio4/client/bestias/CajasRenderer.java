package com.dedsafio4.client.bestias;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja un bicho hecho de cajas con huesos (nodos), como los diseños en three.js: medidas en píxeles,
 * y para arriba y la cabeza mirando a +X. Cada caja es de un color liso de la textura (una fila de
 * cuadraditos de 4 píxeles: el color n va en x = 4n).
 */
public abstract class CajasRenderer<T extends LivingEntity> extends EntityRenderer<T> {
	protected static final class Nodo {
		public float x, y, z, rx, ry, rz;
		final List<float[]> cajas = new ArrayList<>();
		final List<Nodo> hijos = new ArrayList<>();

		Nodo(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	protected final Nodo raiz = new Nodo(0, 0, 0);
	private final int colores;

	protected CajasRenderer(EntityRendererProvider.Context contexto, float sombra, int colores) {
		super(contexto);
		this.shadowRadius = sombra;
		this.colores = colores;
	}

	protected Nodo nodo(Nodo padre, float x, float y, float z) {
		Nodo n = new Nodo(x, y, z);
		padre.hijos.add(n);
		return n;
	}

	/** Una caja de ancho (x) × alto (y) × profundidad (z), centrada en (x, y, z) del nodo. */
	protected void caja(Nodo n, int color, float w, float h, float d, float x, float y, float z) {
		n.cajas.add(new float[]{x - w / 2, y - h / 2, z - d / 2, x + w / 2, y + h / 2, z + d / 2, color});
	}

	/** Acomoda los huesos para este cuadro. */
	protected abstract void animar(T bicho, float parcial);

	/** Cuánto hay que girar el diseño para que mire hacia adelante: -90 si la cabeza mira a +X, 0 si mira a +Z. */
	protected float giroDiseno() {
		return -90;
	}

	/** Cuánto titila en blanco (como el creeper a punto de explotar), de 0 a 1. */
	protected float blanco(T bicho, float parcial) {
		return 0;
	}

	/** Los colores que brillan en la oscuridad (se dibujan a plena luz). */
	protected boolean brilla(int color) {
		return false;
	}

		/** Agrandar todo el bicho (1 = tamaño normal). */
	protected float escala(T bicho, float parcial) {
		return 1;
	}

	@Override
	public void render(T bicho, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		animar(bicho, parcial);
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, bicho.yBodyRotO, bicho.yBodyRot)));
		if (bicho.deathTime > 0) {
			// Al morir se cae de costado, como los mobs de Minecraft.
			float caida = Math.min(1f, Mth.sqrt((bicho.deathTime + parcial - 1) / 20f * 1.6f));
			pose.mulPose(Axis.ZP.rotationDegrees(caida * 90f));
		}
		pose.mulPose(Axis.YP.rotationDegrees(giroDiseno()));   // del diseño al de Minecraft (+Z adelante)
		float escala = escala(bicho, parcial) / 16f;
		pose.scale(escala, escala, escala);
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(bicho)));
		int overlay = LivingEntityRenderer.getOverlayCoords(bicho, blanco(bicho, parcial));
		dibujar(pose, vc, raiz, luz, overlay);
		pose.popPose();
		super.render(bicho, yaw, parcial, pose, buffers, luz);
	}

	private void dibujar(PoseStack pose, VertexConsumer vc, Nodo n, int luz, int overlay) {
		pose.pushPose();
		pose.translate(n.x, n.y, n.z);
		if (n.rx != 0) pose.mulPose(Axis.XP.rotation(n.rx));
		if (n.ry != 0) pose.mulPose(Axis.YP.rotation(n.ry));
		if (n.rz != 0) pose.mulPose(Axis.ZP.rotation(n.rz));
		PoseStack.Pose p = pose.last();
		for (float[] c : n.cajas) caja(vc, p, c, brilla((int) c[6]) ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT : luz, overlay);
		for (Nodo hijo : n.hijos) dibujar(pose, vc, hijo, luz, overlay);
		pose.popPose();
	}

	private void caja(VertexConsumer vc, PoseStack.Pose p, float[] c, int luz, int overlay) {
		float x0 = c[0], y0 = c[1], z0 = c[2], x1 = c[3], y1 = c[4], z1 = c[5];
		float u = (c[6] * 4 + 2) / (colores * 4f), v = 0.5f;
		cara(vc, p, u, v, luz, overlay, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
		cara(vc, p, u, v, luz, overlay, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
		cara(vc, p, u, v, luz, overlay, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		cara(vc, p, u, v, luz, overlay, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
		cara(vc, p, u, v, luz, overlay, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		cara(vc, p, u, v, luz, overlay, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, float u, float v, int luz, int overlay,
							 float nx, float ny, float nz, float... q) {
		for (int i = 0; i < 12; i += 3) {
			vc.addVertex(p, q[i], q[i + 1], q[i + 2]).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay)
					.setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	/** easeInOutCubic. */
	protected static float suave(float t) {
		return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	@Override
	public abstract ResourceLocation getTextureLocation(T bicho);
}
