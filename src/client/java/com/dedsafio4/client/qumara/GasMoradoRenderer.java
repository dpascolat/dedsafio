package com.dedsafio4.client.qumara;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.qumara.GasMoradoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * El Gas Morado como en el diseño: rejilla de 8×4×4 cubitos de ¼ de bloque (más densos en el medio y
 * abajo), translúcidos y con brillo, que respiran; y 14 chispitas que suben y se desvanecen.
 */
public class GasMoradoRenderer extends EntityRenderer<GasMoradoEntity> {
	private static final ResourceLocation BLANCO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/blanco.png");
	/** Materiales: claro, gas, oscuro, brillo (color y opacidad). */
	private static final int[] COLOR = {0xB07CFF, 0x8A4AD8, 0x5A2AA6, 0xD8B8FF};
	private static final float[] OPACIDAD = {0.35f, 0.45f, 0.5f, 0.3f};

	private record Cubito(float x, float y, float z, float fase, float escala, int mat) {}
	private record Chispa(float x, float z, float desfase, float velocidad) {}

	private static final List<Cubito> CUBITOS = new ArrayList<>();
	private static final List<Chispa> CHISPAS = new ArrayList<>();

	static {
		// El mismo generador y semilla que el diseño, para que salga la misma nube.
		int[] semilla = {9};
		java.util.function.DoubleSupplier rnd = () -> (semilla[0] = (semilla[0] * 9301 + 49297) % 233280) / 233280.0;
		for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
			float cx = (x + 0.5f) * 0.25f - 1, cy = (y + 0.5f) * 0.25f, cz = (z + 0.5f) * 0.25f - 0.5f;
			float nx = cx, ny = (cy - 0.35f) / 0.65f, nz = cz / 0.5f;
			float dens = (float) (1 - (nx * nx * 0.8f + ny * ny + nz * nz * 0.9f) + (rnd.getAsDouble() - 0.5) * 0.5);
			if (dens < 0.05f) continue;
			int mat = dens > 0.6f ? 2 : dens > 0.35f ? 1 : rnd.getAsDouble() > 0.7 ? 3 : 0;
			CUBITOS.add(new Cubito(cx, cy, cz, (float) (rnd.getAsDouble() * Math.PI * 2), 0.7f + dens * 0.5f, mat));
		}
		for (int i = 0; i < 14; i++) {
			CHISPAS.add(new Chispa((float) ((rnd.getAsDouble() - 0.5) * 2 * 0.9), (float) ((rnd.getAsDouble() - 0.5) * 0.9),
					(float) rnd.getAsDouble(), (float) (0.25 + rnd.getAsDouble() * 0.3)));
		}
	}

	public GasMoradoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public void render(GasMoradoEntity gas, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		float s = gas.escala(parcial);
		if (s < 0.01f) return;
		float t = (gas.tickCount + parcial) / 20f + gas.getId() * 0.37f;
		// Pegado a la cámara se hace transparente (si no, al agarrado no le deja ver nada).
		double cerca = Math.sqrt(entityRenderDispatcher.distanceToSqr(gas)) - s;
		atenuar = (float) Mth.clamp((cerca - 0.5) / 3, 0.15, 1);
		VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(BLANCO));
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-gas.getYRot()));
		pose.scale(s, s, s);
		for (Cubito c : CUBITOS) {
			float w = Mth.sin(t * 1.4f + c.fase());
			pose.pushPose();
			pose.translate(c.x() + Mth.sin(t * 0.9f + c.fase()) * 0.04f, c.y() + w * 0.035f, c.z() + Mth.cos(t * 1.1f + c.fase()) * 0.04f);
			pose.mulPose(Axis.YP.rotation(Mth.sin(t * 0.5f + c.fase()) * 0.3f));
			cubo(vc, pose.last(), 0.25f * c.escala() * (0.9f + 0.15f * w), COLOR[c.mat()], OPACIDAD[c.mat()]);
			pose.popPose();
		}
		for (Chispa ch : CHISPAS) {
			float k = (t * ch.velocidad() + ch.desfase()) % 1;
			float e = Math.max(0.01f, Mth.sin(k * Mth.PI));
			pose.pushPose();
			pose.translate(ch.x() + Mth.sin(t * 2 + ch.desfase() * 9) * 0.05f, 0.1f + k * 0.85f, ch.z());
			cubo(vc, pose.last(), 0.1f * e, COLOR[3], OPACIDAD[3] + 0.3f);
			pose.popPose();
		}
		pose.popPose();
		super.render(gas, yaw, parcial, pose, buffers, luz);
	}

	private static float atenuar = 1;

	/** Un cubito centrado, del color y la opacidad del material (brilla: no le importa la luz). */
	private static void cubo(VertexConsumer vc, PoseStack.Pose p, float lado, int color, float opacidad) {
		float h = lado / 2;
		int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (int) (255 * Math.min(1, opacidad) * atenuar);
		float[][] caras = {
				{h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1, 0, 0},
				{-h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1, 0, 0},
				{-h, h, h, h, h, h, h, h, -h, -h, h, -h, 0, 1, 0},
				{-h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0, -1, 0},
				{-h, -h, h, h, -h, h, h, h, h, -h, h, h, 0, 0, 1},
				{h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0, 0, -1}};
		for (float[] c : caras) {
			// Un poco más oscuro abajo y a los costados (como la luz del diseño).
			float f = c[13] > 0 ? 1f : c[13] < 0 ? 0.7f : 0.85f;
			for (int i = 0; i < 4; i++) {
				vc.addVertex(p, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor((int) (r * f), (int) (g * f), (int) (b * f), a)
						.setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, c[12], c[13], c[14]);
			}
		}
	}

	@Override
	public boolean shouldRender(GasMoradoEntity gas, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(gas.getBoundingBox().inflate(4));
	}

	@Override
	public ResourceLocation getTextureLocation(GasMoradoEntity gas) {
		return BLANCO;
	}
}
