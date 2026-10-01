package com.dedsafio4.client.qumara;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.qumara.QumaraAgarre;
import com.dedsafio4.qumara.QumaraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Qumara, la Flor Mutante (40 bloques de alto), como en el diseño: cajas con pivotes, en bloques
 * (y arriba, +z la cara), con la textura de cada material repetida una vez por bloque y la sombra
 * falsa por vértice (más oscuro abajo de cada caja). Animaciones: quieta, giro (barrido), grito,
 * transformación (la rosa se abre), debilitada (celeste y marchita) y derrotada (se desploma y queda).
 */
public class QumaraRenderer extends EntityRenderer<QumaraEntity> {
	// Materiales (una textura por material, en textures/entity/qumara/).
	private static final String[] NOMBRES = {"petalo", "petalo_oscuro", "morado", "centro", "lengua", "tallo", "hoja",
			"enredadera", "raiz", "raiz_oscura", "musgo", "tierra", "dientes", "boca", "negro", "espina", "ojo", "rosa", "rosa_oscura"};
	private static final int PETALO = 0, PETALO_OSC = 1, MORADO = 2, CENTRO = 3, LENGUA = 4, TALLO = 5, HOJA = 6, ENREDADERA = 7,
			RAIZ = 8, RAIZ_OSC = 9, MUSGO = 10, TIERRA = 11, DIENTES = 12, BOCA = 13, NEGRO = 14, ESPINA = 15, OJO = 16,
			ROSA = 17, ROSA_OSC = 18;
	private static final RenderType[] TIPOS = new RenderType[NOMBRES.length];

	static {
		for (int i = 0; i < NOMBRES.length; i++) {
			TIPOS[i] = RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/qumara/" + NOMBRES[i] + ".png"));
		}
	}

	private static final class Caja {
		final float w, h, d, x, y, z, rx, ry, rz;
		final int mat;
		/** Índice entre las partes "mutantes" (que aparecen al transformarse); -1 si no es una. */
		int mutante = -1;
		float escala = 1;
		boolean visible = true;

		Caja(float w, float h, float d, int mat, float x, float y, float z, float rx, float ry, float rz) {
			this.w = w; this.h = h; this.d = d; this.mat = mat; this.x = x; this.y = y; this.z = z;
			this.rx = rx; this.ry = ry; this.rz = rz;
		}
	}

	private static final class Nodo {
		final float x, y, z;
		float rx, ry, rz, sx = 1, sy = 1, sz = 1;
		boolean visible = true;
		final List<Caja> cajas = new ArrayList<>();
		final List<Nodo> hijos = new ArrayList<>();

		Nodo(float x, float y, float z, Nodo padre) {
			this.x = x; this.y = y; this.z = z;
			if (padre != null) padre.hijos.add(this);
		}
	}

	private record Cadena(List<Nodo> nodos, Nodo punta) {}

	private final List<Caja> mutantes = new ArrayList<>();
	private final Nodo flor;
	private final Nodo[] tallo = new Nodo[5];
	private final Nodo cabeza, labioSup, labioInf, lenguaRaiz, rosa, brazo, pinza;
	private final List<Nodo> lengua = new ArrayList<>(), petalos = new ArrayList<>(), tentaculos = new ArrayList<>();
	private final List<List<Nodo>> tentaculoCadenas = new ArrayList<>();
	private final List<Nodo> brazoCadena;
	private record CapaRosa(Nodo flap, int k, float inclinacion) {}
	private final List<CapaRosa> capasRosa = new ArrayList<>();

	/** Palabras de las partes que solo tiene la forma mutante (como el filtro del diseño). */
	private static final String[] MUTANTE = {"ojo", "pupila", "diente", "espina", "pua", "tentaculo", "bulbo", "lengua", "boca", "labio", "petalo", "centro"};

	private Caja caja(String nombre, float w, float h, float d, int mat, float x, float y, float z, Nodo padre) {
		return caja(nombre, w, h, d, mat, x, y, z, 0, 0, 0, padre);
	}

	private Caja caja(String nombre, float w, float h, float d, int mat, float x, float y, float z, float rx, float ry, float rz, Nodo padre) {
		Caja c = new Caja(w, h, d, mat, x, y, z, rx, ry, rz);
		for (String m : MUTANTE) {
			if (nombre.contains(m)) {
				c.mutante = mutantes.size();
				mutantes.add(c);
				break;
			}
		}
		padre.cajas.add(c);
		return c;
	}

