package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperResorteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Creeper Resorte: cabeza de creeper oscura sobre un resorte de metal, con una base negra. Al caminar el resorte
 * rebota y, cuando va a explotar, se aplasta. Medidas en píxeles, mirando a +Z.
 */
public class CreeperResorteRenderer extends CajasRenderer<CreeperResorteEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_resorte.png");
	private static final int CABEZA = 0, CLARO = 1, CARA = 2, METAL = 3, METAL_OSCURO = 4, BASE = 5;
	private static final int VUELTAS = 8;
	private static final float PASO = 1.6f, ABAJO = 2.5f;

	private final Nodo[] vueltas = new Nodo[VUELTAS];
	private final Nodo cabeza;

	public CreeperResorteRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.4f, 6);
		caja(raiz, BASE, 7, 1.5f, 7, 0, 1.5f, 0);
		for (int x = -1; x <= 1; x += 2) {
			for (int z = -1; z <= 1; z += 2) caja(raiz, BASE, 2, 1, 2, x * 3, 0.5f, z * 3);
		}
		for (int i = 0; i < VUELTAS; i++) {
			vueltas[i] = nodo(raiz, 0, ABAJO + i * PASO, 0);
			caja(vueltas[i], i % 2 == 0 ? METAL : METAL_OSCURO, 5, 0.9f, 5, i % 2 == 0 ? 0.3f : -0.3f, 0.45f, 0);
		}
		cabeza = nodo(raiz, 0, ABAJO + VUELTAS * PASO, 0);
		caja(cabeza, CABEZA, 8, 8, 8, 0, 4, 0);
		caja(cabeza, CLARO, 8.2f, 1, 8.2f, 0, 7.6f, 0);
		caja(cabeza, CLARO, 2, 1.5f, 8.2f, 2.5f, 1.2f, 0);
		// La cara de creeper.
		caja(cabeza, CARA, 2, 2, 0.3f, -2, 5, 4);
		caja(cabeza, CARA, 2, 2, 0.3f, 2, 5, 4);
		caja(cabeza, CARA, 2, 3, 0.3f, 0, 2.7f, 4);
		caja(cabeza, CARA, 1, 2.5f, 0.3f, -1.5f, 2, 4);
		caja(cabeza, CARA, 1, 2.5f, 0.3f, 1.5f, 2, 4);
	}

	@Override
	protected void animar(CreeperResorteEntity bicho, float parcial) {
		float t = (bicho.tickCount + parcial) * 0.5f;
		float anda = Mth.clamp(bicho.walkAnimation.speed(parcial) * 2.5f, 0, 1);
		// Rebota al caminar y se aplasta antes de explotar.
		float paso = PASO * (1 + Mth.sin(t) * 0.2f * anda) * (1 - bicho.getSwelling(parcial) * 0.55f);
		for (int i = 0; i < VUELTAS; i++) vueltas[i].y = ABAJO + i * paso;
		cabeza.y = ABAJO + VUELTAS * paso;
		cabeza.rz = Mth.sin(t) * 0.06f * anda;
	}

	/** Titila en blanco antes de explotar, como los creepers. */
	@Override
	protected float blanco(CreeperResorteEntity bicho, float parcial) {
		float s = bicho.getSwelling(parcial);
		return (int) (s * 10) % 2 == 0 ? 0 : Mth.clamp(s, 0.5f, 1f);
	}

	@Override
	protected float giroDiseno() {
		return 0;
	}

	@Override
	public ResourceLocation getTextureLocation(CreeperResorteEntity bicho) {
		return TEXTURA;
	}
}
