package com.dedsafio4.neocompat.fabric.api.event.player;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PlayerBlockBreakEvents {
	private PlayerBlockBreakEvents() {}

	public interface Before {
		boolean beforeBlockBreak(Level mundo, Player jugador, BlockPos pos, BlockState estado, BlockEntity bloqueEntidad);
	}

	public interface After {
		void afterBlockBreak(Level mundo, Player jugador, BlockPos pos, BlockState estado, BlockEntity bloqueEntidad);
	}

	public static final Event<Before> BEFORE = new Event<>();
	public static final Event<After> AFTER = new Event<>();
}
