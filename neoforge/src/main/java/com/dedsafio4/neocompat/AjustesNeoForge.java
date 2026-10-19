package com.dedsafio4.neocompat;

import com.dedsafio4.cambios.Cambios;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

/** Partes del mod que en NeoForge se hacen con sus eventos porque NeoForge cambió el código de Minecraft ahí. */
public final class AjustesNeoForge {
	private AjustesNeoForge() {}

	static void registrar(IEventBus juego) {
		// Cambio "respirar agua 1" (en Fabric: LivingEntityMixin#sinBolsasDeAire): con la cabeza en una puerta,
		// trampilla, antorcha, etc. rodeada de agua no se respira (NeoForge decide la respiración en este evento).
		juego.addListener(EventPriority.NORMAL, false, LivingBreatheEvent.class, e -> {
			if (!e.canBreathe() || !(e.getEntity() instanceof Player jugador) || !(jugador.level() instanceof ServerLevel level)) return;
			if (Cambios.nivel(level.getServer(), Cambios.RESPIRAR_AGUA) < 1) return;
			if (cabezaEnBolsaDeAire(level, BlockPos.containing(jugador.getX(), jugador.getEyeY(), jugador.getZ()))) e.setCanBreathe(false);
		});
		// Los bloques del mod que son para usar (Entrega de Misiones, Baneados, Puesto de Intercambio, Bóveda y Cajero
		// de deditas, Mensajero, Vitrina) y el Aldeano se pueden usar aunque otro mod o script (KubeJS, protecciones
		// de zona) cancele el click: esto va último y lo vuelve a habilitar. Romper o poner bloques no se toca.
		juego.addListener(EventPriority.LOWEST, true, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock.class, e -> {
			if (!bloqueLibre(e.getLevel().getBlockState(e.getPos()).getBlock())) return;
			e.setCanceled(false);
			e.setUseBlock(net.neoforged.neoforge.common.util.TriState.DEFAULT);
			e.setUseItem(net.neoforged.neoforge.common.util.TriState.DEFAULT);
		});
		juego.addListener(EventPriority.LOWEST, true, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract.class, e -> {
			if (e.getTarget() instanceof com.dedsafio4.robots.AldeanoRobotEntity) e.setCanceled(false);
		});
		juego.addListener(EventPriority.LOWEST, true, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific.class, e -> {
			if (e.getTarget() instanceof com.dedsafio4.robots.AldeanoRobotEntity) e.setCanceled(false);
		});
	}

	/** Los bloques del mod que cualquiera tiene que poder usar, estén donde estén. */
	public static boolean bloqueLibre(net.minecraft.world.level.block.Block bloque) {
		return bloque instanceof com.dedsafio4.bloques.EntregaMisionBlock
				|| bloque == com.dedsafio4.baneos.Baneos.BLOQUE
				|| bloque == com.dedsafio4.intercambio.ModIntercambio.PUESTO
				|| bloque instanceof com.dedsafio4.bloques.MensajeroBlock
				|| bloque instanceof com.dedsafio4.subastas.VitrinaBlock
				|| bloque instanceof com.dedsafio4.subastas.PujaBlock
				|| bloque instanceof com.dedsafio4.banco.CajeroBlock
				|| bloque instanceof com.dedsafio4.boveda.BovedaBlock;
	}

	/** La cabeza en un bloque sin agua que no es aire, con agua en al menos 2 de los lados o arriba. */
	private static boolean cabezaEnBolsaDeAire(ServerLevel level, BlockPos cabeza) {
		BlockState estado = level.getBlockState(cabeza);
		if (estado.isAir() || !estado.getFluidState().isEmpty()) return false;
		int ladosConAgua = 0;
		for (Direction dir : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			if (level.getFluidState(cabeza.relative(dir)).is(FluidTags.WATER)) ladosConAgua++;
		}
		return ladosConAgua >= 2;
	}
}
