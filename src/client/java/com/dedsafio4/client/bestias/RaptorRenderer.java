package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.RaptorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Los dinosaurios raptor del diseño (Dinosaurio.html y Dinosaurio turquesa.html): torso con rayas, púas
 * en el lomo, cuello y cabeza con cresta, mandíbula con dientes, cola de 4 segmentos, piernas con rodilla
 * y brazos (el Turquesa los tiene largos, con codo y dos garras). Animaciones: caminar, quieto y rugir.
 */
public class RaptorRenderer extends CajasRenderer<RaptorEntity> {
	// Colores de la textura (en este orden).
	private static final int ESCAMAS = 0, OSCURO = 1, PANZA = 2, PUA = 3, PUNTA = 4, OJO = 5, DIENTE = 6, NEGRO = 7, GARRA = 8;

	private final ResourceLocation textura;
	private final Nodo cuerpo, cuello, cabeza, mandibula;
	private final Nodo[] cola = new Nodo[4], caderas = new Nodo[2], rodillas = new Nodo[2], brazos = new Nodo[2];

	public RaptorRenderer(EntityRendererProvider.Context contexto, boolean turquesa) {
		super(contexto, 0.6f, 9);
		textura = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID,
				"textures/entity/" + (turquesa ? "raptor_turquesa" : "raptor_naranja") + ".png");
		cuerpo = nodo(raiz, 0, 16, 0);
		caja(cuerpo, ESCAMAS, 14, 9, 8, 0, 0, 0);
		caja(cuerpo, PANZA, 12, 1, 6.6f, 0, -4.55f, 0);
		for (float x : new float[]{-4, -1, 2, 5}) {
			caja(cuerpo, OSCURO, 1.2f, 0.2f, 8.2f, x, 4.55f, 0);
			caja(cuerpo, OSCURO, 1.2f, 4, 0.2f, x, 2, 4.05f);
			caja(cuerpo, OSCURO, 1.2f, 4, 0.2f, x, 2, -4.05f);
		}
		float[][] lomo = {{-5, 4}, {-2, 5}, {1, 5}, {4, 4}};
		for (float[] p : lomo) pua(cuerpo, p[1], p[0], 4.5f, -0.35f);
		// Cuello y cabeza
		cuello = nodo(cuerpo, 6, 2, 0);
		caja(cuello, ESCAMAS, 7, 5, 5, 3, 0, 0);
		pua(cuello, 3, 1, 2.5f, -0.35f);
		pua(cuello, 3, 4, 2.5f, -0.35f);
		cabeza = nodo(cuello, 6.5f, 0, 0);
		caja(cabeza, ESCAMAS, 7, 6, 6, 3, 1, 0);
		caja(cabeza, ESCAMAS, 6, 3, 5, 9, 1.5f, 0);
		for (int s : new int[]{1, -1}) {
			caja(cabeza, OSCURO, 3, 1, 1, 4, 3.5f, s * 2.6f);
			caja(cabeza, OJO, 2, 2, 0.2f, 4, 2, s * 3.05f);
			caja(cabeza, NEGRO, 1, 2, 0.3f, 4.3f, 2, s * 3.1f);
			caja(cabeza, NEGRO, 1, 0.6f, 0.2f, 11.3f, 2.4f, s * 2.55f);
			for (int i = 0; i < 4; i++) caja(cabeza, DIENTE, 0.8f, 1, 0.8f, 7 + i * 1.5f, -0.5f, s * 2.1f);
		}
		float[][] cresta = {{0, 4}, {2.5f, 3.5f}, {5, 2.5f}};
		for (float[] p : cresta) pua(cabeza, p[1], p[0], 4, -0.6f);
		mandibula = nodo(cabeza, 1.5f, -2, 0);
		caja(mandibula, ESCAMAS, 10, 2, 4.4f, 5, -0.5f, 0);
		caja(mandibula, PANZA, 9, 0.2f, 3.6f, 5, -1.55f, 0);
		for (int s : new int[]{1, -1}) {
			for (int i = 0; i < 4; i++) caja(mandibula, DIENTE, 0.8f, 1, 0.8f, 6.2f + i * 1.5f, 1, s * 1.8f);
		}
		// Cola (cadena hacia atrás)
		float[][] segmentos = {{8, 7, 6, 3}, {7, 5, 5, 3}, {6, 4, 4, 2.5f}, {6, 3, 3, 2}};
		Nodo padre = cuerpo;
		float px = -7;
		for (int i = 0; i < 4; i++) {
			float largo = segmentos[i][0], alto = segmentos[i][1], ancho = segmentos[i][2];
			Nodo seg = nodo(padre, px, i == 0 ? 1 : 0, 0);
			seg.rz = i == 0 ? 0.15f : 0.05f;
			caja(seg, i % 2 == 1 ? OSCURO : ESCAMAS, largo, alto, ancho, -largo / 2, 0, 0);
			pua(seg, segmentos[i][3], -largo / 2, alto / 2, 0.35f);
			cola[i] = seg;
			padre = seg;
			px = -largo;
		}
		// Piernas
		for (int i = 0; i < 2; i++) {
			float z = i == 0 ? 4.5f : -4.5f;
			Nodo cadera = nodo(raiz, -2, 14, z);
			caja(cadera, ESCAMAS, 5, 8, 3.6f, 0, -3, 0);
			Nodo rodilla = nodo(cadera, 0.5f, -7, 0);
			caja(rodilla, OSCURO, 3, 5, 3, 0, -2.5f, 0);
			caja(rodilla, ESCAMAS, 6, 2, 4, 1.5f, -6, 0);
			for (int c = -1; c <= 1; c++) caja(rodilla, GARRA, 1, 1, 0.8f, 5, -6.5f, c * 1.4f);
			caderas[i] = cadera;
			rodillas[i] = rodilla;
		}
		// Brazos
		for (int i = 0; i < 2; i++) {
			float z = i == 0 ? 4.4f : -4.4f;
			Nodo hombro = nodo(cuerpo, 5, -1, z);
			hombro.rz = 0.5f;
			caja(hombro, ESCAMAS, 2, 5, 2, 0, -2.5f, 0);
			if (turquesa) {
				Nodo antebrazo = nodo(hombro, 0, -5, 0);
				antebrazo.rz = 0.6f;
				caja(antebrazo, ESCAMAS, 1.8f, 5, 1.8f, 0, -2.5f, 0);
				caja(antebrazo, GARRA, 1, 2, 0.7f, 0.4f, -5.8f, -0.6f);
				caja(antebrazo, GARRA, 1, 2, 0.7f, 0.4f, -5.8f, 0.6f);
			} else {
				caja(hombro, GARRA, 2, 1, 1.6f, 1, -5.3f, 0);
			}
			brazos[i] = hombro;
		}
	}

	/** Una púa: base del color de las púas y punta clara, inclinada. */
	private void pua(Nodo padre, float s, float x, float y, float inclinacion) {
		Nodo n = nodo(padre, x, y, 0);
		n.rz = inclinacion;
		caja(n, PUA, s * 0.6f, s, s * 0.6f, 0, s / 2, 0);
		caja(n, PUNTA, s * 0.3f, s * 0.5f, s * 0.3f, 0, s * 1.15f, 0);
	}

	@Override
	protected void animar(RaptorEntity raptor, float parcial) {
		float t = (raptor.tickCount + parcial) / 20f;
		float w = Mth.clamp(raptor.walkAnimation.speed(parcial) * 2.5f, 0, 1);
		float f = raptor.walkAnimation.position(parcial) * 0.9f;
		for (int i = 0; i < 2; i++) {
			float fase = i * Mth.PI;
			caderas[i].rz = w * 0.55f * Mth.sin(f + fase);
			rodillas[i].rz = w * -0.35f * Math.max(0, Mth.sin(f + fase + 1.2f));
			brazos[i].rz = 0.5f + 0.15f * Mth.sin((w > 0.5f ? f : t * 1.5f) + fase);
		}
		cuerpo.y = 16 + w * 0.6f * Math.abs(Mth.cos(f)) + (1 - w) * 0.3f * Mth.sin(t * 1.5f);
		cuerpo.rz = w * 0.04f * Mth.sin(f * 2);
		for (int i = 0; i < 4; i++) {
			cola[i].ry = (w > 0.5f ? 0.18f : 0.08f) * Mth.sin(t * (w > 0.5f ? 2.5f : 1.2f) - i * 0.7f);
		}
		// Rugido: abre (0–20 %), sostiene temblando (20–75 %) y cierra.
		float abre = 0;
		if (raptor.rugido >= 0) {
			float k = (raptor.rugido + parcial) / RaptorEntity.TIEMPO_RUGIDO;
			abre = k < 0.2f ? k / 0.2f : k < 0.75f ? 1 + 0.05f * Mth.sin(t * 60) : Math.max(0, 1 - (k - 0.75f) / 0.25f);
		}
		mandibula.rz = -0.65f * abre;
		cuello.rz = 0.5f + 0.2f * abre;
		cabeza.rz = -0.55f + 0.08f * Mth.sin(f) * w + 0.25f * abre;
	}

	@Override
	public ResourceLocation getTextureLocation(RaptorEntity raptor) {
		return textura;
	}
}
