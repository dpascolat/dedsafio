package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.brazalete.TrajeEspacial;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * El Traje Espacial puesto (Modo de Vuelo del Brazalete): el modelo "outer_armor_astronauta" de Patricio, cada caja
 * pegada a la parte del cuerpo que le toca (casco a la cabeza, hombreras a los brazos...), así se mueve con el jugador.
 * Las medidas salen del .bbmodel (Blockbench: x al revés, y para arriba desde los pies), pasadas a cada parte.
 * Al apretar J se genera de abajo para arriba: una línea de luz sube desde los pies y el traje aparece debajo de ella,
 * brillando celeste justo en el borde (ver TrajeAnimacion). Al sacarlo, la línea baja y se va borrando.
 */
public class TrajeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/traje_espacial.png");
	/** Más finito que en Blockbench (ahí las capas eran +1 y +1,25, y el casco de 10). */
	private static final CubeDeformation FINO = new CubeDeformation(0.4f), HOMBRERA = new CubeDeformation(0.6f),
			CASCO = new CubeDeformation(-0.6f);
	private static final ModelPart CASCO_PARTE, PECHERA, BRAZO_DER, BRAZO_IZQ, PIERNA_DER, PIERNA_IZQ;
	/** De los pies a la punta del casco (bloques). */
	private static final float ALTO = 2.15f;

	static {
		MeshDefinition malla = new MeshDefinition();
		var raiz = malla.getRoot();
		raiz.addOrReplaceChild("casco", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -9, -5, 10, 10, 10, CASCO), PartPose.ZERO);
		raiz.addOrReplaceChild("pechera", CubeListBuilder.create().texOffs(0, 20)
				.addBox(-4, 0, -2, 8, 12, 4, FINO), PartPose.ZERO);
		raiz.addOrReplaceChild("brazo_der", CubeListBuilder.create()
				.texOffs(24, 20).addBox(-4, -2, -2, 4, 12, 4, FINO)
				.texOffs(40, 0).addBox(-4.5f, -3, -2.5f, 5, 5, 5, HOMBRERA), PartPose.ZERO);
		raiz.addOrReplaceChild("brazo_izq", CubeListBuilder.create()
				.texOffs(0, 36).addBox(0, -2, -2, 4, 12, 4, FINO)
				.texOffs(40, 10).addBox(-0.5f, -3, -2.5f, 5, 5, 5, HOMBRERA), PartPose.ZERO);
		raiz.addOrReplaceChild("pierna_der", CubeListBuilder.create().texOffs(16, 36)
				.addBox(-2, 0, -2, 4, 12, 4, FINO), PartPose.ZERO);
		raiz.addOrReplaceChild("pierna_izq", CubeListBuilder.create().texOffs(32, 36)
				.addBox(-2, 0, -2, 4, 12, 4, FINO), PartPose.ZERO);
		ModelPart todo = LayerDefinition.create(malla, 64, 64).bakeRoot();
		CASCO_PARTE = todo.getChild("casco");
		PECHERA = todo.getChild("pechera");
		BRAZO_DER = todo.getChild("brazo_der");
		BRAZO_IZQ = todo.getChild("brazo_izq");
		PIERNA_DER = todo.getChild("pierna_der");
		PIERNA_IZQ = todo.getChild("pierna_izq");
	}

	public TrajeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre) {
		super(padre);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador, float giro, float paso,
					   float parcial, float edad, float cabeza, float inclinacion) {
		if (jugador.isInvisible()) return;
		float a = TrajeAnimacion.avance(jugador.getUUID());
		if (TrajeEspacial.puesto(jugador)) {
			if (a < 0) a = 1;
		} else if (a < 0 || !TrajeAnimacion.sacando(jugador.getUUID())) {
			return;
		}
		// (Primero el anillo de luz y después el traje: pedir otro tipo de dibujo cierra el anterior.)
		Corte corte = null;
		if (a < 1) {
			// Solo se ve lo que está por debajo de una línea que sube (al sacarlo, baja). Las posiciones que llegan al
			// dibujar son relativas a la cámara, con y para arriba.
			Vec3 camara = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
			Vec3 pies = jugador.getPosition(parcial).subtract(camara);
			float nivel = (float) pies.y - 0.05f + ALTO * TrajeAnimacion.pieza(a, 0f, 1f);
			corte = new Corte(nivel);
			anillo(buffers.getBuffer(RenderType.lightning()), (float) pies.x, nivel, (float) pies.z, a);
			// Chispitas en la línea.
			if (jugador.getRandom().nextFloat() < 0.5f) {
				double ang = jugador.getRandom().nextDouble() * Math.PI * 2;
				Vec3 donde = jugador.getPosition(parcial);
				jugador.level().addParticle(ParticleTypes.END_ROD, donde.x + Math.cos(ang) * 0.45, camara.y + nivel,
						donde.z + Math.sin(ang) * 0.45, 0, 0.01, 0);
			}
		}
		VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		if (corte != null) v = corte.hacia(v);
		int sombra = LivingEntityRenderer.getOverlayCoords(jugador, 0f);
		PlayerModel<AbstractClientPlayer> m = getParentModel();
		pegado(pose, v, luz, sombra, m.head, CASCO_PARTE);
		pegado(pose, v, luz, sombra, m.body, PECHERA);
		pegado(pose, v, luz, sombra, m.rightArm, BRAZO_DER);
		pegado(pose, v, luz, sombra, m.leftArm, BRAZO_IZQ);
		pegado(pose, v, luz, sombra, m.rightLeg, PIERNA_DER);
		pegado(pose, v, luz, sombra, m.leftLeg, PIERNA_IZQ);
	}

	private static void pegado(PoseStack pose, VertexConsumer v, int luz, int sombra, ModelPart parte, ModelPart caja) {
		pose.pushPose();
		parte.translateAndRotate(pose);
		caja.render(pose, v, luz, sombra);
		pose.popPose();
	}

	/** El anillo de luz celeste que sube con la línea (un marco cuadrado y plano alrededor del jugador, de las dos caras). */
	private static void anillo(VertexConsumer v, float x, float y, float z, float a) {
		float brillo = Math.min(1f, Math.min(a, 1 - a) * 8f);   // aparece y se apaga en las puntas
		if (brillo <= 0) return;
		int alfa = (int) (200 * brillo);
		float af = 0.62f, ad = 0.42f;
		float[][] lados = {{-af, -af, af, -ad}, {-af, ad, af, af}, {-af, -ad, -ad, ad}, {ad, -ad, af, ad}};
		for (float[] l : lados) {
			float x0 = x + l[0], z0 = z + l[1], x1 = x + l[2], z1 = z + l[3];
			v.addVertex(x0, y, z0).setColor(73, 200, 229, alfa);
			v.addVertex(x0, y, z1).setColor(73, 200, 229, alfa);
			v.addVertex(x1, y, z1).setColor(73, 200, 229, alfa);
			v.addVertex(x1, y, z0).setColor(73, 200, 229, alfa);
			v.addVertex(x1, y, z0).setColor(73, 200, 229, alfa);
			v.addVertex(x1, y, z1).setColor(73, 200, 229, alfa);
			v.addVertex(x0, y, z1).setColor(73, 200, 229, alfa);
			v.addVertex(x0, y, z0).setColor(73, 200, 229, alfa);
		}
	}

	/**
	 * Pasa las caras del traje a otro VertexConsumer cortadas a la altura {@code nivel}: queda solo lo de abajo, y lo
	 * que está justo debajo de la línea brilla celeste (como si se estuviera generando).
	 */
	private static final class Corte implements VertexConsumer {
		/** Cuánto por debajo de la línea brilla (bloques). */
		private static final float BRILLO = 0.3f;
		private VertexConsumer destino;
		private final float nivel;
		/** Los 4 vértices de la cara que se está armando: x, y, z, u, v, nx, ny, nz. */
		private final float[][] q = new float[4][];
		private int color, sombra, luz, n;

		Corte(float nivel) {
			this.nivel = nivel;
		}

		Corte hacia(VertexConsumer destino) {
			this.destino = destino;
			return this;
		}

		@Override
		public void addVertex(float x, float y, float z, int c, float u, float vv, int overlay, int light, float nx, float ny, float nz) {
			q[n] = new float[]{x, y, z, u, vv, nx, ny, nz};
			color = c;
			sombra = overlay;
			luz = light;
			if (++n == 4) {
				n = 0;
				cortar();
			}
		}

		/** Corta la cara con la línea (se queda con lo de abajo) y la manda en una o dos caras de 4 puntas. */
		private void cortar() {
			List<float[]> sale = new ArrayList<>(6);
			for (int i = 0; i < 4; i++) {
				float[] p = q[i], s = q[(i + 1) % 4];
				boolean pAbajo = p[1] <= nivel, sAbajo = s[1] <= nivel;
				if (pAbajo) sale.add(p);
				if (pAbajo != sAbajo) {
					float t = (nivel - p[1]) / (s[1] - p[1]);
					float[] r = new float[8];
					for (int k = 0; k < 8; k++) r[k] = p[k] + (s[k] - p[k]) * t;
					sale.add(r);
				}
			}
			if (sale.size() < 3) return;
			if (sale.size() == 3) sale.add(sale.get(2));
			emitir(sale.get(0), sale.get(1), sale.get(2), sale.get(3));
			if (sale.size() == 5) emitir(sale.get(0), sale.get(3), sale.get(4), sale.get(4));
		}

		private void emitir(float[]... vs) {
			for (float[] p : vs) {
				int c = color, l = luz;
				float cerca = (nivel - p[1]) / BRILLO;
				if (cerca < 1) {
					// Justo debajo de la línea: celeste brillante.
					float k = 1 - Math.max(0, cerca);
					int r = (int) Mth.lerp(k, (c >> 16) & 255, 120), g = (int) Mth.lerp(k, (c >> 8) & 255, 230),
							b = (int) Mth.lerp(k, c & 255, 255);
					c = (c & 0xFF000000) | r << 16 | g << 8 | b;
					l = LightTexture.FULL_BRIGHT;
				}
				destino.addVertex(p[0], p[1], p[2], c, p[3], p[4], sombra, l, p[5], p[6], p[7]);
			}
		}

		// El modelo dibuja con el addVertex completo; esto se pasa igual por si algo usa los otros.
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			return destino.addVertex(x, y, z);
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			return destino.setColor(r, g, b, a);
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return destino.setUv(u, v);
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return destino.setUv1(u, v);
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return destino.setUv2(u, v);
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return destino.setNormal(x, y, z);
		}
	}
}
