package com.dedsafio4.neocompat.fabric.api.event.player;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public interface UseEntityCallback {
	Event<UseEntityCallback> EVENT = new Event<>();

	InteractionResult interact(Player jugador, Level mundo, InteractionHand mano, Entity entidad, EntityHitResult golpe);
}
