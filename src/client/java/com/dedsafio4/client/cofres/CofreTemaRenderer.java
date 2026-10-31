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
import java.util.Random;

/**
 * El Cofre de Gleba y el Cofre del Limbo (cofres dobles 2×1): la misma forma de dibujarse que el Cofre de Huesos, cada
 * uno con sus piezas. La tapa gira en la bisagra de atrás y deja ver lo que hay adentro.
 */
public class CofreTemaRenderer implements BlockEntityRenderer<CofreBlockEntity> {
	/** Las piezas de la base no se mueven; las de la tapa se levantan hacia atrás. */
	private record Pieza(float w, float h, float d, int mat, float x, float y, float z, float rx, float ry, float rz, boolean tapa) {}

	/** Texturas de textures/entity/<carpeta>/; desde "brillan" en adelante se ven igual a oscuras. */
	public static final class Tema {
		private final RenderType[] tipos;
		private final int brillan;
		private final List<Pieza> piezas = new ArrayList<>();

		private Tema(String carpeta, int brillan, String... nombres) {
			this.brillan = brillan;
			tipos = new RenderType[nombres.length];
			for (int i = 0; i < nombres.length; i++) {
				tipos[i] = RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID,
						"textures/entity/" + carpeta + "/" + nombres[i] + ".png"));
			}
		}

		private void base(float w, float h, float d, int mat, float x, float y, float z) {
			piezas.add(new Pieza(w, h, d, mat, x, y, z, 0, 0, 0, false));
		}

		private void tapa(float w, float h, float d, int mat, float x, float y, float z) {
			piezas.add(new Pieza(w, h, d, mat, x, y, z, 0, 0, 0, true));
		}

		private void girada(float w, float h, float d, int mat, float x, float y, float z, float rx, float ry, float rz, boolean tapa) {
			piezas.add(new Pieza(w, h, d, mat, x, y, z, rx, ry, rz, tapa));
		}
	}

	private static final float BISAGRA_Y = 0.5f, BISAGRA_Z = -0.46f;

	private static final int CARNE = 0, CARNE_OSCURA = 1, HUESO = 2, ORGANO = 3, ESCLERA = 4, IRIS = 5, PUPILA = 6;
	public static final Tema GLEBA = new Tema("cofre_gleba", ESCLERA, "carne", "carne_oscura", "hueso", "organo", "esclera", "iris", "pupila");

	private static final int ROCA = 0, SOMBRA = 1, CENIZA = 2, GRIETA = 3, VACIO = 4;
	public static final Tema LIMBO = new Tema("cofre_limbo", GRIETA, "roca", "sombra", "ceniza", "grieta", "vacio");

	/** Un ojo que mira al frente. */
	private static void ojoFrente(Tema t, float x, float y, float z, float s, boolean tapa) {
		t.piezas.add(new Pieza(s + 0.04f, s + 0.04f, 0.04f, CARNE_OSCURA, x, y, z, 0, 0, 0, tapa));
		t.piezas.add(new Pieza(s, s, 0.05f, ESCLERA, x, y, z + 0.01f, 0, 0, 0, tapa));
		t.piezas.add(new Pieza(s * 0.6f, s * 0.6f, 0.05f, IRIS, x, y, z + 0.02f, 0, 0, 0, tapa));
		t.piezas.add(new Pieza(s * 0.3f, s * 0.3f, 0.05f, PUPILA, x, y, z + 0.03f, 0, 0, 0, tapa));
	}

	/** Un ojo que mira hacia arriba, sobre la tapa. */
	private static void ojoArriba(Tema t, float x, float y, float z, float s) {
		t.tapa(s + 0.04f, 0.04f, s + 0.04f, CARNE_OSCURA, x, y, z);
		t.tapa(s, 0.05f, s, ESCLERA, x, y + 0.01f, z);
		t.tapa(s * 0.6f, 0.05f, s * 0.6f, IRIS, x, y + 0.02f, z);
		t.tapa(s * 0.3f, 0.05f, s * 0.3f, PUPILA, x, y + 0.03f, z);
	}

	static {
		Random azar = new Random(7);
		// Gleba: la base de carne con un zócalo de hueso y una fila de dientes en el borde.
		Tema g = GLEBA;
		g.base(1.9f, 0.44f, 0.92f, CARNE, 0, 0.27f, 0);
		g.base(1.96f, 0.06f, 0.98f, HUESO, 0, 0.03f, 0);
		g.base(1.72f, 0.2f, 0.78f, CARNE_OSCURA, 0, 0.55f, 0);
		for (int sx : new int[]{-1, 1}) {
			for (int sz : new int[]{-1, 1}) g.base(0.12f, 0.5f, 0.12f, HUESO, sx * 0.92f, 0.25f, sz * 0.44f);
		}
		for (int i = 0; i < 11; i++) g.base(0.09f, 0.1f, 0.06f, HUESO, -0.75f + i * 0.15f, 0.45f, 0.46f);
		// Las venas del frente y los ojos, con el grande en el lugar de la cerradura.
		g.base(0.05f, 0.34f, 0.02f, CARNE_OSCURA, -0.62f, 0.25f, 0.465f);
		g.base(0.34f, 0.05f, 0.02f, CARNE_OSCURA, -0.42f, 0.2f, 0.465f);
		g.base(0.05f, 0.3f, 0.02f, CARNE_OSCURA, 0.56f, 0.28f, 0.465f);
		g.base(0.3f, 0.05f, 0.02f, CARNE_OSCURA, 0.38f, 0.33f, 0.465f);
		ojoFrente(g, 0, 0.27f, 0.47f, 0.28f, false);
		ojoFrente(g, -0.34f, 0.3f, 0.47f, 0.12f, false);
		ojoFrente(g, 0.3f, 0.2f, 0.47f, 0.14f, false);
		ojoFrente(g, 0.72f, 0.27f, 0.47f, 0.1f, false);
		// Adentro: el cerebro y el corazón, hechos de bultos.
		for (int i = 0; i < 26; i++) {
			float s = 0.08f + azar.nextFloat() * 0.08f;
			g.girada(s, s, s, ORGANO, -0.42f + (azar.nextFloat() - 0.5f) * 0.6f, 0.68f + azar.nextFloat() * 0.06f, (azar.nextFloat() - 0.5f) * 0.46f,
					azar.nextFloat(), azar.nextFloat(), 0, false);
		}
		g.base(0.34f, 0.12f, 0.3f, CARNE, 0.42f, 0.71f, 0);
		g.base(0.2f, 0.1f, 0.2f, CARNE, 0.42f, 0.74f, 0.08f);
		g.base(0.12f, 0.08f, 0.1f, ORGANO, 0.48f, 0.76f, -0.1f);
		// La tapa: escalonada, con 5 costillas en arco, la espina y más ojos.
		g.tapa(1.92f, 0.22f, 0.96f, CARNE, 0, 0.61f, 0);
		g.tapa(1.76f, 0.16f, 0.76f, CARNE, 0, 0.8f, 0);
		g.tapa(1.5f, 0.1f, 0.46f, CARNE_OSCURA, 0, 0.92f, 0);
		for (float x : new float[]{-0.76f, -0.38f, 0, 0.38f, 0.76f}) {
			for (int i = 0; i < 6; i++) {
				float a = (i + 0.5f) * Mth.PI / 6, z = Mth.cos(a) * 0.49f, y = 0.5f + Mth.sin(a) * 0.46f;
				g.girada(0.12f, 0.08f, 0.26f, HUESO, x, y, z, a + Mth.PI / 2, 0, 0, true);
			}
		}
		g.tapa(1.7f, 0.08f, 0.1f, HUESO, 0, 0.97f, 0);
		ojoFrente(g, -0.57f, 0.62f, 0.48f, 0.14f, true);
		ojoFrente(g, 0.19f, 0.64f, 0.48f, 0.16f, true);
		ojoFrente(g, 0.57f, 0.6f, 0.48f, 0.1f, true);
		ojoArriba(g, -0.2f, 0.97f, 0.18f, 0.14f);
		ojoArriba(g, 0.57f, 0.89f, 0.25f, 0.12f);

		// Limbo: roca negra con el zócalo y las esquinas más altas, y las grietas violetas que brillan.
		Tema l = LIMBO;
		l.base(1.9f, 0.44f, 0.92f, ROCA, 0, 0.27f, 0);
		l.base(1.98f, 0.08f, 1f, SOMBRA, 0, 0.04f, 0);
		l.base(1.72f, 0.2f, 0.78f, VACIO, 0, 0.55f, 0);
		for (int sx : new int[]{-1, 1}) {
			for (int sz : new int[]{-1, 1}) {
				l.base(0.16f, 0.58f, 0.16f, SOMBRA, sx * 0.91f, 0.29f, sz * 0.42f);
				l.girada(0.1f, 0.34f, 0.1f, CENIZA, sx * 0.97f, 0.76f, sz * 0.46f, sz * 0.3f, 0, -sx * 0.3f, false);
			}
		}
		l.base(0.04f, 0.34f, 0.02f, GRIETA, -0.5f, 0.28f, 0.465f);
		l.base(0.26f, 0.04f, 0.02f, GRIETA, -0.39f, 0.13f, 0.465f);
		l.base(0.04f, 0.18f, 0.02f, GRIETA, -0.28f, 0.2f, 0.465f);
		l.base(0.04f, 0.3f, 0.02f, GRIETA, 0.46f, 0.3f, 0.465f);
		l.base(0.3f, 0.04f, 0.02f, GRIETA, 0.6f, 0.17f, 0.465f);
		l.base(0.24f, 0.04f, 0.02f, GRIETA, 0.36f, 0.4f, 0.465f);
		// La cerradura: un rombo violeta en un marco de sombra.
		l.base(0.34f, 0.34f, 0.04f, SOMBRA, 0, 0.27f, 0.47f);
		l.girada(0.17f, 0.17f, 0.05f, GRIETA, 0, 0.27f, 0.485f, 0, 0, Mth.PI / 4, false);
		// Adentro: cristales que salen del vacío.
		for (int i = 0; i < 12; i++) {
			float s = 0.06f + azar.nextFloat() * 0.06f;
			l.girada(s, s * 2.2f, s, GRIETA, (azar.nextFloat() - 0.5f) * 1.5f, 0.71f, (azar.nextFloat() - 0.5f) * 0.56f,
					(azar.nextFloat() - 0.5f) * 0.6f, azar.nextFloat(), (azar.nextFloat() - 0.5f) * 0.6f, false);
		}
		// La tapa, como la de un sarcófago, con grietas, espinas y el borde de ceniza.
		l.tapa(1.94f, 0.22f, 0.98f, ROCA, 0, 0.61f, 0);
		l.tapa(1.98f, 0.04f, 1f, CENIZA, 0, 0.53f, 0);
		l.tapa(1.74f, 0.16f, 0.74f, ROCA, 0, 0.8f, 0);
		l.tapa(1.4f, 0.09f, 0.4f, SOMBRA, 0, 0.92f, 0);
		l.tapa(1.1f, 0.03f, 0.05f, GRIETA, 0, 0.97f, 0);
		l.tapa(0.04f, 0.14f, 0.02f, GRIETA, -0.62f, 0.62f, 0.495f);
		l.tapa(0.3f, 0.04f, 0.02f, GRIETA, -0.47f, 0.64f, 0.495f);
		l.tapa(0.04f, 0.15f, 0.02f, GRIETA, 0.34f, 0.63f, 0.495f);
		l.tapa(0.36f, 0.04f, 0.02f, GRIETA, 0.5f, 0.6f, 0.495f);
		l.tapa(0.3f, 0.03f, 0.02f, GRIETA, -0.1f, 0.81f, 0.375f);
		for (int i = 0; i < 5; i++) {
			float x = -0.6f + i * 0.3f, alto = i % 2 == 0 ? 0.26f : 0.18f;
			l.girada(0.09f, alto, 0.09f, SOMBRA, x, 0.96f + alto / 2, 0, 0, 0.78f, 0, true);
		}
	}

	private final Tema tema;

	public CofreTemaRenderer(BlockEntityRendererProvider.Context contexto, Tema tema) {
		this.tema = tema;
	}

	@Override
	public void render(CofreBlockEntity cofre, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		BlockState estado = cofre.getBlockState();
		if (!(estado.getBlock() instanceof CofreBlock)) return;
		dibujar(tema, pose, buffers, luz, overlay, estado.getValue(CofreBlock.FACING), cofre.getOpenNess(parcial), true);
	}

	/** También lo usa el ítem (cerrado). El modelo mide 2 de largo (X) con el frente hacia +Z. */
	public static void dibujar(Tema tema, PoseStack pose, MultiBufferSource buffers, int luz, int overlay, Direction frente, float abierto, boolean enElMundo) {
		pose.pushPose();
		if (enElMundo) {
			Direction derecha = frente.getClockWise();
			pose.translate(0.5 + derecha.getStepX() * 0.5, 0, 0.5 + derecha.getStepZ() * 0.5);
			pose.mulPose(Axis.YP.rotationDegrees(-frente.toYRot()));
		}
		float tapa = -1.3f * (1f - (1f - abierto) * (1f - abierto) * (1f - abierto));   // se levanta hacia atrás
		// Por material, así cada textura va en una sola tanda.
		for (int mat = 0; mat < tema.tipos.length; mat++) {
			VertexConsumer vc = buffers.getBuffer(tema.tipos[mat]);
			int l = mat >= tema.brillan ? LightTexture.FULL_BRIGHT : luz;
			for (Pieza p : tema.piezas) {
				if (p.mat() != mat) continue;
				pose.pushPose();
				if (p.tapa() && tapa != 0) {
					pose.translate(0, BISAGRA_Y, BISAGRA_Z);
					pose.mulPose(Axis.XP.rotation(tapa));
					pose.translate(0, -BISAGRA_Y, -BISAGRA_Z);
				}
				pose.translate(p.x(), p.y(), p.z());
				if (p.rx() != 0) pose.mulPose(Axis.XP.rotation(p.rx()));
				if (p.ry() != 0) pose.mulPose(Axis.YP.rotation(p.ry()));
				if (p.rz() != 0) pose.mulPose(Axis.ZP.rotation(p.rz()));
				CofreHuesosRenderer.caja(vc, pose.last(), p.w(), p.h(), p.d(), l, overlay);
				pose.popPose();
			}
		}
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(CofreBlockEntity cofre) {
		return true;
	}
}
