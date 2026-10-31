package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperVisceralEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Creeper Visceral: cabeza de carne roja rodeada de púas, dos bultos a los lados, una columna de tripas moradas
 * torcida y cuatro patas moradas abiertas, como en el dibujo del cartel. Medidas en píxeles, mirando a +Z.
 */
public class CreeperVisceralRenderer extends CajasRenderer<CreeperVisceralEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_visceral.png");
	private static final int CARNE = 0, CLARO = 1, TRIPA = 2, TRIPA_CLARA = 3, PUA = 4, CARA = 5;
	private static final int ANILLOS = 7;
	private static final float PASO_TRIPA = 2f, CADERA = 9;

	private final Nodo cabeza, pata1, pata2, pata3, pata4;

	public CreeperVisceralRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.5f, 6);
		Nodo cuerpo = nodo(raiz, 0, CADERA, 0);
		caja(cuerpo, CARNE, 7, 2.5f, 6, 0, 0, 0);
		caja(cuerpo, CLARO, 4, 1, 3, -5, -0.5f, -3);
		caja(cuerpo, CARNE, 3, 1.5f, 3, -7, -1.5f, -4);
		// La columna de tripas: anillos corridos para los costados, un morado y un rojo.
		for (int i = 0; i < ANILLOS; i++) {
			float lado = Mth.sin(i * 1.1f) * 1.6f;
			caja(cuerpo, i % 3 == 2 ? CARNE : i % 2 == 0 ? TRIPA : TRIPA_CLARA, 4.5f, PASO_TRIPA + 0.2f, 4.5f, lado, 2 + i * PASO_TRIPA, Mth.cos(i * 1.1f) * 0.8f);
		}
		// Los brazos cortitos, con un bulto de carne en la punta.
		for (int x = -1; x <= 1; x += 2) {
			caja(cuerpo, TRIPA, 3.5f, 1.5f, 1.5f, x * 3.2f, 11, 0);
			caja(cuerpo, TRIPA, 1.5f, 3, 1.5f, x * 4.5f, 12.5f, 0);
			caja(cuerpo, CARNE, 4, 4, 4, x * 5.5f, 15.5f, 0);
			caja(cuerpo, CLARO, 2, 1, 4.2f, x * 5.5f, 16.5f, 0);
		}
		pata1 = pata(cuerpo, -1, 1);
		pata2 = pata(cuerpo, 1, 1);
		pata3 = pata(cuerpo, -1, -1);
		pata4 = pata(cuerpo, 1, -1);

		cabeza = nodo(cuerpo, 0, 2 + ANILLOS * PASO_TRIPA, 0);
		caja(cabeza, CARNE, 8, 8, 8, 0, 4, 0);
		caja(cabeza, CLARO, 8.2f, 2, 8.2f, 0, 7, 0);
		caja(cabeza, CLARO, 3, 2, 8.2f, 2, 3, 0);
		// La cara de creeper.
		caja(cabeza, CARA, 2, 2, 0.3f, -2, 5, 4);
		caja(cabeza, CARA, 2, 2, 0.3f, 2, 5, 4);
		caja(cabeza, CARA, 2, 3, 0.3f, 0, 2.7f, 4);
		caja(cabeza, CARA, 1, 2.5f, 0.3f, -1.5f, 2, 4);
		caja(cabeza, CARA, 1, 2.5f, 0.3f, 1.5f, 2, 4);
		// El marco de púas alrededor de la cabeza.
		for (int i = -1; i <= 1; i++) {
			caja(cabeza, PUA, 0.5f, 3, 0.5f, i * 3f, 9.5f, 3.5f);
			caja(cabeza, PUA, 0.5f, 3, 0.5f, i * 3f, 9.5f, -3.5f);
			caja(cabeza, PUA, 3, 0.5f, 0.5f, 5.5f, 4 + i * 3f, 0);
			caja(cabeza, PUA, 3, 0.5f, 0.5f, -5.5f, 4 + i * 3f, 0);
		}
		caja(cabeza, PUA, 8, 0.5f, 0.5f, 0, 11, 3.5f);
		caja(cabeza, PUA, 8, 0.5f, 0.5f, 0, 11, -3.5f);
		caja(cabeza, PUA, 0.5f, 8.5f, 0.5f, 7, 4, 0);
		caja(cabeza, PUA, 0.5f, 8.5f, 0.5f, -7, 4, 0);
	}

	/** Una pata: sale de la cadera para afuera y baja hasta el piso. */
	private Nodo pata(Nodo cuerpo, int x, int z) {
		Nodo p = nodo(cuerpo, x * 3, 0, z * 2);
		caja(p, TRIPA, 5, 2, 2, x * 2.5f, 0, 0);
		caja(p, TRIPA, 2, CADERA + 1, 2, x * 5, -(CADERA + 1) / 2 + 0.5f, 0);
		caja(p, TRIPA_CLARA, 2.4f, 1.2f, 2.4f, x * 5, -4, 0);
		return p;
	}

	@Override
	protected void animar(CreeperVisceralEntity bicho, float parcial) {
		float t = bicho.walkAnimation.position(parcial) * 0.9f, anda = Mth.clamp(bicho.walkAnimation.speed(parcial) * 1.5f, 0, 1);
		float paso = Mth.sin(t) * 0.5f * anda;
		pata1.rx = paso;
		pata4.rx = paso;
		pata2.rx = -paso;
		pata3.rx = -paso;
		float s = bicho.getSwelling(parcial);
		cabeza.rz = Mth.sin((bicho.tickCount + parcial) * 0.15f) * 0.05f + Mth.sin((bicho.tickCount + parcial) * 2f) * s * 0.15f;
		cabeza.ry = (bicho.yBodyRot - bicho.yHeadRot) * Mth.DEG_TO_RAD;
	}

	/** Titila en blanco antes de explotar, como los creepers. */
	@Override
	protected float blanco(CreeperVisceralEntity bicho, float parcial) {
		float s = bicho.getSwelling(parcial);
		return (int) (s * 10) % 2 == 0 ? 0 : Mth.clamp(s, 0.5f, 1f);
	}

	@Override
	protected float giroDiseno() {
		return 0;
	}

	@Override
	public ResourceLocation getTextureLocation(CreeperVisceralEntity bicho) {
		return TEXTURA;
	}
}
