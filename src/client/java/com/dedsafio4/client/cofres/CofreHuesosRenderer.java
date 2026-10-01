package com.dedsafio4.client.cofres;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.cofres.CofreBlock;
import com.dedsafio4.cofres.CofreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * El Cofre de Huesos del diseño (cofre doble 2×1): base de piedra, tapa de 6 costillas de hueso en arco
 * con la espina arriba, tapas laterales con cuernos, cerradura de oro y el tesoro morado y dorado que se
 * sale por abajo. Al abrirlo, la jaula de costillas se levanta hacia atrás.
 */
public class CofreHuesosRenderer implements BlockEntityRenderer<CofreBlockEntity> {
	private static final int HUESO = 0, PIEDRA = 1, INTERIOR = 2, ORO = 3, MORADO = 4;
	private static final String[] NOMBRES = {"hueso", "piedra", "interior", "oro", "morado"};
	private static final RenderType[] TIPOS = new RenderType[NOMBRES.length];

	static {
		for (int i = 0; i < NOMBRES.length; i++) {
			TIPOS[i] = RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID,
					"textures/entity/cofre_huesos/" + NOMBRES[i] + ".png"));
		}
	}

	/** grupo: 0 base, 1 tapa (costillas y espina, gira en la bisagra de atrás), 2-5 los cuernos. */
	private record Pieza(float w, float h, float d, int mat, float x, float y, float z, float rx, float ry, float rz, int grupo) {}

	private static final List<Pieza> PIEZAS = new ArrayList<>();
	/** Los cuernos: dónde está cada grupo y hacia qué lado (sx, sz). */
	private static final int[][] CUERNOS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

	private static void pieza(float w, float h, float d, int mat, float x, float y, float z, float rx, float ry, float rz, int grupo) {
		PIEZAS.add(new Pieza(w, h, d, mat, x, y, z, rx, ry, rz, grupo));
	}

	static {
		pieza(1.9f, 0.36f, 0.92f, PIEDRA, 0, 0.18f, 0, 0, 0, 0, 0);
		pieza(1.96f, 0.06f, 0.98f, HUESO, 0, 0.03f, 0, 0, 0, 0, 0);
		pieza(1.72f, 0.5f, 0.78f, INTERIOR, 0, 0.55f, 0, 0, 0, 0, 0);
		// El tesoro que se sale por el frente (mismo generador que el diseño).
		int[] s = {5};
		java.util.function.DoubleSupplier rnd = () -> (s[0] = (s[0] * 9301 + 49297) % 233280) / 233280.0;
		pieza(1.7f, 0.1f, 0.03f, MORADO, 0, 0.14f, 0.47f, 0, 0, 0, 0);
		for (int i = 0; i < 26; i++) {
			float sz = (float) (0.05 + rnd.getAsDouble() * 0.07);
			boolean gema = i % 3 != 0;
			float x = (float) ((rnd.getAsDouble() - 0.5) * 1.8), y = (float) (0.08 + rnd.getAsDouble() * 0.16), z = (float) (0.47 + rnd.getAsDouble() * 0.06);
			float rx = (float) rnd.getAsDouble(), ry = (float) (rnd.getAsDouble() * 3), rz = (float) rnd.getAsDouble();
			pieza(sz, gema ? sz : sz * 0.4f, sz, gema ? MORADO : ORO, x, y, z, rx, ry, rz, 0);
		}
		// La tapa: 6 costillas en arco a lo ancho, con sus pies, y la espina arriba.
		float[] xs = {-0.75f, -0.45f, -0.15f, 0.15f, 0.45f, 0.75f};
		for (float x : xs) {
			for (int i = 0; i < 6; i++) {
				float a = (i + 0.5f) * Mth.PI / 6, z = Mth.cos(a) * 0.44f, y = 0.38f + Mth.sin(a) * 0.46f;
				pieza(0.15f, 0.1f, 0.26f, i % 2 == 1 ? HUESO : PIEDRA, x, y, z, a + Mth.PI / 2, 0, 0, 1);
			}
			pieza(0.17f, 0.1f, 0.1f, HUESO, x, 0.38f, 0.45f, 0, 0, 0, 0);
			pieza(0.17f, 0.1f, 0.1f, HUESO, x, 0.38f, -0.45f, 0, 0, 0, 0);
		}
		pieza(1.84f, 0.1f, 0.12f, HUESO, 0, 0.86f, 0, 0, 0, 0, 1);
		// Tapas de los costados y cuernos en las 4 esquinas.
		for (int sx : new int[]{-1, 1}) pieza(0.1f, 0.5f, 0.92f, HUESO, sx * 0.93f, 0.6f, 0, 0, 0, 0, 0);
		for (int k = 0; k < CUERNOS.length; k++) {
			int sx = CUERNOS[k][0], szz = CUERNOS[k][1];
			pieza(0.12f, 0.28f, 0.12f, HUESO, 0, 0.14f, 0, 0, 0, 0, 2 + k);
			pieza(0.1f, 0.1f, 0.22f, PIEDRA, sx * 0.02f, 0.3f, -szz * 0.06f, szz * 0.5f, 0, 0, 2 + k);
		}
		// La cerradura de oro.
		pieza(0.3f, 0.3f, 0.05f, HUESO, 0, 0.4f, 0.5f, 0, 0, 0, 0);
		pieza(0.22f, 0.22f, 0.04f, ORO, 0, 0.4f, 0.525f, 0, 0, 0, 0);
		pieza(0.08f, 0.1f, 0.02f, INTERIOR, 0, 0.4f, 0.55f, 0, 0, 0, 0);
	}

	public CofreHuesosRenderer(BlockEntityRendererProvider.Context contexto) {}

	@Override
	public void render(CofreBlockEntity cofre, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		BlockState estado = cofre.getBlockState();
		if (!(estado.getBlock() instanceof CofreBlock)) return;
		dibujar(pose, buffers, luz, overlay, estado.getValue(CofreBlock.FACING), cofre.getOpenNess(parcial), true);
	}

	/** También lo usa el ítem (cerrado). El modelo mide 2 de largo (X) con el frente hacia +Z. */
	public static void dibujar(PoseStack pose, MultiBufferSource buffers, int luz, int overlay, Direction frente, float abierto, boolean enElMundo) {
		pose.pushPose();
		if (enElMundo) {
			Direction derecha = frente.getClockWise();
			pose.translate(0.5 + derecha.getStepX() * 0.5, 0, 0.5 + derecha.getStepZ() * 0.5);
			pose.mulPose(Axis.YP.rotationDegrees(-frente.toYRot()));
		}
		float tapa = -1.3f * (1f - (1f - abierto) * (1f - abierto) * (1f - abierto));   // se levanta hacia atrás
		// Por material, así cada textura va en una sola tanda.
		for (int mat = 0; mat < NOMBRES.length; mat++) {
			VertexConsumer vc = buffers.getBuffer(TIPOS[mat]);
			int l = mat == ORO || mat == MORADO ? LightTexture.FULL_BRIGHT : luz;   // el oro y las gemas brillan
			for (Pieza p : PIEZAS) {
				if (p.mat() != mat) continue;
				pose.pushPose();
				if (p.grupo() == 1 && tapa != 0) {
					pose.translate(0, 0.38f, -0.44f);
					pose.mulPose(Axis.XP.rotation(tapa));
					pose.translate(0, -0.38f, 0.44f);
				} else if (p.grupo() >= 2) {
					int[] c = CUERNOS[p.grupo() - 2];
					pose.translate(c[0] * 0.93f, 0.82f, c[1] * 0.36f);
					pose.mulPose(Axis.ZP.rotation(-c[0] * 0.55f));
				}
				pose.translate(p.x(), p.y(), p.z());
				if (p.rx() != 0) pose.mulPose(Axis.XP.rotation(p.rx()));
				if (p.ry() != 0) pose.mulPose(Axis.YP.rotation(p.ry()));
				if (p.rz() != 0) pose.mulPose(Axis.ZP.rotation(p.rz()));
				caja(vc, pose.last(), p.w(), p.h(), p.d(), l, overlay);
				pose.popPose();
			}
		}
		pose.popPose();
	}

	private static void caja(VertexConsumer vc, PoseStack.Pose p, float w, float h, float d, int luz, int overlay) {
		float x0 = -w / 2, x1 = w / 2, y0 = -h / 2, y1 = h / 2, z0 = -d / 2, z1 = d / 2;
		cara(vc, p, luz, overlay, 1, 0, 0, d, h, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
		cara(vc, p, luz, overlay, -1, 0, 0, d, h, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		cara(vc, p, luz, overlay, 0, 1, 0, w, d, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
		cara(vc, p, luz, overlay, 0, -1, 0, w, d, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		cara(vc, p, luz, overlay, 0, 0, 1, w, h, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		cara(vc, p, luz, overlay, 0, 0, -1, w, h, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
	}

	/** Una cara con la textura a escala de bloque (1 textura = 1 bloque, como el diseño). */
	private static void cara(VertexConsumer vc, PoseStack.Pose p, int luz, int overlay, float nx, float ny, float nz, float ancho, float alto, float... q) {
		float[] us = {0, ancho, ancho, 0}, vs = {alto, alto, 0, 0};
		for (int i = 0; i < 4; i++) {
			vc.addVertex(p, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]).setColor(255, 255, 255, 255).setUv(us[i], vs[i]).setOverlay(overlay)
					.setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	@Override
	public boolean shouldRenderOffScreen(CofreBlockEntity cofre) {
		return true;
	}
}
