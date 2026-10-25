package com.dedsafio4.client.lagartos;

import com.dedsafio4.lagartos.LagartoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dibuja a los Reptisaurios con los modelos del diseño (ModeloLagarto): el Guerrero es el espadachín, el Arquero
 * el arquero y el de la Lanza el lancero. Animaciones como en el diseño: respiran, mueven la cabeza, la cola y sacan
 * la lengua cada 3,2 s; con un objetivo se ponen en guardia (o apuntan con el arco) y si no, en reposo. Arquero:
 * al disparar suelta y vuelve a tensar el arco en 3 fases. Lancero: estocada y tirar la lanza (la mano queda vacía
 * mientras vuela). Espadachín: estocada, tajo y levantar el escudo cuando para un golpe. Además caminan.
 */
public class LagartoRenderer extends EntityRenderer<LagartoEntity> {
	private static final float PI = Mth.PI;
	private final String archivo;

	/** Las poses de los brazos (giros x, y, z), en guardia y en reposo. */
	private static final float[] L_GUARDIA = {-1.25f, -0.35f, 0}, LE_GUARDIA = {-0.2f, 0, 0}, R_GUARDIA = {-1.15f, 0.1f, 0}, RE_GUARDIA = {-0.3f, 0, 0};
	private static final float[] L_REPOSO = {-0.15f, 0, 0.1f}, LE_REPOSO = {-0.2f, 0, 0}, R_REPOSO = {-0.35f, 0, -0.1f}, RE_REPOSO = {-0.4f, 0, 0};
	private static final float[] L_APUNTA = {-PI / 2, -0.12f, 0}, LE_APUNTA = {0, 0, 0}, R_APUNTA = {-PI / 2, 0.75f, 0}, RE_APUNTA = {0, 0, 0.95f};
	private static final float[] L_DESCANSA = {-0.25f, 0, 0.08f}, LE_DESCANSA = {-0.5f, 0, 0}, R_DESCANSA = {0.1f, 0, -0.08f}, RE_DESCANSA = {-0.2f, 0, 0};

	/** Lo que se va moviendo suave (de un lagarto): brazos, el arma, la inclinación, el empujón y la cabeza. */
	private static final class Estado {
		final float[] l = new float[3], le = new float[3], r = new float[3], re = new float[3];
		float sostiene, inclina, empuje, avance, cabeza;
		long antes;
	}

	private final Map<LagartoEntity, Estado> estados = new WeakHashMap<>();

	public LagartoRenderer(EntityRendererProvider.Context contexto, LagartoEntity.Variante variante) {
		super(contexto);
		this.archivo = switch (variante) {
			case REPTISAURIO_ARQUERO -> "arquero";
			case REPTISAURIO_LANZA -> "lancero";
			default -> "espadachin";
		};
		this.shadowRadius = 0.5f;
	}

	private static void hacia(float[] actual, float[] meta, float k) {
		for (int i = 0; i < 3; i++) actual[i] += (meta[i] - actual[i]) * k;
	}

	private static void girar(ModeloLagarto.Hueso h, float[] g) {
		h.giro(g[0], g[1], g[2]);
	}

