package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperNuclearEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Creeper Nuclear con el diseño "Bomba andante": carcasa amarilla con bandas oscuras, cara de creeper,
 * botón rojo arriba y 4 patitas. Camina bamboleándose; al activarse baja el botón, se infla temblando y
 * titila en blanco cada vez más rápido.
 */
public class CreeperNuclearRenderer extends CajasRenderer<CreeperNuclearEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_nuclear.png");
	private static final int AMARILLO = 0, AMARILLO_OSCURO = 1, BANDA = 2, ROJO = 3, CARA = 4, SUELA = 5;

	private final Nodo cuerpo, boton;
	private final Nodo[] patas = new Nodo[4];

	public CreeperNuclearRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.5f, 6);
		cuerpo = nodo(raiz, 0, 3, 0);
		caja(cuerpo, AMARILLO, 12, 12, 12, 0, 6, 0);
		float[][] parches = {{-3, 2}, {2, -4}, {4, 3}, {-4, -2}};
		for (float[] p : parches) {
			caja(cuerpo, AMARILLO_OSCURO, 0.2f, 2, 2, -6.05f, 6 + p[0] * 0.6f, p[1]);
			caja(cuerpo, AMARILLO_OSCURO, 0.2f, 2, 2, 6.05f, 6 + p[1] * 0.6f, p[0]);
		}
		caja(cuerpo, BANDA, 12.4f, 2, 12.4f, 0, 10.5f, 0);
		caja(cuerpo, BANDA, 12.4f, 2, 12.4f, 0, 1.5f, 0);
		caja(cuerpo, BANDA, 10, 1, 10, 0, 12.5f, 0);
		boton = nodo(cuerpo, 0, 13, 0);
		caja(boton, ROJO, 8, 3, 8, 0, 1.5f, 0);
		// La cara (en +Z): ojos, boca y los costados de la boca.
		float[][] cara = {{-3, -1, 1, 3}, {1, 3, 1, 3}, {-1, 1, -2, 1}, {-2, -1, -3, -1}, {1, 2, -3, -1}};
		for (float[] c : cara) caja(cuerpo, CARA, c[1] - c[0], c[3] - c[2], 0.3f, (c[0] + c[1]) / 2, 6 + (c[2] + c[3]) / 2, 6.1f);
		float[][] lugares = {{3.5f, 3.5f}, {-3.5f, 3.5f}, {3.5f, -3.5f}, {-3.5f, -3.5f}};
		for (int i = 0; i < 4; i++) {
			Nodo pata = nodo(raiz, lugares[i][0], 3, lugares[i][1]);
			caja(pata, AMARILLO, 4, 2, 4, 0, -1, 0);
			caja(pata, SUELA, 4.2f, 1, 4.2f, 0, -2.5f, 0);
			patas[i] = pata;
		}
	}

	@Override
	protected float giroDiseno() {
		return 0;   // la cara ya mira a +Z
	}

	private static float tiempo(CreeperNuclearEntity bicho, float parcial) {
		return (bicho.tickCount + parcial) / 20f;
	}

	@Override
	protected float escala(CreeperNuclearEntity bicho, float parcial) {
		float k = bicho.getSwelling(parcial);
		return k <= 0 ? 1 : 1 + 0.18f * k + 0.04f * Mth.sin(tiempo(bicho, parcial) * 30) * k;
	}

	@Override
	protected float blanco(CreeperNuclearEntity bicho, float parcial) {
		float k = bicho.getSwelling(parcial);
		if (k <= 0) return 0;
		return Mth.sin(tiempo(bicho, parcial) * (6 + k * 30)) > 0 ? 0.3f + 0.7f * k : 0;
	}

	@Override
	protected void animar(CreeperNuclearEntity bicho, float parcial) {
		float t = tiempo(bicho, parcial);
		float w = Mth.clamp(bicho.walkAnimation.speed(parcial) * 2.5f, 0, 1);
		float f = t * 7;
		for (int i = 0; i < 4; i++) {
			float fase = f + (i == 0 || i == 3 ? 0 : Mth.PI);
			patas[i].rx = w * 0.5f * Mth.sin(fase);
			patas[i].y = 3 + w * 0.6f * Math.max(0, Mth.sin(fase));
		}
		cuerpo.y = 3 + w * 0.4f * Math.abs(Mth.sin(f));
		cuerpo.rz = w * 0.05f * Mth.sin(f);
		// Al activarse, el botón baja 2 píxeles.
		boton.y = 13 - 2 * Mth.clamp(bicho.getSwelling(parcial) / 0.15f, 0, 1);
	}

	@Override
	public ResourceLocation getTextureLocation(CreeperNuclearEntity bicho) {
		return TEXTURA;
	}
}
