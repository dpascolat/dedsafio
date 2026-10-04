package com.dedsafio4.client.momentito;

import com.dedsafio4.momentito.MomentitoEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * La cámara de cine de /momentito 1: a los que están cerca de la escena (300 bloques), durante los 30 segundos
 * la cámara va pasando por las tomas del diseño (la nave llegando, el héroe, la bomba cayendo, el campo de
 * fuerza visto de lejos, la explosión, el hacker enojado, la nave yéndose...). Mientras tanto se esconde la
 * interfaz, se ve más lejos (si no, el campo de fuerza gigante no entra en la vista) y todo el cielo se pone
 * rojo como con /cielo rojo, pero sin Reviil: lo que aparece es la nave del hacker.
 */
public final class MomentitoCamara {
	private MomentitoCamara() {}

	private static final double CERCA = 300;
	/** La escena cercana (para el cielo) y, si ya empezó la cinemática, la que maneja la cámara. */
	private static MomentitoEntity escena, activo;
	private static boolean interfazAntes, escondiendo, cieloPuesto;

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			escena = buscar(mc);
			activo = escena != null && escena.enCinematica(0) ? escena : null;
			// El cielo rojo con la grieta (sin Reviil) durante toda la escena; al terminar, destello y cielo normal.
			if (escena != null && !cieloPuesto) {
				com.dedsafio4.client.CieloCliente.momentito(true);
				cieloPuesto = true;
			} else if (escena == null && cieloPuesto) {
				com.dedsafio4.client.CieloCliente.momentito(false);
				cieloPuesto = false;
			}
			// La cámara de cine (y sin interfaz) solo desde que la nave sale de la grieta.
			if (activo != null && !escondiendo) {
				interfazAntes = mc.options.hideGui;
				mc.options.hideGui = true;
				escondiendo = true;
			} else if (activo == null && escondiendo) {
				mc.options.hideGui = interfazAntes;
				escondiendo = false;
			}
		});
	}

	private static MomentitoEntity buscar(Minecraft mc) {
		if (mc.level == null || mc.player == null) return null;
		MomentitoEntity mejor = null;
		double distancia = CERCA * CERCA;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof MomentitoEntity m) || m.isRemoved()) continue;
			float t = m.tiempo(0);
			if (t < 0 || t >= MomentitoEntity.DURACION / 20f) continue;
			double d = m.distanceToSqr(mc.player);
			if (d < distancia) {
				distancia = d;
				mejor = m;
			}
		}
		return mejor;
	}

	/** ¿Hay una escena manejando la cámara? */
	public static boolean activa() {
		return activo != null;
	}

	/** Pasa un punto de la escena al mundo (la escena está girada para que el héroe mire al que la empezó). */
	private static Vec3 alMundo(MomentitoEntity m, float x, float y, float z) {
		Vector3f v = new Vector3f(x, y, z).rotateY((180 - m.getYRot()) * Mth.DEG_TO_RAD);
		return m.position().add(v.x, v.y, v.z);
	}

	/** Dónde va la cámara en este cuadro: {x, y, z, giro, inclinación}, o null si no hay escena. */
	public static double[] camara(float parcial) {
		MomentitoEntity m = activo;
		if (m == null) return null;
		float t = Mth.clamp(m.tiempoEscena(parcial), 0, 30);
		float[] toma = MomentitoRenderer.toma(t, m.campo());
		Vec3 desde = alMundo(m, toma[0], toma[1], toma[2]), hacia = alMundo(m, toma[3], toma[4], toma[5]);
		Vec3 d = hacia.subtract(desde);
		double plano = Math.sqrt(d.x * d.x + d.z * d.z);
		float giro = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
		float inclinacion = (float) (-Mth.atan2(d.y, plano) * Mth.RAD_TO_DEG);
		return new double[]{desde.x, desde.y, desde.z, giro, inclinacion};
	}
}
