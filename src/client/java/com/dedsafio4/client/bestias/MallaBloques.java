package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Un modelo hecho de bloquecitos de un color cada uno (los .bin de assets/dedsafio4/modelos, sacados de un .glb):
 * cada cara juntada con las vecinas del mismo color. Se lee la primera vez que se dibuja.
 * <p>
 * Formato: byte cantidad de colores, por color 3 bytes (r, g, b); int cantidad de caras; por cara byte eje (0 x, 1 y,
 * 2 z), byte signo (0 mira para -, 1 para +), byte color, short plano y short u0, v0, u1, v1 (los otros dos ejes).
 * Todo en bloquecitos.
 */
public final class MallaBloques {
	private final ResourceLocation archivo;
	/** Por cara: las 4 esquinas (x, y, z en bloquecitos). */
	private float[] esquinas;
	/** Por cara: el color (ARGB) y hacia dónde mira (0..5: -x, +x, -y, +y, -z, +z). */
	private int[] colores;
	private byte[] normales;

	public MallaBloques(String nombre) {
		this.archivo = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "modelos/" + nombre + ".bin");
	}

	private void cargar() {
		if (esquinas != null) return;
		try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(archivo).open();
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
			Dedsafio4.LOGGER.error("No se pudo leer el modelo " + archivo, ex);
			esquinas = new float[0];
			colores = new int[0];
			normales = new byte[0];
		}
	}

	/** Dibuja todas las caras con la textura blanca de la entidad (el color va en cada vértice). */
	public void dibujar(PoseStack pose, VertexConsumer v, int luz, int overlay) {
		cargar();
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
}
