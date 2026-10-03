package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.ZombiePlataEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Zombie Plata del diseño: zombie verde oliva con manchas, consumido por Qumara: cristales violetas y
 * rosas en los hombros, la cabeza y un brazo, garras de cristal, una cicatriz en zigzag y ojos que
 * brillan (los cristales brillan en la oscuridad). Camina con los brazos estirados como un zombie, pero
 * más rápido, y al pegar sube los dos brazos.
 */
public class ZombiePlataRenderer extends CajasRenderer<ZombiePlataEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/zombie_plata.png");
	private static final int OLIVA = 0, OLIVA_OSCURO = 1, OLIVA_CLARO = 2, BRAZO = 3, ROSA = 4, VIOLETA = 5, CLARO = 6, OJO = 7;

	private final Nodo torso, cabeza;
	private final Nodo[] piernas = new Nodo[2], brazos = new Nodo[2];

	/** El mismo generador "al azar" del diseño, para que las manchas salgan igual. */
	private int semilla = 13;

	private float azar() {
		semilla = semilla * 1103515245 + 12345;
		return (semilla & 0xFFFFFFFFL) / 4294967296f;
	}

	public ZombiePlataRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, 0.5f, 8);
		Nodo cadera = nodo(raiz, 0, 12, 0);
		for (int i = 0; i < 2; i++) {
			Nodo pierna = nodo(cadera, i == 0 ? -2 : 2, 0, 0);
			caja(pierna, OLIVA_OSCURO, 4, 12, 4, 0, -6, 0);
			mugre(pierna, 4, 12, 4, 0, -6, 0, 6);
			piernas[i] = pierna;
		}
		torso = nodo(cadera, 0, 0, 0);
		caja(torso, OLIVA, 8, 12, 4, 0, 6, 0);
		mugre(torso, 8, 12, 4, 0, 6, 0, 14);
		float[][] banda = {{-3, 10}, {-1, 11}, {1, 10}, {3, 11}, {-2, 8}, {2, 8}};
		for (int i = 0; i < banda.length; i++) caja(torso, i % 2 == 1 ? ROSA : CLARO, 1.2f, 1.2f, 0.3f, banda[i][0], banda[i][1], 2.15f);
		cristal(torso, 2.2f, -4, 12, 0, 0.2f, 0.6f);
		cristal(torso, 1.8f, 4, 12, 1, -0.1f, -0.7f);
		for (int i = 0; i < 2; i++) {
			Nodo brazo = nodo(torso, i == 0 ? -6 : 6, 10, 0);
			caja(brazo, BRAZO, 4, 12, 4, 0, -4, 0);
			mugre(brazo, 4, 12, 4, 0, -4, 0, 5);
			if (i == 1) cristal(brazo, 1.4f, 2, -6, 0, 0, -1.2f);
			for (int c = -1; c <= 1; c++) caja(brazo, CLARO, 0.8f, 1.5f, 0.8f, c * 1.3f, -10.5f, 1.4f);
			brazos[i] = brazo;
		}
		cabeza = nodo(torso, 0, 12, 0);
		caja(cabeza, OLIVA, 8, 8, 8, 0, 4, 0);
		mugre(cabeza, 8, 8, 8, 0, 4, 0, 10);
		float[][] cicatriz = {{-4, 5}, {-3, 4}, {-2, 3}, {-1, 4}, {0, 5}, {1, 4}, {2, 3}, {3, 4}, {3.5f, 5}};
		for (int i = 0; i < cicatriz.length; i++) caja(cabeza, i % 2 == 1 ? ROSA : CLARO, 1, 1.4f, 0.3f, cicatriz[i][0] + 0.5f, cicatriz[i][1], 4.15f);
		caja(cabeza, OJO, 1.2f, 1, 0.3f, -2, 5.5f, 4.2f);
		caja(cabeza, OJO, 1.2f, 1, 0.3f, 2, 5.5f, 4.2f);
		caja(cabeza, OLIVA_OSCURO, 4, 1, 0.3f, 0, 1.8f, 4.15f);
		cristal(cabeza, 2.4f, 1, 8, -1, -0.2f, -0.35f);
		cristal(cabeza, 1.6f, -1.5f, 8, 1, 0.3f, 0.5f);
	}

	/** Un cristal de Qumara: prisma violeta, otro rosa inclinado y la punta clara. */
	private void cristal(Nodo padre, float s, float x, float y, float z, float rx, float rz) {
		Nodo n = nodo(padre, x, y, z);
		n.rx = rx;
		n.rz = rz;
		caja(n, VIOLETA, s * 0.7f, s * 1.6f, s * 0.7f, 0, s * 0.8f, 0);
		Nodo b = nodo(n, s * 0.35f, s * 0.5f, s * 0.2f);
		b.rz = -0.5f;
		caja(b, ROSA, s * 0.5f, s * 1.1f, s * 0.5f, 0, 0, 0);
		caja(n, CLARO, s * 0.35f, s * 0.5f, s * 0.35f, 0, s * 1.75f, 0);
	}

	/** Manchitas de mugre en los costados de una caja. */
	private void mugre(Nodo n, float w, float h, float d, float cx, float cy, float cz, int cuantas) {
		for (int i = 0; i < cuantas; i++) {
			int lado = (int) Math.floor(azar() * 4);
			int color = azar() < 0.5f ? OLIVA_OSCURO : OLIVA_CLARO;
			float ancho = lado < 2 ? w : d;
			float u = (float) Math.floor(azar() * ancho) - ancho / 2 + 0.5f, v = (float) Math.floor(azar() * h) - h / 2 + 0.5f;
			switch (lado) {
				case 0 -> caja(n, color, 1, 1, 0.2f, cx + u, cy + v, cz + d / 2 + 0.1f);
				case 1 -> caja(n, color, 1, 1, 0.2f, cx + u, cy + v, cz - d / 2 - 0.1f);
				case 2 -> caja(n, color, 0.2f, 1, 1, cx - w / 2 - 0.1f, cy + v, cz + u);
				default -> caja(n, color, 0.2f, 1, 1, cx + w / 2 + 0.1f, cy + v, cz + u);
			}
		}
	}

	@Override
	protected float giroDiseno() {
		return 0;   // la cara ya mira a +Z
	}

	@Override
	protected boolean brilla(int color) {
		return color >= ROSA;   // los cristales y los ojos
	}

	@Override
	protected void animar(ZombiePlataEntity zombie, float parcial) {
		float t = (zombie.tickCount + parcial) / 20f;
		float w = Mth.clamp(zombie.walkAnimation.speed(parcial) * 2f, 0, 1);
		float f = zombie.walkAnimation.position(parcial) * 0.6662f * 1.4f;
		piernas[0].rx = 0.8f * w * Mth.sin(f);
		piernas[1].rx = -0.8f * w * Mth.sin(f);
		// Brazos estirados hacia adelante; al pegar, suben los dos.
		float golpe = Mth.sin(zombie.getAttackAnim(parcial) * Mth.PI);
		for (int i = 0; i < 2; i++) {
			float lado = i == 0 ? 1 : -1;
			brazos[i].rx = -Mth.HALF_PI - 0.9f * golpe + 0.05f * Mth.sin(t * 3 + i);
			brazos[i].rz = lado * 0.05f * Mth.sin(t * 2 + i);
		}
		torso.rx = 0.12f * golpe;
		// La cabeza mira hacia donde mira el zombie.
		cabeza.ry = -Mth.DEG_TO_RAD * Mth.wrapDegrees(Mth.rotLerp(parcial, zombie.yHeadRotO, zombie.yHeadRot)
				- Mth.rotLerp(parcial, zombie.yBodyRotO, zombie.yBodyRot));
		cabeza.rx = Mth.DEG_TO_RAD * zombie.getViewXRot(parcial);
	}

	@Override
	public ResourceLocation getTextureLocation(ZombiePlataEntity zombie) {
		return TEXTURA;
	}
}
