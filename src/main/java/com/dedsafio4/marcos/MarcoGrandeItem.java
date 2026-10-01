package com.dedsafio4.marcos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Se pone como un marco normal (en paredes, piso o techo), si hay lugar para los 4×4 bloques. */
public class MarcoGrandeItem extends Item {
	public MarcoGrandeItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public InteractionResult useOn(UseOnContext contexto) {
		Direction cara = contexto.getClickedFace();
		BlockPos pos = contexto.getClickedPos().relative(cara);
		Player jugador = contexto.getPlayer();
		ItemStack item = contexto.getItemInHand();
		Level mundo = contexto.getLevel();
		if (jugador != null && !jugador.mayUseItemAt(pos, cara, item)) return InteractionResult.FAIL;
		MarcoGrandeEntity marco = new MarcoGrandeEntity(mundo, pos, cara);
		if (!marco.survives()) return InteractionResult.CONSUME;
		if (!mundo.isClientSide) {
			marco.playPlacementSound();
			mundo.gameEvent(jugador, GameEvent.ENTITY_PLACE, marco.position());
			mundo.addFreshEntity(marco);
		}
		item.consume(1, jugador);
		return InteractionResult.sidedSuccess(mundo.isClientSide);
	}
}