	private Cadena cadena(String nombre, Nodo padre, int segs, float largo, float t0, float t1) {
		List<Nodo> nodos = new ArrayList<>();
		Nodo p = padre;
		float x = 0;
		for (int i = 0; i < segs; i++) {
			Nodo g = new Nodo(x, 0, 0, p);
			float th = t0 - i * (t0 - t1) / Math.max(1, segs - 1);
			caja(nombre + "_seg", largo + 0.3f, th, th, ENREDADERA, largo / 2, 0, 0, g);
			caja(nombre + "_espina", 0.7f, 1.3f, 0.7f, ESPINA, largo / 2, th / 2 + 0.55f, 0, g);
			if (i % 2 == 1) caja(nombre + "_espina_l", 0.7f, 0.7f, 1.3f, ESPINA, largo / 2 + 0.8f, 0, th / 2 + 0.55f, g);
			nodos.add(g);
			p = g;
			x = largo;
		}
		return new Cadena(nodos, new Nodo(x, 0, 0, p));
	}

	public QumaraRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 8f;
		final float PI = Mth.PI;
		flor = new Nodo(0, 0, 0, null);
		// Raíces pegadas al suelo.
		caja("tierra_base", 16, 0.6f, 16, TIERRA, 0, 0.3f, 0, flor);
		caja("raiz_base", 12, 2, 12, RAIZ, 0, 1.6f, 0, flor);
		caja("raiz_base_top", 9, 1, 9, RAIZ_OSC, 0, 3.1f, 0, flor);
		float[][] esquinas = {{-5.2f, -5.2f}, {5.2f, -5.2f}, {-5.2f, 5.2f}, {5.2f, 5.2f}};
		for (float[] e : esquinas) {
			caja("raiz_nudo", 3, 2.6f, 3, RAIZ_OSC, e[0], 1.9f, e[1], flor);
			caja("raiz_musgo", 3.2f, 0.5f, 3.2f, MUSGO, e[0], 3.45f, e[1], flor);
		}
		float[][] grietas = {{0, 6.02f, 0}, {6.02f, 0, PI / 2}, {0, -6.02f, PI}, {-6.02f, 0, -PI / 2}};
		for (int i = 0; i < 4; i++) {
			Nodo g = new Nodo(grietas[i][0], 0, grietas[i][1], flor);
			g.ry = grietas[i][2];
			caja("raiz_grieta_v", 0.6f, 1.8f, 0.1f, NEGRO, -1.5f + i, 1.6f, 0, g);
			caja("raiz_grieta_h", 1.4f, 0.4f, 0.1f, NEGRO, -0.9f + i, 2.2f, 0, g);
		}
		caja("raiz_musgo_top", 5, 0.5f, 4, MUSGO, -1.5f, 3.85f, 2, flor);
		for (int i = 0; i < 10; i++) {
			float a = i * PI / 5 + 0.2f, len = 6 + (i % 3) * 2;
			Nodo g = new Nodo(0, 0.6f, 0, flor);
			g.ry = a;
			caja("raiz_seg", 2.2f, 1.4f, len, RAIZ, 0, 0.7f, 5.5f + len / 2, g);
			caja("raiz_lomo", 1.2f, 0.5f, len * 0.7f, RAIZ_OSC, 0, 1.6f, 5.5f + len * 0.45f, g);
			caja("raiz_nudillo", 2.8f, 1.8f, 1.6f, RAIZ_OSC, 0, 0.9f, 5.5f + len * 0.55f, g);
			if (i % 2 == 1) caja("raiz_musgo_seg", 2.3f, 0.3f, 2.5f, MUSGO, 0, 1.55f, 5.5f + len * 0.25f, g);
			caja("raiz_punta", 1.3f, 0.9f, 2, RAIZ_OSC, 0, 0.45f, 6.5f + len, g);
			Nodo rama = new Nodo(0, 0.5f, 5.5f + len * 0.6f, g);
			rama.ry = (i % 2 == 1 ? 1 : -1) * 0.9f;
			caja("raiz_rama", 1, 0.7f, 3, RAIZ, 0, 0, 1.5f, rama);
		}
		// Tallo (cadena que se mece).
		Nodo padre = flor;
		float py = 3;
		for (int i = 0; i < 5; i++) {
			Nodo g = new Nodo(0, py, 0, padre);
			float w = 6.4f - i * 0.4f;
			caja("tallo_seg", w, 4.5f, w, TALLO, 0, 2.25f, 0, g);
			caja("tallo_espina_a", 0.9f, 0.9f, 1.8f, ESPINA, w / 2, 2.8f, 0.8f, g);
			caja("tallo_espina_b", 1.8f, 0.9f, 0.9f, ESPINA, -0.8f, 1.6f, -w / 2, g);
			tallo[i] = g;
			padre = g;
			py = 4.5f;
		}
		float[][] hojas = {{1, 1, 0.3f}, {2, -1, -0.4f}, {3, 1, 2.2f}, {4, -1, -1.2f}};
		for (float[] h : hojas) {
			float s = h[1];
			Nodo base = new Nodo(0, 2.5f, 0, tallo[(int) h[0]]);
			base.ry = h[2];
			Nodo hoja = new Nodo(s * 2.8f, 0, 0, base);
			hoja.rz = s * -0.45f;
			caja("hoja_m", 9, 0.6f, 5, HOJA, s * 4.5f, 0, 0, hoja);
			caja("hoja_punta", 3.5f, 0.6f, 2.6f, HOJA, s * 10.2f, 0, 0, hoja);
		}
		// Cabeza.
		cabeza = new Nodo(0, 4.5f, 3, tallo[4]);
		caja("centro", 14, 14, 8.6f, CENTRO, 0, 0, 0, cabeza);
		caja("centro_aro", 15.4f, 15.4f, 2, MORADO, 0, 0, -0.9f, cabeza);
		caja("boca_interior", 10, 6, 0.2f, BOCA, 0, -1.5f, 4.32f, cabeza);
		float[][] ojos = {{-4, 4.6f, 2}, {0, 5, 2.4f}, {4, 4.6f, 2}, {-5.6f, 2.4f, 1.2f}, {5.6f, 2.4f, 1.2f}};
		for (float[] o : ojos) {
			caja("ojo", o[2], o[2], 0.4f, OJO, o[0], o[1], 4.4f, cabeza);
			caja("pupila", o[2] * 0.35f, o[2] * 0.8f, 0.2f, NEGRO, o[0], o[1], 4.7f, cabeza);
		}
		labioSup = new Nodo(0, 1.6f, 4.3f, cabeza);
		caja("labio_sup_m", 10.4f, 3.1f, 1.2f, CENTRO, 0, -1.55f, 0.6f, labioSup);
		labioInf = new Nodo(0, -4.6f, 4.3f, cabeza);
		caja("labio_inf_m", 10.4f, 3.1f, 1.2f, CENTRO, 0, 1.55f, 0.6f, labioInf);
		for (int i = 0; i < 7; i++) {
			float x = -4.2f + i * 1.4f;
			boolean impar = i % 2 == 1;
			caja("diente_sup", 0.9f, impar ? 1.4f : 2.1f, 0.9f, DIENTES, x, -3.1f - (impar ? 0.7f : 1.05f), 0.9f, labioSup);
			caja("diente_inf", 0.9f, impar ? 2.1f : 1.4f, 0.9f, DIENTES, x + 0.7f, 3.1f + (impar ? 1.05f : 0.7f), 0.9f, labioInf);
		}
		// Lengua (sale solo al gritar).
		lenguaRaiz = new Nodo(0, -1.6f, 3.9f, cabeza);
		Nodo tp = lenguaRaiz;
		float lz = 0;
		float[][] segs = {{3.4f, 3.2f}, {3, 3}, {2.6f, 2.8f}};
		for (float[] sg : segs) {
			Nodo g = new Nodo(0, 0, lz, tp);
			caja("lengua_seg", sg[0], 0.8f, sg[1] + 0.2f, LENGUA, 0, 0, sg[1] / 2, g);
			caja("lengua_surco", 0.4f, 0.1f, sg[1], NEGRO, 0, 0.42f, sg[1] / 2, g);
			lengua.add(g);
			tp = g;
			lz = sg[1];
		}
		for (float x : new float[]{-0.8f, 0.8f}) caja("lengua_punta", 1, 0.8f, 1.8f, LENGUA, x, 0, 3.6f, 0, x * 0.35f, 0, lengua.get(2));
		// Pétalos (más largos a los lados: ~40 de ancho).
		for (int i = 0; i < 10; i++) {
			float a = i * PI / 5, largo = 8.2f + 6 * Math.abs(Mth.sin(a));
			Nodo pg = new Nodo(-Mth.sin(a) * 6.2f, Mth.cos(a) * 6.2f, -1.4f, cabeza);
			pg.rz = a;
			Nodo flap = new Nodo(0, 0, 0, pg);
			caja("petalo_m", 6.6f, largo, 1, PETALO, 0, largo / 2, 0, flap);
			caja("petalo_borde", 7.2f, 1.2f, 1.02f, PETALO_OSC, 0, 0.8f, 0, flap);
			caja("petalo_punta", 4, 1.2f, 1, PETALO, 0, largo + 0.6f, 0, flap);
			caja("petalo_espina", 0.8f, 1.4f, 0.8f, ESPINA, 0, largo + 1.8f, 0, flap);
			petalos.add(flap);
		}
		for (int i = 0; i < 10; i++) {
			float a = i * PI / 5 + PI / 10;
			Nodo pg = new Nodo(-Mth.sin(a) * 5.8f, Mth.cos(a) * 5.8f, -0.4f, cabeza);
			pg.rz = a;
			Nodo flap = new Nodo(0, 0, 0, pg);
			caja("petalo_int_m", 4, 6.5f, 0.8f, MORADO, 0, 3.25f, 0, flap);
			petalos.add(flap);
		}
		// La rosa gigante (antes de nacer).
		rosa = new Nodo(0, 0, 0.5f, cabeza);
		float[][] capas = {{1.6f, 3, 8.5f, 3.2f, 0.25f, ROSA_OSC}, {3.6f, 5, 9.5f, 4.6f, 0.3f, ROSA}, {5.8f, 6, 9.5f, 6, 0.12f, ROSA},
				{8, 7, 7.5f, 6.6f, -0.18f, ROSA_OSC}};
		for (int k = 0; k < capas.length; k++) {
			float r = capas[k][0], largo = capas[k][2], w = capas[k][3], inc = capas[k][4];
			int n = (int) capas[k][1], mat = (int) capas[k][5];
			for (int i = 0; i < n; i++) {
				float a = i * 2 * PI / n + k * 0.6f;
				Nodo pg = new Nodo(Mth.cos(a) * r, Mth.sin(a) * r, 0, rosa);
				pg.rz = a - PI / 2;
				Nodo flap = new Nodo(0, 0, 0, pg);
				flap.rx = inc;
				caja("rosa_capa_m", w, 1, largo, mat, 0, 0, largo / 2, flap);
				caja("rosa_capa_borde", w * 0.8f, 1.02f, 1, k % 2 == 1 ? ROSA_OSC : ROSA, 0, 0, largo - 0.4f, flap);
				capasRosa.add(new CapaRosa(flap, k, inc));
			}
		}
		for (int i = 0; i < 5; i++) {
			float a = i * 2 * PI / 5 + 0.3f;
			Nodo pg = new Nodo(Mth.cos(a) * 7, Mth.sin(a) * 7, -0.5f, rosa);
			pg.rz = a - PI / 2;
			Nodo flap = new Nodo(0, 0, 0, pg);
			flap.rx = -1.9f;
			caja("rosa_sepalo_m", 2.6f, 0.6f, 6, HOJA, 0, 0, 3, flap);
		}
		// 6 tentáculos alrededor del tallo.
		for (int k = 0; k < 6; k++) {
			Nodo b = new Nodo(0, k % 2 == 1 ? 1.5f : 3.2f, 0, tallo[k % 2 == 1 ? 2 : 1]);
			b.ry = k * PI / 3 + 0.5f;
			Nodo off = new Nodo(2.8f, 0, 0, b);
			Cadena c = cadena("tentaculo", off, 6, 2.4f, 1.8f, 1);
			caja("tentaculo_bulbo", 2, 2, 2, PETALO_OSC, 0.8f, 0, 0, c.punta());
			caja("tentaculo_pua", 1.6f, 0.6f, 0.6f, ESPINA, 2.3f, 0, 0, c.punta());
			tentaculos.add(b);
			tentaculoCadenas.add(c.nodos());
		}
		// El brazo de ataque (sale, da una vuelta entera y se esconde).
		brazo = new Nodo(0, 3, 0, tallo[0]);
		Nodo brazoBase = new Nodo(3, 0, 0, brazo);
		Cadena bc = cadena("brazo_ataque_vid", brazoBase, 7, 3.7f, 3.2f, 1.6f);
		brazoCadena = bc.nodos();
		pinza = bc.punta();
		caja("pinza_base", 3.2f, 3.2f, 3.2f, PETALO_OSC, 1.2f, 0, 0, pinza);
		caja("pinza_boca", 0.2f, 1.8f, 1.8f, BOCA, 2.81f, 0, 0, pinza);
		float[][] garras = {{1.2f, 0}, {-1.2f, 0}, {0, 1.2f}, {0, -1.2f}};
		for (float[] g : garras) caja("pinza_garra", 3.4f, 0.9f, 0.9f, ESPINA, 4, g[0], g[1], 0, g[1] * 0.3f, -g[0] * 0.3f, pinza);
	}

	private static float suave(float x) {
		return QumaraEntity.suave(x);
	}

	// Tintes (color de todo el cuerpo): celeste al debilitarse, marchito al ser derrotada.
	private float tinteR = 1, tinteG = 1, tinteB = 1, apagarOjos = 0;

	/** Pone la pose del momento, igual que el tick() del diseño. */
	private void animar(QumaraEntity q, float parcial) {
		float t = (q.level().getGameTime() % 72000L + parcial) / 20f;
		// Quieta.
		for (int i = 0; i < 5; i++) {
			tallo[i].rz = Mth.sin(t * 0.9f - i * 0.4f) * 0.02f;
			tallo[i].rx = Mth.sin(t * 0.7f - i * 0.3f) * 0.012f;
		}
		cabeza.rx = -0.05f + Mth.sin(t * 1.1f) * 0.04f;
		cabeza.ry = Mth.sin(t * 0.5f) * 0.1f;
		cabeza.rz = 0;
		float m0 = 0.2f + Math.max(0, Mth.sin(t * 1.7f)) * 0.15f;
		labioSup.rx = -m0;
		labioInf.rx = m0;
		for (int i = 0; i < petalos.size(); i++) petalos.get(i).rx = 0.18f + Mth.sin(t * 1.4f + i * 0.8f) * 0.05f;
		for (int i = 0; i < lengua.size(); i++) {
			lengua.get(i).rx = (i == 0 ? 0.05f : 0.28f) + Mth.sin(t * 2.4f - i * 0.8f) * 0.12f;
			lengua.get(i).ry = Mth.sin(t * 1.3f - i * 0.6f) * 0.12f;
		}
		lenguaRaiz.visible = false;
		for (int k = 0; k < 6; k++) {
			List<Nodo> c = tentaculoCadenas.get(k);
			for (int i = 0; i < c.size(); i++) {
				c.get(i).rz = (i == 0 ? 0.25f : -0.32f) + Mth.sin(t * 1.6f + k * 1.3f - i * 0.7f) * 0.12f;
				c.get(i).ry = Mth.sin(t * 1.1f + k - i * 0.6f) * 0.14f;
			}
		}
		// Giro (barrido): sale, carga, vuelta entera, se esconde.
		float aT = q.tBarrido(parcial) / 20f;
		float ext = 0, yaw = -Mth.PI / 2;
		if (aT >= 0 && aT < 7) {
			ext = suave(aT / 1.4f) * (1 - suave((aT - 5.6f) / 1.4f));
			yaw = (float) QumaraEntity.yawBrazo(aT);
		}
		brazo.visible = ext > 0.01f;
		pinza.visible = true;
		brazo.sx = brazo.sy = brazo.sz = Math.max(0.01f, ext);
		brazo.ry = yaw;
		for (int i = 0; i < brazoCadena.size(); i++) {
			brazoCadena.get(i).rz = (1 - ext) * 0.85f + (i == 0 ? -0.1f * ext : 0) + Mth.sin(t * 3 - i * 0.7f) * 0.02f;
			brazoCadena.get(i).ry = (aT > 2 && aT < 5.6f ? 0.06f : 0) + Mth.sin(t * 2 - i) * 0.03f;
		}
		pinza.rx = t * 4 * ext;
		if (ext > 0.01f) cabeza.ry += Mth.sin(yaw + Mth.PI / 2) * 0.18f * ext;
		// Grito.
		float sT = q.tGrito(parcial) / 20f;
		if (sT >= 0 && sT < 3.2f) {
			float r = suave(sT / 0.4f) * (1 - suave((sT - 2.4f) / 0.7f));
			float sx = sT > 0.3f && sT < 2.5f ? Mth.sin(sT * 70) * 0.03f : 0;
			for (Nodo g : tallo) g.rx += -r * 0.05f + sx * 0.2f;
			cabeza.rx += -r * 0.35f + sx;
			cabeza.rz = sx * 1.5f;
			labioSup.rx = -m0 * (1 - r) - r * 1.2f - sx * 3;
			labioInf.rx = m0 * (1 - r) + r * 1.2f + sx * 3;
			float tr = suave((sT - 0.25f) / 0.35f) * (1 - suave((sT - 2.3f) / 0.4f));
			lenguaRaiz.visible = tr > 0.02f;
			lenguaRaiz.sx = Math.max(0.01f, 0.6f + 0.4f * tr);
			lenguaRaiz.sz = Math.max(0.01f, tr);
			for (int i = 0; i < lengua.size(); i++) lengua.get(i).rx += Mth.sin(sT * 20 - i) * 0.1f * tr;
			for (int i = 0; i < petalos.size(); i++) {
				Nodo p = petalos.get(i);
				p.rx = p.rx * (1 - r) - r * 0.45f + Mth.sin(sT * 50 + i) * 0.06f * r;
			}
			for (List<Nodo> c : tentaculoCadenas) for (int i = 0; i < c.size(); i++) c.get(i).rz += r * (i == 0 ? 0.5f : 0.18f);
		}
		// Transformación: la rosa gigante mirando al cielo → Qumara. Antes de nacer queda en el comienzo.
		boolean transformando = q.esRosa() || q.tNacer(parcial) < QumaraEntity.TICKS_NACER;
		rosa.visible = transformando;
		for (Caja m : mutantes) {
			m.visible = true;
			m.escala = 1;
		}
		for (Nodo tr : tentaculos) {
			tr.visible = true;
			tr.sx = tr.sy = tr.sz = 1;
		}
		rosa.sx = rosa.sy = rosa.sz = 1;
		if (transformando) {
			float trT = q.esRosa() ? 0 : q.tNacer(parcial) / 20f;
			float g = suave((trT - 2.4f) / 2);
			float temblor = trT > 0.9f && trT < 4.6f ? Mth.sin(trT * 45) * 0.02f * (0.5f + suave((trT - 0.9f) / 2)) : 0;
			for (int i = 0; i < 5; i++) tallo[i].rz += temblor * (i + 1) * 0.4f;
			cabeza.rx = -1.35f * (1 - g) + cabeza.rx * g + temblor;
			cabeza.ry *= g;
			labioSup.rx *= g;
			labioInf.rx *= g;
			for (CapaRosa c : capasRosa) {
				float o = suave((trT - 1.8f - (3 - c.k()) * 0.2f) / 1.1f);
				c.flap().rx = c.inclinacion() + (-1.5f - c.inclinacion()) * o + (trT < 1.8f ? Mth.sin(t * 1.2f + c.k()) * 0.02f : 0);
			}
			float desaparece = suave((trT - 3.1f) / 0.8f);
			rosa.sx = rosa.sy = rosa.sz = Math.max(0.001f, 1 - desaparece);
			for (Caja m : mutantes) {
				float f = suave((trT - 3.0f - (m.mutante % 7) * 0.08f) / 0.6f);
				m.visible = f > 0.01f;
				m.escala = Math.max(0.001f, f);
			}
			for (int i = 0; i < tentaculos.size(); i++) {
				float f = suave((trT - 3.2f - i * 0.12f) / 0.6f);
				Nodo tr = tentaculos.get(i);
				tr.visible = f > 0.01f;
				tr.sx = tr.sy = tr.sz = Math.max(0.001f, f);
			}
			brazo.visible = false;
			lenguaRaiz.visible = false;
		}
		// Debilitada: se marchita y se pone celeste.
		float wk = Mth.lerp(parcial, q.debilVistoAntes, q.debilVisto);
		if (wk > 0.001f) {
			float br = Mth.sin(t * 1.2f) * 0.03f * wk;
			for (Nodo g : tallo) {
				g.rx += wk * 0.1f + br;
				g.rz *= 1 - wk * 0.7f;
			}
			cabeza.rx += wk * 0.75f;
			cabeza.ry *= 1 - wk * 0.8f;
			labioSup.rx = labioSup.rx * (1 - wk) - wk * 0.1f;
			labioInf.rx = labioInf.rx * (1 - wk) + wk * 0.45f;
			for (int i = 0; i < petalos.size(); i++) {
				Nodo p = petalos.get(i);
				p.rx = p.rx * (1 - wk) + wk * (0.75f + Mth.sin(i * 1.7f) * 0.15f);
			}
			for (List<Nodo> c : tentaculoCadenas) {
				for (int i = 0; i < c.size(); i++) {
					c.get(i).rz = c.get(i).rz * (1 - wk) + wk * (i == 0 ? -0.7f : -0.12f);
					c.get(i).ry *= 1 - wk;
				}
			}
			brazo.sx = brazo.sy = brazo.sz = brazo.sx * Math.max(0.01f, 1 - wk);
		}
		// Derrotada: se estremece, se desploma hacia adelante con un rebote y queda así.
		float dk = 0;
		if (q.derrotada()) {
			float dT = q.tDerrota(parcial) / 20f;
			float x = Mth.clamp((dT - 0.6f) / 2.2f, 0, 1), c1 = 1.4f;
			dk = x <= 0 ? 0 : 1 + (c1 + 1) * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
			float estremece = dT < 0.9f ? Mth.sin(dT * 60) * 0.04f * (1 - dT / 0.9f) : 0;
			float[] doblez = {0.35f, 0.35f, 0.3f, 0.25f, 0.2f};
			for (int i = 0; i < 5; i++) {
				tallo[i].rx = tallo[i].rx * (1 - dk) + doblez[i] * dk + estremece;
				tallo[i].rz = tallo[i].rz * (1 - dk) + (i < 2 ? 0.08f : 0.03f) * dk;
			}
			cabeza.rx = cabeza.rx * (1 - dk) + 0.6f * dk + estremece * 2;
			cabeza.ry *= 1 - dk;
			cabeza.rz = cabeza.rz * (1 - dk) + 0.15f * dk;
			labioSup.rx = labioSup.rx * (1 - dk) - 0.08f * dk;
			labioInf.rx = labioInf.rx * (1 - dk) + 0.6f * dk;
			for (int i = 0; i < petalos.size(); i++) {
				Nodo p = petalos.get(i);
				p.rx = p.rx * (1 - dk) + dk * (1.05f + Mth.sin(i * 2.3f) * 0.2f);
			}
			for (List<Nodo> c : tentaculoCadenas) {
				for (int i = 0; i < c.size(); i++) {
					c.get(i).rz = c.get(i).rz * (1 - dk) + dk * (i == 0 ? -0.9f : -0.08f);
					c.get(i).ry *= 1 - dk;
				}
			}
			brazo.visible = false;
			lenguaRaiz.visible = false;
		}
		// Atrapar / Sacar: el brazo va a buscar al jugador, lo sube a la cara, le tira gas y lo tira lejos.
		if (q.agarrando()) {
			float g = q.tiempoAgarre(parcial), tS = q.tSacar(parcial), tA = q.tAgarre(parcial);
			QumaraAgarre.Pose p = QumaraAgarre.brazo(g, q.poseAlcance());
			brazo.visible = p.e() > 0.01f;
			brazo.sx = brazo.sy = brazo.sz = Math.max(0.01f, p.e());
			brazo.ry = p.yaw();
			boolean quieto = tS < 0 && tA >= QumaraAgarre.SOSTIENE;
			for (int i = 0; i < brazoCadena.size(); i++) {
				brazoCadena.get(i).rz = p.c() + (quieto ? Mth.sin(t * 2 - i * 0.5f) * 0.01f : 0);
				brazoCadena.get(i).ry = 0;
			}
			pinza.rx = 0;
			// La pinza envuelve la cabeza del agarrado: en primera persona no lo deja ver nada, así que para él no se dibuja.
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
			pinza.visible = !(mc.player != null && mc.player.getId() == q.agarradoId() && mc.options.getCameraType().isFirstPerson());
			if (quieto) cabeza.rx += Mth.sin(t * 1.5f) * 0.03f;
			// Boca bien abierta mientras tira gas (se cierra al sacarlo).
			if (tA > 3.0f) {
				float k = suave((tA - 3.0f) / 0.4f) * (tS < 0 ? 1 : 1 - suave(tS / 0.4f)) * (1 + Mth.sin(t * 9) * 0.06f);
				labioSup.rx = labioSup.rx * (1 - k) - 1.0f * k;
				labioInf.rx = labioInf.rx * (1 - k) + 1.0f * k;
				cabeza.rx += 0.12f * k;
			}
		}
		// Colores: blanco → celeste (#8fd8ff) al debilitarse; → marchito (#8a7a66) al ser derrotada.
		float cel = wk * 0.8f, mar = Math.min(1, Math.max(0, dk)) * 0.6f;
		tinteR = Mth.lerp(mar, Mth.lerp(cel, 1, 0x8f / 255f), 0x8a / 255f);
		tinteG = Mth.lerp(mar, Mth.lerp(cel, 1, 0xd8 / 255f), 0x7a / 255f);
		tinteB = Mth.lerp(mar, 1, 0x66 / 255f);
		apagarOjos = q.derrotada() ? 1 : 0;
	}

	// --- Dibujo ---

	/** Una caja ya ubicada, para dibujarlas agrupadas por material (una tanda por textura). */
	private record Pendiente(PoseStack.Pose pose, Caja caja) {}

	private final List<List<Pendiente>> porMaterial = new ArrayList<>();

	@Override
	public void render(QumaraEntity q, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		animar(q, parcial);
		if (porMaterial.isEmpty()) for (int i = 0; i < NOMBRES.length; i++) porMaterial.add(new ArrayList<>());
		for (List<Pendiente> l : porMaterial) l.clear();
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, q.yBodyRotO, q.yBodyRot)));
		juntar(pose, flor);
		pose.popPose();
		int overlay = OverlayTexture.pack(0f, q.hurtTime > 0 && !q.derrotada());
		for (int m = 0; m < NOMBRES.length; m++) {
			List<Pendiente> lista = porMaterial.get(m);
			if (lista.isEmpty()) continue;
			VertexConsumer vc = buffers.getBuffer(TIPOS[m]);
			// Los ojos brillan (salvo derrotada).
			int l = m == OJO && apagarOjos < 0.5f ? LightTexture.FULL_BRIGHT : luz;
			for (Pendiente p : lista) dibujarCaja(vc, p.pose(), p.caja(), l, overlay);
		}
		super.render(q, yaw, parcial, pose, buffers, luz);
	}

	private void juntar(PoseStack pose, Nodo n) {
		if (!n.visible) return;
		pose.pushPose();
		pose.translate(n.x, n.y, n.z);
		if (n.rx != 0) pose.mulPose(Axis.XP.rotation(n.rx));
		if (n.ry != 0) pose.mulPose(Axis.YP.rotation(n.ry));
		if (n.rz != 0) pose.mulPose(Axis.ZP.rotation(n.rz));
		if (n.sx != 1 || n.sy != 1 || n.sz != 1) pose.scale(n.sx, n.sy, n.sz);
		for (Caja c : n.cajas) {
			if (!c.visible) continue;
			pose.pushPose();
			pose.translate(c.x, c.y, c.z);
			if (c.rx != 0) pose.mulPose(Axis.XP.rotation(c.rx));
			if (c.ry != 0) pose.mulPose(Axis.YP.rotation(c.ry));
			if (c.rz != 0) pose.mulPose(Axis.ZP.rotation(c.rz));
			if (c.escala != 1) pose.scale(c.escala, c.escala, c.escala);
			porMaterial.get(c.mat).add(new Pendiente(pose.last().copy(), c));
			pose.popPose();
		}
		for (Nodo hijo : n.hijos) juntar(pose, hijo);
		pose.popPose();
	}

	/** Sombra falsa del diseño: 0,62 abajo → 1 arriba de cada caja (en el mismo espacio de color que el diseño). */
	private static float brillo(float fy, boolean abajo, boolean arriba) {
		float v = 0.62f + 0.38f * fy;
		if (abajo) v *= 0.7f;
		if (arriba) v = Math.min(1, v * 1.06f);
		return (float) Math.pow(v, 1 / 2.2);
	}

	private void dibujarCaja(VertexConsumer vc, PoseStack.Pose p, Caja c, int luz, int overlay) {
		float x0 = -c.w / 2, x1 = c.w / 2, y0 = -c.h / 2, y1 = c.h / 2, z0 = -c.d / 2, z1 = c.d / 2;
		// Caras: +x, -x, +y, -y, +z, -z. Cada una: 4 esquinas (abajo-izq, abajo-der, arriba-der, arriba-izq) y el tamaño de la textura.
		cara(vc, p, luz, overlay, 1, 0, 0, c.d, c.h, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
		cara(vc, p, luz, overlay, -1, 0, 0, c.d, c.h, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		cara(vc, p, luz, overlay, 0, 1, 0, c.w, c.d, 2, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
		cara(vc, p, luz, overlay, 0, -1, 0, c.w, c.d, 1, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		cara(vc, p, luz, overlay, 0, 0, 1, c.w, c.h, 0, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		cara(vc, p, luz, overlay, 0, 0, -1, c.w, c.h, 0, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
	}

	/** tipo: 0 costado (degradé de abajo a arriba), 1 cara de abajo, 2 cara de arriba. */
	private void cara(VertexConsumer vc, PoseStack.Pose p, int luz, int overlay, float nx, float ny, float nz,
					  float ancho, float alto, int tipo, float... q) {
		float[] us = {0, ancho, ancho, 0}, vs = {alto, alto, 0, 0};
		float h = 0;
		for (int i = 0; i < 4; i++) h = Math.max(h, Math.abs(q[i * 3 + 1]) * 2);
		for (int i = 0; i < 4; i++) {
			float y = q[i * 3 + 1];
			float fy = tipo == 1 ? 0 : tipo == 2 ? 1 : (h > 0 ? y / h + 0.5f : 0.5f);
			float b = brillo(fy, tipo == 1, tipo == 2);
			int r = (int) (255 * Math.min(1, b * 0.97f * tinteR)), g = (int) (255 * Math.min(1, b * 0.97f * tinteG)),
					bl = (int) (255 * Math.min(1, b * 1.04f * tinteB));
			vc.addVertex(p, q[i * 3], y, q[i * 3 + 2]).setColor(r, g, bl, 255).setUv(us[i], vs[i]).setOverlay(overlay)
					.setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	/** Mide mucho más que su caja: se dibuja mientras cualquier parte esté a la vista (el brazo llega a 30). */
	@Override
	public boolean shouldRender(QumaraEntity q, Frustum frustum, double x, double y, double z) {
		return frustum.isVisible(q.getBoundingBox().inflate(22, 8, 22));
	}

	@Override
	public ResourceLocation getTextureLocation(QumaraEntity q) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/qumara/petalo.png");
	}
}
