package com.dedsafio4.client.puertas;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.puertas.ModPuertas;
import com.dedsafio4.puertas.PuertaBlock;
import com.dedsafio4.puertas.PuertaBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dibuja la puerta de 3x3 entera desde su bloque principal: el marco fijo y el panel partido al medio.
 * Al abrirse, la mitad de arriba sube y la de abajo baja, escondiéndose en el marco (1,2 s, con
 * aceleración suave). Las luces van en una capa aparte a pleno brillo.
 *
 * Medidas en bloques, como en el diseño: x de -1,5 a 1,5, y de 0 a 3, z = grosor (+z es el frente).
 */
public class PuertaRenderer implements BlockEntityRenderer<PuertaBlockEntity> {
	private static final float W = 3, H = 3, S = H / 2, MARCO = 0.12f, RECORRIDO = S - MARCO;
	private static final float Z_MARCO = 0.11f, Z_PANEL = 0.08f, Z_LUZ = 0.083f;
	private static final float TEX = 128f, PX = 32f;

	private static final ResourceLocation ROSA = textura("puerta_rosa"), ROSA_LUCES = textura("puerta_rosa_luces");
	private static final ResourceLocation VERDE = textura("puerta_verde"), VERDE_LUCES = textura("puerta_verde_luces");

	private static ResourceLocation textura(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/" + nombre + ".png");
	}

	public PuertaRenderer(BlockEntityRendererProvider.Context contexto) {}

	/** easeInOutCubic, como en el diseño. */
	private static float suave(float t) {
		return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	@Override
	public void render(PuertaBlockEntity puerta, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		BlockState estado = puerta.getBlockState();
		boolean verde = estado.is(ModPuertas.PUERTA_VERDE);
		float o = suave(puerta.apertura(parcial)) * RECORRIDO;

		pose.pushPose();
		pose.translate(0.5, 0, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-estado.getValue(PuertaBlock.FACING).toYRot()));
		PoseStack.Pose p = pose.last();

		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(verde ? VERDE : ROSA));
		// Marco fijo (color liso).
		caja(vc, p, -W / 2, H - MARCO, W / 2, H, luz, overlay);
		caja(vc, p, -W / 2, 0, W / 2, MARCO, luz, overlay);
		caja(vc, p, -W / 2, MARCO, -W / 2 + MARCO, H - MARCO, luz, overlay);
		caja(vc, p, W / 2 - MARCO, MARCO, W / 2, H - MARCO, luz, overlay);

		// Las dos mitades: se ve solo la parte que queda fuera del marco.
		float x0 = -W / 2 + MARCO, x1 = W / 2 - MARCO;
		float arribaY0 = S + o, arribaY1 = H - MARCO;           // en el mundo
		float abajoY0 = MARCO, abajoY1 = S - o;
		if (arribaY1 - arribaY0 > 0.001f) {
			cara(vc, p, x0, arribaY0, x1, arribaY1, S, H - MARCO - o, Z_PANEL, luz, overlay);
			borde(vc, p, x0, x1, arribaY0, false, luz, overlay);
		}
		if (abajoY1 - abajoY0 > 0.001f) {
			cara(vc, p, x0, abajoY0, x1, abajoY1, MARCO + o, S, Z_PANEL, luz, overlay);
			borde(vc, p, x0, x1, abajoY1, true, luz, overlay);
		}

