package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperAzaleaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Creeper Raíz de Azalea del diseño: bloque de hojas con manchas, cara de creeper, la flor de azalea
 * arriba (con pétalos que chorrean por los costados y 4 pétalos abiertos que "respiran") y 4 patitas con
 * suela de raíz. Al activarse titila, se infla y la flor se cierra.
 */
public class CreeperAzaleaRenderer extends CajasRenderer<CreeperAzaleaEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_azalea.png");
	private static final int HOJA = 0, HOJA_OSCURA = 1, HOJA_CLARA = 2, PETALO = 3, PETALO_ROSA = 4, CENTRO = 5, CARA = 6, RAIZ = 7;

	private final Nodo cuerpo, flor;
	private final Nodo[] patas = new Nodo[4], petalos = new Nodo[4];

	/** El mismo generador "al azar" del diseño, para que las manchas salgan igual. */
	private int semilla = 9;

	private float azar() {
		semilla = semilla * 1103515245 + 12345;
		return (semilla & 0xFFFFFFFFL) / 4294967296f;
	}

	public CreeperAzaleaRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.5f, 8);
		cuerpo = nodo(raiz, 0, 4, 0);
		caja(cuerpo, HOJA, 12, 15, 12, 0, 7.5f, 0);
		// 40 manchas en los costados (en el frente no, ahí va la cara).
		for (int i = 0; i < 40; i++) {
			int lado = (int) Math.floor(azar() * 4);
			int color = azar() < 0.55f ? HOJA_OSCURA : HOJA_CLARA, s = 1 + (int) Math.floor(azar() * 2);
			float u = (float) Math.floor(azar() * 12) - 6 + 0.5f, v = (float) Math.floor(azar() * 15) - 7.5f + 0.5f;
			switch (lado) {
				case 1 -> caja(cuerpo, color, s, 1, 0.2f, u, 7.5f + v, -6.1f);
				case 2 -> caja(cuerpo, color, 0.2f, 1, s, -6.1f, 7.5f + v, u);
				case 3 -> caja(cuerpo, color, 0.2f, 1, s, 6.1f, 7.5f + v, u);
				default -> {
				}
			}
		}
		float[][] cara = {{-3, -1, 1, 3}, {1, 3, 1, 3}, {-1, 1, -2, 1}, {-2, -1, -3, -1}, {1, 2, -3, -1}};
		for (float[] c : cara) {
			caja(cuerpo, CARA, (c[1] - c[0]) * 1.25f, (c[3] - c[2]) * 1.25f, 0.3f, (c[0] + c[1]) / 2 * 1.25f,
					7 + (c[2] + c[3]) / 2 * 1.25f, 6.1f);
		}
		// La flor.
		flor = nodo(cuerpo, 0, 0, 0);
		caja(flor, PETALO_ROSA, 12.4f, 1.5f, 12.4f, 0, 15.75f, 0);
		int[] l1 = {1, 3, 2, 1, 4, 2, 1, 3, 1, 2, 4, 1}, l2 = {2, 1, 3, 1, 2, 4, 1, 2, 3, 1, 1, 2};
		int[] l3 = {1, 2, 4, 1, 3, 1, 2, 1, 3, 2, 1, 2}, l4 = {3, 1, 2, 2, 1, 4, 1, 3, 1, 2, 1, 1};
		for (int k = 0; k < 12; k++) {
			float u = k - 5.5f;
			boolean impar = k % 2 == 1;
			caja(flor, impar ? PETALO : PETALO_ROSA, 1, l1[k], 0.4f, u, 15 - l1[k] / 2f, 6.2f);
			caja(flor, impar ? PETALO_ROSA : PETALO, 1, l2[k], 0.4f, u, 15 - l2[k] / 2f, -6.2f);
			caja(flor, impar ? PETALO : PETALO_ROSA, 0.4f, l3[k], 1, -6.2f, 15 - l3[k] / 2f, u);
			caja(flor, impar ? PETALO_ROSA : PETALO, 0.4f, l4[k], 1, 6.2f, 15 - l4[k] / 2f, u);
		}
		caja(flor, CENTRO, 3, 1, 3, 0, 17, 0);
		for (int i = 0; i < 4; i++) {
			Nodo giro = nodo(flor, 0, 16.5f, 0);
			giro.ry = i * Mth.HALF_PI + Mth.PI / 4;
			Nodo petalo = nodo(giro, 1.5f, 0, 0);
			caja(petalo, PETALO, 6, 1, 5, 3, 0.5f, 0);
			caja(petalo, PETALO_ROSA, 1.5f, 1.1f, 4, 5.8f, 0.5f, 0);
			petalos[i] = petalo;
		}
		float[][] lugares = {{3.5f, 3.5f}, {-3.5f, 3.5f}, {3.5f, -3.5f}, {-3.5f, -3.5f}};
		for (int i = 0; i < 4; i++) {
			Nodo pata = nodo(raiz, lugares[i][0], 4, lugares[i][1]);
			caja(pata, HOJA_OSCURA, 4, 3, 4, 0, -1.5f, 0);
			caja(pata, RAIZ, 4.2f, 1, 4.2f, 0, -3.5f, 0);
			patas[i] = pata;
		}
	}

	@Override
	protected float giroDiseno() {
		return 0;   // la cara ya mira a +Z
	}

	private static float tiempo(CreeperAzaleaEntity bicho, float parcial) {
		return (bicho.tickCount + parcial) / 20f;
	}

	@Override
	protected float escala(CreeperAzaleaEntity bicho, float parcial) {
		return 1 + 0.18f * bicho.getSwelling(parcial);
	}

	@Override
	protected float blanco(CreeperAzaleaEntity bicho, float parcial) {
		float k = bicho.getSwelling(parcial);
		if (k <= 0) return 0;
		return Mth.sin(tiempo(bicho, parcial) * (8 + k * 30)) > 0 ? 0.3f + 0.7f * k : 0;
	}

	@Override
	protected void animar(CreeperAzaleaEntity bicho, float parcial) {
		float t = tiempo(bicho, parcial), k = bicho.getSwelling(parcial);
		float w = k > 0 ? 0 : Mth.clamp(bicho.walkAnimation.speed(parcial) * 2.5f, 0, 1);
		float f = t * 6;
		for (int i = 0; i < 4; i++) {
			float fase = f + (i == 0 || i == 3 ? 0 : Mth.PI);
			patas[i].rx = w * 0.5f * Mth.sin(fase);
			patas[i].y = 4 + w * 0.5f * Math.max(0, Mth.sin(fase));
		}
		cuerpo.y = 4 + w * 0.35f * Math.abs(Mth.sin(f));
		cuerpo.rz = w * 0.05f * Mth.sin(f);
		flor.ry = 0.08f * Mth.sin(t * 0.8f);
		// Los pétalos respiran; al activarse, la flor se cierra.
		float abierta = k > 0 ? 0.55f - 0.9f * k : 0.55f + 0.08f * Mth.sin(t * 1.5f);
		for (int i = 0; i < 4; i++) petalos[i].rz = abierta * 0.75f + 0.05f * Mth.sin(t * 2 + i);
	}

	@Override
	public ResourceLocation getTextureLocation(CreeperAzaleaEntity bicho) {
		return TEXTURA;
	}
}
