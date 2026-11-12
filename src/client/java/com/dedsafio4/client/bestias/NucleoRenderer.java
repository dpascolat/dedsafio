package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.NucleoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Dibuja el Núcleo: la construcción de bloques (modelos/nucleo.bin, sacada de modelo-minecraft.glb: cada cara de
 * bloque juntada con las vecinas del mismo color) con cada bloque como un píxel, más:
 * <ul>
 * <li>Cargando: tiembla, brilla cada vez más y en el medio crece una bola de luz.</li>
 * <li>Supernova: una bola blanca que se agranda de golpe hasta el radio y queda como una cáscara naranja y roja que
 *     se va apagando.</li>
 * </ul>
 */
public class NucleoRenderer extends EntityRenderer<NucleoEntity> {
	private static final ResourceLocation BLANCO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/nucleo_blanco.png");
	private static final ResourceLocation MALLA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "modelos/nucleo.bin");

	/** Por cara: las 4 esquinas (x, y, z en píxeles). */
	private static float[] esquinas;
	/** Por cara: el color (ARGB) y hacia dónde mira (0..5: -x, +x, -y, +y, -z, +z). */
	private static int[] colores;
	private static byte[] normales;

	public NucleoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 2f;
	}

	@Override
	public ResourceLocation getTextureLocation(NucleoEntity nucleo) {
		return BLANCO;
	}

	private static void cargar() {
		if (esquinas != null) return;
		try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MALLA).open();
			 DataInputStream d = new DataInputStream(new java.io.BufferedInputStream(in))) {
			int nc = d.readUnsignedByte();
			int[] paleta = new int[nc];
			for (int i = 0; i < nc; i++) paleta[i] = 0xFF000000 | d.readUnsignedByte() << 16 | d.readUnsignedByte() << 8 | d.readUnsignedByte();
			int n = d.readInt();
			float[] e = new float[n * 12];
			int[] c = new int[n];
			byte[] nor = new byte[n];
			for (int i = 0; i < n; i++) {
				int eje = d.readByte(), signo = d.readByte(), color = d.readByte();
				float plano = d.readShort(), u0 = d.readShort(), v0 = d.readShort(), u1 = d.readShort(), v1 = d.readShort();
				float[][] q = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
				for (int k = 0; k < 4; k++) {
					float[] p = new float[3];
					p[eje] = plano;
					p[eje == 0 ? 1 : 0] = q[k][0];
					p[eje == 2 ? 1 : 2] = q[k][1];
					System.arraycopy(p, 0, e, i * 12 + k * 3, 3);
				}
				c[i] = paleta[color];
				nor[i] = (byte) (eje * 2 + signo);
			}
			esquinas = e;
			colores = c;
			normales = nor;
		} catch (Exception ex) {
			Dedsafio4.LOGGER.error("No se pudo leer el modelo del Núcleo", ex);
			esquinas = new float[0];
			colores = new int[0];
			normales = new byte[0];
		}
	}

	@Override
	public void render(NucleoEntity nucleo, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		cargar();
		int estado = nucleo.estado();
		float t = nucleo.tiempo() + (estado == NucleoEntity.QUIETO ? 0 : parcial);
		float carga = estado == NucleoEntity.CARGANDO ? Mth.clamp(t / NucleoEntity.CARGA, 0, 1) : 0;
		float escala = nucleo.getScale();

		pose.pushPose();
		// Muriendo: se achica.
		if (nucleo.deathTime > 0) {
			float m = 1 - Math.min(1f, (nucleo.deathTime + parcial) / 20f) * 0.8f;
			pose.translate(0, NucleoEntity.ALTO / 2 * (1 - m), 0);
			pose.scale(m, m, m);
		}
		// Cargando: tiembla cada vez más.
		if (carga > 0) {
			float fuerza = 0.06f * carga;
			pose.translate((Math.random() - 0.5) * fuerza, (Math.random() - 0.5) * fuerza, (Math.random() - 0.5) * fuerza);
		}
		pose.mulPose(Axis.YP.rotationDegrees(-nucleo.getYRot()));
		// El ojo del modelo mira para -x: se lo gira para que mire para adelante.
		pose.mulPose(Axis.YP.rotationDegrees(90));
		pose.scale(NucleoEntity.ESCALA * escala, NucleoEntity.ESCALA * escala, NucleoEntity.ESCALA * escala);
		pose.translate(0, 0, 0.5f);

		int brillo = estado == NucleoEntity.QUIETO ? luz
				: estado == NucleoEntity.SUPERNOVA ? LightTexture.FULL_BRIGHT
				: LightTexture.pack(Math.max(LightTexture.block(luz), (int) (15 * carga)), Math.max(LightTexture.sky(luz), (int) (15 * carga)));
		int overlay = OverlayTexture.pack(OverlayTexture.u(0f), OverlayTexture.v(nucleo.hurtTime > 0 || nucleo.deathTime > 0));
		dibujar(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BLANCO)), brillo, overlay);
		pose.popPose();

		// La luz del medio y la supernova.
		pose.pushPose();
		pose.translate(0, NucleoEntity.ALTO / 2 * escala, 0);
		VertexConsumer v = buffers.getBuffer(RenderType.lightning());
		if (estado == NucleoEntity.CARGANDO) {
			float pulso = 1 + Mth.sin(t * (0.3f + carga)) * 0.12f;
			esfera(v, pose.last().pose(), (0.4f + 1.8f * carga) * pulso * escala, 255, 210, 90, (int) (60 + 120 * carga));
			esfera(v, pose.last().pose(), (0.2f + 0.9f * carga) * pulso * escala, 255, 255, 230, (int) (90 + 150 * carga));
		} else if (estado == NucleoEntity.SUPERNOVA) {
			float radio = nucleo.radio();
			float crece = Mth.clamp(t / 12f, 0, 1);
			crece = 1 - (1 - crece) * (1 - crece) * (1 - crece);
			float apaga = Mth.clamp((NucleoEntity.DURACION - t) / 30f, 0, 1);
			// La onda: blanca al salir, después naranja y roja.
			float blanco = 1 - Mth.clamp((t - 6) / 20f, 0, 1);
			int g = (int) Mth.lerp(blanco, 90, 240), b = (int) Mth.lerp(blanco, 20, 200);
			esfera(v, pose.last().pose(), radio * crece, 255, g, b, (int) ((40 + 70 * blanco) * apaga));
			esfera(v, pose.last().pose(), radio * crece * 0.97f, 255, 60, 10, (int) (35 * apaga));
			// El fogonazo del medio, que se achica.
			float fogonazo = Mth.clamp(1 - t / 25f, 0, 1);
			if (fogonazo > 0) esfera(v, pose.last().pose(), radio * 0.35f * crece * fogonazo + 1f, 255, 255, 230, (int) (200 * fogonazo));
			esfera(v, pose.last().pose(), 1.6f * escala, 255, 200, 90, (int) (120 * apaga));
		}
		pose.popPose();
		super.render(nucleo, giro, parcial, pose, buffers, luz);
	}

	private static void dibujar(PoseStack pose, VertexConsumer v, int luz, int overlay) {
		PoseStack.Pose p = pose.last();
		Matrix4f m = p.pose();
		// Las 6 direcciones ya giradas.
		Vector3f[] dirs = new Vector3f[6];
		for (int i = 0; i < 6; i++) {
			float[] d = new float[3];
			d[i / 2] = (i % 2 == 0) ? -1 : 1;
			dirs[i] = p.normal().transform(new Vector3f(d[0], d[1], d[2])).normalize();
		}
		Vector3f q = new Vector3f();
		float[] e = esquinas;
		for (int i = 0, n = colores.length; i < n; i++) {
			Vector3f nn = dirs[normales[i]];
			int c = colores[i], o = i * 12;
			for (int k = 0; k < 4; k++) {
				m.transformPosition(e[o + k * 3], e[o + k * 3 + 1], e[o + k * 3 + 2], q);
				v.addVertex(q.x, q.y, q.z, c, 0.5f, 0.5f, overlay, luz, nn.x, nn.y, nn.z);
			}
		}
	}

	/** Una esfera de luz (se ve de afuera y de adentro). */
	private static void esfera(VertexConsumer v, Matrix4f m, float r, int cr, int cg, int cb, int alfa) {
		if (r <= 0 || alfa <= 0) return;
		int lat = 14, lon = 28;
		for (int i = 0; i < lat; i++) {
			float a0 = Mth.PI * i / lat - Mth.HALF_PI, a1 = Mth.PI * (i + 1) / lat - Mth.HALF_PI;
			float y0 = Mth.sin(a0) * r, y1 = Mth.sin(a1) * r, r0 = Mth.cos(a0) * r, r1 = Mth.cos(a1) * r;
			for (int j = 0; j < lon; j++) {
				float b0 = Mth.TWO_PI * j / lon, b1 = Mth.TWO_PI * (j + 1) / lon;
				float c0 = Mth.cos(b0), s0 = Mth.sin(b0), c1 = Mth.cos(b1), s1 = Mth.sin(b1);
				v.addVertex(m, r0 * c0, y0, r0 * s0).setColor(cr, cg, cb, alfa);
				v.addVertex(m, r1 * c0, y1, r1 * s0).setColor(cr, cg, cb, alfa);
				v.addVertex(m, r1 * c1, y1, r1 * s1).setColor(cr, cg, cb, alfa);
				v.addVertex(m, r0 * c1, y0, r0 * s1).setColor(cr, cg, cb, alfa);
				// Al revés, para que se vea desde adentro.
				v.addVertex(m, r0 * c1, y0, r0 * s1).setColor(cr, cg, cb, alfa);
				v.addVertex(m, r1 * c1, y1, r1 * s1).setColor(cr, cg, cb, alfa);
				v.addVertex(m, r1 * c0, y1, r1 * s0).setColor(cr, cg, cb, alfa);
				v.addVertex(m, r0 * c0, y0, r0 * s0).setColor(cr, cg, cb, alfa);
			}
		}
	}

	@Override
	public boolean shouldRender(NucleoEntity nucleo, Frustum frustum, double x, double y, double z) {
		if (nucleo.estado() == NucleoEntity.SUPERNOVA) return true;
		// El modelo es más ancho que la caja de choque (las alas): se agranda la caja para no cortarlo en el borde.
		return frustum.isVisible(nucleo.getBoundingBox().inflate(4 * nucleo.getScale()));
	}
}
