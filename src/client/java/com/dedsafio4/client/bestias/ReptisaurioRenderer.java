package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.ReptisaurioEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * El Reptisaurio Salvaje del diseño (Reptisaurio.html): cuerpo beige, cabeza con colmillos, 4 patas y el
 * caparazón de 12 columnas marrones inclinadas (con las mismas medidas "al azar" del diseño).
 * Camina, muerde (se echa atrás y salta adelante), se entierra temblando hasta que solo asoman las
 * puntas del caparazón, acecha bajo tierra y sale de un salto.
 */
public class ReptisaurioRenderer extends CajasRenderer<ReptisaurioEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/reptisaurio.png");
	private static final int MARRON = 0, MARRON_CLARO = 1, MARRON_OSCURO = 2, PIEL = 3, PIEL_OSCURA = 4, PATAS = 5,
			COLMILLO = 6, OJO = 7, BRILLO = 8;

	private final Nodo cabeza;
	private final Nodo[] patas = new Nodo[4];
	private final List<Nodo> columnas = new ArrayList<>();
	private final List<float[]> columnasBase = new ArrayList<>();   // {giro x, fase}

	/** El mismo generador de números "al azar" del diseño, para que el caparazón salga igual. */
	private int semilla = 11;

	private float azar() {
		semilla = semilla * 1103515245 + 12345;
		return (semilla & 0xFFFFFFFFL) / 4294967296f;
	}

	public ReptisaurioRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.45f, 9);
		Nodo cuerpo = nodo(raiz, 0, 7, 0);
		caja(cuerpo, PIEL, 10, 4, 12, 0, 0, 0);
		caja(cuerpo, PIEL_OSCURA, 9, 0.4f, 11, 0, -2.2f, 0);
		cabeza = nodo(cuerpo, 0, 0, 6);
		caja(cabeza, PIEL, 8, 5, 4, 0, 0, 2);
		caja(cabeza, PIEL_OSCURA, 5, 2, 1, 0, -1, 4.5f);
		for (int s : new int[]{1, -1}) {
			caja(cabeza, OJO, 2, 2, 0.3f, s * 2, 1, 4.1f);
			caja(cabeza, BRILLO, 0.7f, 0.7f, 0.2f, s * 2 - 0.5f, 1.6f, 4.3f);
			caja(cabeza, COLMILLO, 1, 3, 1, s * 1.2f, -3, 4.2f);
		}
		Nodo caparazon = nodo(cuerpo, 0, 2, -0.5f);
		int[] colores = {MARRON, MARRON_CLARO, MARRON_OSCURO};
		for (int gx = -1; gx <= 1; gx++) {
			for (int gz = -1; gz <= 2; gz++) {
				float alto = 7 + azar() * 7 + (gx == 0 ? 3 : 0) + (gz <= 0 ? 2 : 0), ancho = 2.6f + azar() * 1.2f;
				Nodo col = nodo(caparazon, gx * 3.2f + (azar() - 0.5f), 0, gz * 3 - 1.5f + (azar() - 0.5f));
				col.rz = -gx * 0.22f + (azar() - 0.5f) * 0.15f;
				col.rx = -0.25f - gz * 0.06f + (azar() - 0.5f) * 0.15f;
				int color = colores[(int) Math.floor(azar() * 3)];
				caja(col, color, ancho, alto, ancho, 0, alto / 2, 0);
				caja(col, MARRON_OSCURO, ancho * 0.6f, 1.2f, ancho * 0.6f, 0, alto + 0.6f, 0);
				columnas.add(col);
				columnasBase.add(new float[]{col.rx, azar() * 6});
			}
		}
		float[][] lugares = {{5.5f, 4.5f}, {-5.5f, 4.5f}, {5.5f, -4.5f}, {-5.5f, -4.5f}};
		for (int i = 0; i < 4; i++) {
			Nodo pata = nodo(raiz, lugares[i][0], 7, lugares[i][1]);
			caja(pata, PATAS, 3.4f, 7, 3.4f, 0, -3.5f, 0);
			patas[i] = pata;
		}
	}

	@Override
	protected float giroDiseno() {
		return 0;   // la cara ya mira a +Z
	}

	private static float facil(float x) {
		x = Mth.clamp(x, 0, 1);
		return x < 0.5f ? 2 * x * x : 1 - (float) Math.pow(-2 * x + 2, 2) / 2;
	}

	/** El mordisco: se echa atrás 1,5 píxeles, salta 6 adelante y vuelve. */
	private static float embestida(float k) {
		return k < 0.3f ? -1.5f * facil(k / 0.3f) : k < 0.5f ? -1.5f + 7.5f * facil((k - 0.3f) / 0.2f) : 6 * (1 - facil((k - 0.5f) / 0.5f));
	}

	@Override
	protected void animar(ReptisaurioEntity bicho, float parcial) {
		float t = (bicho.tickCount + parcial) / 20f;
		float camina = Mth.clamp(bicho.walkAnimation.speed(parcial) * 2.5f, 0, 1);
		float y = 0, z = 0, temblor = 0, mordida = 0, paso = 0.4f + camina;
		float te = bicho.ticksEstado + parcial;
		switch (bicho.estado()) {
			case ReptisaurioEntity.ENTERRANDOSE -> {
				y = -11 * facil(te / ReptisaurioEntity.TIEMPO_ENTERRARSE);
				temblor = 1;
				paso = 2;
			}
			case ReptisaurioEntity.ACECHANDO -> {
				y = -11;
				temblor = 0.4f;
				paso = 2.5f;
			}
			case ReptisaurioEntity.SALIENDO -> {
				float q = Mth.clamp(te / ReptisaurioEntity.TIEMPO_SALIR, 0, 1);
				y = -11 + 11 * facil(q) + 3 * Mth.sin(q * Mth.PI);
			}
			default -> {
			}
		}
		if (bicho.ataque >= 0) {
			float k = (bicho.ataque + parcial) / ReptisaurioEntity.TIEMPO_ATAQUE;
			z = embestida(k);
			mordida = k > 0.3f && k < 0.6f ? 1 : 0;
			paso = 1.4f;
		}
		float f = t * 6 * paso;
		raiz.y = y + 0.3f * Math.abs(Mth.sin(f));
		raiz.z = z;
		raiz.rz = temblor * 0.06f * Mth.sin(t * 40);
		for (int i = 0; i < 4; i++) patas[i].rx = 0.45f * Mth.sin(f + (i == 0 || i == 3 ? 0 : Mth.PI));
		for (int i = 0; i < columnas.size(); i++) {
			float[] base = columnasBase.get(i);
			columnas.get(i).rx = base[0] + 0.03f * Mth.sin(t * 2 + base[1]);
		}
		cabeza.rx = -0.35f * mordida + 0.04f * Mth.sin(t * 1.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(ReptisaurioEntity bicho) {
		return TEXTURA;
	}
}
