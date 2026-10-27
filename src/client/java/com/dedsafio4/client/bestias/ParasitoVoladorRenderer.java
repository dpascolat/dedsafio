package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.ParasitoVoladorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Parásito Volador del diseño (Parasito Volador.html, three.js): cabeza roja con placa oscura, mandíbulas, ojos y
 * antenas; tórax; un abdomen de 6 segmentos que se enrosca para abajo y termina en un aguijón; 6 patas negras colgando
 * y 2 alas que zumban. 1 unidad del diseño = 1 bloque = 16 píxeles; la cabeza mira a +X.
 */
public class ParasitoVoladorRenderer extends CajasRenderer<ParasitoVoladorEntity> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/parasito_volador.png");
	private static final int ROJO = 0, OSCURO = 1, VIENTRE = 2, PATA = 3, OJO = 4, ALA = 5;
	private static final float U = 16;
	/** Dónde va el cuerpo (en unidades) para que quede centrado en la caja de choque. */
	private static final float CUERPO_X = 0.1f, CUERPO_Y = 0.5f;

	private final Nodo cuerpo;
	private final Nodo[] cola = new Nodo[6], patas = new Nodo[6], alas = new Nodo[2];
	private final float[] patasBase = new float[6];

	public ParasitoVoladorRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.3f, 6);
		cuerpo = nodo(raiz, CUERPO_X * U, CUERPO_Y * U, 0);
		cuerpo.rz = 0.12f;

		// Cabeza (+X).
		Nodo cabeza = nodo(cuerpo, 0.55f * U, 0.05f * U, 0);
		c(cabeza, ROJO, 0.32f, 0.26f, 0.3f, 0, 0, 0);
		c(cabeza, OSCURO, 0.34f, 0.06f, 0.32f, -0.01f, 0.15f, 0);
		c(cabeza, PATA, 0.14f, 0.05f, 0.05f, 0.2f, -0.09f, 0.08f);
		c(cabeza, PATA, 0.14f, 0.05f, 0.05f, 0.2f, -0.09f, -0.08f);
		for (int s = -1; s <= 1; s += 2) {
			c(cabeza, OJO, 0.08f, 0.08f, 0.04f, 0.1f, 0.05f, s * 0.16f);
			Nodo antena = nodo(cabeza, 0.12f * U, 0.12f * U, s * 0.08f * U);
			antena.rx = s * -0.4f;
			antena.rz = -0.9f;
			c(antena, PATA, 0.03f, 0.3f, 0.03f, 0, 0.15f, 0);
			Nodo punta = nodo(antena, 0, 0.3f * U, 0);
			punta.rz = -0.7f;
			c(punta, PATA, 0.025f, 0.25f, 0.025f, 0, 0.125f, 0);
		}

		// Tórax y el abdomen que se enrosca para abajo y para atrás (-X).
		c(cuerpo, ROJO, 0.42f, 0.32f, 0.36f, 0.2f, 0, 0);
		c(cuerpo, OSCURO, 0.44f, 0.06f, 0.38f, 0.2f, 0.18f, 0);
		c(cuerpo, VIENTRE, 0.38f, 0.05f, 0.3f, 0.2f, -0.17f, 0);
		Nodo segmento = nodo(cuerpo, -0.02f * U, 0, 0);
		for (int i = 0; i < 6; i++) {
			float largo = 0.2f, ancho = 0.34f - i * 0.045f, alto = 0.3f - i * 0.04f;
			c(segmento, i % 2 == 1 ? OSCURO : ROJO, largo, alto, ancho, -largo / 2, 0, 0);
			c(segmento, VIENTRE, largo * 0.9f, 0.04f, ancho * 0.8f, -largo / 2, -alto / 2 - 0.01f, 0);
			Nodo siguiente = nodo(segmento, (-largo + 0.01f) * U, 0, 0);
			siguiente.rz = 0.22f;
			cola[i] = siguiente;
			segmento = siguiente;
		}
		c(segmento, PATA, 0.14f, 0.04f, 0.04f, -0.07f, 0, 0);

		// 6 patas colgando abajo del tórax.
		float[] xs = {0.32f, 0.2f, 0.08f};
		int n = 0;
		for (int li = 0; li < 3; li++) {
			for (int s = -1; s <= 1; s += 2) {
				Nodo cadera = nodo(cuerpo, xs[li] * U, -0.12f * U, s * 0.17f * U);
				cadera.rx = s * 0.9f;
				cadera.rz = (li - 1) * -0.3f;
				c(cadera, PATA, 0.04f, 0.22f, 0.04f, 0, -0.11f, 0);
				Nodo rodilla = nodo(cadera, 0, -0.22f * U, 0);
				rodilla.rx = s * -1.4f;
				c(rodilla, PATA, 0.03f, 0.26f, 0.03f, 0, -0.13f, 0);
				patas[n] = cadera;
				patasBase[n] = cadera.rx;
				n++;
			}
		}

		// Las alas.
		for (int i = 0; i < 2; i++) {
			int s = i == 0 ? -1 : 1;
			Nodo ala = nodo(cuerpo, 0.18f * U, 0.2f * U, s * 0.12f * U);
			c(ala, ALA, 0.5f, 0.01f, 0.75f, -0.12f, 0, s * 0.37f);
			c(ala, OSCURO, 0.03f, 0.025f, 0.75f, 0.1f, 0.005f, s * 0.37f);
			alas[i] = ala;
		}
	}

	/** Una caja con medidas y lugar en unidades del diseño. */
	private void c(Nodo n, int color, float w, float h, float d, float x, float y, float z) {
		caja(n, color, w * U, h * U, d * U, x * U, y * U, z * U);
	}

	@Override
	protected void animar(ParasitoVoladorEntity bicho, float parcial) {
		float t = (bicho.tickCount + parcial) / 20f;
		cuerpo.y = (CUERPO_Y + Mth.sin(t * 3) * 0.08f) * U;
		cuerpo.rz = 0.12f + Mth.sin(t * 3 + 1) * 0.05f;
		for (int i = 0; i < 2; i++) alas[i].rx = (i == 0 ? -1 : 1) * (0.3f + Mth.sin(t * 38) * 0.6f);
		for (int i = 0; i < 6; i++) cola[i].rz = 0.22f + Mth.sin(t * 4 - i * 0.6f) * 0.06f;
		for (int i = 0; i < 6; i++) {
			int s = i % 2 == 0 ? -1 : 1, li = i / 2;
			patas[i].rx = patasBase[i] + s * Mth.sin(t * 5 + li) * 0.08f;
		}
	}

	@Override
	public ResourceLocation getTextureLocation(ParasitoVoladorEntity bicho) {
		return TEXTURA;
	}
}
