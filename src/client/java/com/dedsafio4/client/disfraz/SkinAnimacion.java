package com.dedsafio4.client.disfraz;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Las animaciones propias de las skins chiquitas de Blockbench (las que tienen "dedsafio4:torso" en el modelo): quieta
 * respira, y caminar, correr, agacharse y saltar tienen cada uno su pose, y se pasa de una a otra de a poco.
 * Por cada parte del jugador (cabeza, cuerpo, brazo izq., brazo der., pierna izq., pierna der.): cuánto se corre en
 * píxeles (x, y, z) y cuánto gira en radianes (x, y, z). Adelante es -Z y arriba es -Y.
 */
final class SkinAnimacion {
	private SkinAnimacion() {}

	/** Qué tan rápido cambia de pose (más = más de golpe). */
	private static final float CAMBIO = 11;

	/** Por jugador: cuánto está corriendo, agachado y en el aire (de 0 a 1) y cuándo se calculó. */
	private static final class Estado {
		float correr, agachado, aire;
		long cuando;
	}

	private static final Map<UUID, Estado> ESTADOS = new HashMap<>();

	/** Cuánto gira la falda (hacia atrás al moverse), en radianes; la lee SkinsGeo. */
	static float falda;

	/** Devuelve null cuando va la animación normal (nadando, volando con élitros, sentado o en la cama). */
	static float[][] calcular(AbstractClientPlayer j, HumanoidModel<?> modelo, float parcial, float torso) {
		if (j.isSwimming() || j.isVisuallySwimming() || j.isFallFlying() || j.isPassenger() || j.isSleeping() || j.isAutoSpinAttack()) return null;
		Estado e = ESTADOS.computeIfAbsent(j.getUUID(), k -> new Estado());
		long ahora = System.nanoTime();
		float dt = Math.min(0.1f, (ahora - e.cuando) / 1.0e9f), k = 1 - (float) Math.exp(-dt * CAMBIO);
		e.cuando = ahora;
		float vel = Mth.clamp(j.walkAnimation.speed(parcial) * 1.3f, 0, 1);
		e.correr += ((j.isSprinting() && vel > 0.1f ? 1 : 0) - e.correr) * k;
		e.agachado += ((modelo.crouching ? 1 : 0) - e.agachado) * k;
		e.aire += ((j.onGround() ? 0 : 1) - e.aire) * k;
		float correr = e.correr, agachado = e.agachado, aire = e.aire;

		float fase = j.walkAnimation.position(parcial) * 0.6662f;
		float paso = Mth.sin(fase), rebote = Math.abs(Mth.cos(fase));
		float tiempo = j.tickCount + parcial;
		float respira = Mth.sin(tiempo * 0.09f) * (1 - vel);

		float pierna = vel * Mth.lerp(correr, 0.85f, 1.3f) * (1 - agachado * 0.35f) * (1 - aire * 0.8f);
		float brazo = vel * Mth.lerp(correr, 0.6f, 1.15f) * (1 - agachado * 0.5f) * (1 - aire);
		float inclinar = correr * 0.3f * vel + agachado * 0.5f;
		float bajar = agachado * 1.1f + (1 - rebote) * vel * Mth.lerp(correr, 0.35f, 0.8f) * (1 - aire) - respira * 0.12f - aire * 0.3f;
		float vaiven = paso * vel * Mth.lerp(correr, 0.1f, 0.2f);

		// El cuerpo gira en la cintura: la cabeza y los hombros van a donde quedó arriba del torso.
		float adelante = -Mth.sin(inclinar) * torso, cae = (1 - Mth.cos(inclinar)) * torso;
		float[] cuerpo = {0, bajar, 0, inclinar, vaiven, vaiven * 0.3f};
		float[] cabeza = {0, bajar + cae, adelante, modelo.head.xRot - inclinar * 0.55f, modelo.head.yRot - vaiven * 0.5f, -vaiven * 0.3f};

		// Los brazos: van al revés que las piernas; corriendo y agachada quedan hacia adelante, y en el aire se abren.
		float frente = -0.45f * correr * vel - 0.7f * agachado;
		float abrir = 0.08f + Mth.sin(tiempo * 0.09f) * 0.03f + aire * 0.75f;
		float[] brazoIzq = {0, bajar + cae, adelante, frente + paso * brazo, -vaiven, -abrir};
		float[] brazoDer = {0, bajar + cae, adelante, frente - paso * brazo, -vaiven, abrir};
		// Con un golpe o algo en la mano, el brazo hace lo de siempre.
		if (modelo.attackTime > 0 || modelo.leftArmPose != HumanoidModel.ArmPose.EMPTY || modelo.rightArmPose != HumanoidModel.ArmPose.EMPTY) {
			brazoIzq[3] = modelo.leftArm.xRot;
			brazoIzq[4] = modelo.leftArm.yRot;
			brazoDer[3] = modelo.rightArm.xRot;
			brazoDer[4] = modelo.rightArm.yRot;
		}

		// Las piernas: agachada se abren un poco y quedan atrás del cuerpo; en el aire, una adelante y otra atrás.
		float atras = agachado * 0.6f;
		float[] piernaIzq = {0, bajar * 0.25f, atras, -paso * pierna - aire * 0.5f - agachado * 0.25f, 0, -agachado * 0.12f};
		float[] piernaDer = {0, bajar * 0.25f, atras, paso * pierna + aire * 0.45f - agachado * 0.25f, 0, agachado * 0.12f};

		falda = -inclinar * 0.35f + rebote * vel * Mth.lerp(correr, 0.05f, 0.18f) + aire * 0.15f;
		return new float[][]{cabeza, cuerpo, brazoIzq, brazoDer, piernaIzq, piernaDer};
	}

	static void limpiar() {
		ESTADOS.clear();
	}
}
