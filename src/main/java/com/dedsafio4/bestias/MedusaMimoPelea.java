package com.dedsafio4.bestias;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Cómo pelea la Medusa Mimo cuando es un jugador: corre, salta para pegar críticos, esquiva de costado, retrocede
 * después de cada golpe y se tapa con el escudo si la van a golpear. De lejos usa el arco y las perlas de ender. Con
 * poca vida escapa y se cura con lo mejor que tenga en la mochila.
 */
class MedusaMimoPelea extends Goal {
	private static final float POCA_VIDA = 0.4f;
	private static final double ALCANCE = 3.2, CERCA = 3.5, ARCO = 10, PERLA = 14;
	private static final int ESPERA_CURA = 100, ESPERA_PERLA = 100, TENSAR = 20, RETROCEDE = 6, HUYE_MAX = 40;

	private final MedusaMimoEntity medusa;
	private int golpe, escudo, cura, perla, atras, huye, cambioLado;
	private float lado = 1;
	private boolean comiendo;

	MedusaMimoPelea(MedusaMimoEntity medusa) {
		this.medusa = medusa;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return medusa.esCopia() && medusa.tieneObjetivo();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void stop() {
		medusa.stopUsingItem();
		medusa.getNavigation().stop();
		medusa.setSprinting(false);
		medusa.equiparArma();
		comiendo = false;
	}

	@Override
	public void tick() {
		LivingEntity objetivo = medusa.getTarget();
		if (objetivo == null) return;
		golpe--;
		cura--;
		perla--;
		double distancia = medusa.distanceTo(objetivo);
		boolean ve = medusa.getSensing().hasLineOfSight(objetivo);
		medusa.getLookControl().setLookAt(objetivo, 30, 30);
		if (comiendo) {
			if (medusa.isUsingItem()) {
				huir(objetivo);
				return;
			}
			comiendo = false;
			medusa.heal(6);
			medusa.equiparArma();
		}
		if (medusa.getHealth() < medusa.getMaxHealth() * POCA_VIDA && cura <= 0) {
			huye++;
			if (distancia < 6 && huye < HUYE_MAX) {
				huir(objetivo);
				return;
			}
			huye = 0;
			cura = ESPERA_CURA;
			if (empezarACurarse()) return;
		}
		if (escudo > 0) {
			if (--escudo == 0) medusa.stopUsingItem();
			medusa.getNavigation().stop();
			return;
		}
		if (ve && distancia > PERLA && perla <= 0) tirarPerla(objetivo);
		ItemStack arco = medusa.buscar(Items.BOW);
		if (!arco.isEmpty() && ve && distancia > ARCO) {
			usarArco(objetivo, arco);
			return;
		}
		if (medusa.isUsingItem() && medusa.getUseItem().getUseAnimation() == UseAnim.BOW) {
			medusa.stopUsingItem();
			medusa.equiparArma();
		}
		if (medusa.getOffhandItem().is(Items.SHIELD) && distancia < 4.5 && (objetivo.swinging || objetivo.getUseItem().getUseAnimation() == UseAnim.BOW)
				&& medusa.getRandom().nextBoolean() && golpe > 2) {
			medusa.startUsingItem(InteractionHand.OFF_HAND);
			escudo = 15;
			return;
		}
		pelear(objetivo, distancia, ve);
	}

	private void pelear(LivingEntity objetivo, double distancia, boolean ve) {
		medusa.setSprinting(true);
		boolean salta = medusa.onGround() && medusa.getRandom().nextInt(12) == 0;
		if (distancia > CERCA) {
			medusa.getNavigation().moveTo(objetivo, 1.25);
			if (salta && distancia < 5) medusa.getJumpControl().jump();
		} else {
			medusa.getNavigation().stop();
			medusa.lookAt(objetivo, 30, 30);
			if (--cambioLado <= 0) {
				lado = medusa.getRandom().nextBoolean() ? 1 : -1;
				cambioLado = 15 + medusa.getRandom().nextInt(25);
			}
			medusa.getMoveControl().strafe(atras > 0 ? -0.8f : 0.5f, lado * 0.6f);
			if (atras > 0) atras--;
			if (salta) medusa.getJumpControl().jump();
		}
		if (distancia <= ALCANCE && ve && golpe <= 0) {
			medusa.swing(InteractionHand.MAIN_HAND);
			medusa.doHurtTarget(objetivo);
			golpe = Mth.clamp(Mth.ceil(20 / medusa.getAttributeValue(Attributes.ATTACK_SPEED)), 4, 30);
			atras = RETROCEDE;
		}
	}

	private void huir(LivingEntity objetivo) {
		medusa.setSprinting(true);
		if (!medusa.getNavigation().isDone()) return;
		Vec3 lejos = DefaultRandomPos.getPosAway(medusa, 16, 7, objetivo.position());
		if (lejos != null) medusa.getNavigation().moveTo(lejos.x, lejos.y, lejos.z, 1.3);
	}

	private boolean empezarACurarse() {
		ItemStack cosa = medusa.mejorCura();
		if (cosa.isEmpty()) return false;
		medusa.stopUsingItem();
		medusa.llevarEnMano(cosa);
		medusa.usarMano();
		comiendo = true;
		return true;
	}

	private void usarArco(LivingEntity objetivo, ItemStack arco) {
		medusa.getNavigation().stop();
		medusa.setSprinting(false);
		if (medusa.getMainHandItem() != arco) medusa.llevarEnMano(arco);
		if (!medusa.isUsingItem()) {
			medusa.usarMano();
			return;
		}
		if (medusa.getTicksUsingItem() < TENSAR) return;
		medusa.stopUsingItem();
		AbstractArrow flecha = ProjectileUtil.getMobArrow(medusa, new ItemStack(Items.ARROW), 1f, arco);
		double dx = objetivo.getX() - medusa.getX(), dz = objetivo.getZ() - medusa.getZ(), dy = objetivo.getY(0.4) - flecha.getY();
		flecha.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.15, dz, 2.2f, 1f);
		medusa.level().addFreshEntity(flecha);
		medusa.playSound(SoundEvents.ARROW_SHOOT, 1f, 1f);
	}

	private void tirarPerla(LivingEntity objetivo) {
		ItemStack pila = medusa.buscar(Items.ENDER_PEARL);
		if (pila.isEmpty()) return;
		perla = ESPERA_PERLA;
		ThrownEnderpearl tiro = new ThrownEnderpearl(medusa.level(), medusa);
		tiro.setItem(pila.copyWithCount(1));
		double dx = objetivo.getX() - medusa.getX(), dz = objetivo.getZ() - medusa.getZ(), dy = objetivo.getY(0.5) - tiro.getY();
		tiro.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.15, dz, 1.5f, 1f);
		medusa.level().addFreshEntity(tiro);
		medusa.swing(InteractionHand.MAIN_HAND);
		medusa.playSound(SoundEvents.ENDER_PEARL_THROW, 1f, 0.6f);
		pila.shrink(1);
	}
}
