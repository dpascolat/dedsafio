package com.dedsafio4.neocompat.fabric.api.event.player;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public interface UseBlockCallback {
	Event<UseBlockCallback> EVENT = new Event<>();

	InteractionResult interact(Player jugador, Level mundo, InteractionHand mano, BlockHitResult golpe);
}
