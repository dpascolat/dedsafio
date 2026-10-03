package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.NarvalEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Narval del diseño (Narval.html): cuerpo azul con manchas, aletas dorsales en gancho, pectorales,
 * cabeza con boca y dientes, el cuerno en espiral y la cola con la aleta en V. Nada ondulando como una
 * ballena (la ola va hacia la cola) y al embestir se echa atrás, sale disparado con la boca abierta y vuelve.
 */
public class NarvalRenderer extends CajasRenderer<NarvalEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/narval.png");
	// Colores de la textura (en este orden).
	private static final int PIEL = 0, OSCURA = 1, PANZA = 2, MANCHA = 3, ALETA = 4, PUNTA = 5, CUERNO = 6, ESPIRAL = 7,
			BOCA = 8, DIENTE = 9, OJO = 10, PUPILA = 11;

	private final Nodo cuerpo, cabeza, mandibula, cola0, cola1, aleta;
	private final Nodo[] pectorales = new Nodo[2];

	public NarvalRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.8f, 12);
		cuerpo = nodo(raiz, 0, 8, 0);
		caja(cuerpo, PIEL, 16, 8, 9, 0, 0, 0);
		caja(cuerpo, PANZA, 14, 0.4f, 7, 0, -4.2f, 0);
		caja(cuerpo, OSCURA, 14, 0.4f, 5, 0, 4.2f, 0);
		float[][] manchas = {{-4, 2.5f}, {1, -2.5f}, {5, 1.5f}, {-1, 3}};
		for (float[] m : manchas) caja(cuerpo, MANCHA, 1.5f, 0.3f, 1.5f, m[0], 4.45f, m[1]);
		dorsal(cuerpo, 9, 4, 4);
		dorsal(cuerpo, 7, -1, 4);
		dorsal(cuerpo, 6, -5.5f, 4);
		for (int i = 0; i < 2; i++) {
			int s = i == 0 ? 1 : -1;
			Nodo f = nodo(cuerpo, 3, -2.5f, s * 4.5f);
			caja(f, ALETA, 5, 1, 5, -1, 0, s * 2.5f);
			pectorales[i] = f;
		}
		// Cabeza
		cabeza = nodo(cuerpo, 8, 0, 0);
		caja(cabeza, PIEL, 7, 6, 8, 3.5f, 1, 0);
		caja(cabeza, OSCURA, 6, 0.4f, 5, 3, 4.2f, 0);
		caja(cabeza, PIEL, 4, 4, 6.5f, 8.5f, 1.5f, 0);
		caja(cabeza, BOCA, 8, 1.5f, 5.5f, 6, -1.6f, 0);
		for (int s : new int[]{1, -1}) {
			caja(cabeza, OJO, 2, 2, 0.2f, 5, 2, s * 4.05f);
			caja(cabeza, PUPILA, 1, 1, 0.3f, 5.5f, 2, s * 4.1f);
			for (int i = 0; i < 4; i++) caja(cabeza, DIENTE, 0.8f, 1.2f, 0.8f, 5 + i * 1.6f, -1.2f, s * 2.8f);
		}
		Nodo cuerno = nodo(cabeza, 10.5f, 2.5f, 0);
		cuerno.rz = 0.12f;
		for (int i = 0; i < 7; i++) {
			float s = 2.2f - i * 0.22f;
			caja(cuerno, i % 2 == 1 ? ESPIRAL : CUERNO, 2.2f, s, s, 1.1f + i * 2, 0, 0);
		}
		mandibula = nodo(cabeza, 2, -3, 0);
		caja(mandibula, PIEL, 9, 2, 7, 4.5f, -0.5f, 0);
		caja(mandibula, PANZA, 8, 0.3f, 5.5f, 4.5f, -1.6f, 0);
		for (int s : new int[]{1, -1}) {
			for (int i = 0; i < 4; i++) caja(mandibula, DIENTE, 0.8f, 1.2f, 0.8f, 4 + i * 1.6f, 1, s * 2.6f);
		}
		// Cola
		cola0 = nodo(cuerpo, -8, 0.5f, 0);
		caja(cola0, PIEL, 10, 6, 7, -5, 0, 0);
		caja(cola0, PANZA, 9, 0.3f, 5, -5, -3.1f, 0);
		dorsal(cola0, 4, -6, 3);
		cola1 = nodo(cola0, -10, 0, 0);
		caja(cola1, OSCURA, 8, 4, 5, -4, 0, 0);
		aleta = nodo(cola1, -8, 0, 0);
		caja(aleta, OSCURA, 3, 2, 3, -1.5f, 0, 0);
		for (int s : new int[]{1, -1}) {
			Nodo pala = nodo(aleta, -2, 0, 0);
			pala.ry = s * 0.55f;
			caja(pala, ALETA, 4, 1, 8, -2, 0, s * 4);
		}
	}

	/** Aleta dorsal en gancho: tres cajas, la punta curvada hacia adelante y más clara. */
	private void dorsal(Nodo padre, float s, float x, float y) {
		Nodo base = nodo(padre, x, y, 0);
		caja(base, ALETA, 3, s * 0.45f, 2, 0, s * 0.22f, 0);
		Nodo medio = nodo(base, 0, s * 0.45f, 0);
		medio.rz = 0.35f;
		caja(medio, ALETA, 2.4f, s * 0.35f, 1.8f, 0, s * 0.17f, 0);
		Nodo punta = nodo(medio, 0, s * 0.35f, 0);
		punta.rz = -0.9f;
		caja(punta, PUNTA, 1.8f, s * 0.3f, 1.5f, 0, s * 0.15f, 0);
	}

	private static float facil(float x) {
		return x < 0.5f ? 2 * x * x : 1 - (float) Math.pow(-2 * x + 2, 2) / 2;
	}

	@Override
	protected void animar(NarvalEntity narval, float parcial) {
		float t = (narval.tickCount + parcial) / 20f;
		boolean nadando = narval.getDeltaMovement().lengthSqr() > 0.0004;
		float velocidad = nadando ? 1 : 0.25f, abre = 0.15f + 0.1f * Mth.sin(t * 0.9f), dx = 0;
		// La embestida: se echa atrás y abre la boca; el avance lo hace la entidad, que sale disparada.
		if (narval.embestida >= 0) {
			float k = (narval.embestida + parcial) / NarvalEntity.TIEMPO_EMBESTIDA;
			if (k < 0.18f) {
				dx = -5 * facil(k / 0.18f);
			} else if (k < 0.42f) {
				float q = facil((k - 0.18f) / 0.24f);
				dx = -5 * (1 - q);   // el avance de verdad lo hace la entidad
				velocidad = 3.2f;
				abre = q;
			} else {
				float q = facil(Math.min(1, (k - 0.42f) / 0.58f));
				velocidad = 1.4f;
				abre = 1 - q;
			}
		}
		float f = t * 4 * velocidad;
		raiz.x = dx;
		raiz.y = 1.2f + 1.2f * Mth.sin(t * 1.4f);
		cuerpo.rz = 0.05f * Mth.sin(f + 0.6f);
		cabeza.rz = -0.04f * Mth.sin(f + 0.6f);
		cola0.rz = 0.14f * Mth.sin(f);
		cola1.rz = 0.24f * Mth.sin(f - 0.8f);
		aleta.rz = 0.4f * Mth.sin(f - 1.6f);
		for (int i = 0; i < 2; i++) pectorales[i].rx = (i == 0 ? 1 : -1) * (0.5f + 0.25f * Mth.sin(f * 0.7f));
		mandibula.rz = -0.55f * Math.max(0, abre);
	}

	@Override
	public ResourceLocation getTextureLocation(NarvalEntity narval) {
		return TEXTURA;
	}
}
