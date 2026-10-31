package com.dedsafio4.cambios;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * De noche con luna_roja 1, los zombis con pico te buscan aunque no te vean: pican lo que les tapa el paso, hacen una
 * torre si estás arriba y un puente si hay un pozo. No tocan cofres ni otros bloques con inventario, ni la Montaña.
 */
public class ZombiMinero extends Goal {
	private static final double ALCANCE = 24;
	/** Cuántos bloques puede poner cada zombi. */
	private static final int MAXIMO_PUESTOS = 32;
	/** Sin avanzar este tiempo, se pone a picar. */
	private static final int TRABADO = 20;
	private static final int ESPERA_PONER = 10;

	private final Zombie zombi;
	private BlockPos picando;
	private int progreso, tiempo, quieto, espera, puestos;
	private Vec3 antes = Vec3.ZERO;

	public ZombiMinero(Zombie zombi) {
		this.zombi = zombi;
	}

	private boolean lunaRoja() {
		return zombi.level() instanceof ServerLevel mundo && mundo.isNight() && Cambios.nivel(mundo.getServer(), Cambios.LUNA_ROJA) >= 1
				&& Zombis.tienePico(zombi);
	}

	@Override
	public boolean canUse() {
		if (!lunaRoja()) return false;
		if (zombi.getTarget() == null && zombi.getRandom().nextInt(20) == 0) {
			zombi.setTarget(zombi.level().getNearestPlayer(zombi, ALCANCE) instanceof LivingEntity jugador && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(jugador) ? jugador : null);
		}
		return zombi.getTarget() != null && zombi.getTarget().isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return lunaRoja() && zombi.getTarget() != null && zombi.getTarget().isAlive();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void stop() {
		soltar();
		quieto = 0;
	}

	private void soltar() {
		if (picando != null) zombi.level().destroyBlockProgress(zombi.getId(), picando, -1);
		picando = null;
		progreso = 0;
	}

	@Override
	public void tick() {
		if (espera > 0) espera--;
		quieto = zombi.position().distanceToSqr(antes) < 0.0004 ? quieto + 1 : 0;
		antes = zombi.position();
		LivingEntity objetivo = zombi.getTarget();
		double dx = objetivo.getX() - zombi.getX(), dz = objetivo.getZ() - zombi.getZ(), dy = objetivo.getY() - zombi.getY();
		double distancia = Math.sqrt(dx * dx + dz * dz);
		if (distancia < 2 && Math.abs(dy) < 2) {
			soltar();
			return;
		}
		Direction hacia = Direction.getNearest(dx, 0, dz);
		ServerLevel mundo = (ServerLevel) zombi.level();
		if (construir(mundo, hacia, distancia, dy)) return;
		if (quieto < TRABADO) {
			soltar();
			return;
		}
		BlockPos bloque = queRomper(mundo, hacia, distancia, dy);
		if (bloque == null) {
			soltar();
			return;
		}
		picar(mundo, bloque);
	}

	/** La torre (sube y se pone un bloque abajo) y el puente (un bloque adelante, sobre un pozo). */
	private boolean construir(ServerLevel mundo, Direction hacia, double distancia, double dy) {
		if (puestos >= MAXIMO_PUESTOS) return false;
		BlockPos abajo = zombi.blockPosition().below();
		if (dy >= 2 && distancia < 3) {
			BlockPos pies = zombi.blockPosition();
			if (!zombi.onGround() || espera > 0 || !mundo.getBlockState(pies.above(2)).isAir() || !mundo.getBlockState(pies).canBeReplaced()) return false;
			zombi.setPos(zombi.getX(), pies.getY() + 1, zombi.getZ());
			poner(mundo, pies);
			return true;
		}
		BlockPos adelante = abajo.relative(hacia);
		if (zombi.onGround() && dy >= -1 && mundo.getBlockState(adelante).canBeReplaced() && mundo.getBlockState(adelante.below()).canBeReplaced()) {
			if (espera == 0) poner(mundo, adelante);
			Vec3 centro = adelante.above().getBottomCenter();
			zombi.getMoveControl().setWantedPosition(centro.x, centro.y, centro.z, 1);
			return true;
		}
		return false;
	}

	private void poner(ServerLevel mundo, BlockPos pos) {
		if (new AABB(pos).intersects(zombi.getBoundingBox())) return;
		mundo.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
		mundo.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1f, 0.9f);
		zombi.swing(InteractionHand.MAIN_HAND);
		espera = ESPERA_PONER;
		puestos++;
	}

	private BlockPos queRomper(ServerLevel mundo, Direction hacia, double distancia, double dy) {
		BlockPos pies = zombi.blockPosition();
		BlockPos[] lugares = distancia < 3 && dy <= -2 ? new BlockPos[]{pies.below()}
				: distancia < 3 && dy >= 2 ? new BlockPos[]{pies.above(2)}
				: new BlockPos[]{pies.relative(hacia).above(), pies.relative(hacia)};
		for (BlockPos lugar : lugares) {
			if (sePuedeRomper(mundo, lugar)) return lugar;
		}
		return null;
	}

	private static boolean sePuedeRomper(ServerLevel mundo, BlockPos pos) {
		BlockState estado = mundo.getBlockState(pos);
		return !estado.isAir() && estado.getFluidState().isEmpty() && estado.getDestroySpeed(mundo, pos) >= 0 && !estado.hasBlockEntity()
				&& !GasToxico.enMontana(mundo, pos.getX(), pos.getZ());
	}

	private void picar(ServerLevel mundo, BlockPos bloque) {
		if (!bloque.equals(picando)) {
			soltar();
			picando = bloque;
			tiempo = Mth.clamp((int) (20 + mundo.getBlockState(bloque).getDestroySpeed(mundo, bloque) * 10), 20, 200);
		}
		zombi.getLookControl().setLookAt(bloque.getCenter());
		if (++progreso % 10 == 0) zombi.swing(InteractionHand.MAIN_HAND);
		mundo.destroyBlockProgress(zombi.getId(), bloque, progreso * 10 / tiempo);
		if (progreso < tiempo) return;
		mundo.destroyBlock(bloque, true, zombi);
		soltar();
		quieto = TRABADO / 2;
	}
}