	private void animar(ModeloLagarto m, Estado e, LagartoEntity lag, float parcial) {
		long ahora = System.nanoTime();
		float dt = e.antes == 0 ? 0.05f : Mth.clamp((ahora - e.antes) / 1.0e9f, 0, 0.05f);
		e.antes = ahora;
		float k = 1 - (float) Math.pow(0.001, dt);
		float t = (lag.tickCount + parcial) / 20f;
		boolean guardia = lag.isAggressive();
		int accion = lag.accion();
		float ta = lag.tAccion(parcial);
		ModeloLagarto.Hueso brazoI = m.hueso("brazo_izq"), antebrazoI = m.hueso("brazo_izq/antebrazo");
		ModeloLagarto.Hueso brazoD = m.hueso("brazo_der"), antebrazoD = m.hueso("brazo_der/antebrazo");
		ModeloLagarto.Hueso torso = m.hueso("torso"), cabeza = m.hueso("cabeza"), cuerpo = m.hueso("cuerpo_rig");

		if (archivo.equals("arquero")) {
			// Apunta con el arco en la mano izquierda y tensa con la derecha.
			float[] l = guardia ? L_APUNTA : L_DESCANSA, le = guardia ? LE_APUNTA : LE_DESCANSA;
			float[] r = guardia ? R_APUNTA : R_DESCANSA, re = guardia ? RE_APUNTA : RE_DESCANSA;
			float desde = accion == LagartoEntity.DISPARO ? ta : 99;
			int arco = 0;
			if (guardia) {
				if (desde < 0.15f) {
					r = new float[]{-PI / 2, 0.35f, 0};
					re = new float[]{0, 0, 0.2f};
				} else if (desde < 0.45f) {
					arco = 1;
					re = new float[]{0, 0, 0.5f};
				} else if (desde < 0.75f) {
					arco = 2;
					re = new float[]{0, 0, 0.75f};
				} else {
					arco = 3;
				}
			}
			float kr = desde < 0.15f ? 0.6f : k;
			hacia(e.l, l, k);
			hacia(e.le, le, k);
			hacia(e.r, r, kr);
			hacia(e.re, re, kr);
			e.sostiene += ((guardia ? PI / 2 : 1.1f) - e.sostiene) * k;
			e.cabeza += ((guardia ? -0.1f : 0) - e.cabeza) * k;
			m.hueso("arco_mano").rx = e.sostiene;
			cabeza.ry = e.cabeza;
			cabeza.rx = Mth.sin(t * 1.7f) * 0.03f;
			for (int i = 0; i < 4; i++) m.hueso("arco_estado_" + i).visible = i == arco;
		} else {
			boolean lancero = archivo.equals("lancero");
			float[] lMeta = guardia ? L_GUARDIA : L_REPOSO, leMeta = guardia ? LE_GUARDIA : LE_REPOSO;
			float[] r = guardia ? R_GUARDIA : R_REPOSO, re = guardia ? RE_GUARDIA : RE_REPOSO;
			float inclina = guardia ? 0.06f : 0, empuje = 0, avance = 0;
			float apunta = lancero ? (guardia ? 0.08f : -PI / 2) : (guardia ? -0.55f : 0.9f);
			float sa = accion == LagartoEntity.ESTOCADA ? ta : 99;
			float st = accion == (lancero ? LagartoEntity.LANZAR : LagartoEntity.TAJO) ? ta : 99;
			// Estocada: el brazo derecho adelante, el arma más afuera y el cuerpo hacia adelante.
			if (sa < 0.3f) {
				r = new float[]{-PI / 2, 0.05f, 0};
				re = new float[]{0, 0, 0};
				empuje = -4;
				inclina = 0.22f;
				avance = 2;
				if (sa < 0.12f) k = 0.55f;
			}
			if (lancero) {
				// Tirar la lanza: el brazo sobre la cabeza, la suelta a los 0,4 s y vuelve a tener una a los 1,3 s.
				if (st < 0.4f) {
					r = new float[]{-2.9f, 0.15f, -0.2f};
					re = new float[]{-0.4f, 0, 0};
					apunta = -0.15f;
					inclina = -0.08f;
				} else if (st < 0.7f) {
					r = new float[]{-1.1f, 0.2f, 0};
					re = new float[]{0, 0, 0};
					inclina = 0.18f;
					k = 0.5f;
				}
				m.hueso("lanza").visible = !(st >= 0.4f && st < 1.3f);
			} else if (st < 0.9f) {
				// Tajo: levanta la espada y la baja en diagonal.
				if (st < 0.25f) {
					r = new float[]{-2.7f, -0.35f, 0.35f};
					re = new float[]{-0.3f, 0, 0};
					apunta = -1.2f;
					inclina = -0.06f;
				} else if (st < 0.5f) {
					r = new float[]{0.7f * -1, 0.55f, 0};
					re = new float[]{0, 0, 0};
					apunta = 0.55f;
					inclina = 0.2f;
					avance = 1.5f;
					k = 0.6f;
				}
			}
			// Paró un golpe: el escudo bien al frente.
			if (accion == LagartoEntity.BLOQUEO && ta < 0.5f) {
				lMeta = new float[]{-1.6f, -0.7f, 0};
				leMeta = new float[]{0, 0, 0};
				inclina = -0.05f;
			}
			hacia(e.l, lMeta, k);
			hacia(e.le, leMeta, k);
			hacia(e.r, r, k);
			hacia(e.re, re, k);
			ModeloLagarto.Hueso mano = m.hueso(lancero ? "lanza_mano" : "espada_mano");
			mano.rx = apunta - e.r[0] - e.re[0];
			e.empuje += (empuje - e.empuje) * k;
			mano.y = -6 + e.empuje;
			e.inclina += (inclina - e.inclina) * k;
			torso.rx = e.inclina;
			e.avance += (avance - e.avance) * k;
			cuerpo.z = e.avance;
			cabeza.rx = Mth.sin(t * 1.7f) * 0.03f - e.inclina * 0.6f;
		}
		girar(brazoI, e.l);
		girar(antebrazoI, e.le);
		girar(brazoD, e.r);
		girar(antebrazoD, e.re);
		// Respira, mueve la cola y saca la lengua.
		torso.sx = 1 + Mth.sin(t * 2) * 0.008f;
		torso.sz = 1 + Mth.sin(t * 2) * 0.012f;
		for (int i = 0; i < 4; i++) m.hueso("cola_" + i).ry = Mth.sin(t * 2.2f - i * 0.7f) * (0.12f + i * 0.05f);
		float tc = t % 3.2f;
		m.hueso("lengua_rig").sz = tc < 0.5f ? Math.max(0.01f, Mth.sin(tc / 0.5f * PI)) : 0.01f;
		// Mira hacia donde mira.
		float cabezaYaw = Mth.wrapDegrees(Mth.rotLerp(parcial, lag.yHeadRotO, lag.yHeadRot) - Mth.rotLerp(parcial, lag.yBodyRotO, lag.yBodyRot));
		cabeza.ry -= Mth.clamp(cabezaYaw, -70, 70) * Mth.DEG_TO_RAD;
		cabeza.rx += lag.getViewXRot(parcial) * Mth.DEG_TO_RAD * 0.6f;
		// Camina: las piernas (y los brazos, si no está peleando) van y vienen.
		float paso = lag.walkAnimation.position(parcial), rapidez = Math.min(1, lag.walkAnimation.speed(parcial));
		float va = Mth.cos(paso * 0.6662f) * 1.1f * rapidez;
		m.hueso("pierna_izq").rx = va;
		m.hueso("pierna_der").rx = -va;
		if (!guardia) {
			brazoI.rx -= va * 0.4f;
			brazoD.rx += va * 0.4f;
		}
	}

	@Override
	public void render(LagartoEntity lag, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		ModeloLagarto m = ModeloLagarto.de(archivo);
		m.reiniciar();
		animar(m, estados.computeIfAbsent(lag, x -> new Estado()), lag, parcial);
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, lag.yBodyRotO, lag.yBodyRot)));
		// Al morir se cae de costado (como los mobs de Minecraft).
		if (lag.deathTime > 0) pose.mulPose(Axis.ZP.rotationDegrees(Math.min(1, Mth.sqrt((lag.deathTime + parcial - 1) / 20f * 1.6f)) * 90));
		pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
		int overlay = OverlayTexture.pack(0f, lag.hurtTime > 0 || lag.deathTime > 0);
		m.dibujar(m.raiz(), pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ModeloLagarto.BLANCO)), luz, overlay, lag.piel(), lag.deathTime > 0);
		pose.popPose();
		super.render(lag, yaw, parcial, pose, buffers, luz);
	}

	@Override
	public ResourceLocation getTextureLocation(LagartoEntity lag) {
		return ModeloLagarto.BLANCO;
	}
}
