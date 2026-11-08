package com.dedsafio4.bestias;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cerebro Rojo: como el Cerebro Amarillo (salta a la cabeza, te deja ciego y te drena hasta matarte, sin poder curarte),
 * pero además TE CONTROLA: mueve tu cuerpo hasta el jugador más cercano (si no hay, cualquier bicho) y le pega con lo
 * que tengas en la mano. No se aplasta contra los techos: sólo se saca matándolo, y el que lo tiene encima no le puede
 * pegar (otro jugador lo tiene que matar, o una flecha que le cae desde arriba).
 */
public class CerebroRojoEntity extends CerebroAmarilloEntity {
	/** Hasta dónde busca a quién hacerte atacar (jugadores; bichos, más cerca). */
	private static final double BUSCA_JUGADOR = 24, BUSCA_BICHO = 12;
	/** A qué distancia le pegas. */
	private static final double ALCANCE = 2.8;
	/** Qué tan rápido te hace caminar (bloques por tick). */
	private static final double PASO = 0.3;
	private LivingEntity victima;

	public CerebroRojoEntity(EntityType<? extends CerebroRojoEntity> tipo, Level mundo) {
		super(tipo, mundo);
	}

	@Override
	protected String cambioDoble() {
		return com.dedsafio4.cambios.Cambios.CEREBRO_ROJO;
	}

	@Override
	protected boolean seAplasta() {
		return false;
	}

	@Override
	protected void montado(Player p) {
		if (!(p instanceof ServerPlayer jugador) || !jugador.isAlive()) return;
		if (tickCount % 10 == 0 || victima == null || !victima.isAlive() || victima.level() != jugador.level()
				|| victima.distanceToSqr(jugador) > BUSCA_JUGADOR * BUSCA_JUGADOR) {
			victima = buscar(jugador);
		}
		if (victima == null) return;
		// Mira a la víctima (el cliente gira la cámara).
		jugador.lookAt(EntityAnchorArgument.Anchor.EYES, victima.getEyePosition());
		Vec3 hacia = victima.position().subtract(jugador.position());
		double distancia = Math.sqrt(hacia.x * hacia.x + hacia.z * hacia.z);
		if (distancia > ALCANCE - 0.6) {
			// Camina hacia ella (y salta si se choca con algo).
			Vec3 antes = jugador.getDeltaMovement();
			double salto = jugador.horizontalCollision && jugador.onGround() ? 0.42 : antes.y;
			jugador.setDeltaMovement(hacia.x / distancia * PASO, salto, hacia.z / distancia * PASO);
			jugador.hurtMarked = true;   // así le llega al cliente
		}
		// Le pega con lo que tenga en la mano, cuando el golpe está cargado.
		if (jugador.distanceTo(victima) <= ALCANCE && jugador.getAttackStrengthScale(0.5f) >= 0.95f && jugador.hasLineOfSight(victima)) {
			jugador.attack(victima);
			jugador.swing(InteractionHand.MAIN_HAND, true);
		}
	}

	/** El jugador más cercano (que no esté en creativo); si no hay ninguno, el bicho más cercano. */
	private LivingEntity buscar(ServerPlayer jugador) {
		LivingEntity mejor = null;
		double cerca = BUSCA_JUGADOR * BUSCA_JUGADOR;
		for (ServerPlayer otro : jugador.serverLevel().players()) {
			if (otro == jugador || !otro.isAlive() || otro.isCreative() || otro.isSpectator()) continue;
			double d = otro.distanceToSqr(jugador);
			if (d < cerca) {
				cerca = d;
				mejor = otro;
			}
		}
		if (mejor != null) return mejor;
		cerca = BUSCA_BICHO * BUSCA_BICHO;
		for (LivingEntity bicho : jugador.level().getEntitiesOfClass(LivingEntity.class, jugador.getBoundingBox().inflate(BUSCA_BICHO),
				b -> b.isAlive() && !(b instanceof Player) && !(b instanceof ArmorStand) && !(b instanceof CerebroAmarilloEntity))) {
			double d = bicho.distanceToSqr(jugador);
			if (d < cerca) {
				cerca = d;
				mejor = bicho;
			}
		}
		return mejor;
	}
}
