package com.dedsafio4.client;

import com.dedsafio4.brazalete.TrajeEspacial;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * La animación de volar con el Traje Espacial (en el aire, con el Modo de Vuelo del Brazalete):
 * - Flotando quieto: derecho, los brazos un poco abiertos y las piernas meciéndose despacio, como un astronauta.
 * - Avanzando: el cuerpo se inclina hacia adelante según la velocidad (hasta casi acostado), los brazos van hacia
 *   atrás pegados al cuerpo, las piernas juntas con un pataleo suave y la cabeza sigue mirando para adelante.
 * PlayerRendererVueloMixin inclina el cuerpo y PlayerModelVueloMixin mueve brazos, piernas y cabeza.
 */
public final class VueloTraje {
	private VueloTraje() {}

	/** Cuánto se inclina a toda velocidad (grados). */
	public static final float INCLINACION_MAXIMA = 75f;
	/** Con esta velocidad (bloques por tick) ya va del todo inclinado. */
	private static final float VELOCIDAD_LLENA = 0.45f;

	/** Lo que lleva inclinado cada jugador (de 0 a 1), para que cambie de a poco. */
	private static final Map<Player, Float> AVANCE = new WeakHashMap<>();

	/** ¿Está volando con el traje? (los otros jugadores no mandan si vuelan: con el traje y sin tocar el piso alcanza) */
	public static boolean volando(Player jugador) {
		return TrajeEspacial.puesto(jugador) && !jugador.onGround() && !jugador.isPassenger() && !jugador.isSpectator();
	}

	/** De 0 (quieto) a 1 (a toda velocidad), suavizado. Se llama una vez por cuadro. */
	public static float avance(Player jugador) {
		double dx = jugador.getX() - jugador.xo, dz = jugador.getZ() - jugador.zo;
		float objetivo = Mth.clamp((float) Math.sqrt(dx * dx + dz * dz) / VELOCIDAD_LLENA, 0f, 1f);
		float actual = AVANCE.getOrDefault(jugador, 0f);
		actual += (objetivo - actual) * 0.08f;
		AVANCE.put(jugador, actual);
		return actual;
	}

	public static float avanceGuardado(Player jugador) {
		return AVANCE.getOrDefault(jugador, 0f);
	}

	/** Brazos, piernas y cabeza (después de la animación normal). */
	public static void posar(PlayerModel<?> m, Player jugador, float edad, float inclinacionCabeza) {
		float f = avanceGuardado(jugador);
		float mecer = Mth.sin(edad * 0.07f), mecer2 = Mth.sin(edad * 0.05f + 1.3f);
		// Brazos: flotando, abiertos y meciéndose; avanzando, hacia atrás y pegados.
		float brazoX = Mth.lerp(f, mecer * 0.15f - 0.1f, 0.55f);
		float brazoZ = Mth.lerp(f, 0.4f + mecer2 * 0.08f, 0.12f);
		m.rightArm.xRot = brazoX;
		m.leftArm.xRot = Mth.lerp(f, -mecer * 0.15f - 0.1f, 0.55f);
		m.rightArm.yRot = 0f;
		m.leftArm.yRot = 0f;
		m.rightArm.zRot = brazoZ;
		m.leftArm.zRot = -brazoZ;
		// Piernas: flotando, un poco abiertas y colgando; avanzando, juntas y pataleando suave.
		float pataleo = Mth.sin(edad * 0.5f) * 0.18f * f;
		m.rightLeg.xRot = Mth.lerp(f, mecer2 * 0.12f + 0.05f, 0.1f) + pataleo;
		m.leftLeg.xRot = Mth.lerp(f, -mecer2 * 0.12f + 0.15f, 0.1f) - pataleo;
		m.rightLeg.yRot = 0f;
		m.leftLeg.yRot = 0f;
		m.rightLeg.zRot = Mth.lerp(f, 0.08f, 0.02f);
		m.leftLeg.zRot = -Mth.lerp(f, 0.08f, 0.02f);
		// La cabeza sigue mirando para adelante aunque el cuerpo esté inclinado.
		m.head.xRot = inclinacionCabeza * Mth.DEG_TO_RAD - f * INCLINACION_MAXIMA * Mth.DEG_TO_RAD;
		m.hat.copyFrom(m.head);
		m.leftSleeve.copyFrom(m.leftArm);
		m.rightSleeve.copyFrom(m.rightArm);
		m.leftPants.copyFrom(m.leftLeg);
		m.rightPants.copyFrom(m.rightLeg);
	}
}
