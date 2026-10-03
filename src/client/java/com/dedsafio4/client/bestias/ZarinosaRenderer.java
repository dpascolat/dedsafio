package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.ZarinosaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * La Zarinosa Espigüeya del diseño: lagartija verde con rayas amarillas, púas beige, ojos rojos y la cola
 * curvada hacia arriba que termina en una espiga. Camina, salta en arco con la boca abierta y las patas
 * abiertas, queda abrazada a la cabeza mordiendo, y sale despedida dando una vuelta.
 * Es astuta: a más de 8 bloques no se la ve.
 */
public class ZarinosaRenderer extends CajasRenderer<ZarinosaEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/zarinosa.png");
	private static final int VERDE = 0, VERDE_OSCURO = 1, AMARILLO = 2, BEIGE = 3, OJO = 4, BOCA = 5;

	private final Nodo cabeza;
	private final Nodo[] cola = new Nodo[5], patas = new Nodo[4];
	private final float[] colaBase = {0.25f, 0.35f, 0.45f, 0.55f, 0.4f};

	public ZarinosaRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.35f, 6);
		Nodo cuerpo = nodo(raiz, 0, 6, 0);
		caja(cuerpo, VERDE, 10, 6, 7, 0, 0, 0);
		caja(cuerpo, VERDE_OSCURO, 9, 0.4f, 6, 0, -3.2f, 0);
		float[] xs = {-4, -2, 0, 2, 4};
		for (int i = 0; i < 5; i++) {
			float x = xs[i], corrido = i % 2 == 1 ? 0.5f : -0.5f;
			caja(cuerpo, AMARILLO, 1, 0.3f, 7.2f, x, 3.1f, 0);
			caja(cuerpo, AMARILLO, 1, 4, 0.3f, x + corrido, 0.5f, 3.6f);
			caja(cuerpo, AMARILLO, 1, 4, 0.3f, x - corrido, 0.5f, -3.6f);
		}
		for (float x : new float[]{-3.5f, -1, 1.5f, 4}) {
			Nodo pua = nodo(cuerpo, x, 3, 0);
			pua.rz = 0.4f;
			caja(pua, BEIGE, 0.8f, 2, 0.8f, 0, 1, 0);
		}
		cabeza = nodo(cuerpo, 5, 1, 0);
		caja(cabeza, VERDE, 5, 5, 6, 2.5f, 0, 0);
		caja(cabeza, VERDE, 3, 3, 5, 6.5f, -0.5f, 0);
		caja(cabeza, AMARILLO, 1, 0.3f, 6.2f, 2, 2.6f, 0);
		caja(cabeza, OJO, 1.4f, 1, 0.3f, 4, 1, 3.05f);
		caja(cabeza, OJO, 1.4f, 1, 0.3f, 4, 1, -3.05f);
		caja(cabeza, BOCA, 3.2f, 0.4f, 5.2f, 6.5f, -1.6f, 0);
		for (int s : new int[]{-1, 1}) {
			Nodo pua = nodo(cabeza, 2, 2.5f, s * 2);
			pua.rz = 0.5f;
			pua.rx = -s * 0.4f;
			caja(pua, BEIGE, 0.8f, 2, 0.8f, 0, 1, 0);
		}
		// Cola curvada hacia arriba, con la espiga en la punta.
		float[][] segmentos = {{4, 4, 4}, {4, 3.4f, 3.4f}, {4, 3, 3}, {3.5f, 2.4f, 2.4f}, {3, 2, 2}};
		Nodo padre = cuerpo;
		float px = -5;
		for (int i = 0; i < 5; i++) {
			float largo = segmentos[i][0];
			Nodo seg = nodo(padre, px, i == 0 ? 1 : 0, 0);
			caja(seg, i % 2 == 1 ? AMARILLO : VERDE, largo, segmentos[i][1], segmentos[i][2], -largo / 2, 0, 0);
			cola[i] = seg;
			padre = seg;
			px = -largo;
		}
		Nodo punta = nodo(padre, px, 0, 0);
		float[][] espiga = {{0, 0}, {0.6f, 0.5f}, {-0.6f, -0.5f}, {0.6f, -0.5f}, {-0.6f, 0.5f}};
		for (float[] e : espiga) {
			Nodo barra = nodo(punta, 0, 0, 0);
			barra.rz = 0.2f + e[0];
			barra.rx = e[1];
			caja(barra, BEIGE, 3.5f, 0.9f, 0.9f, -1.75f, 0, 0);
		}
		float[][] lugares = {{3.5f, 3.8f}, {-3.5f, 3.8f}, {3.5f, -3.8f}, {-3.5f, -3.8f}};
		for (int i = 0; i < 4; i++) {
			Nodo pata = nodo(raiz, lugares[i][0], 4, lugares[i][1]);
			caja(pata, VERDE_OSCURO, 2.4f, 4, 2.4f, 0, -2, 0);
			caja(pata, BEIGE, 3, 1, 3, 0.4f, -3.6f, 0);
			patas[i] = pata;
		}
	}

	private static float facil(float x) {
		x = Mth.clamp(x, 0, 1);
		return x < 0.5f ? 2 * x * x : 1 - (float) Math.pow(-2 * x + 2, 2) / 2;
	}

	@Override
	protected void animar(ZarinosaEntity bicho, float parcial) {
		float t = (bicho.tickCount + parcial) / 20f, te = bicho.ticksEstado + parcial;
		float camina = 0, abrazo = 0, boca = 0, inclinacion = 0, y = 0;
		switch (bicho.estado()) {
			case ZarinosaEntity.SALTANDO -> {
				float k = Mth.clamp(te / 12f, 0, 1);
				inclinacion = 0.5f * Mth.cos(Mth.PI * k);
				abrazo = k;
				boca = 1;
			}
			case ZarinosaEntity.PRENDIDA -> {
				y = -2.5f + 0.4f * Mth.sin(t * 8);
				abrazo = 1;
				boca = 0.6f + 0.4f * Mth.sin(t * 8);
			}
			case ZarinosaEntity.DESPEDIDA -> inclinacion = -Mth.TWO_PI * facil(te / ZarinosaEntity.TIEMPO_DESPEDIDA);
			default -> camina = Mth.clamp(bicho.walkAnimation.speed(parcial) * 2.5f, 0, 1);
		}
		raiz.y = y;
		raiz.rz = inclinacion;
		float f = t * 9;
		for (int i = 0; i < 4; i++) {
			float lado = i < 2 ? 1 : -1;
			patas[i].rz = camina * 0.5f * Mth.sin(f + (i == 0 || i == 3 ? 0 : Mth.PI));
			patas[i].rx = -lado * 0.9f * abrazo;
		}
		for (int i = 0; i < 5; i++) {
			cola[i].rz = colaBase[i] + 0.08f * Mth.sin(t * 3 - i * 0.6f);
			cola[i].ry = 0.12f * Mth.sin(t * 2 - i * 0.5f);
		}
		cabeza.rz = -0.25f * boca;
	}

	/** Astuta: a más de 8 bloques no se la ve (salvo cuando está prendida a alguien). */
	@Override
	public boolean shouldRender(ZarinosaEntity bicho, Frustum frustum, double x, double y, double z) {
		if (bicho.estado() != ZarinosaEntity.PRENDIDA
				&& Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(bicho.position()) > 64) {
			return false;
		}
		return super.shouldRender(bicho, frustum, x, y, z);
	}

	@Override
	public ResourceLocation getTextureLocation(ZarinosaEntity bicho) {
		return TEXTURA;
	}
}