		VertexConsumer luces = buffers.getBuffer(RenderType.entityCutout(verde ? VERDE_LUCES : ROSA_LUCES));
		int brillo = LightTexture.FULL_BRIGHT;
		if (arribaY1 - arribaY0 > 0.001f) cara(luces, p, x0, arribaY0, x1, arribaY1, S, H - MARCO - o, Z_LUZ, brillo, overlay);
		if (abajoY1 - abajoY0 > 0.001f) cara(luces, p, x0, abajoY0, x1, abajoY1, MARCO + o, S, Z_LUZ, brillo, overlay);
		pose.popPose();
	}

	private static float u(float x) {
		return (x + W / 2) * PX / TEX;
	}

	private static float v(float y) {
		return (H - y) * PX / TEX;
	}

	/**
	 * Un rectángulo del panel en el mundo (x0..x1, y0..y1), con la parte de la textura de la puerta de
	 * texY0..texY1; adelante (z) y atrás (-z, espejado).
	 */
	private static void cara(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1,
							 float texY0, float texY1, float z, int luz, int overlay) {
		float u0 = u(x0), u1 = u(x1), vArriba = v(texY1), vAbajo = v(texY0);
		vertice(vc, p, x0, y0, z, u0, vAbajo, 0, 0, 1, luz, overlay);
		vertice(vc, p, x1, y0, z, u1, vAbajo, 0, 0, 1, luz, overlay);
		vertice(vc, p, x1, y1, z, u1, vArriba, 0, 0, 1, luz, overlay);
		vertice(vc, p, x0, y1, z, u0, vArriba, 0, 0, 1, luz, overlay);

		vertice(vc, p, x1, y0, -z, u1, vAbajo, 0, 0, -1, luz, overlay);
		vertice(vc, p, x0, y0, -z, u0, vAbajo, 0, 0, -1, luz, overlay);
		vertice(vc, p, x0, y1, -z, u0, vArriba, 0, 0, -1, luz, overlay);
		vertice(vc, p, x1, y1, -z, u1, vArriba, 0, 0, -1, luz, overlay);
	}

	/** El canto de una mitad (donde se separan), oscuro. */
	private static void borde(VertexConsumer vc, PoseStack.Pose p, float x0, float x1, float y, boolean arriba,
							  int luz, int overlay) {
		float u0 = 112.5f / TEX, u1 = 115.5f / TEX, v0 = 8.5f / TEX, v1 = 11.5f / TEX, z = Z_PANEL;
		if (arriba) {
			vertice(vc, p, x0, y, z, u0, v0, 0, 1, 0, luz, overlay);
			vertice(vc, p, x1, y, z, u1, v0, 0, 1, 0, luz, overlay);
			vertice(vc, p, x1, y, -z, u1, v1, 0, 1, 0, luz, overlay);
			vertice(vc, p, x0, y, -z, u0, v1, 0, 1, 0, luz, overlay);
		} else {
			vertice(vc, p, x0, y, -z, u0, v1, 0, -1, 0, luz, overlay);
			vertice(vc, p, x1, y, -z, u1, v1, 0, -1, 0, luz, overlay);
			vertice(vc, p, x1, y, z, u1, v0, 0, -1, 0, luz, overlay);
			vertice(vc, p, x0, y, z, u0, v0, 0, -1, 0, luz, overlay);
		}
	}

	/** Una caja del marco, de color liso, con todo el grosor. */
	private static void caja(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, int luz, int overlay) {
		float z0 = -Z_MARCO, z1 = Z_MARCO;
		float u0 = 112.5f / TEX, u1 = 115.5f / TEX, v0 = 0.5f / TEX, v1 = 3.5f / TEX;
		// frente y atrás
		vertice(vc, p, x0, y0, z1, u0, v1, 0, 0, 1, luz, overlay);
		vertice(vc, p, x1, y0, z1, u1, v1, 0, 0, 1, luz, overlay);
		vertice(vc, p, x1, y1, z1, u1, v0, 0, 0, 1, luz, overlay);
		vertice(vc, p, x0, y1, z1, u0, v0, 0, 0, 1, luz, overlay);
		vertice(vc, p, x1, y0, z0, u0, v1, 0, 0, -1, luz, overlay);
		vertice(vc, p, x0, y0, z0, u1, v1, 0, 0, -1, luz, overlay);
		vertice(vc, p, x0, y1, z0, u1, v0, 0, 0, -1, luz, overlay);
		vertice(vc, p, x1, y1, z0, u0, v0, 0, 0, -1, luz, overlay);
		// arriba y abajo
		vertice(vc, p, x0, y1, z1, u0, v1, 0, 1, 0, luz, overlay);
		vertice(vc, p, x1, y1, z1, u1, v1, 0, 1, 0, luz, overlay);
		vertice(vc, p, x1, y1, z0, u1, v0, 0, 1, 0, luz, overlay);
		vertice(vc, p, x0, y1, z0, u0, v0, 0, 1, 0, luz, overlay);
		vertice(vc, p, x0, y0, z0, u0, v1, 0, -1, 0, luz, overlay);
		vertice(vc, p, x1, y0, z0, u1, v1, 0, -1, 0, luz, overlay);
		vertice(vc, p, x1, y0, z1, u1, v0, 0, -1, 0, luz, overlay);
		vertice(vc, p, x0, y0, z1, u0, v0, 0, -1, 0, luz, overlay);
		// costados
		vertice(vc, p, x1, y0, z1, u0, v1, 1, 0, 0, luz, overlay);
		vertice(vc, p, x1, y0, z0, u1, v1, 1, 0, 0, luz, overlay);
		vertice(vc, p, x1, y1, z0, u1, v0, 1, 0, 0, luz, overlay);
		vertice(vc, p, x1, y1, z1, u0, v0, 1, 0, 0, luz, overlay);
		vertice(vc, p, x0, y0, z0, u0, v1, -1, 0, 0, luz, overlay);
		vertice(vc, p, x0, y0, z1, u1, v1, -1, 0, 0, luz, overlay);
		vertice(vc, p, x0, y1, z1, u1, v0, -1, 0, 0, luz, overlay);
		vertice(vc, p, x0, y1, z0, u0, v0, -1, 0, 0, luz, overlay);
	}

	private static void vertice(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v,
								float nx, float ny, float nz, int luz, int overlay) {
		vc.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay).setLight(luz).setNormal(p, nx, ny, nz);
	}

	@Override
	public boolean shouldRenderOffScreen(PuertaBlockEntity puerta) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
