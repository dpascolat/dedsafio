package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.NautilusOseoGrandeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * El Nautilus Óseo Grande del diseño (Nautilus Óseo Grande.zip, three.js): el caparazón es una espiral de 46 cámaras
 * de hueso que se achican hacia adentro (con franjas más oscuras y púas), y de la boca salen 15 tentáculos de carne
 * violeta en 3 filas, con la punta de hueso, que cuelgan y ondulan. Mismas medidas que el diseño (1 unidad = 12
 * píxeles), con las puntas de los tentáculos en el piso de la caja de choque y la boca mirando a +X.
 */
public class NautilusOseoGrandeRenderer extends CajasRenderer<NautilusOseoGrandeEntity> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/nautilus_oseo_grande.png");
	private static final int HUESO = 0, FRANJA = 1, CARNE = 2;
	private static final float U = 12;
	/** Para que las puntas de los tentáculos queden en el piso y el bicho centrado (calculado del diseño). */
	private static final float CORRER_X = 0.265f, CORRER_Y = 2.75f;

	private final Nodo mob;
	private final List<Nodo> articulaciones = new ArrayList<>();
	private final List<float[]> datos = new ArrayList<>();   // {giro base, número de articulación, número de tentáculo}

	public NautilusOseoGrandeRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 1.0f, 3);
		mob = nodo(raiz, CORRER_X * U, CORRER_Y * U, 0);
		// El caparazón: la espiral.
		float r0 = 1.4f, k = 0.17f, paso = 0.3f, phi = (float) Math.atan2(-0.98, -0.2);
		int n = 46;
		for (int i = 0; i < n; i++) {
			float t = i * paso, r = r0 * (float) Math.exp(-k * t), a = phi - t;
			float x = r * Mth.cos(a), y = r * Mth.sin(a);
			float tx = Mth.sin(a), ty = -Mth.cos(a);
			float largo = r * paso * 1.3f, alto = r * 0.95f, ancho = r * 0.95f + 0.1f;
			boolean franja = i < 26 && i % 3 == 1;
			cajaGirada(mob, franja ? FRANJA : HUESO, largo, alto, ancho, x, y, 0, (float) Math.atan2(ty, tx));
			if (i < 30 && i % 2 == 0) {
				float sz = r * 0.22f, nx = Mth.cos(a), ny = Mth.sin(a), fuera = alto / 2 + sz / 2 - 0.01f;
				cajaGirada(mob, FRANJA, sz, sz, sz * 1.6f, x + nx * fuera, y + ny * fuera, 0, a);
			}
		}
		cajaGirada(mob, FRANJA, 0.22f, 0.22f, 0.5f, 0, 0, 0, 0.4f);

		// Los tentáculos salen derecho de la boca del caparazón.
		float a0 = phi, x0 = r0 * Mth.cos(a0), y0 = r0 * Mth.sin(a0);
		float ox = 0.98f, oy = -0.2f, largoO = Mth.sqrt(ox * ox + oy * oy);
		ox /= largoO;
		oy /= largoO;
		Nodo cuerpo = nodo(mob, (x0 - ox * 0.15f) * U, (y0 - oy * 0.15f) * U, 0);
		cuerpo.rz = (float) Math.atan2(oy, ox);
		float[][] filas = {{0.1f, 0.35f, 1.25f, -0.12f, 8}, {0.1f, 0, 0.95f, -0.08f, 9}, {0.05f, -0.35f, 0.6f, -0.05f, 10}};
		float[] zs = {-0.5f, -0.25f, 0, 0.25f, 0.5f};
		int ti = 0;
		for (int ri = 0; ri < filas.length; ri++) {
			float[] fila = filas[ri];
			for (int zi = 0; zi < zs.length; zi++) {
				float z = zs[zi];
				Nodo padre = nodo(cuerpo, fila[0] * U, fila[1] * U, z * U);
				padre.rz = fila[2] + (zi % 2 == 1 ? 0.12f : -0.08f);
				padre.rx = z * 0.5f;
				float w = 0.3f - ri * 0.02f;
				int segmentos = (int) fila[4];
				for (int j = 0; j < segmentos; j++) {
					float largo = 0.38f - j * 0.015f;
					caja(padre, j == segmentos - 1 ? FRANJA : CARNE, w * U, largo * U, w * U, 0, -largo / 2 * U, 0);
					Nodo siguiente = nodo(padre, 0, (-largo + 0.02f) * U, 0);
					float base = fila[3] * (1 + j * 0.25f) + (zi == 1 ? 0.06f : 0);
					siguiente.rz = base;
					articulaciones.add(siguiente);
					datos.add(new float[]{base, j, ti});
					padre = siguiente;
					w *= 0.9f;
				}
				ti++;
			}
		}
	}

	/** Una caja del diseño (en unidades) girada en Z sobre su centro. */
	private void cajaGirada(Nodo padre, int color, float w, float h, float d, float x, float y, float z, float rz) {
		Nodo n = nodo(padre, x * U, y * U, z * U);
		n.rz = rz;
		caja(n, color, w * U, h * U, d * U, 0, 0, 0);
	}

	@Override
	protected void animar(NautilusOseoGrandeEntity bicho, float parcial) {
		float t = (bicho.tickCount + parcial) / 20f;
		boolean embiste = bicho.embistiendo();
		// Flota subiendo y bajando, y se mece; al embestir se tira para adelante.
		mob.y = (CORRER_Y + Mth.sin(t * 1.6f) * 0.18f) * U;
		mob.rz = Mth.sin(t * 1.6f + 0.8f) * 0.05f + (embiste ? -0.35f : 0);
		float onda = embiste ? 0.2f : 0.07f, rapidez = embiste ? 7f : 2.4f;
		for (int i = 0; i < articulaciones.size(); i++) {
			float[] d = datos.get(i);
			// Embistiendo, los tentáculos se estiran para atrás (se doblan menos) y se sacuden más rápido.
			float base = embiste ? d[0] * 0.3f + 0.05f : d[0];
			articulaciones.get(i).rz = base + Mth.sin(t * rapidez - d[1] * 0.6f + d[2] * 0.7f) * onda;
		}
	}

	@Override
	public ResourceLocation getTextureLocation(NautilusOseoGrandeEntity bicho) {
		return TEXTURA;
	}
}
