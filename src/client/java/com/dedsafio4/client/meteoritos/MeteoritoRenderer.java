package com.dedsafio4.client.meteoritos;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.meteoritos.MeteoritoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * El meteorito como en el diseño: un casco de vóxeles de radio 7 (1 vóxel = 1 píxel) de roca, con un
 * 20% de grietas de magma que brillan siempre. Gira mientras cae.
 */
public class MeteoritoRenderer extends EntityRenderer<MeteoritoEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/meteorito.png");
	private static final int R = 7;

	/** Una cara visible de un vóxel: posición, hacia dónde mira (0..5) y color de la paleta (0-3 roca, 4-5 magma). */
	private record Cara(int x, int y, int z, int dir, int color) {}

	private static final List<Cara> CARAS = generar();

	public MeteoritoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 0.4f;
	}

	/** El mismo "azar" fijo del diseño: frac(sin(x*12.9898 + y*78.233 + z*37.719) * 43758.5453). */
	private static double hash(double x, double y, double z) {
		double s = Math.sin(x * 12.9898 + y * 78.233 + z * 37.719) * 43758.5453;
		return s - Math.floor(s);
	}

	private static List<Cara> generar() {
		Set<Long> celdas = new HashSet<>();
		List<int[]> vox = new ArrayList<>();
		for (int x = -R; x <= R; x++) for (int y = -R; y <= R; y++) for (int z = -R; z <= R; z++) {
			double d = Math.hypot(Math.hypot(x, y * 1.1), z) + hash(x, y, z) * 0.9;
			if (d > R || d < R - 1.6) continue;
			celdas.add(clave(x, y, z));
			vox.add(new int[]{x, y, z});
		}
		int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		List<Cara> caras = new ArrayList<>();
		for (int[] v : vox) {
			int x = v[0], y = v[1], z = v[2];
			boolean magma = hash(x + 3, y, z) < 0.2;
			int color = magma ? (hash(x, z, y) < 0.5 ? 4 : 5) : (int) Math.floor(hash(x, y, z) * 4) % 4;
			for (int i = 0; i < 6; i++) {
				if (!celdas.contains(clave(x + dirs[i][0], y + dirs[i][1], z + dirs[i][2]))) caras.add(new Cara(x, y, z, i, color));
			}
		}
		return caras;
	}

	private static long clave(int x, int y, int z) {
		return ((long) (x + 64) << 16) | ((long) (y + 64) << 8) | (z + 64);
	}

	@Override
	public void render(MeteoritoEntity meteorito, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		if (meteorito.choco()) return;
		float t = (meteorito.tickCount + parcial) / 20f;
		pose.pushPose();
		pose.mulPose(Axis.XP.rotation(t * 2.6f));
		pose.mulPose(Axis.ZP.rotation(t * 1.4f));
		pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		PoseStack.Pose p = pose.last();
		for (Cara c : CARAS) {
			// La roca con la luz del lugar; el magma brilla siempre.
			cara(vc, p, c, c.color >= 4 ? LightTexture.FULL_BRIGHT : Math.max(luz, LightTexture.pack(8, 8)));
		}
		pose.popPose();
		super.render(meteorito, yaw, parcial, pose, buffers, luz);
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, Cara c, int luz) {
		float x0 = c.x - 0.5f, y0 = c.y - 0.5f, z0 = c.z - 0.5f, x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
		float u = ((c.color % 8) * 2 + 1) / 16f, v = ((c.color / 8) * 2 + 1) / 16f;
		float[] q = switch (c.dir) {
			case 0 -> new float[]{x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1};
			case 1 -> new float[]{x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0};
			case 2 -> new float[]{x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0};
			case 3 -> new float[]{x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1};
			case 4 -> new float[]{x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1};
			default -> new float[]{x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0};
		};
		float nx = c.dir == 0 ? 1 : c.dir == 1 ? -1 : 0, ny = c.dir == 2 ? 1 : c.dir == 3 ? -1 : 0, nz = c.dir == 4 ? 1 : c.dir == 5 ? -1 : 0;
		for (int i = 0; i < 12; i += 3) {
			vc.addVertex(p, q[i], q[i + 1], q[i + 2]).setColor(255, 255, 255, 255).setUv(u, v)
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(MeteoritoEntity meteorito) {
		return TEXTURA;
	}
}
