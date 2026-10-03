package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperPastelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Creeper Pastel del diseño: una torta de bizcocho con manchas, glaseado que chorrea por los costados,
 * confites rojos arriba, cara de creeper de chocolate y 4 patitas. Mide 1 bloque de alto. Camina
 * bamboleándose y, al activarse, se infla y titila como un creeper.
 */
public class CreeperPastelRenderer extends CajasRenderer<CreeperPastelEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_pastel.png");
	private static final int BIZCOCHO = 0, BIZCOCHO_OSCURO = 1, BIZCOCHO_CLARO = 2, GLASEADO = 3, GLASEADO_SOMBRA = 4,
			CONFITE = 5, CHOCOLATE = 6;

	private final Nodo cuerpo;
	private final Nodo[] patas = new Nodo[4];

	/** El mismo generador "al azar" del diseño, para que las manchas salgan igual. */
	private int semilla = 5;

	private float azar() {
		semilla = semilla * 1103515245 + 12345;
		return (semilla & 0xFFFFFFFFL) / 4294967296f;
	}

	public CreeperPastelRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.5f, 7);
		cuerpo = nodo(raiz, 0, 3, 0);
		caja(cuerpo, BIZCOCHO, 12, 11.5f, 12, 0, 5.75f, 0);
		// Manchas del bizcocho en los 4 costados (en el frente, salvo donde va la cara).
		for (int i = 0; i < 26; i++) {
			int lado = (int) Math.floor(azar() * 4), u = (int) Math.floor(azar() * 11) - 5, v = (int) Math.floor(azar() * 8) + 1;
			int color = azar() < 0.6f ? BIZCOCHO_OSCURO : BIZCOCHO_CLARO, s = 1 + (int) Math.floor(azar() * 2);
			switch (lado) {
				case 0 -> {
					if (!(v > 3 && v < 10 && Math.abs(u) < 4)) caja(cuerpo, color, s, 1, 0.2f, u, v, 6.1f);
				}
				case 1 -> caja(cuerpo, color, s, 1, 0.2f, u, v, -6.1f);
				case 2 -> caja(cuerpo, color, 0.2f, 1, s, -6.1f, v, u);
				default -> caja(cuerpo, color, 0.2f, 1, s, 6.1f, v, u);
			}
		}
		// Glaseado arriba y chorreados.
		caja(cuerpo, GLASEADO, 12.4f, 1.5f, 12.4f, 0, 12.25f, 0);
		int[] frente = {1, 3, 2, 1, 4, 2, 1, 3, 1, 2, 4, 1}, atras = {2, 1, 3, 1, 2, 4, 1, 2, 3, 1, 1, 2};
		int[] izquierda = {1, 2, 4, 1, 3, 1, 2, 1, 3, 2, 1, 2}, derecha = {3, 1, 2, 2, 1, 4, 1, 3, 1, 2, 1, 1};
		for (int k = 0; k < 12; k++) {
			float u = k - 5.5f;
			caja(cuerpo, k % 3 != 0 ? GLASEADO : GLASEADO_SOMBRA, 1, frente[k], 0.4f, u, 11.5f - frente[k] / 2f, 6.2f);
			caja(cuerpo, GLASEADO, 1, atras[k], 0.4f, u, 11.5f - atras[k] / 2f, -6.2f);
			caja(cuerpo, GLASEADO, 0.4f, izquierda[k], 1, -6.2f, 11.5f - izquierda[k] / 2f, u);
			caja(cuerpo, GLASEADO, 0.4f, derecha[k], 1, 6.2f, 11.5f - derecha[k] / 2f, u);
		}
		// Confites rojos arriba.
		float[][] confites = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}, {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-4, 0}, {4, 0}, {0, 4}, {0, -4}};
		for (float[] c : confites) caja(cuerpo, CONFITE, 1, 0.4f, 1, c[0], 13.15f, c[1]);
		// Cara de creeper de chocolate.
		float[][] cara = {{-3, -1, 1, 3}, {1, 3, 1, 3}, {-1, 1, -2, 1}, {-2, -1, -3, -1}, {1, 2, -3, -1}};
		for (float[] c : cara) caja(cuerpo, CHOCOLATE, c[1] - c[0], c[3] - c[2], 0.3f, (c[0] + c[1]) / 2, 5.5f + (c[2] + c[3]) / 2, 6.1f);
		float[][] lugares = {{3.5f, 3.5f}, {-3.5f, 3.5f}, {3.5f, -3.5f}, {-3.5f, -3.5f}};
		for (int i = 0; i < 4; i++) {
			Nodo pata = nodo(raiz, lugares[i][0], 3, lugares[i][1]);
			caja(pata, BIZCOCHO_OSCURO, 4, 2, 4, 0, -1, 0);
			caja(pata, GLASEADO, 4.2f, 1, 4.2f, 0, -2.5f, 0);
			patas[i] = pata;
		}
	}

	@Override
	protected float giroDiseno() {
		return 0;   // la cara ya mira a +Z
	}

	private static float tiempo(CreeperPastelEntity bicho, float parcial) {
		return (bicho.tickCount + parcial) / 20f;
	}

	@Override
	protected float escala(CreeperPastelEntity bicho, float parcial) {
		float k = bicho.getSwelling(parcial);
		return k <= 0 ? 1 : 1 + 0.18f * k + 0.03f * Mth.sin(tiempo(bicho, parcial) * 30) * k;
	}

	@Override
	protected float blanco(CreeperPastelEntity bicho, float parcial) {
		float k = bicho.getSwelling(parcial);
		if (k <= 0) return 0;
		return Mth.sin(tiempo(bicho, parcial) * (8 + k * 30)) > 0 ? 0.3f + 0.7f * k : 0;
	}

	@Override
	protected void animar(CreeperPastelEntity bicho, float parcial) {
		float w = Mth.clamp(bicho.walkAnimation.speed(parcial) * 2.5f, 0, 1);
		if (bicho.getSwelling(parcial) > 0) w = 0;
		float f = tiempo(bicho, parcial) * 7;
		for (int i = 0; i < 4; i++) {
			float fase = f + (i == 0 || i == 3 ? 0 : Mth.PI);
			patas[i].rx = w * 0.5f * Mth.sin(fase);
			patas[i].y = 3 + w * 0.5f * Math.max(0, Mth.sin(fase));
		}
		cuerpo.y = 3 + w * 0.35f * Math.abs(Mth.sin(f));
		cuerpo.rz = w * 0.05f * Mth.sin(f);
	}

	@Override
	public ResourceLocation getTextureLocation(CreeperPastelEntity bicho) {
		return TEXTURA;
	}
}
