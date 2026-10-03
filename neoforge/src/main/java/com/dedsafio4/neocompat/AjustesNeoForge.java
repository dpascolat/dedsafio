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
