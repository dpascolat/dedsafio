package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.NautilusOseoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Nautilus Óseo del diseño: caparazón de hueso recto (8 segmentos que se achican hacia atrás, con anillos,
 * púas arriba y una punta), la cabeza de carne con capucha y ojos de pupila roja que brilla, y 8 tentáculos
 * para adelante que ondulan mientras vuela. Medidas en píxeles, mirando a +Z.
 */
public class NautilusOseoRenderer extends CajasRenderer<NautilusOseoEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/nautilus_oseo.png");
	private static final int HUESO = 0, ANILLO = 1, CARNE = 2, CARNE_OSCURA = 3, OJO = 4, PUPILA = 5;
	private static final float[][] ANILLO_TENTACULOS = {{-2.4f, -2.4f}, {0, -2.8f}, {2.4f, -2.4f}, {-2.8f, 0}, {2.8f, 0}, {-2, 2}, {2, 2}, {0, 0}};

	private final Nodo cuerpo, caparazon;
	private final Nodo[] tentaculos = new Nodo[8], puntas = new Nodo[8];
	private final float[][] giroBase = new float[8][2];

	public NautilusOseoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.4f, 8);
		// Todo va centrado en la altura de la caja de choque (0,7 bloques).
		cuerpo = nodo(raiz, 0, 5.6f, 0);
		caparazon = nodo(cuerpo, 0, 0, 0);
		float z = 2;
		for (int i = 0; i < 8; i++) {
			float s = 9 - i * 0.85f, largo = 3.2f - i * 0.12f;
			z -= largo / 2;
			caja(caparazon, HUESO, s, s, largo, 0, 0, z);
			caja(caparazon, ANILLO, s + 0.4f, s + 0.4f, 0.6f, 0, 0, z - largo / 2);
			z -= largo / 2;
		}
		caja(caparazon, ANILLO, 1.6f, 1.6f, 2, 0, 0, z - 1);
		caja(caparazon, ANILLO, 10, 10, 1, 0, 0, 2.3f);
		for (int i : new int[]{0, 2, 4}) {
			float s = 9 - i * 0.85f;
			caja(caparazon, HUESO, 1.2f, 2, 1.2f, 0, s / 2 + 1, 0.4f - i * 3);
		}
		caja(cuerpo, CARNE, 7.5f, 7.5f, 3, 0, 0, 4.2f);
		caja(cuerpo, ANILLO, 8.4f, 2, 3.4f, 0, 3.4f, 4.2f);
		for (int lado = -1; lado <= 1; lado += 2) {
			caja(cuerpo, OJO, 1, 2, 2, lado * 4, 0.8f, 4.3f);
			caja(cuerpo, PUPILA, 0.4f, 1, 1, lado * 4.6f, 0.9f, 4.6f);
		}
		for (int i = 0; i < 8; i++) {
			float x = ANILLO_TENTACULOS[i][0], y = ANILLO_TENTACULOS[i][1];
			float largo = 9 + ((i * 7) % 4) + (i == 7 ? 3 : 0);
			Nodo t = nodo(cuerpo, x, y, 5.6f);
			giroBase[i][0] = -y * 0.03f + 0.04f;
			giroBase[i][1] = x * 0.03f;
			caja(t, CARNE, 1.4f, 1.4f, largo * 0.6f, 0, 0, largo * 0.3f);
			Nodo punta = nodo(t, 0, 0, largo * 0.6f);
			caja(punta, CARNE_OSCURA, 1, 1, largo * 0.4f, 0, 0, largo * 0.2f);
			tentaculos[i] = t;
			puntas[i] = punta;
		}
	}

	@Override
	protected void animar(NautilusOseoEntity bicho, float parcial) {
		float t = (bicho.tickCount + parcial) * 0.15f;
		float velocidad = Mth.clamp((float) bicho.getDeltaMovement().length() * 6, 0, 1);
		cuerpo.y = 5.6f + Mth.sin(t * 0.6f) * 0.6f;
		cuerpo.rx = Mth.sin(t * 0.6f + 1) * 0.05f;
		caparazon.ry = Mth.sin(t * 0.5f) * 0.04f;
		float onda = 0.08f + 0.1f * velocidad;
		for (int i = 0; i < 8; i++) {
			float fase = t * (1.4f + velocidad) + i * 0.8f;
			tentaculos[i].rx = giroBase[i][0] + Mth.sin(fase) * onda;
			tentaculos[i].ry = giroBase[i][1] + Mth.cos(fase * 0.9f) * onda;
			puntas[i].rx = Mth.sin(fase - 0.9f) * onda * 1.6f;
			puntas[i].ry = Mth.cos(fase * 0.9f - 0.9f) * onda * 1.6f;
		}
	}

	@Override
	protected float giroDiseno() {
		return 0;
	}

	@Override
	protected boolean brilla(int color) {
		return color == PUPILA;
	}

	@Override
	public ResourceLocation getTextureLocation(NautilusOseoEntity bicho) {
		return TEXTURA;
	}
}
