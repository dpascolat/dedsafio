package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.GusanoCarneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Gusano de Carne del diseño (Parásito): 9 segmentos de cuero rojizo con anillos oscuros, vientre claro y ganchos,
 * cabeza con boca de lamprea (labio cuadrado y 8 dientes) y gotas de jugo gástrico que brillan. Repta con una onda
 * que recorre el cuerpo. Medidas en píxeles, mirando a +Z.
 */
public class GusanoCarneRenderer extends CajasRenderer<GusanoCarneEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/gusano_carne.png");
	private static final int CUERO = 0, ANILLO = 1, VIENTRE = 2, BOCA = 3, DIENTE = 4, JUGO = 5;
	private static final int N = 9;

	private final Nodo cuerpo, cabeza;
	private final Nodo[] segmentos = new Nodo[N];

	public GusanoCarneRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.3f, 8);
		cuerpo = nodo(raiz, 0, 3.2f, 0);
		float z = 0;
		for (int i = 0; i < N; i++) {
			float t = i / (N - 1f), s = 5.5f - Math.abs(t - 0.3f) * 3.2f, largo = 3;
			Nodo g = nodo(cuerpo, 0, 0, z);
			caja(g, CUERO, s, s, largo, 0, 0, 0);
			caja(g, ANILLO, s + 0.4f, s + 0.4f, 0.8f, 0, 0, -largo / 2);
			caja(g, VIENTRE, s * 0.7f, 0.3f, largo * 0.85f, 0, -s / 2 - 0.15f, 0);
			if (i % 2 == 0 && i > 0 && i < N - 1) {
				for (int lado = -1; lado <= 1; lado += 2) caja(g, ANILLO, 0.8f, 0.8f, 1.4f, lado * (s / 2 + 0.4f), -s / 2 + 0.6f, 0.4f);
			}
			if (i == 3) caja(g, JUGO, 0.6f, 1.2f, 0.6f, 0.6f, -s / 2 - 0.6f, 0.5f);
			if (i == N - 1) caja(g, ANILLO, 1.6f, 1.6f, 2, 0, 0, -largo / 2 - 1);
			segmentos[i] = g;
			z -= largo;
		}
		cabeza = nodo(cuerpo, 0, 0, 0);
		float hs = 6.4f;
		caja(cabeza, CUERO, hs, hs, 3, 0, 0, 3);
		caja(cabeza, ANILLO, hs + 0.4f, 1.2f, 1, 0, hs / 2 - 0.6f, 5);
		caja(cabeza, ANILLO, hs + 0.4f, 1.2f, 1, 0, -hs / 2 + 0.6f, 5);
		caja(cabeza, ANILLO, 1.2f, hs - 2.4f, 1, -hs / 2 + 0.6f, 0, 5);
		caja(cabeza, ANILLO, 1.2f, hs - 2.4f, 1, hs / 2 - 0.6f, 0, 5);
		caja(cabeza, BOCA, hs - 2.4f, hs - 2.4f, 0.4f, 0, 0, 4.6f);
		float[][] dientes = {{-1.6f, 1.6f}, {0, 1.8f}, {1.6f, 1.6f}, {-1.6f, -1.6f}, {0, -1.8f}, {1.6f, -1.6f}, {-1.8f, 0}, {1.8f, 0}};
		for (float[] d : dientes) caja(cabeza, DIENTE, 0.6f, 0.6f, 1, d[0] * 0.7f, d[1] * 0.7f, 5.1f);
		caja(cabeza, JUGO, 0.6f, 1.6f, 0.6f, -1.2f, -hs / 2 - 0.8f, 4.9f);
		caja(cabeza, JUGO, 0.5f, 1, 0.5f, 1.4f, -hs / 2 - 0.5f, 4.7f);
	}

	@Override
	protected void animar(GusanoCarneEntity bicho, float parcial) {
		float t = (bicho.tickCount + parcial) * 0.15f;
		float anda = Mth.clamp(bicho.walkAnimation.speed(parcial) * 2.5f, 0.3f, 1f);
		// La onda recorre el cuerpo de la cabeza a la cola.
		for (int i = 0; i < N; i++) {
			Nodo g = segmentos[i];
			g.x = Mth.sin(0.8f * i - t * 2) * 1.2f * anda;
			g.y = Mth.sin(0.6f * i + 1 - t) * 0.6f;
			g.ry = Mth.cos(0.8f * i - t * 2) * 0.12f * anda;
		}
		cabeza.x = Mth.sin(-t * 2) * 0.6f * anda;
		cabeza.ry = Mth.cos(-t * 2) * 0.1f * anda;
	}

	@Override
	protected float giroDiseno() {
		return 0;
	}

	@Override
	protected boolean brilla(int color) {
		return color == JUGO;
	}

	@Override
	public ResourceLocation getTextureLocation(GusanoCarneEntity bicho) {
		return TEXTURA;
	}
}
